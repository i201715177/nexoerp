package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoService;
import com.farmacia.sistema.domain.venta.Venta;
import com.farmacia.sistema.domain.venta.VentaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/web/analytics")
public class AnalyticsWebController {

    private final VentaService ventaService;
    private final ProductoService productoService;

    public AnalyticsWebController(VentaService ventaService, ProductoService productoService) {
        this.ventaService = ventaService;
        this.productoService = productoService;
    }

    @GetMapping
    public String analytics(Model model) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime fin = hoy.atTime(LocalTime.MAX);

        // Ventas mensual últimos 12 meses
        Map<String, BigDecimal> ventasMensuales = new LinkedHashMap<>();
        for (int i = 11; i >= 0; i--) {
            LocalDate inicioMes = hoy.minusMonths(i).withDayOfMonth(1);
            LocalDate finMes = inicioMes.plusMonths(1).minusDays(1);
            List<Venta> ventas = ventaService.listarEntreFechas(inicioMes.atStartOfDay(), finMes.atTime(LocalTime.MAX));
            BigDecimal total = ventas.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            ventasMensuales.put(inicioMes.getMonth().toString().substring(0, 3) + " " + inicioMes.getYear(), total);
        }
        model.addAttribute("ventasMensuales", ventasMensuales);

        // Análisis ABC (30 días)
        List<Venta> ventas30d = ventaService.listarEntreFechas(hoy.minusDays(30).atStartOfDay(), fin);
        Map<String, BigDecimal> ventaPorProducto = ventas30d.stream()
                .flatMap(v -> v.getItems().stream())
                .collect(Collectors.groupingBy(
                        vi -> vi.getProducto().getNombre(),
                        Collectors.reducing(BigDecimal.ZERO, vi -> vi.getSubtotal(), BigDecimal::add)));

        BigDecimal totalVentas = ventaPorProducto.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Map<String, Object>> analisisABC = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        List<Map.Entry<String, BigDecimal>> sorted = ventaPorProducto.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed()).collect(Collectors.toList());
        for (Map.Entry<String, BigDecimal> entry : sorted) {
            acumulado = acumulado.add(entry.getValue());
            double pct = totalVentas.compareTo(BigDecimal.ZERO) > 0
                    ? acumulado.multiply(BigDecimal.valueOf(100)).divide(totalVentas, 1, RoundingMode.HALF_UP).doubleValue() : 0;
            String clasificacion = pct <= 80 ? "A" : pct <= 95 ? "B" : "C";
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("producto", entry.getKey());
            row.put("venta", entry.getValue());
            row.put("porcentajeAcum", pct);
            row.put("clase", clasificacion);
            analisisABC.add(row);
        }
        model.addAttribute("analisisABC", analisisABC);

        // Margen de utilidad por categoría
        List<Producto> productos = productoService.listarTodos();
        Map<String, BigDecimal[]> margenPorCategoria = new LinkedHashMap<>();
        for (Producto p : productos) {
            String cat = p.getCategoria() != null ? p.getCategoria() : "Sin categoría";
            margenPorCategoria.computeIfAbsent(cat, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            margenPorCategoria.get(cat)[0] = margenPorCategoria.get(cat)[0].add(p.getPrecioVenta());
            margenPorCategoria.get(cat)[1] = margenPorCategoria.get(cat)[1].add(p.getCostoUnitario());
        }
        Map<String, Double> margenCategorias = new LinkedHashMap<>();
        margenPorCategoria.forEach((cat, vals) -> {
            if (vals[0].compareTo(BigDecimal.ZERO) > 0) {
                double margen = vals[0].subtract(vals[1]).multiply(BigDecimal.valueOf(100))
                        .divide(vals[0], 1, RoundingMode.HALF_UP).doubleValue();
                margenCategorias.put(cat, margen);
            }
        });
        model.addAttribute("margenCategorias", margenCategorias);

        return "analytics";
    }
}
