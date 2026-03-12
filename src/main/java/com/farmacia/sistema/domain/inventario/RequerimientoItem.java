package com.farmacia.sistema.domain.inventario;

import com.farmacia.sistema.domain.producto.Producto;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "requerimiento_items", indexes = {
        @Index(name = "idx_req_item_requerimiento", columnList = "requerimiento_id")
})
public class RequerimientoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "requerimiento_id", nullable = false)
    private Requerimiento requerimiento;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @NotNull
    @Column(nullable = false)
    private Integer cantidad;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Requerimiento getRequerimiento() { return requerimiento; }
    public void setRequerimiento(Requerimiento requerimiento) { this.requerimiento = requerimiento; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}
