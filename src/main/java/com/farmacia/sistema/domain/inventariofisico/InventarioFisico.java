package com.farmacia.sistema.domain.inventariofisico;

import com.farmacia.sistema.domain.inventario.Almacen;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventario_fisico", indexes = {
        @Index(name = "idx_invf_tenant", columnList = "tenant_id")
})
@EntityListeners(TenantEntityListener.class)
public class InventarioFisico implements TenantSupport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    /** ABIERTO, EN_PROCESO, CERRADO, AJUSTADO */
    @Column(nullable = false, length = 20)
    private String estado = "ABIERTO";

    @ManyToOne @JoinColumn(name = "almacen_id")
    private Almacen almacen;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "usuario_registro", length = 100)
    private String usuarioRegistro;

    @Column(name = "usuario_cierre", length = 100)
    private String usuarioCierre;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @OneToMany(mappedBy = "inventarioFisico", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventarioFisicoDetalle> detalles = new ArrayList<>();

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Override public Long getTenantId() { return tenantId; }
    @Override public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Almacen getAlmacen() { return almacen; }
    public void setAlmacen(Almacen almacen) { this.almacen = almacen; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
    public String getUsuarioCierre() { return usuarioCierre; }
    public void setUsuarioCierre(String usuarioCierre) { this.usuarioCierre = usuarioCierre; }
    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }
    public List<InventarioFisicoDetalle> getDetalles() { return detalles; }
    public void setDetalles(List<InventarioFisicoDetalle> detalles) { this.detalles = detalles; }
}
