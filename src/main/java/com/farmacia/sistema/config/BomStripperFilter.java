package com.farmacia.sistema.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Elimina el BOM (Byte Order Mark) del inicio de las respuestas HTML para evitar Quirks Mode.
 * Orden positivo para ejecutarse DESPUÉS del filtro de Spring Security (-100).
 */
@Component
@Order(1)
public class BomStripperFilter extends OncePerRequestFilter {

    private static final byte[] UTF8_BOM = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isHtmlResponse(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (response.isCommitted()) {
            filterChain.doFilter(request, response);
            return;
        }

        BomStripperResponseWrapper wrapper = new BomStripperResponseWrapper(response);
        filterChain.doFilter(request, wrapper);

        if (response.isCommitted()) {
            return;
        }

        byte[] content;
        try {
            content = wrapper.getContent();
        } catch (Exception e) {
            return;
        }
        if (content != null && content.length >= 3
                && content[0] == UTF8_BOM[0] && content[1] == UTF8_BOM[1] && content[2] == UTF8_BOM[2]) {
            byte[] sinBom = new byte[content.length - 3];
            System.arraycopy(content, 3, sinBom, 0, sinBom.length);
            content = sinBom;
        }
        if (content != null && content.length > 0 && !response.isCommitted()) {
            response.setContentType("text/html; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setContentLength(content.length);
            response.getOutputStream().write(content);
        }
    }

    private boolean isHtmlResponse(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri == null) return false;
        if (uri.equals("/")) return true;
        if (!uri.startsWith("/web/")) return false;
        if (uri.contains("/comprobante") || uri.contains("/excel") || uri.contains("/pdf")) {
            return false;
        }
        return true;
    }
}
