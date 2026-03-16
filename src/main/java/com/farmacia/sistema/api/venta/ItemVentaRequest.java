package com.farmacia.sistema.api.venta;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ItemVentaRequest {

    @NotNull
    private Long productoId;

    @NotNull
    @Min(1)
    private Integer cantidad;

    private BigDecimal precioUnitario;

    private BigDecimal descuento;

    /** Para productos controlados DIGEMID: número de receta (obligatorio si requiere receta). */
    private String numeroReceta;
    private String tipoReceta;
    private String nombreMedico;
    private String cmpMedico;
    private String especialidadMedico;
    private String nombrePaciente;
    private String documentoPaciente;
    private String direccionPaciente;
    private Integer cantidadPrescrita;

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public String getNumeroReceta() { return numeroReceta; }
    public void setNumeroReceta(String numeroReceta) { this.numeroReceta = numeroReceta; }
    public String getTipoReceta() { return tipoReceta; }
    public void setTipoReceta(String tipoReceta) { this.tipoReceta = tipoReceta; }
    public String getNombreMedico() { return nombreMedico; }
    public void setNombreMedico(String nombreMedico) { this.nombreMedico = nombreMedico; }
    public String getNombrePaciente() { return nombrePaciente; }
    public void setNombrePaciente(String nombrePaciente) { this.nombrePaciente = nombrePaciente; }
    public String getDocumentoPaciente() { return documentoPaciente; }
    public void setDocumentoPaciente(String documentoPaciente) { this.documentoPaciente = documentoPaciente; }
    public String getCmpMedico() { return cmpMedico; }
    public void setCmpMedico(String cmpMedico) { this.cmpMedico = cmpMedico; }
    public String getEspecialidadMedico() { return especialidadMedico; }
    public void setEspecialidadMedico(String especialidadMedico) { this.especialidadMedico = especialidadMedico; }
    public String getDireccionPaciente() { return direccionPaciente; }
    public void setDireccionPaciente(String direccionPaciente) { this.direccionPaciente = direccionPaciente; }
    public Integer getCantidadPrescrita() { return cantidadPrescrita; }
    public void setCantidadPrescrita(Integer cantidadPrescrita) { this.cantidadPrescrita = cantidadPrescrita; }
}

