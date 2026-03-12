package com.farmacia.sistema.domain.empresa;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "facturas_saas", indexes = {
        @Index(name = "idx_factura_empresa", columnList = "empresa_id"),
        @Index(name = "idx_factura_estado", columnList = "estado")
})
public class FacturaSaaS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanSuscripcion plan;

    @NotNull
    @Column(name = "periodo_desde", nullable = false)
    private LocalDate periodoDesde;

    @NotNull
    @Column(name = "periodo_hasta", nullable = false)
    private LocalDate periodoHasta;

    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoFactura estado = EstadoFactura.PENDIENTE;

    @Column(name = "numero_factura", length = 30)
    private String numeroFactura;

    @NotNull
    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public PlanSuscripcion getPlan() { return plan; }
    public void setPlan(PlanSuscripcion plan) { this.plan = plan; }

    public LocalDate getPeriodoDesde() { return periodoDesde; }
    public void setPeriodoDesde(LocalDate periodoDesde) { this.periodoDesde = periodoDesde; }

    public LocalDate getPeriodoHasta() { return periodoHasta; }
    public void setPeriodoHasta(LocalDate periodoHasta) { this.periodoHasta = periodoHasta; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public EstadoFactura getEstado() { return estado; }
    public void setEstado(EstadoFactura estado) { this.estado = estado; }

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
}
