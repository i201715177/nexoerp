package com.farmacia.sistema.domain.inventario;

import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoService;
import com.farmacia.sistema.domain.sucursal.Sucursal;
import com.farmacia.sistema.domain.sucursal.SucursalRepository;
import com.farmacia.sistema.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InventarioService {

    private final InventarioMovimientoRepository movimientoRepository;
    private final AlmacenRepository almacenRepository;
    private final StockAlmacenRepository stockAlmacenRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final ProductoService productoService;
    private final SucursalRepository sucursalRepository;
    private final TransferenciaRepository transferenciaRepository;

    public InventarioService(InventarioMovimientoRepository movimientoRepository,
                             AlmacenRepository almacenRepository,
                             StockAlmacenRepository stockAlmacenRepository,
                             LoteProductoRepository loteProductoRepository,
                             ProductoService productoService,
                             SucursalRepository sucursalRepository,
                             TransferenciaRepository transferenciaRepository) {
        this.movimientoRepository = movimientoRepository;
        this.almacenRepository = almacenRepository;
        this.stockAlmacenRepository = stockAlmacenRepository;
        this.loteProductoRepository = loteProductoRepository;
        this.productoService = productoService;
        this.sucursalRepository = sucursalRepository;
        this.transferenciaRepository = transferenciaRepository;
    }

    // --- Almacenes ---
    /** Asegura que cada sucursal del tenant tenga al menos un almacén (para transferencias). */
    public void asegurarAlmacenesParaSucursales() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) return;
        List<Sucursal> sucursales = sucursalRepository.findByTenantId(tenantId);
        for (Sucursal s : sucursales) {
            if (almacenRepository.findBySucursal_Id(s.getId()).isEmpty()) {
                Almacen a = new Almacen();
                a.setCodigo("ALM-" + s.getCodigo());
                a.setNombre("Almacén " + s.getNombre());
                a.setPrincipal(false);
                a.setSucursal(s);
                crearAlmacen(a);
            }
        }
    }

    public List<Almacen> listarAlmacenes() {
        asegurarAlmacenesParaSucursales();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return almacenRepository.findByTenantIdOrderByPrincipalDescNombreAsc(tenantId);
        }
        return almacenRepository.findByOrderByPrincipalDescNombreAsc();
    }

    public Almacen obtenerAlmacenPorId(Long id) {
        return almacenRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Almacén no encontrado"));
    }

    public Optional<Almacen> obtenerAlmacenPrincipal() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return almacenRepository.findFirstByTenantIdAndPrincipalTrue(tenantId);
        }
        return almacenRepository.findFirstByPrincipalTrue();
    }

    public Almacen crearAlmacen(Almacen a) {
        if (a.isPrincipal()) {
            almacenRepository.findFirstByPrincipalTrue().ifPresent(prev -> {
                prev.setPrincipal(false);
                almacenRepository.save(prev);
            });
        }
        return almacenRepository.save(a);
    }

    // --- Kardex ---
    /** El producto ya debe tener stockActual actualizado (restado). */
    public void registrarSalidaVenta(Producto producto, int cantidad, String referencia) {
        InventarioMovimiento m = new InventarioMovimiento();
        m.setProducto(producto);
        m.setTipo("SALIDA");
        m.setCantidad(-cantidad);
        m.setSaldoDespues(producto.getStockActual());
        m.setReferencia(referencia != null ? referencia : "Venta");
        movimientoRepository.save(m);
    }

    /** Si el producto ya tiene stock actualizado, se usa ese valor como saldoDespues. */
    public void registrarEntrada(Producto producto, int cantidad, String referencia) {
        InventarioMovimiento m = new InventarioMovimiento();
        m.setProducto(producto);
        m.setTipo("ENTRADA");
        m.setCantidad(cantidad);
        m.setSaldoDespues(producto.getStockActual());
        m.setReferencia(referencia != null ? referencia : "Entrada");
        movimientoRepository.save(m);
    }

    /** El producto ya debe tener stockActual actualizado. */
    public void registrarAjuste(Producto producto, int cantidad, String referencia) {
        String tipo = cantidad > 0 ? "AJUSTE_ENTRADA" : "AJUSTE_SALIDA";
        InventarioMovimiento m = new InventarioMovimiento();
        m.setProducto(producto);
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        m.setSaldoDespues(producto.getStockActual());
        m.setReferencia(referencia != null ? referencia : "Ajuste");
        movimientoRepository.save(m);
    }

    public List<InventarioMovimiento> kardexPorProducto(Long productoId) {
        return movimientoRepository.findByProductoIdOrderByFechaDesc(productoId);
    }

    // --- Stock por almacén ---
    public int obtenerStockEnAlmacen(Long almacenId, Long productoId) {
        return stockAlmacenRepository.findByAlmacenIdAndProductoId(almacenId, productoId)
                .map(StockAlmacen::getCantidad)
                .orElse(0);
    }

    /** Stock consolidado por producto en una sucursal (sumando todos sus almacenes). */
    public java.util.Map<com.farmacia.sistema.domain.producto.Producto, Integer> stockPorSucursal(Long sucursalId) {
        java.util.List<StockAlmacen> stocks = stockAlmacenRepository.findByAlmacen_Sucursal_Id(sucursalId);
        java.util.Map<com.farmacia.sistema.domain.producto.Producto, Integer> mapa = new java.util.HashMap<>();
        for (StockAlmacen sa : stocks) {
            var prod = sa.getProducto();
            if (prod == null) continue;
            int cantidad = sa.getCantidad() != null ? sa.getCantidad() : 0;
            mapa.merge(prod, cantidad, Integer::sum);
        }
        return mapa;
    }

    public void asegurarStockAlmacen(Almacen almacen, Producto producto, int cantidadInicial) {
        StockAlmacen sa = stockAlmacenRepository.findByAlmacenIdAndProductoId(almacen.getId(), producto.getId())
                .orElseGet(() -> {
                    StockAlmacen s = new StockAlmacen();
                    s.setAlmacen(almacen);
                    s.setProducto(producto);
                    int base = producto.getStockActual() != null ? producto.getStockActual() : 0;
                    s.setCantidad(base);
                    return stockAlmacenRepository.save(s);
                });
        sa.setCantidad(cantidadInicial);
        stockAlmacenRepository.save(sa);
    }

    /** Sincroniza stock del producto en el almacén principal (alta/edición de producto). */
    public void asegurarStockInicialEnPrincipal(Producto producto) {
        obtenerAlmacenPrincipal().ifPresent(principal -> {
            int cantidad = producto.getStockActual() != null ? producto.getStockActual() : 0;
            asegurarStockAlmacen(principal, producto, cantidad);
        });
    }

    /** Actualiza stock en almacén principal (ventas/NC). */
    public void actualizarStockPrincipal(Long productoId, int delta) {
        obtenerAlmacenPrincipal().ifPresent(principal -> actualizarStockAlmacen(principal.getId(), productoId, delta));
    }

    /** Stock total de un producto en una sucursal (suma de todos sus almacenes). */
    public int obtenerStockEnSucursal(Long sucursalId, Long productoId) {
        if (sucursalId == null || productoId == null) return 0;
        java.util.List<Almacen> almacenes = almacenRepository.findBySucursal_Id(sucursalId);
        int total = 0;
        for (Almacen a : almacenes) {
            total += obtenerStockEnAlmacen(a.getId(), productoId);
        }
        return total;
    }

    /**
     * Stock disponible para ventas generales (sin sucursal asociada).
     * Usa el almacén principal si existe; si no, recurre al stockActual del producto.
     */
    public int obtenerStockParaVentaGeneral(Long productoId) {
        if (productoId == null) return 0;
        Producto p = productoService.obtenerPorId(productoId);
        return obtenerAlmacenPrincipal()
                .map(alm -> obtenerStockEnAlmacen(alm.getId(), productoId))
                .orElse(p.getStockActual() != null ? p.getStockActual() : 0);
    }

    /** Actualiza stock en la sucursal (primer almacén de la sucursal). Para ventas por sucursal. */
    public void actualizarStockEnSucursal(Long sucursalId, Long productoId, int delta) {
        if (sucursalId == null) return;
        java.util.List<Almacen> almacenes = almacenRepository.findBySucursal_Id(sucursalId);
        if (almacenes.isEmpty()) return;
        actualizarStockAlmacen(almacenes.get(0).getId(), productoId, delta);
    }

    /** Id del primer almacén de la sucursal (para descuentos de requerimientos). Vacío si la sucursal no tiene almacenes. */
    public Optional<Long> obtenerIdPrimerAlmacenDeSucursal(Long sucursalId) {
        if (sucursalId == null) return Optional.empty();
        return almacenRepository.findBySucursal_Id(sucursalId).stream()
                .findFirst()
                .map(Almacen::getId);
    }

    public void actualizarStockAlmacen(Long almacenId, Long productoId, int delta) {
        Almacen almacen = obtenerAlmacenPorId(almacenId);
        Producto producto = productoService.obtenerPorId(productoId);
        StockAlmacen sa = stockAlmacenRepository.findByAlmacenIdAndProductoId(almacenId, productoId)
                .orElseGet(() -> {
                    StockAlmacen s = new StockAlmacen();
                    s.setAlmacen(almacen);
                    s.setProducto(producto);
                    s.setCantidad(0);
                    return stockAlmacenRepository.save(s);
                });
        int nuevaCantidad = sa.getCantidad() + delta;
        if (nuevaCantidad < 0)
            throw new IllegalArgumentException("Stock insuficiente en almacén para producto " + producto.getNombre() + " (quedaría " + nuevaCantidad + ").");
        sa.setCantidad(nuevaCantidad);
        stockAlmacenRepository.save(sa);
    }

    /**
     * Fase 1: Envío. Resta en origen y registra la transferencia como ENVIADO.
     * El destino debe confirmar recepción para que se sume en su almacén.
     */
    public Transferencia transferir(Long almacenOrigenId, Long almacenDestinoId, Long productoId, int cantidad, String referencia, String usuarioEnvio) {
        if (almacenOrigenId.equals(almacenDestinoId)) throw new IllegalArgumentException("Origen y destino deben ser distintos");
        Producto producto = productoService.obtenerPorId(productoId);
        int stockOrigen = obtenerStockEnAlmacen(almacenOrigenId, productoId);
        if (stockOrigen < cantidad) throw new IllegalArgumentException("Stock insuficiente en almacén origen");
        Almacen almacenOrigen = obtenerAlmacenPorId(almacenOrigenId);
        Almacen almacenDestino = obtenerAlmacenPorId(almacenDestinoId);
        actualizarStockAlmacen(almacenOrigenId, productoId, -cantidad);
        InventarioMovimiento salida = new InventarioMovimiento();
        salida.setProducto(producto);
        salida.setAlmacen(almacenOrigen);
        salida.setTipo("TRANSFERENCIA_SALIDA");
        salida.setCantidad(-cantidad);
        salida.setSaldoDespues(stockOrigen - cantidad);
        salida.setReferencia(referencia != null ? referencia : "Traslado a " + almacenDestino.getNombre());
        movimientoRepository.save(salida);
        Transferencia t = new Transferencia();
        t.setAlmacenOrigen(almacenOrigen);
        t.setAlmacenDestino(almacenDestino);
        t.setProducto(producto);
        t.setCantidad(cantidad);
        t.setReferencia(referencia);
        t.setEstado("ENVIADO");
        t.setUsuarioEnvio(usuarioEnvio != null ? usuarioEnvio : "Sistema");
        t = transferenciaRepository.save(t);
        return t;
    }

    /** Fase 2: Confirmar recepción. Suma en destino y marca la transferencia como RECIBIDO. */
    public void confirmarRecepcion(Long transferenciaId, String usuarioRecepcion) {
        Transferencia t = transferenciaRepository.findById(transferenciaId)
                .orElseThrow(() -> new EntityNotFoundException("Transferencia no encontrada"));
        if (!"ENVIADO".equals(t.getEstado())) throw new IllegalArgumentException("Esta transferencia ya fue recibida o no está en estado ENVIADO.");
        Long almacenDestinoId = t.getAlmacenDestino().getId();
        Long productoId = t.getProducto().getId();
        int cantidad = t.getCantidad();
        actualizarStockAlmacen(almacenDestinoId, productoId, cantidad);
        int stockDestino = obtenerStockEnAlmacen(almacenDestinoId, productoId);
        InventarioMovimiento entrada = new InventarioMovimiento();
        entrada.setProducto(t.getProducto());
        entrada.setAlmacen(t.getAlmacenDestino());
        entrada.setTipo("TRANSFERENCIA_ENTRADA");
        entrada.setCantidad(cantidad);
        entrada.setSaldoDespues(stockDestino);
        entrada.setReferencia(t.getReferencia() != null ? t.getReferencia() : "Recepción desde " + t.getAlmacenOrigen().getNombre());
        movimientoRepository.save(entrada);
        t.setEstado("RECIBIDO");
        t.setFechaRecepcion(LocalDateTime.now());
        t.setUsuarioRecepcion(usuarioRecepcion != null ? usuarioRecepcion : "Sistema");
        transferenciaRepository.save(t);
    }

    public List<Transferencia> listarPendientesDeRecepcion(Long almacenDestinoId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) return List.of();
        List<Transferencia> lista = almacenDestinoId != null
                ? transferenciaRepository.findByTenantIdAndAlmacenDestinoIdAndEstadoOrderByFechaEnvioDesc(tenantId, almacenDestinoId, "ENVIADO")
                : transferenciaRepository.findByTenantIdAndEstadoOrderByFechaEnvioDesc(tenantId, "ENVIADO");
        for (Transferencia t : lista) {
            if (t.getAlmacenOrigen() != null) { t.getAlmacenOrigen().getNombre(); if (t.getAlmacenOrigen().getSucursal() != null) t.getAlmacenOrigen().getSucursal().getNombre(); }
            if (t.getAlmacenDestino() != null) { t.getAlmacenDestino().getNombre(); if (t.getAlmacenDestino().getSucursal() != null) t.getAlmacenDestino().getSucursal().getNombre(); }
            if (t.getProducto() != null) t.getProducto().getNombre();
        }
        return lista;
    }

    public Optional<Transferencia> obtenerTransferenciaPorId(Long id) {
        return transferenciaRepository.findById(id);
    }

    /** Historial de transferencias (todas, con estado) para la grilla. */
    public List<Transferencia> listarHistorialTransferencias() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) return List.of();
        List<Transferencia> todas = transferenciaRepository.findByTenantIdOrderByFechaEnvioDesc(tenantId);
        for (Transferencia t : todas) {
            if (t.getAlmacenOrigen() != null) { t.getAlmacenOrigen().getNombre(); if (t.getAlmacenOrigen().getSucursal() != null) t.getAlmacenOrigen().getSucursal().getNombre(); }
            if (t.getAlmacenDestino() != null) { t.getAlmacenDestino().getNombre(); if (t.getAlmacenDestino().getSucursal() != null) t.getAlmacenDestino().getSucursal().getNombre(); }
            if (t.getProducto() != null) t.getProducto().getNombre();
        }
        return todas.size() > 200 ? todas.subList(0, 200) : todas;
    }

    /** Elimina una transferencia. Si ENVIADO: devuelve stock a origen. Si RECIBIDO: quita de destino y devuelve a origen. */
    public void eliminarTransferencia(Long id) {
        Transferencia t = transferenciaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transferencia no encontrada"));
        Long origenId = t.getAlmacenOrigen().getId();
        Long destinoId = t.getAlmacenDestino().getId();
        Long productoId = t.getProducto().getId();
        int cantidad = t.getCantidad();
        if ("ENVIADO".equals(t.getEstado())) {
            actualizarStockAlmacen(origenId, productoId, cantidad);
        } else if ("RECIBIDO".equals(t.getEstado())) {
            actualizarStockAlmacen(destinoId, productoId, -cantidad);
            actualizarStockAlmacen(origenId, productoId, cantidad);
        }
        transferenciaRepository.delete(t);
    }

    /** Actualiza cantidad y referencia de una transferencia ENVIADO. Ajusta stock en origen si cambia la cantidad. */
    public void actualizarTransferencia(Long id, int nuevaCantidad, String nuevaReferencia) {
        Transferencia t = transferenciaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transferencia no encontrada"));
        if (!"ENVIADO".equals(t.getEstado())) throw new IllegalArgumentException("Solo se puede actualizar una transferencia pendiente (ENVIADO).");
        int antigua = t.getCantidad();
        if (nuevaCantidad != antigua) {
            if (nuevaCantidad < 0) throw new IllegalArgumentException("La cantidad debe ser positiva.");
            int stockOrigen = obtenerStockEnAlmacen(t.getAlmacenOrigen().getId(), t.getProducto().getId());
            if (nuevaCantidad > antigua && (nuevaCantidad - antigua) > stockOrigen)
                throw new IllegalArgumentException("No hay stock suficiente en origen para aumentar la cantidad.");
            actualizarStockAlmacen(t.getAlmacenOrigen().getId(), t.getProducto().getId(), antigua - nuevaCantidad);
        }
        t.setCantidad(nuevaCantidad);
        t.setReferencia(nuevaReferencia != null ? nuevaReferencia : t.getReferencia());
        transferenciaRepository.save(t);
    }

    /** Pendientes de recepción para una sucursal (busca almacenes de esa sucursal). */
    public List<Transferencia> listarPendientesPorSucursal(Long sucursalId) {
        if (sucursalId == null) return listarPendientesDeRecepcion(null);
        List<Almacen> almacenesSucursal = almacenRepository.findBySucursal_Id(sucursalId);
        if (almacenesSucursal.isEmpty()) return List.of();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) return List.of();
        List<Transferencia> resultado = new java.util.ArrayList<>();
        for (Almacen a : almacenesSucursal) {
            resultado.addAll(transferenciaRepository.findByTenantIdAndAlmacenDestinoIdAndEstadoOrderByFechaEnvioDesc(tenantId, a.getId(), "ENVIADO"));
        }
        for (Transferencia t : resultado) {
            if (t.getAlmacenOrigen() != null) { t.getAlmacenOrigen().getNombre(); if (t.getAlmacenOrigen().getSucursal() != null) t.getAlmacenOrigen().getSucursal().getNombre(); }
            if (t.getAlmacenDestino() != null) { t.getAlmacenDestino().getNombre(); if (t.getAlmacenDestino().getSucursal() != null) t.getAlmacenDestino().getSucursal().getNombre(); }
            if (t.getProducto() != null) t.getProducto().getNombre();
        }
        resultado.sort((a, b) -> b.getFechaEnvio().compareTo(a.getFechaEnvio()));
        return resultado;
    }

    // --- Ajuste de inventario ---
    public void ajustar(Long productoId, int cantidad, String motivo) {
        Producto producto = productoService.obtenerPorId(productoId);
        int nuevoStock = producto.getStockActual() + cantidad;
        if (nuevoStock < 0) throw new IllegalArgumentException("El stock no puede quedar negativo");
        productoService.actualizarStock(productoId, nuevoStock);
        producto.setStockActual(nuevoStock);
        registrarAjuste(producto, cantidad, motivo != null ? motivo : "Ajuste manual");
        Optional<Almacen> principal = obtenerAlmacenPrincipal();
        if (principal.isPresent()) {
            actualizarStockAlmacen(principal.get().getId(), productoId, cantidad);
        }
    }

    // --- Lotes ---
    public List<LoteProducto> lotesPorProducto(Long productoId) {
        return loteProductoRepository.findByProductoIdOrderByFechaVencimientoAsc(productoId);
    }

    public LoteProducto registrarLote(Long productoId, String numeroLote, LocalDate fechaVencimiento, int cantidad) {
        Producto producto = productoService.obtenerPorId(productoId);
        LoteProducto lote = new LoteProducto();
        lote.setProducto(producto);
        String numLote = (numeroLote != null && !numeroLote.isBlank()) ? numeroLote.trim() : generarNumeroLote();
        lote.setNumeroLote(numLote);
        lote.setFechaVencimiento(fechaVencimiento);
        lote.setCantidadActual(cantidad);
        lote = loteProductoRepository.save(lote);
        return lote;
    }

    /** Genera número de lote único: LOTE-YYYYMMDD-NNN (NNN = secuencia del día por tenant). */
    private String generarNumeroLote() {
        String prefix = "LOTE-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Long tenantId = TenantContext.getTenantId();
        int n = 1;
        if (tenantId != null) {
            n = loteProductoRepository.countByTenantIdAndNumeroLoteStartingWith(tenantId, prefix) + 1;
        }
        return prefix + "-" + String.format("%03d", n);
    }

    public List<LoteProducto> lotesPorVencer(int dias) {
        LocalDate desde = LocalDate.now();
        LocalDate hasta = desde.plusDays(dias);
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return loteProductoRepository.findByTenantIdAndFechaVencimientoBetweenAndCantidadActualGreaterThan(tenantId, desde, hasta, 0);
        }
        return loteProductoRepository.findByFechaVencimientoBetweenAndCantidadActualGreaterThan(desde, hasta, 0);
    }

    /** Lotes ya vencidos (fecha de vencimiento anterior a hoy) con stock &gt; 0. */
    public List<LoteProducto> lotesVencidos() {
        LocalDate hoy = LocalDate.now();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return loteProductoRepository.findByTenantIdAndFechaVencimientoBeforeAndCantidadActualGreaterThan(tenantId, hoy, 0);
        }
        return loteProductoRepository.findByFechaVencimientoBeforeAndCantidadActualGreaterThan(hoy, 0);
    }

    public LoteProducto obtenerLotePorId(Long id) {
        return loteProductoRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Lote no encontrado"));
    }

    /** Elimina el lote y resta su cantidad del stock del producto. */
    public void eliminarLote(Long loteId) {
        LoteProducto lote = obtenerLotePorId(loteId);
        Producto p = lote.getProducto();
        int restar = lote.getCantidadActual() != null ? lote.getCantidadActual() : 0;
        if (p != null && restar > 0) {
            int actual = p.getStockActual() != null ? p.getStockActual() : 0;
            productoService.actualizarStock(p.getId(), Math.max(0, actual - restar));
        }
        loteProductoRepository.delete(lote);
    }

    /** Actualiza fecha de vencimiento y cantidad del lote; ajusta stock del producto si cambia la cantidad. */
    public void actualizarLote(Long loteId, LocalDate fechaVencimiento, int cantidad) {
        LoteProducto lote = obtenerLotePorId(loteId);
        int cantidadAnterior = lote.getCantidadActual() != null ? lote.getCantidadActual() : 0;
        int diferencia = cantidad - cantidadAnterior;
        if (diferencia != 0) {
            Producto p = lote.getProducto();
            if (p != null) {
                int actual = p.getStockActual() != null ? p.getStockActual() : 0;
                productoService.actualizarStock(p.getId(), Math.max(0, actual + diferencia));
            }
        }
        lote.setFechaVencimiento(fechaVencimiento);
        lote.setCantidadActual(cantidad);
        loteProductoRepository.save(lote);
    }

    public long contarProductosBajoMinimo() {
        return productoService.listarTodos().stream()
                .filter(p -> p.getStockActual() != null && p.getStockMinimo() != null && p.getStockActual() < p.getStockMinimo())
                .count();
    }

     public long contarProductosSobreMaximo() {
         return productoService.listarTodos().stream()
                 .filter(p -> p.getStockActual() != null
                         && p.getStockMaximo() != null
                         && p.getStockActual() > p.getStockMaximo())
                 .count();
     }
}
