package com.farmacia.sistema.domain.finanzas;

import com.farmacia.sistema.domain.caja.CajaTurno;
import com.farmacia.sistema.domain.caja.CajaTurnoRepository;
import com.farmacia.sistema.domain.caja.CajaTurnoService;
import com.farmacia.sistema.domain.compra.CompraService;
import com.farmacia.sistema.domain.compra.CuentaPagar;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoService;
import com.farmacia.sistema.domain.venta.Venta;
import com.farmacia.sistema.domain.venta.VentaItem;
import com.farmacia.sistema.domain.venta.VentaItemRepository;
import com.farmacia.sistema.domain.venta.VentaRepository;
import com.farmacia.sistema.domain.venta.VentaService;
import com.farmacia.sistema.dto.CuentaCobrarDto;
import com.farmacia.sistema.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FinanzasService {

    private final VentaRepository ventaRepository;
    private final VentaItemRepository ventaItemRepository;
    private final CajaTurnoRepository cajaTurnoRepository;
    private final CompraService compraService;
    private final ProductoService productoService;

    public FinanzasService(VentaRepository ventaRepository,
                           VentaItemRepository ventaItemRepository,
                           CajaTurnoRepository cajaTurnoRepository,
                           CompraService compraService,
                           ProductoService productoService) {
        this.ventaRepository = ventaRepository;
        this.ventaItemRepository = ventaItemRepository;
        this.cajaTurnoRepository = cajaTurnoRepository;
        this.compraService = compraService;
        this.productoService = productoService;
    }

    private List<Venta> ventasDelTenant() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return ventaRepository.findByTenantId(tenantId);
        }
        return ventaRepository.findAll();
    }

    private LocalDateTime inicioDelDia(LocalDate fecha) {
        return fecha.atStartOfDay();
    }

    private LocalDateTime finDelDia(LocalDate fecha) {
        return fecha.atTime(LocalTime.MAX);
    }

    public BigDecimal totalVentasEntre(LocalDate desde, LocalDate hasta) {
        LocalDateTime ini = inicioDelDia(desde);
        LocalDateTime fin = finDelDia(hasta);
        return ventasDelTenant().stream()
                .filter(v -> v.getFechaHora() != null &&
                        !v.getFechaHora().isBefore(ini) &&
                        !v.getFechaHora().isAfter(fin))
                .map(Venta::getTotal)
                .filter(t -> t != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalCuentasPorPagarPendientes() {
        return compraService.listarCuentasPendientes().stream()
                .map(CuentaPagar::getSaldoPendiente)
                .filter(m -> m != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Ventas con saldo pendiente de cobro (lo pagado es menor que el total). Usa ventas con pagos cargados (no ítems, para evitar MultipleBagFetchException). */
    public List<Venta> ventasConSaldoPendiente() {
        Long tenantId = TenantContext.getTenantId();
        List<Venta> ventas = tenantId != null
                ? ventaRepository.findByTenantIdWithPagosOrderByFechaHoraDesc(tenantId)
                : ventaRepository.findAllWithPagosOrderByFechaHoraDesc();
        List<Venta> resultado = new ArrayList<>();
        for (Venta v : ventas) {
            BigDecimal total = v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO;
            BigDecimal pagado = v.getPagos() != null
                    ? v.getPagos().stream()
                    .map(p -> p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    : BigDecimal.ZERO;
            if (pagado.compareTo(total) < 0) {
                resultado.add(v);
            }
        }
        return resultado;
    }

    /** Detalle de cuentas por cobrar como DTOs para la vista (evita lazy y expresiones complejas). */
    public List<CuentaCobrarDto> listarCuentasPorCobrarDetalle() {
        List<Venta> ventas = ventasConSaldoPendiente();
        List<Long> ventaIds = ventas.stream().map(Venta::getId).filter(id -> id != null).distinct().collect(Collectors.toList());
        Map<Long, String> productosPorVentaId = new LinkedHashMap<>();
        if (!ventaIds.isEmpty()) {
            List<VentaItem> items = ventaItemRepository.findByVentaIdInWithProducto(ventaIds);
            for (Long vid : ventaIds) {
                String nombres = items.stream()
                        .filter(i -> i.getVenta() != null && vid.equals(i.getVenta().getId()))
                        .map(i -> i.getProducto() != null && i.getProducto().getNombre() != null ? i.getProducto().getNombre() : "")
                        .filter(n -> !n.isEmpty())
                        .distinct()
                        .collect(Collectors.joining(", "));
                productosPorVentaId.put(vid, nombres.isEmpty() ? "—" : nombres);
            }
        }
        List<CuentaCobrarDto> lista = new ArrayList<>();
        for (Venta v : ventas) {
            BigDecimal total = v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO;
            BigDecimal pagado = v.getPagos() != null
                    ? v.getPagos().stream()
                    .map(p -> p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    : BigDecimal.ZERO;
            CuentaCobrarDto dto = new CuentaCobrarDto();
            dto.setVentaId(v.getId());
            String nombreCliente = v.getNombreClienteVenta();
            if (nombreCliente == null || nombreCliente.isBlank()) {
                if (v.getCliente() != null) {
                    String n = v.getCliente().getNombres() != null ? v.getCliente().getNombres() : "";
                    String a = v.getCliente().getApellidos() != null ? v.getCliente().getApellidos() : "";
                    nombreCliente = (n + " " + a).trim();
                    if (nombreCliente.isEmpty()) nombreCliente = "-";
                } else {
                    nombreCliente = "-";
                }
            }
            dto.setNombreCliente(nombreCliente);
            dto.setNombreProductos(v.getId() != null ? productosPorVentaId.getOrDefault(v.getId(), "—") : "—");
            dto.setFechaHora(v.getFechaHora());
            dto.setTotal(total);
            dto.setPagado(pagado);
            dto.setSaldo(total.subtract(pagado));
            lista.add(dto);
        }
        return lista;
    }

    public BigDecimal totalCuentasPorCobrar() {
        return ventasConSaldoPendiente().stream()
                .map(v -> {
                    BigDecimal total = v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO;
                    BigDecimal pagado = v.getPagos() != null
                            ? v.getPagos().stream()
                            .map(p -> p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            : BigDecimal.ZERO;
                    return total.subtract(pagado);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Productos más vendidos (por cantidad) en un período. */
    public List<Map.Entry<Producto, Long>> productosMasVendidos(LocalDate desde, LocalDate hasta, int max) {
        LocalDateTime ini = inicioDelDia(desde);
        LocalDateTime fin = finDelDia(hasta);
        Map<Producto, Long> mapa = new java.util.HashMap<>();
        ventasDelTenant().stream()
                .filter(v -> v.getFechaHora() != null &&
                        !v.getFechaHora().isBefore(ini) &&
                        !v.getFechaHora().isAfter(fin))
                .flatMap(v -> v.getItems().stream())
                .forEach(item -> {
                    Producto p = item.getProducto();
                    if (p == null) return;
                    long cant = item.getCantidad() != null ? item.getCantidad() : 0;
                    mapa.merge(p, cant, Long::sum);
                });
        return mapa.entrySet().stream()
                .sorted(Map.Entry.<Producto, Long>comparingByValue().reversed())
                .limit(max)
                .collect(Collectors.toList());
    }

    /** Productos sin rotación (sin ventas en el período). */
    public List<Producto> productosSinRotacion(LocalDate desde, LocalDate hasta) {
        LocalDateTime ini = inicioDelDia(desde);
        LocalDateTime fin = finDelDia(hasta);
        java.util.Set<Long> vendidos = ventasDelTenant().stream()
                .filter(v -> v.getFechaHora() != null &&
                        !v.getFechaHora().isBefore(ini) &&
                        !v.getFechaHora().isAfter(fin))
                .flatMap(v -> v.getItems().stream())
                .map(item -> item.getProducto() != null ? item.getProducto().getId() : null)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        return productoService.listarTodos().stream()
                .filter(p -> !vendidos.contains(p.getId()))
                .collect(Collectors.toList());
    }

    /** Ranking de vendedores por monto vendido en el período (usa usuario del turno de caja). */
    public List<Map.Entry<String, BigDecimal>> rankingVendedores(LocalDate desde, LocalDate hasta) {
        LocalDateTime ini = inicioDelDia(desde);
        LocalDateTime fin = finDelDia(hasta);
        Map<String, BigDecimal> mapa = new java.util.HashMap<>();
        ventasDelTenant().stream()
                .filter(v -> v.getFechaHora() != null &&
                        !v.getFechaHora().isBefore(ini) &&
                        !v.getFechaHora().isAfter(fin))
                .forEach(v -> {
                    String nombre = "SIN_TURNO";
                    if (v.getCajaTurno() != null) {
                        String nv = v.getCajaTurno().getNombreVendedor();
                        nombre = (nv != null && !nv.isBlank()) ? nv : (v.getCajaTurno().getUsuario() != null ? v.getCajaTurno().getUsuario() : "SIN_TURNO");
                    }
                    BigDecimal total = v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO;
                    mapa.merge(nombre, total, BigDecimal::add);
                });
        return mapa.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toList());
    }

    public List<CajaTurno> ultimosTurnosCaja(int max) {
        Long tenantId = TenantContext.getTenantId();
        List<CajaTurno> turnos = tenantId != null
                ? cajaTurnoRepository.findByTenantIdOrderByFechaAperturaDesc(tenantId)
                : cajaTurnoRepository.findByOrderByFechaAperturaDesc();
        return turnos.stream()
                .limit(max)
                .collect(Collectors.toList());
    }

    public BigDecimal calcularUtilidadEntre(LocalDate desde, LocalDate hasta) {
        LocalDateTime ini = inicioDelDia(desde);
        LocalDateTime fin = finDelDia(hasta);
        return ventasDelTenant().stream()
                .filter(v -> v.getFechaHora() != null &&
                        !v.getFechaHora().isBefore(ini) &&
                        !v.getFechaHora().isAfter(fin))
                .flatMap(v -> v.getItems().stream())
                .map(item -> {
                    BigDecimal precio = item.getPrecioUnitario();
                    Producto producto = item.getProducto();
                    BigDecimal costo = producto != null && producto.getCostoUnitario() != null
                            ? producto.getCostoUnitario()
                            : BigDecimal.ZERO;
                    BigDecimal margenUnitario = precio.subtract(costo);
                    BigDecimal cantidad = BigDecimal.valueOf(item.getCantidad());
                    BigDecimal descuento = item.getDescuento() != null ? item.getDescuento() : BigDecimal.ZERO;
                    return margenUnitario.multiply(cantidad).subtract(descuento);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<Producto, MargenProductoResumen> margenPorProducto() {
        return margenPorProductoEntre(null, null);
    }

    public Map<Producto, MargenProductoResumen> margenPorProductoEntre(LocalDate desde, LocalDate hasta) {
        List<Venta> ventas = ventasDelTenant();
        final LocalDateTime ini;
        final LocalDateTime fin;
        if (desde != null && hasta != null) {
            ini = inicioDelDia(desde);
            fin = finDelDia(hasta);
        } else {
            ini = null;
            fin = null;
        }
        Map<Long, Producto> productosPorId = productoService.listarTodos().stream()
                .collect(Collectors.toMap(Producto::getId, p -> p));

        Map<Producto, MargenProductoResumen> mapa = new java.util.HashMap<>();

        ventas.stream()
                .filter(v -> {
                    if (ini == null || fin == null) return true;
                    return v.getFechaHora() != null &&
                            !v.getFechaHora().isBefore(ini) &&
                            !v.getFechaHora().isAfter(fin);
                })
                .flatMap(v -> v.getItems().stream())
                .forEach(item -> {
                    Producto p = item.getProducto();
                    if (p == null) return;
                    Producto producto = productosPorId.get(p.getId());
                    if (producto == null) return;
                    MargenProductoResumen res = mapa.computeIfAbsent(producto, k -> new MargenProductoResumen());
                    BigDecimal precio = item.getPrecioUnitario();
                    BigDecimal costo = producto.getCostoUnitario() != null ? producto.getCostoUnitario() : BigDecimal.ZERO;
                    BigDecimal margenUnitario = precio.subtract(costo);
                    BigDecimal cantidad = BigDecimal.valueOf(item.getCantidad());
                    BigDecimal ventaBruta = precio.multiply(cantidad);
                    BigDecimal costoTotal = costo.multiply(cantidad);
                    BigDecimal descuento = item.getDescuento() != null ? item.getDescuento() : BigDecimal.ZERO;
                    BigDecimal margen = margenUnitario.multiply(cantidad).subtract(descuento);

                    res.totalVentas = res.totalVentas.add(ventaBruta.subtract(descuento));
                    res.totalCosto = res.totalCosto.add(costoTotal);
                    res.totalMargen = res.totalMargen.add(margen);
                });

        return mapa;
    }

    public List<Map.Entry<Producto, MargenProductoResumen>> topMargenPorProducto(int max) {
        return margenPorProducto().entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getValue().totalMargen, Comparator.reverseOrder()))
                .limit(max)
                .collect(Collectors.toList());
    }

    public List<Map.Entry<Producto, MargenProductoResumen>> topMargenPorProductoEntre(LocalDate desde, LocalDate hasta, int max) {
        return margenPorProductoEntre(desde, hasta).entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getValue().totalMargen, Comparator.reverseOrder()))
                .limit(max)
                .collect(Collectors.toList());
    }

    public static class MargenProductoResumen {
        public BigDecimal totalVentas = BigDecimal.ZERO;
        public BigDecimal totalCosto = BigDecimal.ZERO;
        public BigDecimal totalMargen = BigDecimal.ZERO;

        public BigDecimal getMargenPorcentaje() {
            if (totalVentas == null || totalVentas.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            return totalMargen.multiply(BigDecimal.valueOf(100)).divide(totalVentas, 2, java.math.RoundingMode.HALF_UP);
        }
    }
}

