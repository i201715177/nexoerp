package com.farmacia.sistema.domain.devolucion;

import com.farmacia.sistema.domain.proveedor.Proveedor;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "devolucion_proveedor", indexes = {
        @Index(name = "idx_devprov_tenant", columnList = "tenant_id")
})
@EntityListeners(TenantEntityListener.class)
public class DevolucionProveedor implements TenantSupport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(nullable = false)
    private LocalDateTime fecha;

    /** PENDIENTE, ENVIADA, ACEPTADA, RECHAZADA */
    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    /** PRODUCTO_DANADO, PRODUCTO_VENCIDO, ERROR_PEDIDO, DEFECTUOSO, OTRO */
    @Column(nullable = false, length = 30)
    private String motivo;

    @Column(precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "nota_credito_proveedor", length = 50)
    private String notaCreditoProveedor;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "usuario_registro", length = 100)
    private String usuarioRegistro;

    @OneToMany(mappedBy = "devolucion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DevolucionProveedorItem> items = new ArrayList<>();

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Override public Long getTenantId() { return tenantId; }
    @Override public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public Proveedor getProveedor() { return proveedor; }
    public void setProveedor(Proveedor proveedor) { this.proveedor = proveedor; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getNotaCreditoProveedor() { return notaCreditoProveedor; }
    public void setNotaCreditoProveedor(String s) { this.notaCreditoProveedor = s; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
    public List<DevolucionProveedorItem> getItems() { return items; }
    public void setItems(List<DevolucionProveedorItem> items) { this.items = items; }
}
