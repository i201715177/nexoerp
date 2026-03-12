package com.farmacia.sistema.domain.usuario;

import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "usuarios", indexes = {
        @Index(name = "idx_usr_tenant", columnList = "tenant_id"),
        @Index(name = "idx_usr_username", columnList = "username")
})
@EntityListeners(TenantEntityListener.class)
public class Usuario implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @Column(length = 150)
    private String nombreCompleto;

    @NotBlank
    @Column(nullable = false, length = 30)
    private String rol = "VENDEDOR";

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /** Sucursal asignada (obligatoria para VENDEDOR; opcional para ADMIN/SAAS_ADMIN). */
    @Column(name = "sucursal_id")
    private Long sucursalId;

    @Override
    public Long getTenantId() { return tenantId; }

    @Override
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public Long getSucursalId() { return sucursalId; }
    public void setSucursalId(Long sucursalId) { this.sucursalId = sucursalId; }
}
