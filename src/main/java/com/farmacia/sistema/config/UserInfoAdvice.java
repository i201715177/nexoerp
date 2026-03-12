package com.farmacia.sistema.config;

import com.farmacia.sistema.domain.sucursal.SucursalService;
import com.farmacia.sistema.security.TenantUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class UserInfoAdvice {

    private final SucursalService sucursalService;

    public UserInfoAdvice(SucursalService sucursalService) {
        this.sucursalService = sucursalService;
    }

    @ModelAttribute("usuarioActual")
    public String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String nombre = auth.getName();
        if ("anonymousUser".equalsIgnoreCase(nombre)) {
            return null;
        }
        return nombre;
    }

    @ModelAttribute("sucursalIdUsuario")
    public Long sucursalIdUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof TenantUserDetails tud) {
            return tud.getSucursalId();
        }
        return null;
    }

    @ModelAttribute("sucursalNombreUsuario")
    public String sucursalNombreUsuario() {
        Long sucursalId = sucursalIdUsuario();
        if (sucursalId == null) return null;
        try {
            return sucursalService.obtenerPorId(sucursalId).getNombre();
        } catch (Exception e) {
            return null;
        }
    }
}

