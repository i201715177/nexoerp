package com.farmacia.sistema.domain.caja;

import com.farmacia.sistema.domain.sucursal.Sucursal;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "caja_turnos", indexes = {
        @Index(name = "idx_caja_tenant", columnList = "tenant_id"),
        @Index(name = "idx_caja_tenant_estado", columnList = "tenant_id, estado"),
        @Index(name = "idx_caja_tenant_fecha", columnList = "tenant_id, fecha_apertura")
})
@EntityListeners(TenantEntityListener.class)
public class CajaTurno implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @NotNull
    @Column(name = "monto_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoInicial;

    @Column(name = "monto_cierre", precision = 12, scale = 2)
    private BigDecimal montoCierre;

    @NotNull
    @Column(nullable = false, length = 20)
    private String estado = "ABIERTO";

    @Column(length = 255)
    private String observaciones;

    @ManyToOne
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @Column(name = "usuario", length = 50)
    private String usuario;

    /** Nombre del vendedor/cajero que atiende (puede ser distinto del usuario logueado). */
    @Column(name = "nombre_vendedor", length = 100)
    private String nombreVendedor;

    /** Si true, no se muestra en el historial de turnos (solo oculto; auditoría y ventas se mantienen). */
    @Column(name = "oculto_en_historial", nullable = false)
    private boolean ocultoEnHistorial = false;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public BigDecimal getMontoInicial() {
        return montoInicial;
    }

    public void setMontoInicial(BigDecimal montoInicial) {
        this.montoInicial = montoInicial;
    }

    public BigDecimal getMontoCierre() {
        return montoCierre;
    }

    public void setMontoCierre(BigDecimal montoCierre) {
        this.montoCierre = montoCierre;
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

    public Sucursal getSucursal() {
        return sucursal;
    }

    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getNombreVendedor() {
        return nombreVendedor;
    }

    public void setNombreVendedor(String nombreVendedor) {
        this.nombreVendedor = nombreVendedor;
    }

    public boolean isOcultoEnHistorial() {
        return ocultoEnHistorial;
    }

    public void setOcultoEnHistorial(boolean ocultoEnHistorial) {
        this.ocultoEnHistorial = ocultoEnHistorial;
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
