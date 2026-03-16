package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.bpa.BpaService;
import com.farmacia.sistema.domain.inventario.InventarioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/web/bpa")
public class BpaWebController {

    private final BpaService service;
    private final InventarioService inventarioService;

    public BpaWebController(BpaService service, InventarioService inventarioService) {
        this.service = service;
        this.inventarioService = inventarioService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("checklists", service.listar());
        model.addAttribute("almacenes", inventarioService.listarAlmacenes());
        model.addAttribute("tipos", new String[]{"LIMPIEZA", "FUMIGACION", "INSPECCION", "CAPACITACION"});
        return "bpa";
    }

    @PostMapping
    public String crear(@RequestParam(required = false) Long almacenId,
                        @RequestParam String tipo,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                        @RequestParam(required = false, defaultValue = "true") boolean piso,
                        @RequestParam(required = false, defaultValue = "true") boolean paredes,
                        @RequestParam(required = false, defaultValue = "true") boolean techo,
                        @RequestParam(required = false, defaultValue = "true") boolean iluminacion,
                        @RequestParam(required = false, defaultValue = "true") boolean ventilacion,
                        @RequestParam(required = false, defaultValue = "true") boolean ordenados,
                        @RequestParam(required = false, defaultValue = "true") boolean separacion,
                        @RequestParam(required = false, defaultValue = "true") boolean vencidosSep,
                        @RequestParam(required = false, defaultValue = "true") boolean extintor,
                        @RequestParam(required = false, defaultValue = "true") boolean botiquin,
                        @RequestParam(required = false) String observaciones,
                        @RequestParam(required = false) String accionCorrectiva,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate proximaRevision,
                        Authentication auth, RedirectAttributes ra) {
        service.crear(almacenId, tipo, fecha, piso, paredes, techo, iluminacion, ventilacion,
                ordenados, separacion, vencidosSep, extintor, botiquin, observaciones, accionCorrectiva,
                proximaRevision, auth.getName());
        ra.addFlashAttribute("ok", "Checklist BPA registrado");
        return "redirect:/web/bpa";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("checklist", service.obtenerPorId(id));
        return "bpa-detalle";
    }
}
