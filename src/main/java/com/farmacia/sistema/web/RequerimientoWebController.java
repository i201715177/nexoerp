package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.inventario.Requerimiento;
import com.farmacia.sistema.domain.inventario.RequerimientoService;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoService;
import com.farmacia.sistema.domain.sucursal.SucursalService;
import com.farmacia.sistema.security.TenantUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/web/requerimientos")
public class RequerimientoWebController {

    private final RequerimientoService requerimientoService;
    private final SucursalService sucursalService;
    private final ProductoService productoService;

    public RequerimientoWebController(RequerimientoService requerimientoService,
                                      SucursalService sucursalService,
                                      ProductoService productoService) {
        this.requerimientoService = requerimientoService;
        this.sucursalService = sucursalService;
        this.productoService = productoService;
    }

    @GetMapping
    public String listar(Model model) {
        Long sucursalIdUsuario = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof TenantUserDetails tud) {
            sucursalIdUsuario = tud.getSucursalId();
        }
        List<Requerimiento> requerimientos = sucursalIdUsuario != null
                ? requerimientoService.listarPorSucursal(sucursalIdUsuario)
                : requerimientoService.listarTodos();
        model.addAttribute("requerimientos", requerimientos);
        model.addAttribute("sucursales", sucursalService.listarTodas());
        model.addAttribute("productos", productoService.listarTodos());
        model.addAttribute("marcas", productoService.listarMarcasDistintas());
        model.addAttribute("sucursalIdVendedor", sucursalIdUsuario);
        return "requerimientos";
    }

    @PostMapping
    public String crear(@RequestParam("sucursalId") Long sucursalId,
                        @RequestParam(value = "productoId", required = false) List<Long> productoIds,
                        @RequestParam(value = "cantidad", required = false) List<Integer> cantidades,
                        @RequestParam(value = "observaciones", required = false) String observaciones,
                        RedirectAttributes ra) {
        Long sucursalIdUsuario = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof TenantUserDetails tud) {
            sucursalIdUsuario = tud.getSucursalId();
        }
        if (sucursalIdUsuario != null) {
            sucursalId = sucursalIdUsuario;
        }
        try {
            requerimientoService.crear(sucursalId, observaciones, productoIds != null ? productoIds : List.of(), cantidades != null ? cantidades : List.of());
            ra.addFlashAttribute("mensaje", "Requerimiento registrado. La central lo atenderá.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/requerimientos";
    }

    @PostMapping("/marcar-en-camino")
    public String marcarEnCamino(@RequestParam("id") Long id, RedirectAttributes ra) {
        try {
            requerimientoService.marcarEnCamino(id);
            ra.addFlashAttribute("mensaje", "Requerimiento marcado como En camino. Realice la transferencia desde Inventario.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/requerimientos";
    }

    @PostMapping("/marcar-recibido")
    public String marcarRecibido(@RequestParam("id") Long id, RedirectAttributes ra) {
        Long sucursalIdUsuario = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof TenantUserDetails tud) {
            sucursalIdUsuario = tud.getSucursalId();
        }
        try {
            Requerimiento r = requerimientoService.obtenerPorId(id);
            if (sucursalIdUsuario != null && (r.getSucursal() == null || !r.getSucursal().getId().equals(sucursalIdUsuario))) {
                ra.addFlashAttribute("error", "Solo puede confirmar la llegada de requerimientos de su sucursal.");
                return "redirect:/web/requerimientos";
            }
            requerimientoService.marcarRecibido(id);
            ra.addFlashAttribute("mensaje", "Requerimiento marcado como Recibido. El stock de la sucursal se ha actualizado.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/requerimientos";
    }
}
