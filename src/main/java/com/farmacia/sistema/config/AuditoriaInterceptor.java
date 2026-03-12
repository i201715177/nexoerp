package com.farmacia.sistema.config;

import com.farmacia.sistema.domain.auditoria.AuditoriaAccion;
import com.farmacia.sistema.domain.auditoria.AuditoriaAccionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Audita solo llamadas a la API REST (/api/**).
 * Las acciones web (/web/**) se auditan desde los controladores vía AuditoriaService.
 */
@Component
public class AuditoriaInterceptor implements HandlerInterceptor {

    private final AuditoriaAccionRepository auditoriaAccionRepository;

    public AuditoriaInterceptor(AuditoriaAccionRepository auditoriaAccionRepository) {
        this.auditoriaAccionRepository = auditoriaAccionRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String metodo = request.getMethod();
        if (!"POST".equalsIgnoreCase(metodo) &&
                !"PUT".equalsIgnoreCase(metodo) &&
                !"DELETE".equalsIgnoreCase(metodo)) {
            return true;
        }

        String url = request.getRequestURI();

        // Las rutas /web/** se auditan en detalle desde los controladores
        if (url.startsWith("/web/") || url.equals("/login") || url.equals("/logout")) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anónimo";

        AuditoriaAccion a = new AuditoriaAccion();
        a.setUsuario(usuario);
        a.setMetodo(metodo);
        a.setUrl(url);
        a.setIp(request.getRemoteAddr());
        a.setAccion("API " + metodo);
        a.setDetalle("Petición a " + url);
        auditoriaAccionRepository.save(a);

        return true;
    }
}
