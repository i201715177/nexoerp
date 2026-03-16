package com.farmacia.sistema.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Agrega headers de seguridad HTTP a todas las respuestas.
 */
@Component
@Order(0)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpRes = (HttpServletResponse) response;
        httpRes.setHeader("X-Content-Type-Options", "nosniff");
        httpRes.setHeader("X-Frame-Options", "SAMEORIGIN");
        httpRes.setHeader("X-XSS-Protection", "1; mode=block");
        httpRes.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        httpRes.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        httpRes.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");

        chain.doFilter(request, response);
    }
}
