package com.farmacia.sistema.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Añade atributos al modelo para todas las vistas web (sidebar, redirect tras cambiar tenant, etc.).
 */
@ControllerAdvice(basePackages = "com.farmacia.sistema.web")
public class WebModelAdvice {

    @ModelAttribute("currentRequestUri")
    public String currentRequestUri(HttpServletRequest request) {
        if (request == null) {
            return "/web/dashboard";
        }
        String uri = request.getRequestURI();
        return (uri != null && !uri.isBlank()) ? uri : "/web/dashboard";
    }
}
