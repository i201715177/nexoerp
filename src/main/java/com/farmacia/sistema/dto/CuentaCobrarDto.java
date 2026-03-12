package com.farmacia.sistema.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para mostrar una venta con saldo pendiente (cuenta por cobrar) sin depender de entidades JPA en la vista.
 */
public class CuentaCobrarDto {

    private Long ventaId;
    private String nombreCliente;
    private String nombreProductos;
    private LocalDateTime fechaHora;
    private BigDecimal total;
    private BigDecimal pagado;
    private BigDecimal saldo;

    public Long getVentaId() { return ventaId; }
    public void setVentaId(Long ventaId) { this.ventaId = ventaId; }
    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
    public String getNombreProductos() { return nombreProductos; }
    public void setNombreProductos(String nombreProductos) { this.nombreProductos = nombreProductos; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public BigDecimal getPagado() { return pagado; }
    public void setPagado(BigDecimal pagado) { this.pagado = pagado; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
}
