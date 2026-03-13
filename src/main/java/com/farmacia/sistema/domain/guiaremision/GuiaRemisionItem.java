package com.farmacia.sistema.domain.guiaremision;

import com.farmacia.sistema.domain.producto.Producto;
import jakarta.persistence.*;

@Entity
@Table(name = "guia_remision_items", indexes = {
        @Index(name = "idx_gri_guia", columnList = "guia_remision_id"),
        @Index(name = "idx_gri_producto", columnList = "producto_id")
})
public class GuiaRemisionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "guia_remision_id", nullable = false)
    private GuiaRemision guiaRemision;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(length = 20)
    private String unidadMedida = "UND";

    @Column(length = 255)
    private String descripcion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GuiaRemision getGuiaRemision() { return guiaRemision; }
    public void setGuiaRemision(GuiaRemision guiaRemision) { this.guiaRemision = guiaRemision; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
