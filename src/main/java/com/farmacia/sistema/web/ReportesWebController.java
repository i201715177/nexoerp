package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.compra.CompraService;
import com.farmacia.sistema.domain.compra.CuentaPagar;
import com.farmacia.sistema.domain.finanzas.FinanzasService;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.venta.Venta;
import com.farmacia.sistema.domain.venta.VentaItem;
import com.farmacia.sistema.domain.venta.VentaService;
import com.farmacia.sistema.dto.CuentaCobrarDto;
import com.farmacia.sistema.export.ExcelExportUtil;
import com.farmacia.sistema.export.PdfExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/web/reportes")
@Transactional(readOnly = true)
public class ReportesWebController {

    private final FinanzasService finanzasService;
    private final VentaService ventaService;
    private final CompraService compraService;

    public ReportesWebController(FinanzasService finanzasService, VentaService ventaService, CompraService compraService) {
        this.finanzasService = finanzasService;
        this.ventaService = ventaService;
        this.compraService = compraService;
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

    private static final BigDecimal IGV_RATE = new BigDecimal("0.18");
    private static final String[] MESES = {"", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};

    private String trimestre(int mes) {
        if (mes <= 3) return "Q1";
        if (mes <= 6) return "Q2";
        if (mes <= 9) return "Q3";
        return "Q4";
    }

    @GetMapping("/api/detalle-ventas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> detalleVentasMes(
            @RequestParam(value = "desde", required = false) String desdeStr,
            @RequestParam(value = "hasta", required = false) String hastaStr) {

        LocalDate hoy = LocalDate.now();
        LocalDate desde = (desdeStr == null || desdeStr.isBlank()) ? hoy.withDayOfMonth(1) : LocalDate.parse(desdeStr);
        LocalDate hasta = (hastaStr == null || hastaStr.isBlank()) ? hoy : LocalDate.parse(hastaStr);

        List<Venta> ventas = ventaService.listarVentasEntreFechasConDetalle(desde, hasta);

        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm:ss");

        List<Map<String, Object>> filas = new ArrayList<>();
        BigDecimal totalGeneral = BigDecimal.ZERO;
        BigDecimal totalDescuento = BigDecimal.ZERO;
        BigDecimal totalIgv = BigDecimal.ZERO;
        int totalItems = 0;
        Set<String> vendedoresSet = new LinkedHashSet<>();
        Set<String> categoriasSet = new LinkedHashSet<>();
        Set<String> mesesSet = new LinkedHashSet<>();

        for (Venta v : ventas) {
            String vendedor = "—";
            String cajero = "—";
            if (v.getCajaTurno() != null) {
                if (v.getCajaTurno().getNombreVendedor() != null && !v.getCajaTurno().getNombreVendedor().isBlank()) {
                    vendedor = v.getCajaTurno().getNombreVendedor();
                } else if (v.getCajaTurno().getUsuario() != null) {
                    vendedor = v.getCajaTurno().getUsuario();
                }
                cajero = v.getCajaTurno().getUsuario() != null ? v.getCajaTurno().getUsuario() : "—";
            }
            if (!"—".equals(vendedor)) vendedoresSet.add(vendedor);

            String cliente = v.getNombreClienteVenta() != null ? v.getNombreClienteVenta() : "—";
            if ((cliente.isBlank() || "—".equals(cliente)) && v.getCliente() != null) {
                String n = v.getCliente().getNombres() != null ? v.getCliente().getNombres() : "";
                String a = v.getCliente().getApellidos() != null ? v.getCliente().getApellidos() : "";
                cliente = (n + " " + a).trim();
            }
            if (cliente.isBlank()) cliente = "CLIENTE GENERAL";

            String clienteDoc = "";
            String clienteTipoDoc = "";
            String clienteDireccion = "";
            String tipoCliente = "General";
            if (v.getCliente() != null) {
                clienteDoc = v.getCliente().getNumeroDocumento() != null ? v.getCliente().getNumeroDocumento() : "";
                clienteTipoDoc = v.getCliente().getTipoDocumento() != null ? v.getCliente().getTipoDocumento() : "";
                clienteDireccion = v.getCliente().getDireccion() != null ? v.getCliente().getDireccion() : "";
                if ("RUC".equalsIgnoreCase(clienteTipoDoc)) {
                    tipoCliente = "Empresa";
                } else if ("DNI".equalsIgnoreCase(clienteTipoDoc)) {
                    tipoCliente = "Persona Natural";
                }
            }

            String fecha = v.getFechaHora() != null ? v.getFechaHora().format(fmtFecha) : "—";
            String hora = v.getFechaHora() != null ? v.getFechaHora().format(fmtHora) : "—";
            int anio = v.getFechaHora() != null ? v.getFechaHora().getYear() : 0;
            int mes = v.getFechaHora() != null ? v.getFechaHora().getMonthValue() : 0;
            String mesNombre = mes > 0 && mes <= 12 ? MESES[mes] : "—";
            String trim = mes > 0 ? trimestre(mes) : "—";
            if (mes > 0) mesesSet.add(mesNombre);

            int horaNum = v.getFechaHora() != null ? v.getFechaHora().getHour() : 0;
            String turno = horaNum < 12 ? "Mañana" : horaNum < 18 ? "Tarde" : "Noche";

            String serie = v.getSerieComprobante() != null ? v.getSerieComprobante() : "—";
            String correlativo = v.getNumeroComprobante() != null ? v.getNumeroComprobante() : "—";
            String comprobante = serie + "-" + correlativo;
            String tipoComp = v.getTipoComprobante() != null ? ("FAC".equals(v.getTipoComprobante()) ? "Factura" : "Boleta") : "—";
            String estado = v.getEstado() != null ? v.getEstado() : "EMITIDA";

            String canal;
            String puntoVenta;
            if (v.getCajaTurno() != null && v.getCajaTurno().getSucursal() != null) {
                canal = "Sucursal";
                puntoVenta = v.getCajaTurno().getSucursal().getNombre();
            } else {
                canal = "Central";
                puntoVenta = "Caja Central";
            }
            String cajaId = v.getCajaTurno() != null ? "Caja #" + v.getCajaTurno().getId() : "—";

            BigDecimal totalVenta = v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO;
            BigDecimal pagado = BigDecimal.ZERO;
            if (v.getPagos() != null) {
                for (var p : v.getPagos()) {
                    if (p.getMonto() != null) pagado = pagado.add(p.getMonto());
                }
            }
            String estadoPago;
            if (pagado.compareTo(totalVenta) >= 0) {
                estadoPago = "Pagado";
            } else if (pagado.compareTo(BigDecimal.ZERO) > 0) {
                estadoPago = "Parcial";
            } else {
                estadoPago = "Pendiente";
            }

            String observaciones = v.getCajaTurno() != null && v.getCajaTurno().getObservaciones() != null
                    ? v.getCajaTurno().getObservaciones() : "";

            if (v.getItems() != null) {
                for (VentaItem item : v.getItems()) {
                    BigDecimal sub = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
                    BigDecimal igvItem = sub.multiply(IGV_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
                    BigDecimal totalConIgv = sub.add(igvItem);
                    String cat = item.getProducto() != null && item.getProducto().getCategoria() != null
                            ? item.getProducto().getCategoria() : "Sin categoría";
                    categoriasSet.add(cat);
                    String unidadMedida = item.getProducto() != null && item.getProducto().getUnidadMedida() != null
                            ? item.getProducto().getUnidadMedida() : "UND";

                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("ventaId", v.getId());
                    fila.put("fecha", fecha);
                    fila.put("hora", hora);
                    fila.put("anio", anio);
                    fila.put("mes", mesNombre);
                    fila.put("trimestre", trim);
                    fila.put("turno", turno);
                    fila.put("clienteDoc", clienteDoc);
                    fila.put("clienteTipoDoc", clienteTipoDoc);
                    fila.put("cliente", cliente);
                    fila.put("tipoCliente", tipoCliente);
                    fila.put("clienteDireccion", clienteDireccion);
                    fila.put("productoId", item.getProducto() != null ? item.getProducto().getId() : 0);
                    fila.put("producto", item.getProducto() != null ? item.getProducto().getNombre() : "?");
                    fila.put("categoria", cat);
                    fila.put("unidadMedida", unidadMedida);
                    fila.put("cantidad", item.getCantidad());
                    fila.put("precioUnit", item.getPrecioUnitario() != null ? item.getPrecioUnitario().toPlainString() : "0");
                    fila.put("subtotal", sub.toPlainString());
                    fila.put("descuento", item.getDescuento() != null ? item.getDescuento().toPlainString() : "0");
                    fila.put("igv", igvItem.toPlainString());
                    fila.put("totalItem", totalConIgv.toPlainString());
                    fila.put("serie", serie);
                    fila.put("correlativo", correlativo);
                    fila.put("nroComprobante", comprobante);
                    fila.put("tipoComprobante", tipoComp);
                    fila.put("medioPago", v.getPagosResumen());
                    fila.put("estadoPago", estadoPago);
                    fila.put("vendedor", vendedor);
                    fila.put("cajero", cajero);
                    fila.put("puntoVenta", puntoVenta);
                    fila.put("cajaId", cajaId);
                    fila.put("canal", canal);
                    fila.put("estado", estado);
                    fila.put("observaciones", observaciones);
                    filas.add(fila);
                    totalItems++;
                    totalDescuento = totalDescuento.add(item.getDescuento() != null ? item.getDescuento() : BigDecimal.ZERO);
                    totalIgv = totalIgv.add(igvItem);
                }
            }
            totalGeneral = totalGeneral.add(totalVenta);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("desde", desde.format(fmtFecha));
        resp.put("hasta", hasta.format(fmtFecha));
        resp.put("totalVentas", ventas.size());
        resp.put("totalItems", totalItems);
        resp.put("totalGeneral", totalGeneral.toPlainString());
        resp.put("totalDescuento", totalDescuento.toPlainString());
        resp.put("totalIgv", totalIgv.toPlainString());
        resp.put("vendedores", new ArrayList<>(vendedoresSet));
        resp.put("categorias", new ArrayList<>(categoriasSet));
        resp.put("meses", new ArrayList<>(mesesSet));
        resp.put("detalle", filas);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/api/detalle-utilidad")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> detalleUtilidad(
            @RequestParam(value = "desde", required = false) String desdeStr,
            @RequestParam(value = "hasta", required = false) String hastaStr) {

        LocalDate hoy = LocalDate.now();
        LocalDate desde = (desdeStr == null || desdeStr.isBlank()) ? hoy.minusDays(30) : LocalDate.parse(desdeStr);
        LocalDate hasta = (hastaStr == null || hastaStr.isBlank()) ? hoy : LocalDate.parse(hastaStr);

        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Map<Producto, FinanzasService.MargenProductoResumen> mapa = finanzasService.margenPorProductoEntre(desde, hasta);

        List<Map<String, Object>> filas = new ArrayList<>();
        BigDecimal totalVentas = BigDecimal.ZERO;
        BigDecimal totalCosto = BigDecimal.ZERO;
        BigDecimal totalMargen = BigDecimal.ZERO;
        BigDecimal totalIgvUtil = BigDecimal.ZERO;
        Set<String> categoriasUtil = new LinkedHashSet<>();

        for (Map.Entry<Producto, FinanzasService.MargenProductoResumen> e : mapa.entrySet()) {
            Producto p = e.getKey();
            FinanzasService.MargenProductoResumen r = e.getValue();
            BigDecimal igvProd = r.totalVentas.multiply(IGV_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
            String cat = p.getCategoria() != null ? p.getCategoria() : "Sin categoría";
            categoriasUtil.add(cat);

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("productoId", p.getId());
            fila.put("codigo", p.getCodigo() != null ? p.getCodigo() : "—");
            fila.put("producto", p.getNombre());
            fila.put("categoria", cat);
            fila.put("presentacion", p.getPresentacion() != null ? p.getPresentacion() : "—");
            fila.put("unidadMedida", p.getUnidadMedida() != null ? p.getUnidadMedida() : "UND");
            fila.put("marca", p.getMarca() != null ? p.getMarca() : "—");
            fila.put("laboratorio", p.getLaboratorio() != null ? p.getLaboratorio() : "—");
            fila.put("precioVenta", p.getPrecioVenta() != null ? p.getPrecioVenta().toPlainString() : "0");
            fila.put("costoUnit", p.getCostoUnitario() != null ? p.getCostoUnitario().toPlainString() : "0");
            fila.put("stockActual", p.getStockActual() != null ? p.getStockActual() : 0);
            fila.put("totalVentas", r.totalVentas.toPlainString());
            fila.put("totalCosto", r.totalCosto.toPlainString());
            fila.put("totalMargen", r.totalMargen.toPlainString());
            fila.put("igv", igvProd.toPlainString());
            fila.put("ventaConIgv", r.totalVentas.add(igvProd).toPlainString());
            fila.put("margenPct", r.getMargenPorcentaje().toPlainString());
            filas.add(fila);
            totalVentas = totalVentas.add(r.totalVentas);
            totalCosto = totalCosto.add(r.totalCosto);
            totalMargen = totalMargen.add(r.totalMargen);
            totalIgvUtil = totalIgvUtil.add(igvProd);
        }

        filas.sort((a, b) -> new BigDecimal(b.get("totalMargen").toString()).compareTo(new BigDecimal(a.get("totalMargen").toString())));

        BigDecimal margenPctGlobal = totalVentas.compareTo(BigDecimal.ZERO) > 0
                ? totalMargen.multiply(BigDecimal.valueOf(100)).divide(totalVentas, 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("desde", desde.format(fmtFecha));
        resp.put("hasta", hasta.format(fmtFecha));
        resp.put("totalProductos", filas.size());
        resp.put("totalVentas", totalVentas.toPlainString());
        resp.put("totalCosto", totalCosto.toPlainString());
        resp.put("totalMargen", totalMargen.toPlainString());
        resp.put("totalIgv", totalIgvUtil.toPlainString());
        resp.put("margenPctGlobal", margenPctGlobal.toPlainString());
        resp.put("categorias", new ArrayList<>(categoriasUtil));
        resp.put("detalle", filas);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/api/detalle-cxp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> detalleCuentasPorPagar() {
        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<CuentaPagar> cuentas = compraService.listarCuentasPendientes();

        List<Map<String, Object>> filas = new ArrayList<>();
        BigDecimal totalPendiente = BigDecimal.ZERO;
        BigDecimal totalMonto = BigDecimal.ZERO;
        BigDecimal totalPagado = BigDecimal.ZERO;
        int vencidas = 0;

        for (CuentaPagar cp : cuentas) {
            BigDecimal monto = cp.getMontoTotal() != null ? cp.getMontoTotal() : BigDecimal.ZERO;
            BigDecimal saldo = cp.getSaldoPendiente() != null ? cp.getSaldoPendiente() : BigDecimal.ZERO;
            BigDecimal pagadoCxp = monto.subtract(saldo);
            BigDecimal igvCxp = monto.multiply(IGV_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
            boolean vencida = cp.getFechaVencimiento() != null && cp.getFechaVencimiento().isBefore(LocalDate.now());
            if (vencida) vencidas++;

            long diasVenc = 0;
            if (cp.getFechaVencimiento() != null) {
                diasVenc = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), cp.getFechaVencimiento());
            }

            String tipoDocProv = "";
            String rucProv = "";
            String provNombre = "";
            String provContacto = "";
            String provTelefono = "";
            String provEmail = "";
            String provDireccion = "";
            if (cp.getProveedor() != null) {
                provNombre = cp.getProveedor().getRazonSocial() != null ? cp.getProveedor().getRazonSocial() : "—";
                rucProv = cp.getProveedor().getNumeroDocumento() != null ? cp.getProveedor().getNumeroDocumento() : "—";
                tipoDocProv = cp.getProveedor().getTipoDocumento() != null ? cp.getProveedor().getTipoDocumento() : "";
                provContacto = cp.getProveedor().getContacto() != null ? cp.getProveedor().getContacto() : "";
                provTelefono = cp.getProveedor().getTelefono() != null ? cp.getProveedor().getTelefono() : "";
                provEmail = cp.getProveedor().getEmail() != null ? cp.getProveedor().getEmail() : "";
                provDireccion = cp.getProveedor().getDireccion() != null ? cp.getProveedor().getDireccion() : "";
            }

            String ocNumero = "";
            String ocEstado = "";
            String ocRef = "";
            if (cp.getOrdenCompra() != null) {
                ocNumero = "OC #" + cp.getOrdenCompra().getId();
                ocEstado = cp.getOrdenCompra().getEstado() != null ? cp.getOrdenCompra().getEstado() : "";
                ocRef = cp.getOrdenCompra().getNumeroDocumento() != null ? cp.getOrdenCompra().getNumeroDocumento() : "";
            }

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", cp.getId());
            fila.put("tipoDocProv", tipoDocProv);
            fila.put("rucProveedor", rucProv);
            fila.put("proveedor", provNombre);
            fila.put("contacto", provContacto);
            fila.put("telefono", provTelefono);
            fila.put("email", provEmail);
            fila.put("direccion", provDireccion);
            fila.put("ordenCompra", ocNumero);
            fila.put("ocEstado", ocEstado);
            fila.put("ocReferencia", ocRef);
            fila.put("fechaEmision", cp.getFechaEmision() != null ? cp.getFechaEmision().format(fmtFecha) : "—");
            fila.put("fechaVencimiento", cp.getFechaVencimiento() != null ? cp.getFechaVencimiento().format(fmtFecha) : "—");
            fila.put("diasVencimiento", diasVenc);
            fila.put("montoTotal", monto.toPlainString());
            fila.put("igv", igvCxp.toPlainString());
            fila.put("montoConIgv", monto.add(igvCxp).toPlainString());
            fila.put("pagado", pagadoCxp.toPlainString());
            fila.put("saldoPendiente", saldo.toPlainString());
            fila.put("estado", cp.getEstado() != null ? cp.getEstado() : "PENDIENTE");
            fila.put("vencida", vencida);
            fila.put("observaciones", cp.getObservaciones() != null ? cp.getObservaciones() : "");
            filas.add(fila);
            totalMonto = totalMonto.add(monto);
            totalPagado = totalPagado.add(pagadoCxp);
            totalPendiente = totalPendiente.add(saldo);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("totalCuentas", cuentas.size());
        resp.put("totalMonto", totalMonto.toPlainString());
        resp.put("totalPagado", totalPagado.toPlainString());
        resp.put("totalPendiente", totalPendiente.toPlainString());
        resp.put("cuentasVencidas", vencidas);
        resp.put("detalle", filas);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/api/detalle-cxc")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> detalleCuentasPorCobrar() {
        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm:ss");

        List<Venta> ventasPend = finanzasService.ventasConSaldoPendiente();

        List<Map<String, Object>> filas = new ArrayList<>();
        BigDecimal totalSaldo = BigDecimal.ZERO;
        BigDecimal totalVentaCxc = BigDecimal.ZERO;
        BigDecimal totalPagadoCxc = BigDecimal.ZERO;

        for (Venta v : ventasPend) {
            BigDecimal totalV = v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO;
            BigDecimal pagadoV = v.getPagos() != null
                    ? v.getPagos().stream().map(p -> p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add)
                    : BigDecimal.ZERO;
            BigDecimal saldoV = totalV.subtract(pagadoV);
            BigDecimal igvCxc = totalV.multiply(IGV_RATE).setScale(2, java.math.RoundingMode.HALF_UP);

            String cliente = v.getNombreClienteVenta() != null ? v.getNombreClienteVenta() : "";
            String clienteDoc = "";
            String clienteTipoDoc = "";
            String tipoCliente = "General";
            String clienteDireccion = "";
            if (v.getCliente() != null) {
                String n = v.getCliente().getNombres() != null ? v.getCliente().getNombres() : "";
                String a = v.getCliente().getApellidos() != null ? v.getCliente().getApellidos() : "";
                if (!n.isBlank() || !a.isBlank()) cliente = (n + " " + a).trim();
                clienteDoc = v.getCliente().getNumeroDocumento() != null ? v.getCliente().getNumeroDocumento() : "";
                clienteTipoDoc = v.getCliente().getTipoDocumento() != null ? v.getCliente().getTipoDocumento() : "";
                clienteDireccion = v.getCliente().getDireccion() != null ? v.getCliente().getDireccion() : "";
                if ("RUC".equalsIgnoreCase(clienteTipoDoc)) tipoCliente = "Empresa";
                else if ("DNI".equalsIgnoreCase(clienteTipoDoc)) tipoCliente = "Persona Natural";
            }
            if (cliente.isBlank()) cliente = "CLIENTE GENERAL";

            String fecha = v.getFechaHora() != null ? v.getFechaHora().format(fmtFecha) : "—";
            String hora = v.getFechaHora() != null ? v.getFechaHora().format(fmtHora) : "—";
            String tipoComp = v.getTipoComprobante() != null ? ("FAC".equals(v.getTipoComprobante()) ? "Factura" : "Boleta") : "—";
            String comprobante = (v.getSerieComprobante() != null ? v.getSerieComprobante() : "") +
                    "-" + (v.getNumeroComprobante() != null ? v.getNumeroComprobante() : "");
            String medioPago = v.getPagosResumen();

            long diasDeuda = v.getFechaHora() != null
                    ? java.time.temporal.ChronoUnit.DAYS.between(v.getFechaHora().toLocalDate(), LocalDate.now()) : 0;
            String antigüedad = diasDeuda <= 7 ? "0-7 días" : diasDeuda <= 30 ? "8-30 días" : diasDeuda <= 60 ? "31-60 días" : "60+ días";

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("ventaId", v.getId());
            fila.put("fecha", fecha);
            fila.put("hora", hora);
            fila.put("clienteDoc", clienteDoc);
            fila.put("clienteTipoDoc", clienteTipoDoc);
            fila.put("cliente", cliente);
            fila.put("tipoCliente", tipoCliente);
            fila.put("clienteDireccion", clienteDireccion);
            fila.put("tipoComprobante", tipoComp);
            fila.put("comprobante", comprobante);
            fila.put("total", totalV.toPlainString());
            fila.put("igv", igvCxc.toPlainString());
            fila.put("totalConIgv", totalV.add(igvCxc).toPlainString());
            fila.put("pagado", pagadoV.toPlainString());
            fila.put("saldo", saldoV.toPlainString());
            fila.put("medioPago", medioPago);
            fila.put("diasDeuda", diasDeuda);
            fila.put("antiguedad", antigüedad);
            fila.put("estado", v.getEstado() != null ? v.getEstado() : "EMITIDA");
            filas.add(fila);
            totalVentaCxc = totalVentaCxc.add(totalV);
            totalPagadoCxc = totalPagadoCxc.add(pagadoV);
            totalSaldo = totalSaldo.add(saldoV);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("totalCuentas", ventasPend.size());
        resp.put("totalVentas", totalVentaCxc.toPlainString());
        resp.put("totalPagado", totalPagadoCxc.toPlainString());
        resp.put("totalSaldo", totalSaldo.toPlainString());
        resp.put("detalle", filas);
        return ResponseEntity.ok(resp);
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

