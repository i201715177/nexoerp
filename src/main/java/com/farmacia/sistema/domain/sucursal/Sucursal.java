package com.farmacia.sistema.domain.sucursal;

import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "sucursales",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "codigo"}),
        indexes = {
                @Index(name = "idx_suc_tenant", columnList = "tenant_id"),
                @Index(name = "idx_suc_tenant_codigo", columnList = "tenant_id, codigo")
        })
@EntityListeners(TenantEntityListener.class)
public class Sucursal implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String codigo;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String direccion;

    @Column(nullable = false)
    private boolean activa = true;

    /** Sucursal central/almacén: de aquí sale el stock cuando se confirma un requerimiento. Solo una por tenant. */
    @Column(name = "es_central", nullable = false)
    private boolean central = false;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public boolean isCentral() {
        return central;
    }

    public void setCentral(boolean central) {
        this.central = central;
    }

    @Override
    public Long getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}

