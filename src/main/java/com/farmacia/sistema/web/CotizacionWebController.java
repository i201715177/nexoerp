package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.cliente.ClienteService;
import com.farmacia.sistema.domain.cotizacion.Cotizacion;
import com.farmacia.sistema.domain.cotizacion.CotizacionService;
import com.farmacia.sistema.domain.producto.ProductoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/web/cotizaciones")
public class CotizacionWebController {

    private final CotizacionService cotizacionService;
    private final ProductoService productoService;
    private final ClienteService clienteService;

    public CotizacionWebController(CotizacionService cotizacionService, ProductoService productoService,
                                    ClienteService clienteService) {
        this.cotizacionService = cotizacionService;
        this.productoService = productoService;
        this.clienteService = clienteService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("cotizaciones", cotizacionService.listar());
        model.addAttribute("productos", productoService.listarTodos());
        model.addAttribute("clientes", clienteService.listarTodos());
        return "cotizaciones";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Cotizacion c = cotizacionService.obtenerPorId(id);
        model.addAttribute("cotizacion", c);
        return "cotizacion-detalle";
    }

    @PostMapping
    public String crear(@RequestParam(required = false) Long clienteId,
                        @RequestParam(required = false) String nombreCliente,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vigenciaHasta,
                        @RequestParam(required = false) String observaciones,
                        @RequestParam List<Long> productoIds,
                        @RequestParam List<Integer> cantidades,
                        @RequestParam(required = false) List<BigDecimal> precios,
                        @RequestParam(required = false) List<BigDecimal> descuentos,
                        Authentication auth, RedirectAttributes ra) {
        try {
            cotizacionService.crear(clienteId, nombreCliente, vigenciaHasta, observaciones,
                    auth.getName(), productoIds, cantidades, precios, descuentos);
            ra.addFlashAttribute("ok", "Cotización creada exitosamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/cotizaciones";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam String estado, RedirectAttributes ra) {
        cotizacionService.cambiarEstado(id, estado);
        ra.addFlashAttribute("ok", "Estado actualizado");
        return "redirect:/web/cotizaciones";
    }
}
