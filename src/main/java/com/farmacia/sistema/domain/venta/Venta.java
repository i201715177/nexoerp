package com.farmacia.sistema.domain.venta;

import com.farmacia.sistema.domain.caja.CajaTurno;
import com.farmacia.sistema.domain.cliente.Cliente;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "ventas", indexes = {
        @Index(name = "idx_venta_tenant", columnList = "tenant_id"),
        @Index(name = "idx_venta_tenant_fecha", columnList = "tenant_id, fecha_hora"),
        @Index(name = "idx_venta_tenant_estado", columnList = "tenant_id, estado")
})
@EntityListeners(TenantEntityListener.class)
public class Venta implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "nombre_cliente_venta", length = 255)
    private String nombreClienteVenta;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuentoTotal = BigDecimal.ZERO;

    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 20)
    private String estado = "EMITIDA";

    @Column(name = "tipo_comprobante", length = 10)
    private String tipoComprobante;

    @Column(name = "serie_comprobante", length = 10)
    private String serieComprobante;

    @Column(name = "numero_comprobante", length = 20)
    private String numeroComprobante;

    @Column(name = "estado_sunat", length = 20)
    private String estadoSunat;

    @ManyToOne
    @JoinColumn(name = "caja_turno_id")
    private CajaTurno cajaTurno;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VentaItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagoVenta> pagos = new ArrayList<>();

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public String getNombreClienteVenta() {
        return nombreClienteVenta;
    }

    public void setNombreClienteVenta(String nombreClienteVenta) {
        this.nombreClienteVenta = nombreClienteVenta;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDescuentoTotal() {
        return descuentoTotal;
    }

    public void setDescuentoTotal(BigDecimal descuentoTotal) {
        this.descuentoTotal = descuentoTotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(String tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public String getSerieComprobante() {
        return serieComprobante;
    }

    public void setSerieComprobante(String serieComprobante) {
        this.serieComprobante = serieComprobante;
    }

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    public void setNumeroComprobante(String numeroComprobante) {
        this.numeroComprobante = numeroComprobante;
    }

    public String getEstadoSunat() {
        return estadoSunat;
    }

    public void setEstadoSunat(String estadoSunat) {
        this.estadoSunat = estadoSunat;
    }

    public CajaTurno getCajaTurno() {
        return cajaTurno;
    }

    public void setCajaTurno(CajaTurno cajaTurno) {
        this.cajaTurno = cajaTurno;
    }

    public List<VentaItem> getItems() {
        return items;
    }

    public List<PagoVenta> getPagos() {
        return pagos;
    }

    public void setPagos(List<PagoVenta> pagos) {
        this.pagos = pagos;
    }

    public String getPagosResumen() {
        if (pagos == null || pagos.isEmpty()) return "-";
        return pagos.stream()
                .map(p -> p.getMedioPago() + ": S/" + (p.getMonto() != null ? p.getMonto().toString() : "0"))
                .collect(Collectors.joining(", "));
    }

    public void setItems(List<VentaItem> items) {
        this.items = items;
    }

    @Override
    public Long getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}

