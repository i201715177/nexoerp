package com.farmacia.sistema.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.farmacia.sistema.tenant.TenantContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // No usar JWT en rutas web: la autenticación debe venir solo de la sesión (form login).
        // Así se evita que un token en cabecera sobrescriba al usuario de sesión (ej. vendedor)
        // y haga que aparezcan pestañas de otro rol tras una acción.
        if (request.getRequestURI().startsWith(request.getContextPath() + "/web")
                || request.getRequestURI().startsWith("/web")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = resolveToken(request);
        if (jwt != null && tokenProvider.validateToken(jwt)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String username = tokenProvider.getUsername(jwt);
            String roles = tokenProvider.getRoles(jwt);
            List<SimpleGrantedAuthority> authorities = Arrays.stream(roles.split(","))
                    .filter(r -> !r.isBlank())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            Long tenantId = tokenProvider.getTenantId(jwt);
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    TenantContext.clear();
                }
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

