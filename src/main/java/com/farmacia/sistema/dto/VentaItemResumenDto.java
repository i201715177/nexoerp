package com.farmacia.sistema.dto;

/**
 * Resumen de un ítem de venta para mostrar en listados (producto vendido y cantidad).
 */
public class VentaItemResumenDto {

    private String nombreProducto;
    private int cantidad;
    private String subtotal;

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public String getSubtotal() { return subtotal; }
    public void setSubtotal(String subtotal) { this.subtotal = subtotal; }
}
