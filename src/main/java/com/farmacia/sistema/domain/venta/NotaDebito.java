package com.farmacia.sistema.domain.venta;

import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "notas_debito", indexes = {
        @Index(name = "idx_nd_tenant", columnList = "tenant_id"),
        @Index(name = "idx_nd_venta", columnList = "venta_id")
})
@EntityListeners(TenantEntityListener.class)
public class NotaDebito implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @Column(name = "serie", length = 10, nullable = false)
    private String serie;

    @NotNull
    @Column(nullable = false, length = 20)
    private String numero;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fecha;

    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(length = 255)
    private String motivo;

    @NotNull
    @Column(nullable = false, length = 20)
    private String estado = "EMITIDA";

    @Column(name = "estado_sunat", length = 20)
    private String estadoSunat = "PENDIENTE";

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }
    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getEstadoSunat() { return estadoSunat; }
    public void setEstadoSunat(String estadoSunat) { this.estadoSunat = estadoSunat; }
    @Override public Long getTenantId() { return tenantId; }
    @Override public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getSerieNumero() {
        return (serie != null ? serie : "FD01") + "-" + (numero != null ? numero : "");
    }
}
