package com.farmacia.sistema.domain.compra;

import com.farmacia.sistema.domain.inventario.InventarioService;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoService;
import com.farmacia.sistema.domain.proveedor.Proveedor;
import com.farmacia.sistema.domain.proveedor.ProveedorService;
import com.farmacia.sistema.dto.ProveedorComparacionDto;
import com.farmacia.sistema.dto.SugerenciaCompraDto;
import com.farmacia.sistema.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class CompraService {

    private final OrdenCompraRepository ordenRepository;
    private final CuentaPagarRepository cuentaPagarRepository;
    private final ProveedorService proveedorService;
    private final ProductoService productoService;
    private final InventarioService inventarioService;

    public CompraService(OrdenCompraRepository ordenRepository,
                         CuentaPagarRepository cuentaPagarRepository,
                         ProveedorService proveedorService,
                         ProductoService productoService,
                         InventarioService inventarioService) {
        this.ordenRepository = ordenRepository;
        this.cuentaPagarRepository = cuentaPagarRepository;
        this.proveedorService = proveedorService;
        this.productoService = productoService;
        this.inventarioService = inventarioService;
    }

    public List<OrdenCompra> listarOrdenes() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return ordenRepository.findByTenantIdOrderByFechaEmisionDescWithItems(tenantId);
        }
        return ordenRepository.findAllByOrderByFechaEmisionDesc();
    }

    public OrdenCompra obtenerOrdenPorId(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden de compra no encontrada"));
    }

    public OrdenCompra crearOrden(Long proveedorId, Long productoId, int cantidad, BigDecimal precioUnitario,
                                  LocalDate fechaEsperada, String observaciones) {
        Proveedor proveedor = proveedorService.obtenerPorId(proveedorId);
        Producto producto = productoService.obtenerPorId(productoId);

        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        if (precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio unitario debe ser mayor que cero");
        }

        OrdenCompra orden = new OrdenCompra();
        orden.setProveedor(proveedor);
        orden.setFechaEsperada(fechaEsperada);
        orden.setObservaciones(observaciones);
        orden.setEstado("EMITIDA");

        BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        orden.setSubtotal(subtotal);
        orden.setTotal(subtotal);

        OrdenCompraItem item = new OrdenCompraItem();
        item.setOrden(orden);
        item.setProducto(producto);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(precioUnitario);
        item.setSubtotal(subtotal);
        orden.getItems().add(item);

        return ordenRepository.save(orden);
    }

    /** Marca la orden como recibida, ingresa mercadería a inventario y genera cuenta por pagar. */
    public void recibirOrden(Long ordenId) {
        OrdenCompra orden = obtenerOrdenPorId(ordenId);
        if (!"EMITIDA".equals(orden.getEstado())) {
            throw new IllegalArgumentException("Solo se pueden recibir órdenes en estado EMITIDA");
        }
        for (OrdenCompraItem item : orden.getItems()) {
            Producto producto = productoService.obtenerPorId(item.getProducto().getId());
            int nuevaCantidad = producto.getStockActual() + item.getCantidad();
            productoService.actualizarStock(producto.getId(), nuevaCantidad);
            producto.setStockActual(nuevaCantidad);
            inventarioService.registrarEntrada(producto, item.getCantidad(), "OC #" + orden.getId());
            inventarioService.actualizarStockPrincipal(producto.getId(), item.getCantidad());
        }
        orden.setEstado("RECIBIDA");
        ordenRepository.save(orden);

        CuentaPagar cxp = new CuentaPagar();
        cxp.setProveedor(orden.getProveedor());
        cxp.setOrdenCompra(orden);
        cxp.setFechaEmision(LocalDate.now());
        cxp.setFechaVencimiento(LocalDate.now().plusDays(30));
        cxp.setMontoTotal(orden.getTotal());
        cxp.setSaldoPendiente(orden.getTotal());
        cxp.setEstado("PENDIENTE");
        cxp.setObservaciones("OC #" + orden.getId());
        cuentaPagarRepository.save(cxp);
    }

    public List<CuentaPagar> listarCuentasPendientes() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return cuentaPagarRepository.findByTenantIdAndEstadoOrderByFechaVencimientoAscWithOrden(tenantId, "PENDIENTE");
        }
        return cuentaPagarRepository.findByEstadoOrderByFechaVencimientoAsc("PENDIENTE");
    }

    public List<CuentaPagar> listarTodasCuentas() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return cuentaPagarRepository.findByTenantIdWithOrden(tenantId);
        }
        return cuentaPagarRepository.findAll();
    }

    public void marcarCuentaPagada(Long cuentaId) {
        CuentaPagar cuenta = cuentaPagarRepository.findById(cuentaId)
                .orElseThrow(() -> new EntityNotFoundException("Cuenta por pagar no encontrada"));
        cuenta.setSaldoPendiente(BigDecimal.ZERO);
        cuenta.setEstado("PAGADA");
        cuentaPagarRepository.save(cuenta);
    }

    /** Elimina una cuenta por pagar del historial. */
    public void eliminarCuenta(Long cuentaId) {
        CuentaPagar cuenta = cuentaPagarRepository.findById(cuentaId)
                .orElseThrow(() -> new EntityNotFoundException("Cuenta por pagar no encontrada"));
        cuenta.setOrdenCompra(null);
        cuentaPagarRepository.save(cuenta);
        cuentaPagarRepository.delete(cuenta);
    }

    /** Comparación simple de proveedores para un producto: último precio por proveedor. */
    public List<ProveedorComparacionDto> compararProveedoresPorProducto(Long productoId) {
        if (productoId == null) {
            return List.of();
        }
        List<OrdenCompra> ordenes = listarOrdenes();
        Map<Long, ProveedorComparacionDto> mapa = new LinkedHashMap<>();
        for (OrdenCompra oc : ordenes) {
            if (oc.getItems() == null) continue;
            for (OrdenCompraItem item : oc.getItems()) {
                if (item.getProducto() == null || !productoId.equals(item.getProducto().getId())) continue;
                Proveedor prov = oc.getProveedor();
                if (prov == null) continue;
                Long provId = prov.getId();
                if (!mapa.containsKey(provId)) {
                    ProveedorComparacionDto dto = new ProveedorComparacionDto();
                    dto.setProveedorId(provId);
                    dto.setProveedorNombre(prov.getRazonSocial());
                    dto.setUltimoPrecio(item.getPrecioUnitario());
                    dto.setCantidad(item.getCantidad() != null ? item.getCantidad() : 0);
                    dto.setFechaUltimaCompra(oc.getFechaEmision());
                    mapa.put(provId, dto);
                }
            }
        }
        return new ArrayList<>(mapa.values());
    }

    /** Elimina una orden. Si está RECIBIDA, revierte stock y anula la cuenta por pagar asociada. */
    public void eliminarOrden(Long ordenId) {
        OrdenCompra orden = obtenerOrdenPorId(ordenId);
        if ("RECIBIDA".equals(orden.getEstado())) {
            for (OrdenCompraItem item : orden.getItems()) {
                if (item.getProducto() == null) continue;
                Producto producto = productoService.obtenerPorId(item.getProducto().getId());
                int nuevoStock = (producto.getStockActual() != null ? producto.getStockActual() : 0) - item.getCantidad();
                if (nuevoStock < 0) nuevoStock = 0;
                productoService.actualizarStock(producto.getId(), nuevoStock);
                inventarioService.actualizarStockPrincipal(producto.getId(), -item.getCantidad());
            }
            for (CuentaPagar cxp : cuentaPagarRepository.findByOrdenCompra_Id(ordenId)) {
                cxp.setEstado("ANULADA");
                cxp.setSaldoPendiente(BigDecimal.ZERO);
                cxp.setOrdenCompra(null);
                cuentaPagarRepository.save(cxp);
            }
        }
        ordenRepository.delete(orden);
    }

    /** Actualiza una orden en estado EMITIDA (proveedor, producto, cantidad, precio, fechas, observaciones). */
    public OrdenCompra actualizarOrden(Long ordenId, Long proveedorId, Long productoId, int cantidad,
                                       BigDecimal precioUnitario, LocalDate fechaEsperada, String observaciones) {
        OrdenCompra orden = obtenerOrdenPorId(ordenId);
        if (!"EMITIDA".equals(orden.getEstado())) {
            throw new IllegalArgumentException("Solo se pueden modificar órdenes en estado EMITIDA");
        }
        if (cantidad <= 0 || precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cantidad y precio unitario deben ser mayores que cero");
        }
        Proveedor proveedor = proveedorService.obtenerPorId(proveedorId);
        Producto producto = productoService.obtenerPorId(productoId);
        orden.setProveedor(proveedor);
        orden.setFechaEsperada(fechaEsperada);
        orden.setObservaciones(observaciones);
        BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        orden.setSubtotal(subtotal);
        orden.setTotal(subtotal);
        if (orden.getItems() == null || orden.getItems().isEmpty()) {
            OrdenCompraItem item = new OrdenCompraItem();
            item.setOrden(orden);
            item.setProducto(producto);
            item.setCantidad(cantidad);
            item.setPrecioUnitario(precioUnitario);
            item.setSubtotal(subtotal);
            orden.getItems().add(item);
        } else {
            OrdenCompraItem item = orden.getItems().get(0);
            item.setProducto(producto);
            item.setCantidad(cantidad);
            item.setPrecioUnitario(precioUnitario);
            item.setSubtotal(subtotal);
        }
        return ordenRepository.save(orden);
    }

    /** Sugerencias de compra basadas en productos bajo mínimo y stock máximo (si existe). */
    public List<SugerenciaCompraDto> sugerenciasCompra() {
        List<Producto> productos = productoService.listarTodos();
        List<SugerenciaCompraDto> resultado = new ArrayList<>();
        for (Producto p : productos) {
            Integer stock = p.getStockActual();
            Integer minimo = p.getStockMinimo();
            if (stock == null || minimo == null || stock >= minimo) continue;
            Integer maximo = p.getStockMaximo();
            int sugerida;
            if (maximo != null && maximo > stock) {
                sugerida = maximo - stock;
            } else {
                sugerida = (minimo * 2) - stock;
            }
            if (sugerida < 1) sugerida = 1;
            SugerenciaCompraDto dto = new SugerenciaCompraDto();
            dto.setProductoId(p.getId());
            dto.setProductoNombre(p.getNombre());
            dto.setStockActual(stock);
            dto.setStockMinimo(minimo);
            dto.setStockMaximo(maximo);
            dto.setCantidadSugerida(sugerida);
            resultado.add(dto);
        }
        resultado.sort(Comparator.comparingInt(s -> s.getStockActual() != null ? s.getStockActual() : 0));
        return resultado;
    }
}

