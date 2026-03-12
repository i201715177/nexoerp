package com.farmacia.sistema.domain.inventario;

import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventario_movimientos", indexes = {
        @Index(name = "idx_invmov_tenant", columnList = "tenant_id"),
        @Index(name = "idx_invmov_tenant_fecha", columnList = "tenant_id, fecha"),
        @Index(name = "idx_invmov_tenant_prod", columnList = "tenant_id, producto_id")
})
@EntityListeners(TenantEntityListener.class)
public class InventarioMovimiento implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne
    @JoinColumn(name = "almacen_id")
    private Almacen almacen;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    /** ENTRADA, SALIDA, AJUSTE_ENTRADA, AJUSTE_SALIDA, TRANSFERENCIA_ENTRADA, TRANSFERENCIA_SALIDA */
    @NotNull
    @Column(nullable = false, length = 30)
    private String tipo;

    @NotNull
    @Column(nullable = false)
    private Integer cantidad;

    /** Saldo en almacén (o global) después del movimiento */
    @Column(name = "saldo_despues")
    private Integer saldoDespues;

    @Column(length = 255)
    private String referencia;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Almacen getAlmacen() { return almacen; }
    public void setAlmacen(Almacen almacen) { this.almacen = almacen; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public Integer getSaldoDespues() { return saldoDespues; }
    public void setSaldoDespues(Integer saldoDespues) { this.saldoDespues = saldoDespues; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    @Override
    public Long getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
