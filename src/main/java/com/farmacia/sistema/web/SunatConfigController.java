package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.empresa.Empresa;
import com.farmacia.sistema.domain.empresa.EmpresaService;
import com.farmacia.sistema.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/web/sunat-config")
public class SunatConfigController {

    private static final Logger log = LoggerFactory.getLogger(SunatConfigController.class);
    private final EmpresaService empresaService;

    public SunatConfigController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    public String mostrar(Model model) {
        Empresa empresa = obtenerEmpresaActual();
        model.addAttribute("empresa", empresa);
        model.addAttribute("tieneCertificado", empresa.tieneCertificadoConfigurado());
        return "sunat-config";
    }

    @PostMapping("/series")
    public String guardarSeries(@RequestParam("serieFactura") String serieFactura,
                                @RequestParam("serieBoleta") String serieBoleta,
                                @RequestParam("serieNotaCredito") String serieNotaCredito,
                                @RequestParam("serieNotaDebito") String serieNotaDebito,
                                @RequestParam("serieGuiaRemision") String serieGuiaRemision,
                                RedirectAttributes ra) {
        try {
            Empresa empresa = obtenerEmpresaActual();
            empresa.setSerieFactura(serieFactura != null ? serieFactura.trim().toUpperCase() : "F001");
            empresa.setSerieBoleta(serieBoleta != null ? serieBoleta.trim().toUpperCase() : "B001");
            empresa.setSerieNotaCredito(serieNotaCredito != null ? serieNotaCredito.trim().toUpperCase() : "FC01");
            empresa.setSerieNotaDebito(serieNotaDebito != null ? serieNotaDebito.trim().toUpperCase() : "FD01");
            empresa.setSerieGuiaRemision(serieGuiaRemision != null ? serieGuiaRemision.trim().toUpperCase() : "T001");
            empresaService.guardarDirecto(empresa);
            ra.addFlashAttribute("mensaje", "Series de comprobantes actualizadas correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al guardar series: " + e.getMessage());
        }
        return "redirect:/web/sunat-config";
    }

    @PostMapping("/certificado")
    public String subirCertificado(@RequestParam("archivoPfx") MultipartFile archivoPfx,
                                   @RequestParam("certificadoPassword") String password,
                                   @RequestParam(value = "solUsuario", required = false) String solUsuario,
                                   @RequestParam(value = "solPassword", required = false) String solPassword,
                                   @RequestParam(value = "sunatModo", required = false) String sunatModo,
                                   RedirectAttributes ra) {
        try {
            if (archivoPfx.isEmpty()) {
                ra.addFlashAttribute("error", "Debe seleccionar un archivo .pfx o .p12");
                return "redirect:/web/sunat-config";
            }
            String nombre = archivoPfx.getOriginalFilename();
            if (nombre != null && !nombre.toLowerCase().endsWith(".pfx") && !nombre.toLowerCase().endsWith(".p12")) {
                ra.addFlashAttribute("error", "El archivo debe ser .pfx o .p12");
                return "redirect:/web/sunat-config";
            }
            if (password == null || password.isBlank()) {
                ra.addFlashAttribute("error", "La contraseña del certificado es obligatoria.");
                return "redirect:/web/sunat-config";
            }

            Empresa empresa = obtenerEmpresaActual();
            empresa.setCertificadoPfx(archivoPfx.getBytes());
            empresa.setCertificadoPassword(password);
            empresa.setCertificadoNombreArchivo(nombre);
            if (solUsuario != null && !solUsuario.isBlank()) empresa.setSolUsuario(solUsuario);
            if (solPassword != null && !solPassword.isBlank()) empresa.setSolPassword(solPassword);
            empresa.setSunatModo(sunatModo != null ? sunatModo.trim().toUpperCase() : "BETA");
            empresa.setSunatHabilitado(true);
            empresaService.guardarDirecto(empresa);

            log.info("Certificado digital subido para empresa {} ({})", empresa.getNombre(), empresa.getRuc());
            ra.addFlashAttribute("mensaje", "Certificado digital configurado exitosamente para " + empresa.getNombre() + ". Facturación electrónica ACTIVADA en modo " + empresa.getSunatModo() + ".");
        } catch (Exception e) {
            log.error("Error subiendo certificado: {}", e.getMessage());
            ra.addFlashAttribute("error", "Error al subir certificado: " + e.getMessage());
        }
        return "redirect:/web/sunat-config";
    }

    @PostMapping("/desactivar")
    public String desactivar(RedirectAttributes ra) {
        try {
            Empresa empresa = obtenerEmpresaActual();
            empresa.setSunatHabilitado(false);
            empresaService.guardarDirecto(empresa);
            ra.addFlashAttribute("mensaje", "Facturación electrónica desactivada. Los comprobantes se seguirán generando en PDF interno.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/web/sunat-config";
    }

    @PostMapping("/activar")
    public String activar(RedirectAttributes ra) {
        try {
            Empresa empresa = obtenerEmpresaActual();
            if (!empresa.tieneCertificadoConfigurado()) {
                ra.addFlashAttribute("error", "No puede activar sin subir primero un certificado digital.");
                return "redirect:/web/sunat-config";
            }
            empresa.setSunatHabilitado(true);
            empresaService.guardarDirecto(empresa);
            ra.addFlashAttribute("mensaje", "Facturación electrónica ACTIVADA en modo " + empresa.getSunatModo() + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/web/sunat-config";
    }

    @PostMapping("/eliminar-certificado")
    public String eliminarCertificado(RedirectAttributes ra) {
        try {
            Empresa empresa = obtenerEmpresaActual();
            empresa.setCertificadoPfx(null);
            empresa.setCertificadoPassword(null);
            empresa.setCertificadoNombreArchivo(null);
            empresa.setSolUsuario(null);
            empresa.setSolPassword(null);
            empresa.setSunatHabilitado(false);
            empresa.setSunatModo("DEMO");
            empresaService.guardarDirecto(empresa);
            ra.addFlashAttribute("mensaje", "Certificado eliminado. Facturación electrónica desactivada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/web/sunat-config";
    }

    private Empresa obtenerEmpresaActual() {
        Long tid = TenantContext.getTenantId();
        return tid != null ? empresaService.obtenerPorId(tid) : empresaService.empresaPorDefecto();
    }
}
