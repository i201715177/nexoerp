package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.empresa.Empresa;
import com.farmacia.sistema.domain.empresa.EmpresaService;
import com.farmacia.sistema.domain.empresa.FacturaSaaS;
import com.farmacia.sistema.domain.empresa.FacturaSaaSService;
import com.farmacia.sistema.export.ExcelExportUtil;
import com.farmacia.sistema.export.FacturaSaaSPdfUtil;
import com.farmacia.sistema.export.PdfExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/web/admin/facturacion")
public class FacturacionController {

    private final FacturaSaaSService facturaSaaSService;
    private final EmpresaService empresaService;
    private final com.farmacia.sistema.domain.auditoria.AuditoriaService auditoriaService;
    private final String emisorNombre;
    private final String emisorRuc;
    private final String emisorDireccion;

    public FacturacionController(FacturaSaaSService facturaSaaSService,
                                EmpresaService empresaService,
                                com.farmacia.sistema.domain.auditoria.AuditoriaService auditoriaService,
                                @Value("${app.saas.emisor.nombre:NexoERP}") String emisorNombre,
                                @Value("${app.saas.emisor.ruc:}") String emisorRuc,
                                @Value("${app.saas.emisor.direccion:}") String emisorDireccion) {
        this.facturaSaaSService = facturaSaaSService;
        this.empresaService = empresaService;
        this.auditoriaService = auditoriaService;
        this.emisorNombre = emisorNombre;
        this.emisorRuc = emisorRuc;
        this.emisorDireccion = emisorDireccion;
    }

    @GetMapping
    public String listar(Model model,
                         @RequestParam(value = "empresaId", required = false) Long empresaId) {
        List<FacturaSaaS> facturas = empresaId != null
                ? facturaSaaSService.listarPorEmpresa(empresaId)
                : facturaSaaSService.listarTodas();
        List<Empresa> empresas = empresaService.listarTodasConPlan();

        long vencidas = facturaSaaSService.countPendientesVencidas();
        long porVencer = facturaSaaSService.countPendientesPorVencer(7);

        model.addAttribute("facturas", facturas);
        model.addAttribute("empresas", empresas);
        model.addAttribute("empresaFiltroId", empresaId);
        model.addAttribute("facturasVencidasCount", vencidas);
        model.addAttribute("facturasPorVencerCount", porVencer);
        return "saas-facturacion";
    }

    @PostMapping("/generar")
    public String generarFactura(@RequestParam("empresaId") Long empresaId,
                                 RedirectAttributes ra) {
        try {
            FacturaSaaS f = facturaSaaSService.generarSiguientePeriodo(empresaId);
            Empresa e = empresaService.obtenerPorId(empresaId);
            auditoriaService.registrarCreacion("factura_saas",
                    "Empresa: " + e.getNombre(),
                    "Periodo " + f.getPeriodoDesde() + " a " + f.getPeriodoHasta() + " | " + f.getMonto());
            ra.addFlashAttribute("mensaje", "Factura " + (f.getNumeroFactura() != null ? f.getNumeroFactura() + " " : "") + "generada: " + f.getPeriodoDesde() + " - " + f.getPeriodoHasta() + " (" + f.getMonto() + ").");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/web/admin/facturacion";
    }

    @PostMapping("/pagar")
    public String marcarComoPagada(@RequestParam("facturaId") Long facturaId,
                                   @RequestParam(value = "fechaPago", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaPago,
                                   RedirectAttributes ra) {
        try {
            FacturaSaaS f = facturaSaaSService.marcarComoPagada(facturaId, fechaPago);
            auditoriaService.registrarAccion("PUT", "Factura marcada como pagada",
                    "ID " + facturaId + " | Empresa: " + f.getEmpresa().getNombre() + " | Fecha pago: " + f.getFechaPago());
            ra.addFlashAttribute("mensaje", "Factura marcada como pagada.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/web/admin/facturacion";
    }

    @PostMapping("/cancelar")
    public String cancelar(@RequestParam("facturaId") Long facturaId,
                           RedirectAttributes ra) {
        try {
            FacturaSaaS f = facturaSaaSService.cancelar(facturaId);
            auditoriaService.registrarAccion("PUT", "Factura cancelada",
                    "ID " + facturaId + " | Empresa: " + f.getEmpresa().getNombre());
            ra.addFlashAttribute("mensaje", "Factura cancelada.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/web/admin/facturacion";
    }

    @GetMapping("/comprobante")
    public void imprimirComprobante(@RequestParam("facturaId") Long facturaId,
                                    HttpServletResponse response) {
        try {
            FacturaSaaS f = facturaSaaSService.obtenerPorIdConEmpresaYPlan(facturaId);
            response.setContentType("application/pdf");
            String nombreArchivo = "comprobante_saas_" + (f.getNumeroFactura() != null ? f.getNumeroFactura() : f.getId()) + ".pdf";
            response.setHeader("Content-Disposition", "attachment; filename=" + nombreArchivo);
            FacturaSaaSPdfUtil.export(f, emisorNombre, emisorRuc, emisorDireccion, response.getOutputStream());
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el comprobante de la factura SaaS.", e);
        }
    }

    @GetMapping("/excel")
    public void exportarExcel(@RequestParam(value = "empresaId", required = false) Long empresaId,
                              HttpServletResponse response) throws Exception {
        List<FacturaSaaS> facturas = empresaId != null
                ? facturaSaaSService.listarPorEmpresa(empresaId)
                : facturaSaaSService.listarTodas();

        String[] headers = {"N°", "Empresa", "Plan", "Periodo", "Monto", "Estado", "Emisión", "Vencimiento", "Pago"};
        List<Object[]> filas = new ArrayList<>();
        for (FacturaSaaS f : facturas) {
            filas.add(new Object[]{
                    f.getNumeroFactura() != null ? f.getNumeroFactura() : "",
                    f.getEmpresa() != null ? f.getEmpresa().getNombre() : "",
                    f.getPlan() != null ? f.getPlan().getNombre() : "",
                    f.getPeriodoDesde() + " - " + f.getPeriodoHasta(),
                    f.getMonto(),
                    f.getEstado().name(),
                    f.getFechaEmision(),
                    f.getFechaVencimiento(),
                    f.getFechaPago()
            });
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=facturas_saas_" + LocalDate.now() + ".xlsx");
        try (Workbook wb = ExcelExportUtil.crearReporte("Facturación SaaS",
                "Exportado el " + LocalDate.now(), headers, filas)) {
            wb.write(response.getOutputStream());
        }
    }

    @GetMapping("/pdf")
    public void exportarPdf(@RequestParam(value = "empresaId", required = false) Long empresaId,
                            HttpServletResponse response) {
        try {
            List<FacturaSaaS> facturas = empresaId != null
                    ? facturaSaaSService.listarPorEmpresa(empresaId)
                    : facturaSaaSService.listarTodas();

            String[] headers = {"N°", "Empresa", "Plan", "Periodo", "Monto", "Estado", "Emisión", "Vencimiento", "Pago"};
            List<Object[]> filas = new ArrayList<>();
            for (FacturaSaaS f : facturas) {
                filas.add(new Object[]{
                        f.getNumeroFactura() != null ? f.getNumeroFactura() : "",
                        f.getEmpresa() != null ? f.getEmpresa().getNombre() : "",
                        f.getPlan() != null ? f.getPlan().getNombre() : "",
                        f.getPeriodoDesde() + " - " + f.getPeriodoHasta(),
                        f.getMonto(),
                        f.getEstado().name(),
                        f.getFechaEmision(),
                        f.getFechaVencimiento(),
                        f.getFechaPago()
                });
            }
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=facturas_saas_" + LocalDate.now() + ".pdf");
            PdfExportUtil.crearReporte(response.getOutputStream(), "Facturación SaaS",
                    "Exportado el " + LocalDate.now(), headers, filas, null);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el PDF de facturación SaaS.", e);
        }
    }
}
