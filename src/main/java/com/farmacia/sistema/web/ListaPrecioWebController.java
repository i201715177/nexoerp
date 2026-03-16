package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.listaprecio.ListaPrecioService;
import com.farmacia.sistema.domain.producto.ProductoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/web/listas-precio")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR')")
public class ListaPrecioWebController {

    private final ListaPrecioService service;
    private final ProductoService productoService;

    public ListaPrecioWebController(ListaPrecioService service, ProductoService productoService) {
        this.service = service;
        this.productoService = productoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("listas", service.listar());
        model.addAttribute("productos", productoService.listarTodos());
        model.addAttribute("tiposCliente", new String[]{"FARMACIA", "CLINICA", "HOSPITAL", "DROGUERIA", "MAYORISTA", "GENERAL"});
        return "listas-precio";
    }

    @PostMapping
    public String crear(@RequestParam String nombre, @RequestParam(required = false) String tipoCliente,
                        @RequestParam(required = false) Double descuento,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                        RedirectAttributes ra) {
        service.crear(nombre, tipoCliente, descuento, fechaInicio, fechaFin);
        ra.addFlashAttribute("ok", "Lista de precios creada");
        return "redirect:/web/listas-precio";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("lista", service.obtenerPorId(id));
        model.addAttribute("detalles", service.obtenerDetalles(id));
        model.addAttribute("productos", productoService.listarTodos());
        return "lista-precio-detalle";
    }

    @PostMapping("/{id}/producto")
    public String agregarProducto(@PathVariable Long id, @RequestParam Long productoId,
                                   @RequestParam(required = false) BigDecimal precioEspecial,
                                   @RequestParam(required = false) Double descuentoPct,
                                   @RequestParam(required = false) Integer cantidadMinima,
                                   RedirectAttributes ra) {
        service.agregarProducto(id, productoId, precioEspecial, descuentoPct, cantidadMinima);
        ra.addFlashAttribute("ok", "Producto agregado a la lista");
        return "redirect:/web/listas-precio/" + id;
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        service.toggle(id);
        ra.addFlashAttribute("ok", "Estado actualizado");
        return "redirect:/web/listas-precio";
    }
}
