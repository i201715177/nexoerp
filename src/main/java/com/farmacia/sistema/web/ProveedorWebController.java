package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.auditoria.AuditoriaService;
import com.farmacia.sistema.domain.proveedor.Proveedor;
import com.farmacia.sistema.domain.proveedor.ProveedorService;
import com.farmacia.sistema.export.ExcelExportUtil;
import com.farmacia.sistema.export.PdfExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.farmacia.sistema.domain.auditoria.AuditoriaService.cmp;
import static com.farmacia.sistema.domain.auditoria.AuditoriaService.nuevoCambios;

@Controller
@RequestMapping("/web/proveedores")
@PreAuthorize("hasAnyRole('ADMIN', 'SAAS_ADMIN')")
public class ProveedorWebController {

    private final ProveedorService proveedorService;
    private final AuditoriaService auditoriaService;

    public ProveedorWebController(ProveedorService proveedorService,
                                  AuditoriaService auditoriaService) {
        this.proveedorService = proveedorService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public String listar(Model model) {
        List<Proveedor> proveedores = proveedorService.listarTodos();
        if (proveedores == null) proveedores = Collections.emptyList();
        model.addAttribute("proveedores", proveedores);
        model.addAttribute("proveedor", new Proveedor());
        return "proveedores";
    }

    @GetMapping("/excel")
    public void exportarExcel(HttpServletResponse response) throws Exception {
        List<Proveedor> proveedores = proveedorService.listarTodos();
        if (proveedores == null) proveedores = Collections.emptyList();
        String[] headers = {"Tipo Doc", "N° Documento", "Razón Social", "Contacto",
                "Teléfono", "Email", "Dirección", "Activo"};
        List<Object[]> filas = new ArrayList<>();
        for (Proveedor p : proveedores) {
            filas.add(new Object[]{
                    p.getTipoDocumento(), p.getNumeroDocumento(), p.getRazonSocial(),
                    p.getContacto(), p.getTelefono(), p.getEmail(),
                    p.getDireccion(), p.isActivo()
            });
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=proveedores_" + LocalDate.now() + ".xlsx");
        try (Workbook wb = ExcelExportUtil.crearReporte("Listado de Proveedores",
                "Exportado el " + LocalDate.now(), headers, filas)) {
            wb.write(response.getOutputStream());
        }
    }

    @GetMapping("/pdf")
    public void exportarPdf(HttpServletResponse response) throws Exception {
        List<Proveedor> proveedores = proveedorService.listarTodos();
        if (proveedores == null) proveedores = Collections.emptyList();
        String[] headers = {"Tipo Doc", "N° Documento", "Razón Social", "Contacto",
                "Teléfono", "Email", "Activo"};
        List<Object[]> filas = new ArrayList<>();
        for (Proveedor p : proveedores) {
            filas.add(new Object[]{
                    p.getTipoDocumento(), p.getNumeroDocumento(), p.getRazonSocial(),
                    p.getContacto(), p.getTelefono(), p.getEmail(), p.isActivo()
            });
        }
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=proveedores_" + LocalDate.now() + ".pdf");
        PdfExportUtil.crearReporte(response.getOutputStream(), "Listado de Proveedores",
                "Exportado el " + LocalDate.now(), headers, filas, null);
    }

    @PostMapping
    public String crear(@ModelAttribute("proveedor") @Valid Proveedor proveedor,
                        BindingResult bindingResult,
                        Model model) {
        if (bindingResult.hasErrors()) {
            List<Proveedor> list = proveedorService.listarTodos();
            model.addAttribute("proveedores", list != null ? list : Collections.emptyList());
            return "proveedores";
        }
        try {
            if (proveedor.getId() == null) {
                proveedorService.crear(proveedor);
                auditoriaService.registrarCreacion("proveedor", proveedor.getRazonSocial(),
                        "RUC/DNI: " + proveedor.getNumeroDocumento()
                                + " | Contacto: " + proveedor.getContacto());
            } else {
                Proveedor anterior = proveedorService.obtenerPorId(proveedor.getId());
                Map<String, String[]> cambios = nuevoCambios();
                cmp(cambios, "Razón social", anterior.getRazonSocial(), proveedor.getRazonSocial());
                cmp(cambios, "N° doc", anterior.getNumeroDocumento(), proveedor.getNumeroDocumento());
                cmp(cambios, "Contacto", anterior.getContacto(), proveedor.getContacto());
                cmp(cambios, "Teléfono", anterior.getTelefono(), proveedor.getTelefono());
                cmp(cambios, "Email", anterior.getEmail(), proveedor.getEmail());
                cmp(cambios, "Dirección", anterior.getDireccion(), proveedor.getDireccion());
                cmp(cambios, "Activo", anterior.isActivo(), proveedor.isActivo());

                proveedorService.actualizar(proveedor.getId(), proveedor);
                auditoriaService.registrarActualizacion("proveedor", anterior.getRazonSocial(), cambios);
            }
        } catch (IllegalArgumentException e) {
            List<Proveedor> list = proveedorService.listarTodos();
            model.addAttribute("proveedores", list != null ? list : Collections.emptyList());
            model.addAttribute("errorProveedor", e.getMessage());
            return "proveedores";
        }
        return "redirect:/web/proveedores";
    }
}
