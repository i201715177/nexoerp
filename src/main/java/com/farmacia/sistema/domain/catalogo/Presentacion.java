package com.farmacia.sistema.domain.catalogo;

import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;

@Entity
@Table(name = "presentaciones", indexes = @Index(name = "idx_pres_tenant", columnList = "tenant_id"))
@EntityListeners(TenantEntityListener.class)
public class Presentacion implements TenantSupport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Override public Long getTenantId() { return tenantId; }
    @Override public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
