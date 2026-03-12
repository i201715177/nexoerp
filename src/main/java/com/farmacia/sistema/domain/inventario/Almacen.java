package com.farmacia.sistema.domain.inventario;

import com.farmacia.sistema.domain.sucursal.Sucursal;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "almacenes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "codigo"}),
        indexes = {
                @Index(name = "idx_alm_tenant", columnList = "tenant_id"),
                @Index(name = "idx_alm_tenant_codigo", columnList = "tenant_id, codigo")
        })
@EntityListeners(TenantEntityListener.class)
public class Almacen implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String codigo;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private boolean principal = false;

    @ManyToOne
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public boolean isPrincipal() { return principal; }
    public void setPrincipal(boolean principal) { this.principal = principal; }
    public Sucursal getSucursal() { return sucursal; }
    public void setSucursal(Sucursal sucursal) { this.sucursal = sucursal; }

    @Override
    public Long getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
