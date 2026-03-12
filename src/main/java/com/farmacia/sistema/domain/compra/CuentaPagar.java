package com.farmacia.sistema.domain.compra;

import com.farmacia.sistema.domain.proveedor.Proveedor;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cuentas_pagar", indexes = {
        @Index(name = "idx_cp_tenant", columnList = "tenant_id"),
        @Index(name = "idx_cp_tenant_estado", columnList = "tenant_id, estado"),
        @Index(name = "idx_cp_tenant_venc", columnList = "tenant_id, fecha_vencimiento")
})
@EntityListeners(TenantEntityListener.class)
public class CuentaPagar implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @ManyToOne
    @JoinColumn(name = "orden_compra_id")
    private OrdenCompra ordenCompra;

    @NotNull
    @Column(nullable = false)
    private LocalDate fechaEmision;

    @Column
    private LocalDate fechaVencimiento;

    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoPendiente;

    @Column(length = 20)
    private String estado = "PENDIENTE"; // PENDIENTE, PAGADA, ANULADA

    @Column(length = 255)
    private String observaciones;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public OrdenCompra getOrdenCompra() {
        return ordenCompra;
    }

    public void setOrdenCompra(OrdenCompra ordenCompra) {
        this.ordenCompra = ordenCompra;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public BigDecimal getSaldoPendiente() {
        return saldoPendiente;
    }

    public void setSaldoPendiente(BigDecimal saldoPendiente) {
        this.saldoPendiente = saldoPendiente;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
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

