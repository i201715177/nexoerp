package com.farmacia.sistema.api.venta;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class CrearVentaRequest {

    @NotNull
    private Long clienteId;

    private String nombreClienteVenta;

    @NotEmpty
    @Valid
    private List<ItemVentaRequest> items;

    private BigDecimal descuentoTotal;

    @Valid
    private List<PagoRequest> pagos;

    private Long cajaTurnoId;

    /** BOL = Boleta, FAC = Factura. Por defecto Boleta. */
    private String tipoComprobante = "BOL";

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getNombreClienteVenta() {
        return nombreClienteVenta;
    }

    public void setNombreClienteVenta(String nombreClienteVenta) {
        this.nombreClienteVenta = nombreClienteVenta;
    }

    public List<ItemVentaRequest> getItems() {
        return items;
    }

    public void setItems(List<ItemVentaRequest> items) {
        this.items = items;
    }

    public BigDecimal getDescuentoTotal() {
        return descuentoTotal;
    }

    public void setDescuentoTotal(BigDecimal descuentoTotal) {
        this.descuentoTotal = descuentoTotal;
    }

    public List<PagoRequest> getPagos() {
        return pagos;
    }

    public void setPagos(List<PagoRequest> pagos) {
        this.pagos = pagos;
    }

    public Long getCajaTurnoId() {
        return cajaTurnoId;
    }

    public void setCajaTurnoId(Long cajaTurnoId) {
        this.cajaTurnoId = cajaTurnoId;
    }

    public String getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(String tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }
}

