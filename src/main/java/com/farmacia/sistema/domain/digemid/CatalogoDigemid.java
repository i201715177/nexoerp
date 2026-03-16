package com.farmacia.sistema.domain.digemid;

import jakarta.persistence.*;

@Entity
@Table(name = "catalogo_digemid", indexes = {
        @Index(name = "idx_catdig_principio", columnList = "principio_activo"),
        @Index(name = "idx_catdig_nombre", columnList = "nombre_comercial")
})
public class CatalogoDigemid {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "principio_activo", nullable = false, length = 200)
    private String principioActivo;

    @Column(name = "nombre_comercial", length = 200)
    private String nombreComercial;

    /** ESTUPEFACIENTE, PSICOTROPICO, SUJETO_FISCALIZACION, PRECURSOR */
    @Column(name = "tipo_producto_controlado", length = 30)
    private String tipoProductoControlado;

    /** LISTA_I, LISTA_II, LISTA_III, LISTA_IV */
    @Column(name = "lista_control", length = 20)
    private String listaControl;

    @Column(name = "requiere_receta", nullable = false)
    private boolean requiereReceta = true;

    /** RECETA_SIMPLE, RECETA_RETENIDA, RECETA_ESPECIAL, RECETA_ESPECIAL_NUMERADA */
    @Column(name = "tipo_receta", nullable = false, length = 40)
    private String tipoReceta;

    @Column(name = "control_stock_especial", nullable = false)
    private boolean controlStockEspecial = true;

    @Column(length = 500)
    private String observacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPrincipioActivo() { return principioActivo; }
    public void setPrincipioActivo(String v) { this.principioActivo = v; }
    public String getNombreComercial() { return nombreComercial; }
    public void setNombreComercial(String v) { this.nombreComercial = v; }
    public String getTipoProductoControlado() { return tipoProductoControlado; }
    public void setTipoProductoControlado(String v) { this.tipoProductoControlado = v; }
    public String getListaControl() { return listaControl; }
    public void setListaControl(String v) { this.listaControl = v; }
    public boolean isRequiereReceta() { return requiereReceta; }
    public void setRequiereReceta(boolean v) { this.requiereReceta = v; }
    public String getTipoReceta() { return tipoReceta; }
    public void setTipoReceta(String v) { this.tipoReceta = v; }
    public boolean isControlStockEspecial() { return controlStockEspecial; }
    public void setControlStockEspecial(boolean v) { this.controlStockEspecial = v; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String v) { this.observacion = v; }
}
