package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.auditoria.AuditoriaAccion;
import com.farmacia.sistema.domain.auditoria.AuditoriaAccionRepository;
import com.farmacia.sistema.export.ExcelExportUtil;
import com.farmacia.sistema.export.PdfExportUtil;
import com.farmacia.sistema.tenant.TenantContext;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/web/auditoria")
@PreAuthorize("hasAnyRole('ADMIN', 'SAAS_ADMIN')")
public class AuditoriaWebController {

    private final AuditoriaAccionRepository auditoriaAccionRepository;

    public AuditoriaWebController(AuditoriaAccionRepository auditoriaAccionRepository) {
        this.auditoriaAccionRepository = auditoriaAccionRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("registros", obtenerRegistros());
        return "auditoria";
    }

    @GetMapping("/excel")
    public void exportarExcel(HttpServletResponse response) throws Exception {
        List<AuditoriaAccion> registros = obtenerRegistros();
        String[] headers = {"Fecha/Hora", "Usuario", "Método", "Acción", "URL", "IP", "Detalle"};
        List<Object[]> filas = new ArrayList<>();
        for (AuditoriaAccion a : registros) {
            filas.add(new Object[]{
                    a.getFechaHora(), a.getUsuario(), a.getMetodo(),
                    a.getAccion(), a.getUrl(), a.getIp(), a.getDetalle()
            });
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=auditoria_" + LocalDate.now() + ".xlsx");
        try (Workbook wb = ExcelExportUtil.crearReporte("Registro de Auditoría",
                "Exportado el " + LocalDate.now(), headers, filas)) {
            wb.write(response.getOutputStream());
        }
    }

    @GetMapping("/pdf")
    public void exportarPdf(HttpServletResponse response) throws Exception {
        List<AuditoriaAccion> registros = obtenerRegistros();
        String[] headers = {"Fecha/Hora", "Usuario", "Método", "Acción", "Detalle"};
        List<Object[]> filas = new ArrayList<>();
        for (AuditoriaAccion a : registros) {
            filas.add(new Object[]{
                    a.getFechaHora(), a.getUsuario(), a.getMetodo(),
                    a.getAccion(), a.getDetalle()
            });
        }
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=auditoria_" + LocalDate.now() + ".pdf");
        PdfExportUtil.crearReporte(response.getOutputStream(), "Registro de Auditoría",
                "Exportado el " + LocalDate.now(), headers, filas,
                new float[]{2, 1.5f, 1, 2, 4});
    }

    private List<AuditoriaAccion> obtenerRegistros() {
        Long tenantId = TenantContext.getTenantId();
        return (tenantId != null)
                ? auditoriaAccionRepository.findTop100ByTenantIdOrderByFechaHoraDesc(tenantId)
                : auditoriaAccionRepository.findTop100ByOrderByFechaHoraDesc();
    }
}

