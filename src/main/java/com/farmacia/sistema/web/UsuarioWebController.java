package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.sucursal.SucursalService;
import com.farmacia.sistema.domain.usuario.UsuarioService;
import com.farmacia.sistema.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/web/usuarios")
@PreAuthorize("hasRole('GERENTE')")
public class UsuarioWebController {

    private final UsuarioService usuarioService;
    private final SucursalService sucursalService;

    public UsuarioWebController(UsuarioService usuarioService, SucursalService sucursalService) {
        this.usuarioService = usuarioService;
        this.sucursalService = sucursalService;
    }

    @GetMapping
    public String listar(Model model) {
        Long tenantId = TenantContext.getTenantId();
        model.addAttribute("usuarios", usuarioService.listarPorTenant(tenantId));
        model.addAttribute("sucursales", sucursalService.listarTodas());
        model.addAttribute("roles", new String[]{"ADMIN", "VENDEDOR", "QUIMICO_FARMACEUTICO", "AUDITOR"});
        return "usuarios";
    }

    @PostMapping
    public String crear(@RequestParam String username, @RequestParam String password,
                        @RequestParam(required = false) String nombreCompleto,
                        @RequestParam String rol,
                        @RequestParam(required = false) Long sucursalId,
                        RedirectAttributes ra) {
        try {
            usuarioService.crear(username, password, nombreCompleto, rol, TenantContext.getTenantId(), sucursalId);
            ra.addFlashAttribute("ok", "Usuario creado exitosamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/usuarios";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam boolean activo, RedirectAttributes ra) {
        usuarioService.cambiarEstado(id, activo);
        ra.addFlashAttribute("ok", activo ? "Usuario activado" : "Usuario desactivado");
        return "redirect:/web/usuarios";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, @RequestParam String nuevoPassword, RedirectAttributes ra) {
        usuarioService.resetPassword(id, nuevoPassword);
        ra.addFlashAttribute("ok", "Contraseña restablecida");
        return "redirect:/web/usuarios";
    }

    @PostMapping("/{id}/sucursal")
    public String asignarSucursal(@PathVariable Long id, @RequestParam(required = false) Long sucursalId, RedirectAttributes ra) {
        usuarioService.actualizarSucursal(id, sucursalId);
        ra.addFlashAttribute("ok", "Sucursal actualizada");
        return "redirect:/web/usuarios";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        usuarioService.eliminar(id);
        ra.addFlashAttribute("ok", "Usuario eliminado");
        return "redirect:/web/usuarios";
    }
}
