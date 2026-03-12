package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.empresa.Empresa;
import com.farmacia.sistema.domain.empresa.EmpresaService;
import com.farmacia.sistema.domain.empresa.FacturaSaaS;
import com.farmacia.sistema.domain.empresa.FacturaSaaSService;
import com.farmacia.sistema.domain.empresa.SolicitudSuscripcion;
import com.farmacia.sistema.domain.empresa.SolicitudSuscripcionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/web/admin/solicitudes")
public class AdminSolicitudesController {

    private final SolicitudSuscripcionService solicitudService;
    private final EmpresaService empresaService;
    private final FacturaSaaSService facturaSaaSService;

    public AdminSolicitudesController(SolicitudSuscripcionService solicitudService,
                                     EmpresaService empresaService,
                                     FacturaSaaSService facturaSaaSService) {
        this.solicitudService = solicitudService;
        this.empresaService = empresaService;
        this.facturaSaaSService = facturaSaaSService;
    }

    @GetMapping
    public String listar(HttpServletRequest request, Model model) {
        if ("sin_permiso".equals(request.getParameter("error"))) {
            model.addAttribute("error", "No tiene permiso para acceder a esa opción.");
        }
        List<SolicitudSuscripcion> solicitudes = solicitudService.listarTodas();
        List<Empresa> empresas = empresaService.listarTodas();
        long pendientes = solicitudes.stream().filter(s -> "PENDIENTE".equals(s.getEstado())).count();
        model.addAttribute("solicitudes", solicitudes);
        model.addAttribute("empresas", empresas);
        model.addAttribute("pendientesCount", pendientes);
        return "saas-solicitudes";
    }

    @PostMapping("/estado")
    public String cambiarEstado(@RequestParam("id") Long id,
                                @RequestParam("estado") String estado,
                                @RequestParam(value = "empresaId", required = false) Long empresaId,
                                RedirectAttributes ra) {
        SolicitudSuscripcion solicitud = solicitudService.cambiarEstado(id, estado);

        if (!"CONVERTIDA".equals(estado)) {
            ra.addFlashAttribute("mensaje", "Estado actualizado.");
            return "redirect:/web/admin/solicitudes";
        }

        Long empresaObjetivoId = empresaId;
        if (empresaObjetivoId == null) {
            empresaObjetivoId = empresaService.buscarPorNombreExacto(solicitud.getNombreEmpresa())
                    .map(Empresa::getId)
                    .orElse(null);
        }

        if (empresaObjetivoId == null) {
            ra.addFlashAttribute("mensaje", "Solicitud marcada como convertida. No se pudo asociar automáticamente a ninguna empresa; revise Facturación si corresponde.");
            return "redirect:/web/admin/solicitudes";
        }

        var facturaPagada = facturaSaaSService.marcarUltimaPendienteComoPagada(empresaObjetivoId);
        if (facturaPagada.isPresent()) {
            FacturaSaaS f = facturaPagada.get();
            ra.addFlashAttribute("mensaje", "Solicitud marcada como convertida. Factura " + (f.getNumeroFactura() != null ? f.getNumeroFactura() : "") + " marcada como pagada en Facturación.");
        } else {
            ra.addFlashAttribute("mensaje", "Solicitud marcada como convertida. No había factura pendiente para esa empresa.");
        }

        return "redirect:/web/admin/solicitudes";
    }

    @PostMapping("/eliminar")
    public String eliminar(@RequestParam("id") Long id, RedirectAttributes ra) {
        try {
            solicitudService.eliminar(id);
            ra.addFlashAttribute("mensaje", "Solicitud eliminada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo eliminar la solicitud.");
        }
        return "redirect:/web/admin/solicitudes";
    }
}
