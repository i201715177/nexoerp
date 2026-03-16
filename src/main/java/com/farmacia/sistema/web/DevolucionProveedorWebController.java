package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.devolucion.DevolucionProveedorService;
import com.farmacia.sistema.domain.producto.ProductoService;
import com.farmacia.sistema.domain.proveedor.ProveedorService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/web/devoluciones-proveedor")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR')")
public class DevolucionProveedorWebController {

    private final DevolucionProveedorService service;
    private final ProveedorService proveedorService;
    private final ProductoService productoService;

    public DevolucionProveedorWebController(DevolucionProveedorService service,
                                             ProveedorService proveedorService,
                                             ProductoService productoService) {
        this.service = service;
        this.proveedorService = proveedorService;
        this.productoService = productoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("devoluciones", service.listar());
        model.addAttribute("proveedores", proveedorService.listarTodos());
        model.addAttribute("productos", productoService.listarTodos());
        model.addAttribute("motivos", new String[]{"PRODUCTO_DANADO", "PRODUCTO_VENCIDO", "ERROR_PEDIDO", "DEFECTUOSO", "OTRO"});
        return "devoluciones-proveedor";
    }

    @PostMapping
    public String crear(@RequestParam Long proveedorId, @RequestParam String motivo,
                        @RequestParam(required = false) String observaciones,
                        @RequestParam List<Long> productoIds,
                        @RequestParam List<Integer> cantidades,
                        @RequestParam(required = false) List<String> lotes,
                        Authentication auth, RedirectAttributes ra) {
        try {
            service.crear(proveedorId, motivo, observaciones, auth.getName(), productoIds, cantidades, lotes);
            ra.addFlashAttribute("ok", "Devolución registrada");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/devoluciones-proveedor";
    }

    @PostMapping("/{id}/enviar")
    public String enviar(@PathVariable Long id, RedirectAttributes ra) {
        service.enviar(id);
        ra.addFlashAttribute("ok", "Devolución enviada y stock descontado");
        return "redirect:/web/devoluciones-proveedor";
    }

    @PostMapping("/{id}/respuesta")
    public String respuesta(@PathVariable Long id, @RequestParam String estado,
                            @RequestParam(required = false) String notaCredito, RedirectAttributes ra) {
        service.registrarRespuesta(id, estado, notaCredito);
        ra.addFlashAttribute("ok", "Respuesta registrada");
        return "redirect:/web/devoluciones-proveedor";
    }
}
