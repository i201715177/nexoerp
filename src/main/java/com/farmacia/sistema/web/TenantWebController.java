package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.empresa.EmpresaService;
import com.farmacia.sistema.config.TenantInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/web/empresas")
public class TenantWebController {

    private final EmpresaService empresaService;

    public TenantWebController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @PreAuthorize("hasRole('SAAS_ADMIN')")
    @PostMapping("/cambiar")
    public String cambiarEmpresa(@RequestParam("tenantId") Long tenantId,
                                 @RequestParam(value = "redirect", required = false) String redirect,
                                 HttpServletRequest request) {
        empresaService.obtenerPorId(tenantId);
        request.getSession().setAttribute(TenantInterceptor.SESSION_TENANT_ID, tenantId);
        if (redirect != null && !redirect.isBlank()) {
            return "redirect:" + redirect;
        }
        return "redirect:/";
    }
}

