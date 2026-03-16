package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.cotizacion.CotizacionService;
import com.farmacia.sistema.domain.inventario.InventarioService;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoService;
import com.farmacia.sistema.domain.reclamacion.ReclamacionService;
import com.farmacia.sistema.domain.venta.Venta;
import com.farmacia.sistema.domain.venta.VentaService;
import com.farmacia.sistema.tenant.TenantContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/web/dashboard")
public class DashboardWebController {

    private final VentaService ventaService;
    private final ProductoService productoService;
    private final InventarioService inventarioService;
    private final CotizacionService cotizacionService;
    private final ReclamacionService reclamacionService;

    public DashboardWebController(VentaService ventaService, ProductoService productoService,
                                   InventarioService inventarioService,
                                   CotizacionService cotizacionService,
                                   ReclamacionService reclamacionService) {
        this.ventaService = ventaService;
        this.productoService = productoService;
        this.inventarioService = inventarioService;
        this.cotizacionService = cotizacionService;
        this.reclamacionService = reclamacionService;
    }

    @GetMapping
    public String dashboard(Model model) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime finHoy = hoy.atTime(LocalTime.MAX);
        LocalDateTime inicio7d = hoy.minusDays(7).atStartOfDay();
        LocalDateTime inicio30d = hoy.minusDays(30).atStartOfDay();

        List<Venta> ventasHoy = ventaService.listarEntreFechas(inicioHoy, finHoy);
        List<Venta> ventas7d = ventaService.listarEntreFechas(inicio7d, finHoy);
        List<Venta> ventas30d = ventaService.listarEntreFechas(inicio30d, finHoy);

        BigDecimal totalHoy = ventasHoy.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total7d = ventas7d.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total30d = ventas30d.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("ventasHoyCount", ventasHoy.size());
        model.addAttribute("totalHoy", totalHoy);
        model.addAttribute("ventas7dCount", ventas7d.size());
        model.addAttribute("total7d", total7d);
        model.addAttribute("ventas30dCount", ventas30d.size());
        model.addAttribute("total30d", total30d);

        List<Producto> productos = productoService.listarTodos();
        long stockBajo = productos.stream().filter(p -> p.isActivo() && p.getStockActual() <= p.getStockMinimo()).count();
        model.addAttribute("stockBajo", stockBajo);

        LocalDate en30d = hoy.plusDays(30);
        LocalDate en60d = hoy.plusDays(60);
        LocalDate en90d = hoy.plusDays(90);
        long porVencer30 = productos.stream().filter(p -> p.getFechaVencimiento() != null && !p.getFechaVencimiento().isBefore(hoy) && !p.getFechaVencimiento().isAfter(en30d)).count();
        long porVencer60 = productos.stream().filter(p -> p.getFechaVencimiento() != null && !p.getFechaVencimiento().isBefore(hoy) && !p.getFechaVencimiento().isAfter(en60d)).count();
        long porVencer90 = productos.stream().filter(p -> p.getFechaVencimiento() != null && !p.getFechaVencimiento().isBefore(hoy) && !p.getFechaVencimiento().isAfter(en90d)).count();
        long vencidos = productos.stream().filter(p -> p.getFechaVencimiento() != null && p.getFechaVencimiento().isBefore(hoy)).count();
        model.addAttribute("porVencer30", porVencer30);
        model.addAttribute("porVencer60", porVencer60);
        model.addAttribute("porVencer90", porVencer90);
        model.addAttribute("vencidos", vencidos);
        model.addAttribute("totalProductos", productos.size());

        // Top 10 productos vendidos (últimos 30 días)
        Map<String, Integer> topProductos = new LinkedHashMap<>();
        ventas30d.stream()
                .flatMap(v -> v.getItems().stream())
                .collect(Collectors.groupingBy(
                        vi -> vi.getProducto().getNombre(),
                        Collectors.summingInt(vi -> vi.getCantidad())))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> topProductos.put(e.getKey(), e.getValue()));
        model.addAttribute("topProductos", topProductos);

        // Ventas diarias últimos 7 días para gráfico
        Map<String, BigDecimal> ventasDiarias = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = hoy.minusDays(i);
            BigDecimal totalDia = ventas7d.stream()
                    .filter(v -> v.getFechaHora().toLocalDate().equals(dia))
                    .map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            ventasDiarias.put(dia.toString(), totalDia);
        }
        model.addAttribute("ventasDiarias", ventasDiarias);

        model.addAttribute("cotizacionesPendientes", cotizacionService.countPendientes());
        model.addAttribute("reclamacionesPendientes", reclamacionService.countPendientes());

        List<Producto> productosStockBajo = productos.stream()
                .filter(p -> p.isActivo() && p.getStockActual() <= p.getStockMinimo())
                .sorted(Comparator.comparingInt(Producto::getStockActual))
                .limit(10)
                .collect(Collectors.toList());
        model.addAttribute("productosStockBajo", productosStockBajo);

        List<Producto> productosProxVencer = productos.stream()
                .filter(p -> p.getFechaVencimiento() != null && !p.getFechaVencimiento().isBefore(hoy) && !p.getFechaVencimiento().isAfter(en90d))
                .sorted(Comparator.comparing(Producto::getFechaVencimiento))
                .limit(10)
                .collect(Collectors.toList());
        model.addAttribute("productosProxVencer", productosProxVencer);

        return "dashboard";
    }
}
