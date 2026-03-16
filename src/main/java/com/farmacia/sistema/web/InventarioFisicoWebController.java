package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.inventario.Almacen;
import com.farmacia.sistema.domain.inventario.InventarioService;
import com.farmacia.sistema.domain.inventariofisico.InventarioFisico;
import com.farmacia.sistema.domain.inventariofisico.InventarioFisicoDetalle;
import com.farmacia.sistema.domain.inventariofisico.InventarioFisicoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/web/inventario-fisico")
public class InventarioFisicoWebController {

    private final InventarioFisicoService service;
    private final InventarioService inventarioService;

    public InventarioFisicoWebController(InventarioFisicoService service, InventarioService inventarioService) {
        this.service = service;
        this.inventarioService = inventarioService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("inventarios", service.listar());
        model.addAttribute("almacenes", inventarioService.listarAlmacenes());
        return "inventario-fisico";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        InventarioFisico inv = service.obtenerPorId(id);
        List<InventarioFisicoDetalle> detalles = service.obtenerDetalles(id);
        model.addAttribute("inventario", inv);
        model.addAttribute("detalles", detalles);
        return "inventario-fisico-detalle";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR')")
    public String crear(@RequestParam(required = false) Long almacenId,
                        @RequestParam(required = false) String observaciones,
                        Authentication auth, RedirectAttributes ra) {
        try {
            Almacen almacen = almacenId != null ? inventarioService.obtenerAlmacenPorId(almacenId) : null;
            service.crear(almacen, observaciones, auth.getName());
            ra.addFlashAttribute("ok", "Inventario físico creado. Registre los conteos.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/inventario-fisico";
    }

    @PostMapping("/{id}/conteo")
    public String registrarConteo(@PathVariable Long id,
                                  @RequestParam List<Long> detalleIds,
                                  @RequestParam List<Integer> stocksFisicos,
                                  @RequestParam(required = false) List<String> observaciones,
                                  RedirectAttributes ra) {
        for (int i = 0; i < detalleIds.size(); i++) {
            if (stocksFisicos.get(i) != null) {
                String obs = observaciones != null && observaciones.size() > i ? observaciones.get(i) : null;
                service.registrarConteo(detalleIds.get(i), stocksFisicos.get(i), obs);
            }
        }
        ra.addFlashAttribute("ok", "Conteos registrados");
        return "redirect:/web/inventario-fisico/" + id;
    }

    @PostMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR')")
    public String cerrar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        service.cerrar(id, auth.getName());
        ra.addFlashAttribute("ok", "Inventario cerrado");
        return "redirect:/web/inventario-fisico/" + id;
    }

    @PostMapping("/{id}/ajustar")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR')")
    public String ajustar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            service.aplicarAjustes(id, auth.getName());
            ra.addFlashAttribute("ok", "Ajustes aplicados al inventario");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/inventario-fisico/" + id;
    }
}
