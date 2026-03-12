package com.farmacia.sistema.domain.inventario;

import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;

@Entity
@Table(name = "stock_almacen",
        uniqueConstraints = @UniqueConstraint(columnNames = {"almacen_id", "producto_id"}),
        indexes = {
                @Index(name = "idx_stock_tenant", columnList = "tenant_id"),
                @Index(name = "idx_stock_tenant_prod", columnList = "tenant_id, producto_id")
        })
@EntityListeners(TenantEntityListener.class)
public class StockAlmacen implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad = 0;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Almacen getAlmacen() { return almacen; }
    public void setAlmacen(Almacen almacen) { this.almacen = almacen; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    @Override
    public Long getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
