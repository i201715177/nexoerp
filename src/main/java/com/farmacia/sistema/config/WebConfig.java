package com.farmacia.sistema.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuditoriaInterceptor auditoriaInterceptor;
    private final TenantInterceptor tenantInterceptor;

    public WebConfig(AuditoriaInterceptor auditoriaInterceptor,
                     TenantInterceptor tenantInterceptor) {
        this.auditoriaInterceptor = auditoriaInterceptor;
        this.tenantInterceptor = tenantInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/web/**", "/api/**");
        registry.addInterceptor(auditoriaInterceptor)
                .addPathPatterns("/web/**", "/api/**");
    }
}

