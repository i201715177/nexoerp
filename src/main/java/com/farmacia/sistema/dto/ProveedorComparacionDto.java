package com.farmacia.sistema.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProveedorComparacionDto {

    private Long proveedorId;
    private String proveedorNombre;
    private BigDecimal ultimoPrecio;
    private Integer cantidad;
    private LocalDateTime fechaUltimaCompra;

    public Long getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Long proveedorId) {
        this.proveedorId = proveedorId;
    }

    public String getProveedorNombre() {
        return proveedorNombre;
    }

    public void setProveedorNombre(String proveedorNombre) {
        this.proveedorNombre = proveedorNombre;
    }

    public BigDecimal getUltimoPrecio() {
        return ultimoPrecio;
    }

    public void setUltimoPrecio(BigDecimal ultimoPrecio) {
        this.ultimoPrecio = ultimoPrecio;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public LocalDateTime getFechaUltimaCompra() {
        return fechaUltimaCompra;
    }

    public void setFechaUltimaCompra(LocalDateTime fechaUltimaCompra) {
        this.fechaUltimaCompra = fechaUltimaCompra;
    }
}

