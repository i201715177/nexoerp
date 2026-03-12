package com.farmacia.sistema.config;

import com.farmacia.sistema.domain.empresa.Empresa;
import com.farmacia.sistema.domain.empresa.EmpresaService;
import com.farmacia.sistema.tenant.TenantContext;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class TenantInfoAdvice {

    private final EmpresaService empresaService;

    public TenantInfoAdvice(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @ModelAttribute("empresaActual")
    public Empresa empresaActual() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return null;
        }
        try {
            return empresaService.obtenerPorId(tenantId);
        } catch (Exception e) {
            return null;
        }
    }

    @ModelAttribute("empresasTodas")
    public List<Empresa> empresasTodas() {
        try {
            return empresaService.listarTodasConPlan();
        } catch (Exception e) {
            return List.of();
        }
    }
}

