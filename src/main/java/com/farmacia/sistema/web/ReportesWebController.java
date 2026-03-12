package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.finanzas.FinanzasService;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.export.ExcelExportUtil;
import com.farmacia.sistema.export.PdfExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/web/reportes")
public class ReportesWebController {

    private final FinanzasService finanzasService;

    public ReportesWebController(FinanzasService finanzasService) {
        this.finanzasService = finanzasService;
    }

    @GetMapping
    public String dashboard(@RequestParam(value = "desde", required = false) String desdeStr,
                            @RequestParam(value = "hasta", required = false) String hastaStr,
                            Model model) {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = (desdeStr == null || desdeStr.isBlank()) ? hoy.minusDays(30) : LocalDate.parse(desdeStr);
        LocalDate hasta = (hastaStr == null || hastaStr.isBlank()) ? hoy : LocalDate.parse(hastaStr);

        ReportData data = construirDatos(desde, hasta);

        // Métricas BI adicionales
        BigDecimal ventasDia = finanzasService.totalVentasEntre(hoy, hoy);
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        BigDecimal ventasMes = finanzasService.totalVentasEntre(inicioMes, hoy);
        LocalDate inicioAnio = hoy.withDayOfYear(1);
        BigDecimal ventasAnio = finanzasService.totalVentasEntre(inicioAnio, hoy);

        model.addAttribute("desde", data.desde());
        model.addAttribute("hasta", data.hasta());
        model.addAttribute("ventasPeriodo", data.ventasPeriodo());
        model.addAttribute("utilidadPeriodo", data.utilidadPeriodo());
        model.addAttribute("cxpPendiente", data.cxpPendiente());
        model.addAttribute("cxcPendiente", data.cxcPendiente());
        model.addAttribute("masVendidos", data.masVendidos());
        model.addAttribute("sinRotacion", data.sinRotacion());
        model.addAttribute("rankingVendedores", data.rankingVendedores());
        model.addAttribute("margenProductos", data.margenProductos());
        model.addAttribute("ventasDia", ventasDia);
        model.addAttribute("ventasMes", ventasMes);
        model.addAttribute("ventasAnio", ventasAnio);

        return "reportes";
    }

    @GetMapping("/excel")
    public void exportarExcel(@RequestParam(value = "desde", required = false) String desdeStr,
                              @RequestParam(value = "hasta", required = false) String hastaStr,
                              HttpServletResponse response) throws IOException {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = (desdeStr == null || desdeStr.isBlank()) ? hoy.minusDays(30) : LocalDate.parse(desdeStr);
        LocalDate hasta = (hastaStr == null || hastaStr.isBlank()) ? hoy : LocalDate.parse(hastaStr);

        ReportData data = construirDatos(desde, hasta);
        String subtitulo = String.format("Período: %s a %s", desde, hasta);

        XSSFWorkbook wb = new XSSFWorkbook();

        String[] hdrResumen = {"Métrica", "Valor (S/)"};
        List<Object[]> filasResumen = new ArrayList<>();
        filasResumen.add(new Object[]{"Ventas en el período", data.ventasPeriodo()});
        filasResumen.add(new Object[]{"Utilidad bruta", data.utilidadPeriodo()});
        filasResumen.add(new Object[]{"Cuentas por pagar", data.cxpPendiente()});
        filasResumen.add(new Object[]{"Cuentas por cobrar", data.cxcPendiente()});
        ExcelExportUtil.agregarHoja(wb, "Resumen", "Reporte Gerencial", subtitulo, hdrResumen, filasResumen);

        String[] hdrVendidos = {"Producto", "Cantidad Vendida"};
        List<Object[]> filasVendidos = new ArrayList<>();
        for (Map.Entry<Producto, Long> e : data.masVendidos()) {
            filasVendidos.add(new Object[]{e.getKey().getNombre(), e.getValue()});
        }
        ExcelExportUtil.agregarHoja(wb, "Más Vendidos", "Productos Más Vendidos", subtitulo, hdrVendidos, filasVendidos);

        String[] hdrSin = {"Producto", "Presentación", "Categoría"};
        List<Object[]> filasSin = new ArrayList<>();
        for (Producto p : data.sinRotacion()) {
            filasSin.add(new Object[]{p.getNombre(), p.getPresentacion(), p.getCategoria()});
        }
        ExcelExportUtil.agregarHoja(wb, "Sin Rotación", "Productos Sin Rotación", subtitulo, hdrSin, filasSin);

        String[] hdrRanking = {"Vendedor", "Ventas (S/)"};
        List<Object[]> filasRanking = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : data.rankingVendedores()) {
            filasRanking.add(new Object[]{e.getKey(), e.getValue()});
        }
        ExcelExportUtil.agregarHoja(wb, "Ranking Vendedores", "Ranking de Vendedores", subtitulo, hdrRanking, filasRanking);

        String[] hdrMargen = {"Producto", "Ventas", "Costo", "Margen", "% Margen"};
        List<Object[]> filasMargen = new ArrayList<>();
        for (Map.Entry<Producto, FinanzasService.MargenProductoResumen> e : data.margenProductos()) {
            filasMargen.add(new Object[]{
                    e.getKey().getNombre(), e.getValue().totalVentas,
                    e.getValue().totalCosto, e.getValue().totalMargen,
                    e.getValue().getMargenPorcentaje()
            });
        }
        ExcelExportUtil.agregarHoja(wb, "Margen Productos", "Utilidad por Producto", subtitulo, hdrMargen, filasMargen);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=reportes_" + desde + "_" + hasta + ".xlsx");
        wb.write(response.getOutputStream());
        wb.close();
    }

    @GetMapping("/pdf")
    public void exportarPdf(@RequestParam(value = "desde", required = false) String desdeStr,
                            @RequestParam(value = "hasta", required = false) String hastaStr,
                            HttpServletResponse response) throws IOException {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = (desdeStr == null || desdeStr.isBlank()) ? hoy.minusDays(30) : LocalDate.parse(desdeStr);
        LocalDate hasta = (hastaStr == null || hastaStr.isBlank()) ? hoy : LocalDate.parse(hastaStr);

        ReportData data = construirDatos(desde, hasta);

        String[] hdrResumen = {"Métrica", "Valor (S/)"};
        List<Object[]> filasResumen = new ArrayList<>();
        filasResumen.add(new Object[]{"Ventas en el período", data.ventasPeriodo()});
        filasResumen.add(new Object[]{"Utilidad bruta", data.utilidadPeriodo()});
        filasResumen.add(new Object[]{"Cuentas por pagar", data.cxpPendiente()});
        filasResumen.add(new Object[]{"Cuentas por cobrar", data.cxcPendiente()});
        for (Map.Entry<Producto, Long> e : data.masVendidos()) {
            filasResumen.add(new Object[]{"Top vendido: " + e.getKey().getNombre(), e.getValue()});
        }
        for (Map.Entry<String, BigDecimal> e : data.rankingVendedores()) {
            filasResumen.add(new Object[]{"Vendedor: " + e.getKey(), e.getValue()});
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=reportes_" + desde + "_" + hasta + ".pdf");
        PdfExportUtil.crearReporte(response.getOutputStream(),
                "Reporte Gerencial NexoERP",
                "Período: " + desde + " a " + hasta,
                hdrResumen, filasResumen, new float[]{3, 2});
    }

    private ReportData construirDatos(LocalDate desde, LocalDate hasta) {
        BigDecimal ventasPeriodo = finanzasService.totalVentasEntre(desde, hasta);
        BigDecimal utilidadPeriodo = finanzasService.calcularUtilidadEntre(desde, hasta);
        BigDecimal cxpPendiente = finanzasService.totalCuentasPorPagarPendientes();
        BigDecimal cxcPendiente = finanzasService.totalCuentasPorCobrar();

        List<Map.Entry<Producto, Long>> masVendidos = finanzasService.productosMasVendidos(desde, hasta, 5);
        List<Producto> sinRotacion = finanzasService.productosSinRotacion(desde, hasta);
        List<Map.Entry<String, BigDecimal>> rankingVendedores = finanzasService.rankingVendedores(desde, hasta);
        List<Map.Entry<Producto, FinanzasService.MargenProductoResumen>> margenProductos =
                finanzasService.topMargenPorProductoEntre(desde, hasta, 5);

        return new ReportData(desde, hasta, ventasPeriodo, utilidadPeriodo, cxpPendiente, cxcPendiente,
                masVendidos, sinRotacion, rankingVendedores, margenProductos);
    }

    private record ReportData(LocalDate desde,
                              LocalDate hasta,
                              BigDecimal ventasPeriodo,
                              BigDecimal utilidadPeriodo,
                              BigDecimal cxpPendiente,
                              BigDecimal cxcPendiente,
                              List<Map.Entry<Producto, Long>> masVendidos,
                              List<Producto> sinRotacion,
                              List<Map.Entry<String, BigDecimal>> rankingVendedores,
                              List<Map.Entry<Producto, FinanzasService.MargenProductoResumen>> margenProductos) {
    }
}

