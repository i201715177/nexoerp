package com.farmacia.sistema.domain.listaprecio;

import com.farmacia.sistema.domain.producto.Producto;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lista_precio_detalle")
public class ListaPrecioDetalle {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lista_precio_id", nullable = false)
    private ListaPrecio listaPrecio;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "precio_especial", precision = 12, scale = 2)
    private BigDecimal precioEspecial;

    @Column(name = "descuento_porcentaje")
    private Double descuentoPorcentaje;

    @Column(name = "cantidad_minima")
    private Integer cantidadMinima;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ListaPrecio getListaPrecio() { return listaPrecio; }
    public void setListaPrecio(ListaPrecio lp) { this.listaPrecio = lp; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public BigDecimal getPrecioEspecial() { return precioEspecial; }
    public void setPrecioEspecial(BigDecimal v) { this.precioEspecial = v; }
    public Double getDescuentoPorcentaje() { return descuentoPorcentaje; }
    public void setDescuentoPorcentaje(Double v) { this.descuentoPorcentaje = v; }
    public Integer getCantidadMinima() { return cantidadMinima; }
    public void setCantidadMinima(Integer v) { this.cantidadMinima = v; }
}
