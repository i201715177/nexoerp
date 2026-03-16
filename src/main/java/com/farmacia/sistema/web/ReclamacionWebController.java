package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.reclamacion.ReclamacionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/web/libro-reclamaciones")
public class ReclamacionWebController {

    private final ReclamacionService service;

    public ReclamacionWebController(ReclamacionService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("reclamaciones", service.listar());
        model.addAttribute("pendientes", service.countPendientes());
        return "libro-reclamaciones";
    }

    @PostMapping
    public String crear(@RequestParam String tipo, @RequestParam String clienteNombre,
                        @RequestParam(required = false) String clienteDocumento,
                        @RequestParam(required = false) String clienteTelefono,
                        @RequestParam(required = false) String clienteEmail,
                        @RequestParam(required = false) String clienteDireccion,
                        @RequestParam String detalle,
                        @RequestParam(required = false) String productoServicio,
                        @RequestParam(required = false) String montoReclamado,
                        Authentication auth, RedirectAttributes ra) {
        service.crear(tipo, clienteNombre, clienteDocumento, clienteTelefono, clienteEmail,
                clienteDireccion, detalle, productoServicio, montoReclamado, auth.getName());
        ra.addFlashAttribute("ok", "Reclamación registrada exitosamente");
        return "redirect:/web/libro-reclamaciones";
    }

    @PostMapping("/{id}/responder")
    public String responder(@PathVariable Long id, @RequestParam String respuesta,
                            @RequestParam String estado, RedirectAttributes ra) {
        service.responder(id, respuesta, estado);
        ra.addFlashAttribute("ok", "Respuesta registrada");
        return "redirect:/web/libro-reclamaciones";
    }
}
