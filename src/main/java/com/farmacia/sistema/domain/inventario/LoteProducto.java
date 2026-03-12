package com.farmacia.sistema.domain.inventario;

import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Entity
@Table(name = "lotes_producto", indexes = {
        @Index(name = "idx_lote_tenant", columnList = "tenant_id"),
        @Index(name = "idx_lote_tenant_venc", columnList = "tenant_id, fecha_vencimiento")
})
@EntityListeners(TenantEntityListener.class)
public class LoteProducto implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "numero_lote", nullable = false, length = 50)
    private String numeroLote;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "cantidad_actual", nullable = false)
    private Integer cantidadActual = 0;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public String getNumeroLote() { return numeroLote; }
    public void setNumeroLote(String numeroLote) { this.numeroLote = numeroLote; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public Integer getCantidadActual() { return cantidadActual; }
    public void setCantidadActual(Integer cantidadActual) { this.cantidadActual = cantidadActual; }

    @Override
    public Long getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
