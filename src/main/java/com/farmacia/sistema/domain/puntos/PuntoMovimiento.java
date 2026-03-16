package com.farmacia.sistema.domain.puntos;

import com.farmacia.sistema.domain.cliente.Cliente;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "punto_movimientos", indexes = {
        @Index(name = "idx_pm_tenant", columnList = "tenant_id"),
        @Index(name = "idx_pm_cliente", columnList = "cliente_id")
})
@EntityListeners(TenantEntityListener.class)
public class PuntoMovimiento implements TenantSupport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    /** ACUMULACION, CANJE, VENCIMIENTO, AJUSTE */
    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private Integer puntos;

    @Column(length = 200)
    private String referencia;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Override public Long getTenantId() { return tenantId; }
    @Override public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Integer getPuntos() { return puntos; }
    public void setPuntos(Integer puntos) { this.puntos = puntos; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
