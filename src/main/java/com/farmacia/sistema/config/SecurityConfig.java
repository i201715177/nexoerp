package com.farmacia.sistema.config;

import com.farmacia.sistema.security.JwtAuthenticationFilter;
import com.farmacia.sistema.security.TenantUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.Collection;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error", "/favicon.ico").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("SAAS_ADMIN")
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/solicitar-suscripcion", "/solicitar-suscripcion/**").permitAll()
                        .requestMatchers("/web/admin/**").hasAnyRole("SAAS_ADMIN", "GERENTE")
                        .requestMatchers("/web/compras/**").hasAnyRole("ADMIN", "SAAS_ADMIN")
                        .requestMatchers("/web/proveedores/**").hasAnyRole("ADMIN", "SAAS_ADMIN")
                        .requestMatchers("/web/finanzas/**").hasAnyRole("ADMIN", "SAAS_ADMIN")
                        .requestMatchers("/web/sucursales/**").hasAnyRole("ADMIN", "SAAS_ADMIN")
                        .requestMatchers("/web/auditoria/**").hasAnyRole("ADMIN", "SAAS_ADMIN")
                        .requestMatchers("/web/**").hasAnyRole("ADMIN", "SAAS_ADMIN", "VENDEDOR")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(tenantAuthenticationSuccessHandler())
                        .permitAll()
                )
                .logout(logout -> logout.permitAll())
                .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler()));

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .contentTypeOptions(c -> {})
        );

        return http.build();
    }

    /**
     * Al hacer login exitoso, guarda el tenantId del usuario en la sesion HTTP
     * para que el TenantInterceptor lo use automaticamente.
     */
    @Bean
    public AuthenticationSuccessHandler tenantAuthenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                Authentication authentication) throws IOException {
                if (authentication.getPrincipal() instanceof TenantUserDetails tud) {
                    request.getSession().setAttribute("TENANT_ID", tud.getTenantId());
                }
                // Gerente solo ve Admin SaaS, Facturación y Solicitudes → ir directo ahí
                boolean esSoloGerente = authentication.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_GERENTE".equals(a.getAuthority()))
                        && authentication.getAuthorities().stream()
                        .noneMatch(a -> "ROLE_SAAS_ADMIN".equals(a.getAuthority()));
                if (esSoloGerente) {
                    response.sendRedirect(request.getContextPath() + "/web/admin/solicitudes");
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/web/productos");
            }
        };
    }

    /**
     * Cuando un usuario hace clic en algo no permitido (ej. gerente en Productos),
     * en lugar de mostrar la página de error 403, redirige a una pantalla permitida
     * con un mensaje en la misma interfaz.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            String contextPath = request.getContextPath();
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            boolean esSoloGerente = auth != null && auth.getAuthorities() != null
                    && auth.getAuthorities().stream().anyMatch(a -> "ROLE_GERENTE".equals(a.getAuthority()))
                    && auth.getAuthorities().stream().noneMatch(a -> "ROLE_SAAS_ADMIN".equals(a.getAuthority()));
            String redirectUrl = esSoloGerente
                    ? contextPath + "/web/admin/solicitudes?error=sin_permiso"
                    : contextPath + "/web/productos?error=sin_permiso";
            response.sendRedirect(redirectUrl);
        };
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }
}

