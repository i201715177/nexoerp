package com.farmacia.sistema.domain.inventariofisico;

import com.farmacia.sistema.domain.producto.Producto;
import jakarta.persistence.*;

@Entity
@Table(name = "inventario_fisico_detalle")
public class InventarioFisicoDetalle {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inventario_fisico_id", nullable = false)
    private InventarioFisico inventarioFisico;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "stock_sistema", nullable = false)
    private Integer stockSistema;

    @Column(name = "stock_fisico")
    private Integer stockFisico;

    @Column(name = "diferencia")
    private Integer diferencia;

    @Column(length = 300)
    private String observacion;

    /** true = ajuste aplicado al sistema */
    @Column(name = "ajustado", nullable = false)
    private boolean ajustado = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InventarioFisico getInventarioFisico() { return inventarioFisico; }
    public void setInventarioFisico(InventarioFisico inventarioFisico) { this.inventarioFisico = inventarioFisico; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Integer getStockSistema() { return stockSistema; }
    public void setStockSistema(Integer stockSistema) { this.stockSistema = stockSistema; }
    public Integer getStockFisico() { return stockFisico; }
    public void setStockFisico(Integer stockFisico) { this.stockFisico = stockFisico; }
    public Integer getDiferencia() { return diferencia; }
    public void setDiferencia(Integer diferencia) { this.diferencia = diferencia; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public boolean isAjustado() { return ajustado; }
    public void setAjustado(boolean ajustado) { this.ajustado = ajustado; }
}
