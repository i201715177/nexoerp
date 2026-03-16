package com.farmacia.sistema.domain.temperatura;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Clave de API para recibir lecturas de temperatura desde sensores/gateway.
 * Una por tenant (empresa).
 */
@Entity
@Table(name = "config_temperatura_api", indexes = {
        @Index(name = "idx_cta_tenant", columnList = "tenant_id"),
        @Index(name = "idx_cta_apikey", columnList = "api_key", unique = true)
})
public class ConfiguracionTemperaturaApi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private Long tenantId;

    @Column(name = "api_key", nullable = false, unique = true, length = 64)
    private String apiKey;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @PrePersist
    void onPersist() {
        if (fechaGeneracion == null) fechaGeneracion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
}
