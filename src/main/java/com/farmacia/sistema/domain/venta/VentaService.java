package com.farmacia.sistema.domain.venta;

import com.farmacia.sistema.api.venta.CrearVentaRequest;
import com.farmacia.sistema.api.venta.ItemVentaRequest;
import com.farmacia.sistema.api.venta.PagoRequest;
import com.farmacia.sistema.domain.cliente.Cliente;
import com.farmacia.sistema.domain.caja.CajaTurnoService;
import com.farmacia.sistema.domain.cliente.ClienteService;
import com.farmacia.sistema.domain.inventario.InventarioService;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoService;
import com.farmacia.sistema.domain.sucursal.Sucursal;
import com.farmacia.sistema.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.farmacia.sistema.dto.VentaItemResumenDto;
import com.farmacia.sistema.dto.VentaResumenDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ClienteService clienteService;
    private final ProductoService productoService;
    private final CajaTurnoService cajaTurnoService;
    private final SequenceComprobanteService sequenceComprobanteService;
    private final InventarioService inventarioService;

    public VentaService(VentaRepository ventaRepository,
                        ClienteService clienteService,
                        ProductoService productoService,
                        CajaTurnoService cajaTurnoService,
                        SequenceComprobanteService sequenceComprobanteService,
                        InventarioService inventarioService) {
        this.ventaRepository = ventaRepository;
        this.clienteService = clienteService;
        this.productoService = productoService;
        this.cajaTurnoService = cajaTurnoService;
        this.sequenceComprobanteService = sequenceComprobanteService;
        this.inventarioService = inventarioService;
    }

    private List<Venta> ventasDelTenant() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return ventaRepository.findByTenantId(tenantId);
        }
        return ventaRepository.findAll();
    }

    public List<Venta> listarTodas() {
        return ventasDelTenant();
    }

    public List<VentaResumenDto> listarVentasResumenParaWeb() {
        Long tenantId = TenantContext.getTenantId();
        List<Venta> ventas = tenantId != null
                ? ventaRepository.findByTenantIdOrderByFechaHoraDescWithItems(tenantId)
                : ventaRepository.findAllByOrderByFechaHoraDescWithItems();
        List<VentaResumenDto> resultado = new ArrayList<>();
        for (Venta v : ventas) {
            VentaResumenDto dto = new VentaResumenDto();
            dto.setId(v.getId());
            String soloNombre = "";
            String numeroDoc = "";
            if (v.getCliente() != null) {
                String nombres = v.getCliente().getNombres() != null ? v.getCliente().getNombres().trim() : "";
                String apellidos = v.getCliente().getApellidos() != null ? v.getCliente().getApellidos().trim() : "";
                soloNombre = (nombres + " " + apellidos).trim();
                if (soloNombre.isEmpty()) soloNombre = v.getNombreClienteVenta() != null ? v.getNombreClienteVenta() : "";
                numeroDoc = v.getCliente().getNumeroDocumento() != null ? v.getCliente().getNumeroDocumento().trim() : "";
            } else {
                soloNombre = v.getNombreClienteVenta() != null ? v.getNombreClienteVenta() : "";
            }
            dto.setNombreClienteVenta(soloNombre);
            dto.setNumeroDocumentoCliente(numeroDoc);
            dto.setFechaHora(v.getFechaHora());
            dto.setTotal(v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO);
            dto.setTipoComprobante(v.getTipoComprobante());
            dto.setSerieComprobante(v.getSerieComprobante());
            dto.setNumeroComprobante(v.getNumeroComprobante());
            dto.setDescuentoTotal(v.getDescuentoTotal() != null ? v.getDescuentoTotal() : BigDecimal.ZERO);
            List<VentaItemResumenDto> itemsDto = new ArrayList<>();
            if (v.getItems() != null) {
                dto.setItemsCount(v.getItems().size());
                for (VentaItem vi : v.getItems()) {
                    VentaItemResumenDto idto = new VentaItemResumenDto();
                    idto.setNombreProducto(vi.getProducto() != null ? vi.getProducto().getNombre() : "?");
                    idto.setCantidad(vi.getCantidad() != null ? vi.getCantidad() : 0);
                    idto.setSubtotal(vi.getSubtotal() != null ? vi.getSubtotal().toString() : "");
                    itemsDto.add(idto);
                }
            } else {
                dto.setItemsCount(0);
            }
            dto.setItems(itemsDto);
            try {
                dto.setPagosResumen(v.getPagosResumen() != null ? v.getPagosResumen() : "-");
            } catch (Exception e) {
                dto.setPagosResumen("-");
            }
            dto.setEstado(v.getEstado() != null ? v.getEstado() : "EMITIDA");
            resultado.add(dto);
        }
        return resultado;
    }

    public Venta obtenerPorId(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada"));
    }

    /**
     * Registra una devolución total: los productos vuelven al almacén (stock actual se incrementa)
     * y la venta queda anulada (estado ANULADA). El número de comprobante se conserva pero la
     * boleta queda anulada. Solo aplicable a ventas en estado EMITIDA.
     */
    public Venta anularPorDevolucion(Long ventaId) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada"));
        String estadoActual = venta.getEstado() != null ? venta.getEstado() : "EMITIDA";
        if (!"EMITIDA".equals(estadoActual)) {
            throw new IllegalStateException(
                    "Solo se puede devolver una venta emitida. Estado actual: " + estadoActual);
        }
        String refDevolucion = "Devolución venta #" + (venta.getNumeroComprobante() != null ? venta.getNumeroComprobante() : ventaId);
        if (venta.getItems() != null) {
            for (VentaItem item : venta.getItems()) {
                Producto p = item.getProducto();
                if (p == null) continue;
                int cant = item.getCantidad() != null ? item.getCantidad() : 0;
                if (cant <= 0) continue;
                int stockActual = p.getStockActual() != null ? p.getStockActual() : 0;
                p.setStockActual(stockActual + cant);
                inventarioService.registrarEntrada(p, cant, refDevolucion);
                inventarioService.actualizarStockPrincipal(p.getId(), cant);
            }
        }
        venta.setEstado("ANULADA");
        return ventaRepository.save(venta);
    }

    public Venta crearVenta(CrearVentaRequest request) {
        Cliente cliente = clienteService.obtenerPorId(request.getClienteId());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("La venta debe tener al menos un ítem");
        }

        Venta venta = new Venta();
        venta.setCliente(cliente);
        String nombreClienteVenta = request.getNombreClienteVenta();
        if (nombreClienteVenta == null || nombreClienteVenta.isBlank()) {
            StringBuilder sb = new StringBuilder();
            if (cliente.getNombres() != null) {
                sb.append(cliente.getNombres());
            }
            if (cliente.getApellidos() != null && !cliente.getApellidos().isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(" ");
                }
                sb.append(cliente.getApellidos());
            }
            nombreClienteVenta = sb.toString();
        }
        venta.setNombreClienteVenta(nombreClienteVenta);
        venta.setFechaHora(LocalDateTime.now());

        Long sucursalIdVenta = null;
        if (request.getCajaTurnoId() != null) {
            var cajaTurno = cajaTurnoService.obtenerPorId(request.getCajaTurnoId());
            venta.setCajaTurno(cajaTurno);
            if (cajaTurno != null && cajaTurno.getSucursal() != null) {
                sucursalIdVenta = cajaTurno.getSucursal().getId();
            }
        }

        String tipo = request.getTipoComprobante() != null && "FAC".equalsIgnoreCase(request.getTipoComprobante().trim()) ? "FAC" : "BOL";
        venta.setTipoComprobante(tipo);
        venta.setSerieComprobante("001");
        venta.setNumeroComprobante(String.format("%08d", sequenceComprobanteService.getNextNumeroPorTipo(tipo)));
        venta.setEstadoSunat("PENDIENTE");

        if ("FAC".equals(tipo)) {
            if (cliente.getTipoDocumento() == null || !"RUC".equalsIgnoreCase(cliente.getTipoDocumento().trim())) {
                throw new IllegalArgumentException("Para emitir Factura el cliente debe tener RUC (documento de 11 dígitos). Seleccione un cliente con RUC o regístrelo en Clientes.");
            }
        }

        BigDecimal descuentoTotalVenta = request.getDescuentoTotal() != null ? request.getDescuentoTotal() : BigDecimal.ZERO;
        venta.setDescuentoTotal(descuentoTotalVenta);

        BigDecimal subtotalVenta = BigDecimal.ZERO;

        for (ItemVentaRequest itemReq : request.getItems()) {
            Producto producto = productoService.obtenerPorId(itemReq.getProductoId());
            int cantidad = itemReq.getCantidad();
            int stockActual = producto.getStockActual() != null ? producto.getStockActual() : 0;

            if (sucursalIdVenta != null) {
                int stockSucursal = inventarioService.obtenerStockEnSucursal(sucursalIdVenta, producto.getId());
                if (stockSucursal < cantidad) {
                    throw new IllegalArgumentException("Stock insuficiente en sucursal para el producto: " + producto.getNombre() + " (disponible: " + stockSucursal + ")");
                }
            } else {
                // Para ventas de ADMIN / SAAS_ADMIN (sin sucursal), validar solo contra el stockActual global
                if (stockActual < cantidad) {
                    throw new IllegalArgumentException("Stock insuficiente para el producto: " + producto.getNombre() + " (disponible: " + stockActual + ")");
                }
            }

            BigDecimal precioUnitario = itemReq.getPrecioUnitario() != null
                    ? itemReq.getPrecioUnitario()
                    : producto.getPrecioVenta();

            BigDecimal descuentoItem = itemReq.getDescuento() != null ? itemReq.getDescuento() : BigDecimal.ZERO;
            BigDecimal subtotalItem = precioUnitario.multiply(BigDecimal.valueOf(cantidad)).subtract(descuentoItem);

            if (sucursalIdVenta != null) {
                // Venta por sucursal: se controla y descuenta por almacenes de la sucursal
                int nuevoStock = stockActual - cantidad;
                if (nuevoStock < 0) {
                    throw new IllegalArgumentException("Stock insuficiente para el producto: " + producto.getNombre());
                }
                producto.setStockActual(nuevoStock);
                productoService.actualizarStock(producto.getId(), nuevoStock);
                inventarioService.actualizarStockEnSucursal(sucursalIdVenta, producto.getId(), -cantidad);
            } else {
                // Venta general (ADMIN / SAAS_ADMIN): solo se maneja el stock global del producto
                int nuevoStock = stockActual - cantidad;
                if (nuevoStock < 0) {
                    throw new IllegalArgumentException("Stock insuficiente para el producto: " + producto.getNombre());
                }
                producto.setStockActual(nuevoStock);
                productoService.actualizarStock(producto.getId(), nuevoStock);
            }
            inventarioService.registrarSalidaVenta(producto, cantidad, "Venta #" + venta.getNumeroComprobante());

            VentaItem item = new VentaItem();
            item.setVenta(venta);
            item.setProducto(producto);
            item.setCantidad(cantidad);
            item.setPrecioUnitario(precioUnitario);
            item.setDescuento(descuentoItem);
            item.setSubtotal(subtotalItem);

            venta.getItems().add(item);
            subtotalVenta = subtotalVenta.add(subtotalItem);
        }

        venta.setSubtotal(subtotalVenta);
        BigDecimal total = subtotalVenta.subtract(descuentoTotalVenta);
        venta.setTotal(total);

        if (request.getPagos() == null || request.getPagos().isEmpty()) {
            throw new IllegalArgumentException("Indique al menos un medio de pago y el monto (efectivo, tarjeta, transferencia, Yape, Plin).");
        }
        BigDecimal sumaPagos = request.getPagos().stream()
                .map(PagoRequest::getMonto)
                .filter(m -> m != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Se permite pago parcial: si sumaPagos < total, el saldo queda como cuenta por cobrar (crédito al cliente).
        if (sumaPagos.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Indique al menos un pago con monto mayor a cero.");
        }
        if (request.getPagos() != null && !request.getPagos().isEmpty()) {
            for (PagoRequest pagoReq : request.getPagos()) {
                if (pagoReq.getMonto() == null || pagoReq.getMonto().compareTo(BigDecimal.ZERO) <= 0) continue;
                PagoVenta pago = new PagoVenta();
                pago.setVenta(venta);
                pago.setMedioPago(pagoReq.getMedioPago() != null ? pagoReq.getMedioPago().trim() : "EFECTIVO");
                pago.setMonto(pagoReq.getMonto());
                venta.getPagos().add(pago);
            }
        }

        Venta guardada = ventaRepository.save(venta);

        // Programa de puntos: 1 punto por cada S/10 de consumo (redondeo hacia abajo)
        BigDecimal totalVenta = guardada.getTotal() != null ? guardada.getTotal() : BigDecimal.ZERO;
        BigDecimal factor = new BigDecimal("10");
        int puntosGanados = totalVenta.divide(factor, 0, java.math.RoundingMode.DOWN).intValue();
        if (puntosGanados > 0 && guardada.getCliente() != null) {
            var cli = guardada.getCliente();
            Integer actuales = cli.getPuntos() != null ? cli.getPuntos() : 0;
            cli.setPuntos(actuales + puntosGanados);
        }

        return guardada;
    }

    public BigDecimal getMontoTotalVendido() {
        return ventasDelTenant().stream()
                .filter(v -> "EMITIDA".equals(v.getEstado()))
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long getVentasHoyCount() {
        LocalDate hoy = LocalDate.now();
        return ventasDelTenant().stream()
                .filter(v -> "EMITIDA".equals(v.getEstado()))
                .filter(v -> v.getFechaHora() != null && v.getFechaHora().toLocalDate().equals(hoy))
                .count();
    }

    public BigDecimal getVentasHoyMonto() {
        LocalDate hoy = LocalDate.now();
        return ventasDelTenant().stream()
                .filter(v -> "EMITIDA".equals(v.getEstado()))
                .filter(v -> v.getFechaHora() != null && v.getFechaHora().toLocalDate().equals(hoy))
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public java.util.List<Venta> listarPorCliente(Long clienteId) {
        return ventaRepository.findByClienteIdOrderByFechaHoraDesc(clienteId);
    }

    public BigDecimal getTotalCompradoPorCliente(Long clienteId) {
        return ventaRepository.findByClienteIdOrderByFechaHoraDesc(clienteId).stream()
                .map(Venta::getTotal)
                .filter(t -> t != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getDescripcionUltimaVentaPorCliente(Long clienteId) {
        return ventaRepository.findFirstByClienteIdOrderByFechaHoraDesc(clienteId)
                .map(venta -> {
                    if (venta.getItems() == null || venta.getItems().isEmpty()) {
                        return "-";
                    }
                    return venta.getItems().stream()
                            .map(item -> item.getProducto() != null ? item.getProducto().getNombre() : "")
                            .filter(n -> n != null && !n.isBlank())
                            .distinct()
                            .limit(3)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("-");
                })
                .orElse("-");
    }

    public List<Venta> listarPorSucursalEntreFechas(Long sucursalId, LocalDate desde, LocalDate hasta) {
        if (sucursalId == null) {
            return List.of();
        }
        LocalDateTime ini = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(java.time.LocalTime.MAX);
        return ventasDelTenant().stream()
                .filter(v -> v.getFechaHora() != null &&
                        !v.getFechaHora().isBefore(ini) &&
                        !v.getFechaHora().isAfter(fin))
                .filter(v -> v.getCajaTurno() != null &&
                        v.getCajaTurno().getSucursal() != null &&
                        sucursalId.equals(v.getCajaTurno().getSucursal().getId()))
                .sorted(Comparator.comparing(Venta::getFechaHora).reversed())
                .toList();
    }

    public BigDecimal totalVentasPorSucursalEntreFechas(Long sucursalId, LocalDate desde, LocalDate hasta) {
        return listarPorSucursalEntreFechas(sucursalId, desde, hasta).stream()
                .map(Venta::getTotal)
                .filter(t -> t != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

