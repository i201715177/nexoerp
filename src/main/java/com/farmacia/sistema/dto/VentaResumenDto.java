package com.farmacia.sistema.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO para listar ventas en la vista con detalle de productos vendidos.
 */
public class VentaResumenDto {

    private Long id;
    private String nombreClienteVenta;
    /** Número de documento del cliente: RUC si factura, DNI si boleta. */
    private String numeroDocumentoCliente;
    private LocalDateTime fechaHora;
    private BigDecimal total;
    private String tipoComprobante;
    private String serieComprobante;
    private String numeroComprobante;
    private BigDecimal descuentoTotal;
    private int itemsCount;
    private String pagosResumen;
    private String estado;
    private List<VentaItemResumenDto> items = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombreClienteVenta() { return nombreClienteVenta; }
    public void setNombreClienteVenta(String nombreClienteVenta) { this.nombreClienteVenta = nombreClienteVenta; }
    public String getNumeroDocumentoCliente() { return numeroDocumentoCliente; }
    public void setNumeroDocumentoCliente(String numeroDocumentoCliente) { this.numeroDocumentoCliente = numeroDocumentoCliente; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }
    public String getSerieComprobante() { return serieComprobante; }
    public void setSerieComprobante(String serieComprobante) { this.serieComprobante = serieComprobante; }
    public String getNumeroComprobante() { return numeroComprobante; }
    public void setNumeroComprobante(String numeroComprobante) { this.numeroComprobante = numeroComprobante; }
    public BigDecimal getDescuentoTotal() { return descuentoTotal; }
    public void setDescuentoTotal(BigDecimal descuentoTotal) { this.descuentoTotal = descuentoTotal; }
    public int getItemsCount() { return itemsCount; }
    public void setItemsCount(int itemsCount) { this.itemsCount = itemsCount; }
    public String getPagosResumen() { return pagosResumen; }
    public void setPagosResumen(String pagosResumen) { this.pagosResumen = pagosResumen; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public List<VentaItemResumenDto> getItems() { return items; }
    public void setItems(List<VentaItemResumenDto> items) { this.items = items != null ? items : new ArrayList<>(); }

    /** Texto completo: "Producto A x 2, Producto B x 1" para detalle en card. */
    public String getItemsDetalle() {
        if (items == null || items.isEmpty()) return "—";
        return items.stream()
                .map(i -> (i.getNombreProducto() != null ? i.getNombreProducto() : "?") + " x " + i.getCantidad())
                .collect(Collectors.joining(", "));
    }

    /** Texto corto para columna tabla (máx. ~60 caracteres). */
    public String getItemsDetalleCorto() {
        String full = getItemsDetalle();
        if (full == null || full.length() <= 60) return full;
        return full.substring(0, 57) + "...";
    }

    public String getComprobante() {
        if (tipoComprobante != null && serieComprobante != null && numeroComprobante != null) {
            return tipoComprobante + "-" + serieComprobante + "-" + numeroComprobante;
        }
        return "-";
    }
}
