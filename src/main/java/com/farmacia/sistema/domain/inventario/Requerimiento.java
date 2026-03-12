package com.farmacia.sistema.domain.inventario;

import com.farmacia.sistema.domain.sucursal.Sucursal;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Solicitud de una sucursal a la empresa central para recibir productos (stock).
 * Estados: PENDIENTE → EN_CAMINO (central envió transferencia) → RECIBIDO.
 */
@Entity
@Table(name = "requerimientos", indexes = {
        @Index(name = "idx_req_tenant", columnList = "tenant_id"),
        @Index(name = "idx_req_sucursal_estado", columnList = "tenant_id, sucursal_id, estado")
})
@EntityListeners(TenantEntityListener.class)
public class Requerimiento implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    /** PENDIENTE, EN_CAMINO, RECIBIDO */
    @NotNull
    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @NotNull
    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "fecha_en_camino")
    private LocalDateTime fechaEnCamino;

    @Column(name = "fecha_recibido")
    private LocalDateTime fechaRecibido;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @OneToMany(mappedBy = "requerimiento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequerimientoItem> items = new ArrayList<>();

    @Override
    public Long getTenantId() { return tenantId; }
    @Override
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Sucursal getSucursal() { return sucursal; }
    public void setSucursal(Sucursal sucursal) { this.sucursal = sucursal; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }
    public LocalDateTime getFechaEnCamino() { return fechaEnCamino; }
    public void setFechaEnCamino(LocalDateTime fechaEnCamino) { this.fechaEnCamino = fechaEnCamino; }
    public LocalDateTime getFechaRecibido() { return fechaRecibido; }
    public void setFechaRecibido(LocalDateTime fechaRecibido) { this.fechaRecibido = fechaRecibido; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public List<RequerimientoItem> getItems() { return items; }
    public void setItems(List<RequerimientoItem> items) { this.items = items != null ? items : new ArrayList<>(); }
}
