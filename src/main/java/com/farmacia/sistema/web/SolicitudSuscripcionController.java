package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.empresa.PlanSuscripcion;
import com.farmacia.sistema.domain.empresa.PlanSuscripcionService;
import com.farmacia.sistema.domain.empresa.SolicitudSuscripcion;
import com.farmacia.sistema.domain.empresa.SolicitudSuscripcionService;
import com.farmacia.sistema.notification.NotificacionSolicitudService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/solicitar-suscripcion")
public class SolicitudSuscripcionController {

    private final SolicitudSuscripcionService solicitudService;
    private final PlanSuscripcionService planSuscripcionService;
    private final NotificacionSolicitudService notificacionSolicitudService;

    public SolicitudSuscripcionController(SolicitudSuscripcionService solicitudService,
                                          PlanSuscripcionService planSuscripcionService,
                                          NotificacionSolicitudService notificacionSolicitudService) {
        this.solicitudService = solicitudService;
        this.planSuscripcionService = planSuscripcionService;
        this.notificacionSolicitudService = notificacionSolicitudService;
    }

    @GetMapping
    public String formulario(Model model) {
        model.addAttribute("solicitud", new SolicitudSuscripcion());
        model.addAttribute("planes", listarPlanesSeguro());
        return "solicitar-suscripcion";
    }

    @PostMapping
    public String enviar(SolicitudSuscripcion solicitud, RedirectAttributes ra) {
        try {
            // Validaciones de campos obligatorios básicos
            if (solicitud.getNombreContacto() == null || solicitud.getNombreContacto().isBlank()
                    || solicitud.getEmail() == null || solicitud.getEmail().isBlank()
                    || solicitud.getNombreEmpresa() == null || solicitud.getNombreEmpresa().isBlank()) {
                ra.addFlashAttribute("error", "Complete los campos obligatorios: nombre de contacto, correo y nombre de empresa.");
                return "redirect:/solicitar-suscripcion";
            }

            // Validación de tipo y número de documento
            String tipoDoc = solicitud.getTipoDocumento() != null ? solicitud.getTipoDocumento().trim() : "";
            String numeroDoc = solicitud.getNumeroDocumento() != null ? solicitud.getNumeroDocumento().trim() : "";
            if (!tipoDoc.isEmpty() || !numeroDoc.isEmpty()) {
                if (tipoDoc.isEmpty() || numeroDoc.isEmpty()) {
                    ra.addFlashAttribute("error", "Si indica tipo de documento debe ingresar también el número (y viceversa).");
                    return "redirect:/solicitar-suscripcion";
                }
                if ("DNI".equalsIgnoreCase(tipoDoc)) {
                    if (!numeroDoc.matches("\\d{8}")) {
                        ra.addFlashAttribute("error", "El DNI debe tener exactamente 8 dígitos numéricos.");
                        return "redirect:/solicitar-suscripcion";
                    }
                } else if ("RUC".equalsIgnoreCase(tipoDoc)) {
                    if (!numeroDoc.matches("\\d{11}")) {
                        ra.addFlashAttribute("error", "El RUC debe tener exactamente 11 dígitos numéricos.");
                        return "redirect:/solicitar-suscripcion";
                    }
                }
            }

            solicitud.setEstado("PENDIENTE");
            solicitudService.guardar(solicitud);
            notificacionSolicitudService.notificarNuevaSolicitud(solicitud);
            ra.addFlashAttribute("mensaje", "Solicitud enviada correctamente. Nos pondremos en contacto a la brevedad.");
            return "redirect:/solicitar-suscripcion?enviado=true";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo enviar la solicitud. Intente de nuevo o contacte al administrador.");
            return "redirect:/solicitar-suscripcion";
        }
    }

    private List<PlanSuscripcion> listarPlanesSeguro() {
        try {
            return planSuscripcionService.listarActivos();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
