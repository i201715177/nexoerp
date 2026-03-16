package com.farmacia.sistema.domain.devolucion;

import com.farmacia.sistema.domain.producto.Producto;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "devolucion_proveedor_items")
public class DevolucionProveedorItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "devolucion_id", nullable = false)
    private DevolucionProveedor devolucion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "costo_unitario", precision = 12, scale = 2)
    private BigDecimal costoUnitario;

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(length = 50)
    private String lote;

    @Column(length = 300)
    private String motivo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DevolucionProveedor getDevolucion() { return devolucion; }
    public void setDevolucion(DevolucionProveedor d) { this.devolucion = d; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
