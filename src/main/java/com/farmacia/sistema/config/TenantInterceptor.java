package com.farmacia.sistema.config;

import com.farmacia.sistema.domain.empresa.Empresa;
import com.farmacia.sistema.domain.empresa.EmpresaService;
import com.farmacia.sistema.security.TenantUserDetails;
import com.farmacia.sistema.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);
    public static final String SESSION_TENANT_ID = "TENANT_ID";

    private final EmpresaService empresaService;

    public TenantInterceptor(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (response.isCommitted()) {
            return false;
        }

        HttpSession session = request.getSession();
        Object attr = session.getAttribute(SESSION_TENANT_ID);
        Long tenantId;

        if (attr instanceof Long) {
            tenantId = (Long) attr;
        } else {
            tenantId = resolverTenantDesdeUsuario();
            if (tenantId == null) {
                try {
                    tenantId = empresaService.empresaPorDefecto().getId();
                } catch (Exception e) {
                    log.error("No se pudo resolver tenant: {}", e.getMessage());
                    response.sendRedirect(request.getContextPath() + "/login");
                    return false;
                }
            }
            session.setAttribute(SESSION_TENANT_ID, tenantId);
        }

        try {
            Empresa empresa = empresaService.obtenerPorId(tenantId);
            if (!empresa.isActiva()) {
                log.warn("Empresa suspendida (id={}).", tenantId);
                // SAAS_ADMIN puede seguir entrando para reactivar desde Admin SaaS
                if (tieneRolSaasAdmin()) {
                    TenantContext.setTenantId(tenantId);
                    return true;
                }
                session.invalidate();
                response.sendRedirect(request.getContextPath() + "/login?error");
                return false;
            }
        } catch (Exception e) {
            // En una base nueva (como en un despliegue fresco) aún no existe la empresa con ese ID.
            // Creamos/obtenemos la empresa por defecto y continuamos con ese tenant.
            log.warn("Empresa no encontrada (id={}). Creando/seleccionando empresa por defecto.", tenantId);
            try {
                Empresa porDefecto = empresaService.empresaPorDefecto();
                Long nuevoTenantId = porDefecto.getId();
                session.setAttribute(SESSION_TENANT_ID, nuevoTenantId);
                TenantContext.setTenantId(nuevoTenantId);
                return true;
            } catch (Exception ex) {
                log.error("No se pudo recuperar o crear empresa por defecto: {}", ex.getMessage());
                session.removeAttribute(SESSION_TENANT_ID);
                response.sendRedirect(request.getContextPath() + "/login");
                return false;
            }
        }

        TenantContext.setTenantId(tenantId);
        return true;
    }

    private Long resolverTenantDesdeUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof TenantUserDetails tud) {
            return tud.getTenantId();
        }
        return null;
    }

    private boolean tieneRolSaasAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SAAS_ADMIN".equals(a.getAuthority()));
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}

