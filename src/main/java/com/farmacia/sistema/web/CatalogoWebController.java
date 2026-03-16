package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.catalogo.CatalogoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/web/catalogos")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR')")
public class CatalogoWebController {

    private final CatalogoService service;

    public CatalogoWebController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("categorias", service.listarCategorias());
        model.addAttribute("laboratorios", service.listarLaboratorios());
        model.addAttribute("presentaciones", service.listarPresentaciones());
        return "catalogos";
    }

    @PostMapping("/categorias")
    public String crearCategoria(@RequestParam String nombre, @RequestParam(required = false) String descripcion, RedirectAttributes ra) {
        service.crearCategoria(nombre, descripcion);
        ra.addFlashAttribute("ok", "Categoría creada");
        return "redirect:/web/catalogos";
    }

    @PostMapping("/categorias/{id}/toggle")
    public String toggleCategoria(@PathVariable Long id, RedirectAttributes ra) {
        service.toggleCategoria(id);
        ra.addFlashAttribute("ok", "Estado actualizado");
        return "redirect:/web/catalogos";
    }

    @PostMapping("/laboratorios")
    public String crearLaboratorio(@RequestParam String nombre, @RequestParam(required = false) String pais, RedirectAttributes ra) {
        service.crearLaboratorio(nombre, pais);
        ra.addFlashAttribute("ok", "Laboratorio creado");
        return "redirect:/web/catalogos";
    }

    @PostMapping("/laboratorios/{id}/toggle")
    public String toggleLaboratorio(@PathVariable Long id, RedirectAttributes ra) {
        service.toggleLaboratorio(id);
        ra.addFlashAttribute("ok", "Estado actualizado");
        return "redirect:/web/catalogos";
    }

    @PostMapping("/presentaciones")
    public String crearPresentacion(@RequestParam String nombre, RedirectAttributes ra) {
        service.crearPresentacion(nombre);
        ra.addFlashAttribute("ok", "Presentación creada");
        return "redirect:/web/catalogos";
    }

    @PostMapping("/presentaciones/{id}/toggle")
    public String togglePresentacion(@PathVariable Long id, RedirectAttributes ra) {
        service.togglePresentacion(id);
        ra.addFlashAttribute("ok", "Estado actualizado");
        return "redirect:/web/catalogos";
    }
}
