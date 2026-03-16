package com.farmacia.sistema.domain.digemid;

import com.farmacia.sistema.domain.inventario.Almacen;
import com.farmacia.sistema.domain.inventario.AlmacenRepository;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoRepository;
import com.farmacia.sistema.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DigemidService {

    private final ProductoRepository productoRepository;
    private final StockControladoRepository stockControladoRepository;
    private final MovimientoControladoRepository movimientoControladoRepository;
    private final RegistroRecetaRepository registroRecetaRepository;
    private final DistribucionControladaRepository distribucionControladaRepository;
    private final AlmacenRepository almacenRepository;

    public DigemidService(ProductoRepository productoRepository,
                          StockControladoRepository stockControladoRepository,
                          MovimientoControladoRepository movimientoControladoRepository,
                          RegistroRecetaRepository registroRecetaRepository,
                          DistribucionControladaRepository distribucionControladaRepository,
                          AlmacenRepository almacenRepository) {
        this.productoRepository = productoRepository;
        this.stockControladoRepository = stockControladoRepository;
        this.movimientoControladoRepository = movimientoControladoRepository;
        this.registroRecetaRepository = registroRecetaRepository;
        this.distribucionControladaRepository = distribucionControladaRepository;
        this.almacenRepository = almacenRepository;
    }

    private Long tenantId() {
        return TenantContext.getTenantId();
    }

    // --- Productos controlados ---
    public List<Producto> listarProductosControlados() {
        Long tid = tenantId();
        if (tid != null) {
            return productoRepository.findByTenantIdAndRequiereRecetaTrue(tid);
        }
        return productoRepository.findAll().stream()
                .filter(Producto::isRequiereReceta)
                .collect(Collectors.toList());
    }

    public boolean esProductoControlado(Long productoId) {
        return productoRepository.findById(productoId)
                .map(Producto::isRequiereReceta)
                .orElse(false);
    }

    // --- Stock controlado ---
    public List<StockControlado> listarStockControlado() {
        Long tid = tenantId();
        if (tid == null) return List.of();
        return stockControladoRepository.findByTenantId(tid);
    }

    public List<StockControlado> stockControladoPorProducto(Long productoId) {
        return stockControladoRepository.findByProductoId(productoId);
    }

    public int stockControladoTotalProducto(Long productoId) {
        return stockControladoRepository.findByProductoId(productoId).stream()
                .mapToInt(sc -> sc.getCantidad() != null ? sc.getCantidad() : 0)
                .sum();
    }

    public StockControlado registrarEntradaControlada(Long productoId, Long almacenId, String lote, LocalDate fechaVencimiento, int cantidad, String referencia, String ubicacionAlmacen) {
        Producto producto = productoRepository.findById(productoId).orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));
        Almacen almacen = almacenId != null ? almacenRepository.findById(almacenId).orElse(null) : null;
        String loteKey = lote != null && !lote.isBlank() ? lote.trim() : "S/L";
        Optional<StockControlado> opt = almacen != null
                ? stockControladoRepository.findByProductoIdAndAlmacenIdAndLote(productoId, almacenId, loteKey)
                : Optional.empty();
        StockControlado sc = opt.orElseGet(() -> {
            StockControlado s = new StockControlado();
            s.setProducto(producto);
            s.setAlmacen(almacen);
            s.setLote(loteKey);
            s.setFechaVencimiento(fechaVencimiento);
            s.setCantidad(0);
            return stockControladoRepository.save(s);
        });
        sc.setCantidad((sc.getCantidad() != null ? sc.getCantidad() : 0) + cantidad);
        sc.setFechaVencimiento(fechaVencimiento != null ? fechaVencimiento : sc.getFechaVencimiento());
        if (ubicacionAlmacen != null && !ubicacionAlmacen.isBlank()) {
            sc.setUbicacionAlmacen(ubicacionAlmacen.trim());
        }
        sc = stockControladoRepository.save(sc);
        MovimientoControlado mov = new MovimientoControlado();
        mov.setProducto(producto);
        mov.setAlmacen(almacen);
        mov.setTipo("ENTRADA");
        mov.setCantidad(cantidad);
        mov.setSaldoDespues(sc.getCantidad());
        mov.setLote(loteKey);
        mov.setReferencia(referencia != null ? referencia : "Entrada controlado");
        movimientoControladoRepository.save(mov);
        return sc;
    }

    public void registrarSalidaControlada(Long productoId, Long almacenId, int cantidad, String tipo, String referencia, Long ventaId, Long registroRecetaId, Long mermaId) {
        Producto producto = productoRepository.findById(productoId).orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));
        List<StockControlado> stocks = almacenId != null
                ? stockControladoRepository.findByProductoId(productoId).stream().filter(s -> almacenId.equals(s.getAlmacen() != null ? s.getAlmacen().getId() : null)).toList()
                : stockControladoRepository.findByProductoId(productoId);
        int restante = cantidad;
        for (StockControlado sc : stocks) {
            if (restante <= 0) break;
            int disp = sc.getCantidad() != null ? sc.getCantidad() : 0;
            int salir = Math.min(restante, disp);
            if (salir <= 0) continue;
            sc.setCantidad(disp - salir);
            stockControladoRepository.save(sc);
            MovimientoControlado mov = new MovimientoControlado();
            mov.setProducto(producto);
            mov.setAlmacen(sc.getAlmacen());
            mov.setTipo(tipo != null ? tipo : "SALIDA_VENTA");
            mov.setCantidad(-salir);
            mov.setSaldoDespues(sc.getCantidad());
            mov.setLote(sc.getLote());
            mov.setReferencia(referencia);
            mov.setVentaId(ventaId);
            mov.setRegistroRecetaId(registroRecetaId);
            mov.setMermaId(mermaId);
            movimientoControladoRepository.save(mov);
            restante -= salir;
        }
        if (restante > 0) {
            throw new IllegalArgumentException("Stock controlado insuficiente para el producto " + producto.getNombre() + ". Faltan " + restante + " unidades.");
        }
    }

    // --- Registro receta ---
    public RegistroReceta guardarRegistroReceta(RegistroReceta reg) {
        return registroRecetaRepository.save(reg);
    }

    public Optional<RegistroReceta> obtenerRegistroRecetaPorId(Long id) {
        return registroRecetaRepository.findById(id);
    }

    public List<RegistroReceta> listarRegistrosReceta() {
        Long tid = tenantId();
        if (tid == null) return List.of();
        return registroRecetaRepository.findByTenantIdOrderByFechaRegistroDesc(tid);
    }

    /** Alertas: recetas duplicadas (mismo número en mismo día o ya usado). */
    public List<String> alertasRecetaDuplicada(String numeroReceta, Long excluirRegistroId) {
        if (numeroReceta == null || numeroReceta.isBlank()) return List.of();
        Long tid = tenantId();
        if (tid == null) return List.of();
        List<RegistroReceta> existentes = registroRecetaRepository.findByTenantIdAndNumeroRecetaIgualExcluyendoId(tid, numeroReceta.trim(), excluirRegistroId);
        if (existentes.isEmpty()) return List.of();
        return existentes.stream()
                .map(r -> "Receta " + numeroReceta + " ya registrada el " + r.getFechaRegistro() + " para " + (r.getProducto() != null ? r.getProducto().getNombre() : "?"))
                .collect(Collectors.toList());
    }

    public boolean existeRecetaConNumero(String numeroReceta, Long excluirId) {
        Long tid = tenantId();
        if (tid == null) return false;
        return !registroRecetaRepository.findByTenantIdAndNumeroRecetaIgualExcluyendoId(tid, numeroReceta != null ? numeroReceta.trim() : "", excluirId).isEmpty();
    }

    // --- Distribución controlada ---
    public List<DistribucionControlada> listarDistribuciones() {
        Long tid = tenantId();
        if (tid == null) return List.of();
        return distribucionControladaRepository.findByTenantIdOrderByFechaEnvioDesc(tid);
    }

    public DistribucionControlada crearDistribucion(Long productoId, Long almacenOrigenId, Long almacenDestinoId, int cantidad, String lote, String referencia, String usuarioEnvio,
                                                       String clienteRuc, String clienteRazonSocial, String tipoEstablecimiento, String numeroFactura, String numeroGuiaRemision) {
        if (almacenOrigenId.equals(almacenDestinoId)) throw new IllegalArgumentException("Origen y destino deben ser distintos");
        Producto producto = productoRepository.findById(productoId).orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));
        Almacen origen = almacenRepository.findById(almacenOrigenId).orElseThrow(() -> new EntityNotFoundException("Almacén origen no encontrado"));
        Almacen destino = almacenRepository.findById(almacenDestinoId).orElseThrow(() -> new EntityNotFoundException("Almacén destino no encontrado"));
        int stockOrigen = stockControladoPorProducto(productoId).stream()
                .filter(s -> almacenOrigenId.equals(s.getAlmacen() != null ? s.getAlmacen().getId() : null))
                .mapToInt(s -> s.getCantidad() != null ? s.getCantidad() : 0)
                .sum();
        if (stockOrigen < cantidad) throw new IllegalArgumentException("Stock controlado insuficiente en origen");
        registrarSalidaControlada(productoId, almacenOrigenId, cantidad, "TRANSFERENCIA_SALIDA", referencia != null ? referencia : "Distribución a " + destino.getNombre(), null, null, null);
        DistribucionControlada d = new DistribucionControlada();
        d.setProducto(producto);
        d.setAlmacenOrigen(origen);
        d.setAlmacenDestino(destino);
        d.setCantidad(cantidad);
        d.setLote(lote);
        d.setReferencia(referencia);
        d.setUsuarioEnvio(usuarioEnvio != null ? usuarioEnvio : "Sistema");
        d.setEstado("ENVIADO");
        d.setClienteRuc(clienteRuc);
        d.setClienteRazonSocial(clienteRazonSocial);
        d.setTipoEstablecimiento(tipoEstablecimiento);
        d.setNumeroFactura(numeroFactura);
        d.setNumeroGuiaRemision(numeroGuiaRemision);
        return distribucionControladaRepository.save(d);
    }

    public void confirmarRecepcionDistribucion(Long distribucionId, String usuarioRecepcion) {
        DistribucionControlada d = distribucionControladaRepository.findById(distribucionId)
                .orElseThrow(() -> new EntityNotFoundException("Distribución no encontrada"));
        if (!"ENVIADO".equals(d.getEstado())) throw new IllegalStateException("La distribución no está pendiente de recepción");
        registrarEntradaControlada(d.getProducto().getId(), d.getAlmacenDestino().getId(), d.getLote(), null, d.getCantidad(), "Recepción distribución #" + d.getId(), null);
        d.setEstado("RECIBIDO");
        d.setFechaRecepcion(LocalDateTime.now());
        d.setUsuarioRecepcion(usuarioRecepcion != null ? usuarioRecepcion : "Sistema");
        distribucionControladaRepository.save(d);
    }

    // --- Kardex controlado ---
    public List<MovimientoControlado> kardexControladoPorProducto(Long productoId) {
        Long tid = tenantId();
        if (tid != null) {
            return movimientoControladoRepository.findByTenantIdAndProductoIdOrderByFechaDesc(tid, productoId);
        }
        return movimientoControladoRepository.findByProductoIdOrderByFechaDesc(productoId);
    }

    // --- Alertas ---
    public List<StockControlado> alertasStockControladoBajo(int umbral) {
        Long tid = tenantId();
        if (tid == null) return List.of();
        return stockControladoRepository.findByTenantId(tid).stream()
                .filter(s -> (s.getCantidad() != null ? s.getCantidad() : 0) <= umbral && (s.getCantidad() != null ? s.getCantidad() : 0) >= 0)
                .toList();
    }

    public List<StockControlado> alertasVencimientoProximo(int dias) {
        LocalDate desde = LocalDate.now();
        LocalDate hasta = desde.plusDays(dias);
        Long tid = tenantId();
        if (tid == null) return List.of();
        return stockControladoRepository.findByTenantIdAndFechaVencimientoBetween(tid, desde, hasta).stream()
                .filter(s -> (s.getCantidad() != null ? s.getCantidad() : 0) > 0)
                .toList();
    }

    public List<StockControlado> alertasVencidos() {
        Long tid = tenantId();
        if (tid == null) return List.of();
        return stockControladoRepository.findByTenantIdAndFechaVencimientoBeforeAndCantidadGreaterThan(tid, LocalDate.now(), 0);
    }

    /** Resumen para dashboard: cantidad de productos controlados con stock bajo, por vencer, recetas duplicadas recientes. */
    public Map<String, Object> resumenAlertas() {
        Map<String, Object> m = new HashMap<>();
        List<Producto> controlados = listarProductosControlados();
        int conStockBajo = 0;
        for (Producto p : controlados) {
            int stock = stockControladoTotalProducto(p.getId());
            Integer min = p.getStockMinimo();
            if (min != null && stock < min) conStockBajo++;
        }
        m.put("productosControlados", controlados.size());
        m.put("productosControladosStockBajo", conStockBajo);
        m.put("porVencer", alertasVencimientoProximo(30).size());
        m.put("vencidos", alertasVencidos().size());
        return m;
    }

    /** Reporte regulatorio: movimientos de controlados en rango de fechas. */
    public List<MovimientoControlado> reporteRegulatorioMovimientos(LocalDate desde, LocalDate hasta) {
        Long tid = tenantId();
        if (tid == null) return List.of();
        LocalDateTime ini = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(23, 59, 59);
        return movimientoControladoRepository.findByTenantIdAndFechaBetweenOrderByFechaDesc(tid, ini, fin);
    }

    public List<RegistroReceta> reporteRegulatorioRecetas(LocalDate desde, LocalDate hasta) {
        Long tid = tenantId();
        if (tid == null) return List.of();
        return registroRecetaRepository.findByTenantIdAndFechaRegistroBetweenOrderByFechaRegistroDesc(tid, desde.atStartOfDay(), hasta.atTime(23, 59, 59));
    }

    // --- Destrucción autorizada ---
    public void registrarDestruccion(Long productoId, Long almacenId, int cantidad, String motivo, String documento, String usuario) {
        registrarSalidaControlada(productoId, almacenId, cantidad, "DESTRUCCION", "Destrucción autorizada: " + (motivo != null ? motivo : ""), null, null, null);
        List<MovimientoControlado> movs = movimientoControladoRepository.findByProductoIdOrderByFechaDesc(productoId);
        if (!movs.isEmpty()) {
            MovimientoControlado ultimo = movs.get(0);
            ultimo.setUsuarioRegistro(usuario);
            ultimo.setNumeroDocumento(documento);
            ultimo.setMotivo(motivo);
            movimientoControladoRepository.save(ultimo);
        }
    }

    // --- Alerta exceso de venta ---
    public List<Map<String, Object>> alertasExcesoVenta(int diasAnalisis, int umbralVentas) {
        Long tid = tenantId();
        if (tid == null) return List.of();
        List<Map<String, Object>> alertas = new ArrayList<>();
        LocalDateTime desde = LocalDateTime.now().minusDays(diasAnalisis);
        LocalDateTime hasta = LocalDateTime.now();
        List<Producto> controlados = listarProductosControlados();
        for (Producto p : controlados) {
            List<MovimientoControlado> ventas = movimientoControladoRepository.findByTenantIdAndProductoIdOrderByFechaDesc(tid, p.getId()).stream()
                    .filter(m -> "SALIDA_VENTA".equals(m.getTipo()) && m.getFecha() != null && !m.getFecha().isBefore(desde) && !m.getFecha().isAfter(hasta))
                    .toList();
            int totalVendido = ventas.stream().mapToInt(m -> Math.abs(m.getCantidad() != null ? m.getCantidad() : 0)).sum();
            if (totalVendido >= umbralVentas) {
                Map<String, Object> alerta = new HashMap<>();
                alerta.put("producto", p);
                alerta.put("totalVendido", totalVendido);
                alerta.put("numTransacciones", ventas.size());
                alertas.add(alerta);
            }
        }
        return alertas;
    }

    // --- Reportes específicos por tipo de producto controlado ---
    public List<MovimientoControlado> reportePorTipoProducto(String tipoProductoControlado, LocalDate desde, LocalDate hasta) {
        Long tid = tenantId();
        if (tid == null) return List.of();
        return movimientoControladoRepository.findByTenantIdAndProducto_TipoProductoControladoAndFechaBetweenOrderByFechaDesc(tid, tipoProductoControlado, desde.atStartOfDay(), hasta.atTime(23, 59, 59));
    }

    public List<Producto> listarPorTipoProductoControlado(String tipoProductoControlado) {
        Long tid = tenantId();
        if (tid == null) return List.of();
        return productoRepository.findByTenantIdAndTipoProductoControlado(tid, tipoProductoControlado);
    }

    public List<StockControlado> reporteControlLotes() {
        Long tid = tenantId();
        if (tid == null) return List.of();
        return stockControladoRepository.findByTenantId(tid).stream()
                .filter(s -> s.getCantidad() != null && s.getCantidad() > 0)
                .sorted((a, b) -> {
                    if (a.getFechaVencimiento() == null && b.getFechaVencimiento() == null) return 0;
                    if (a.getFechaVencimiento() == null) return 1;
                    if (b.getFechaVencimiento() == null) return -1;
                    return a.getFechaVencimiento().compareTo(b.getFechaVencimiento());
                })
                .toList();
    }

    public List<DistribucionControlada> reporteHistorialDistribucion(LocalDate desde, LocalDate hasta) {
        Long tid = tenantId();
        if (tid == null) return List.of();
        return distribucionControladaRepository.findByTenantIdOrderByFechaEnvioDesc(tid).stream()
                .filter(d -> {
                    if (d.getFechaEnvio() == null) return false;
                    LocalDate f = d.getFechaEnvio().toLocalDate();
                    return !f.isBefore(desde) && !f.isAfter(hasta);
                })
                .toList();
    }

    public Map<String, Object> resumenAlertasCompleto() {
        Map<String, Object> m = resumenAlertas();
        m.put("excesoVenta", alertasExcesoVenta(7, 50).size());
        return m;
    }
}
