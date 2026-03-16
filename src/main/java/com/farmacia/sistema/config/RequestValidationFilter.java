package com.farmacia.sistema.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filtro de seguridad para validar requests entrantes.
 * Protege contra: payloads excesivos, headers maliciosos, request flooding.
 */
@Component
@Order(1)
public class RequestValidationFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestValidationFilter.class);
    private static final int MAX_HEADER_SIZE = 8192;
    private static final int MAX_PARAM_LENGTH = 4096;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;

        if (containsSuspiciousPatterns(httpReq)) {
            log.warn("Request bloqueado por patrón sospechoso: {} {}", httpReq.getMethod(), httpReq.getRequestURI());
            ((HttpServletResponse) response).sendError(400, "Solicitud rechazada");
            return;
        }

        String queryString = httpReq.getQueryString();
        if (queryString != null && queryString.length() > MAX_PARAM_LENGTH) {
            log.warn("Query string excesivamente largo: {}", httpReq.getRequestURI());
            ((HttpServletResponse) response).sendError(400, "Parámetros excesivos");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean containsSuspiciousPatterns(HttpServletRequest request) {
        String uri = request.getRequestURI().toLowerCase();
        String query = request.getQueryString();
        String combined = uri + (query != null ? "?" + query : "");

        return combined.contains("../") ||
               combined.contains("..\\") ||
               combined.contains("<script") ||
               combined.contains("javascript:") ||
               combined.contains("eval(") ||
               combined.contains("union+select") ||
               combined.contains("'; drop") ||
               combined.contains("1=1");
    }
}
