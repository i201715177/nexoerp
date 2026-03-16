package com.farmacia.sistema.domain.temperatura;

import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "registro_temperatura", indexes = {
        @Index(name = "idx_rt_tenant", columnList = "tenant_id"),
        @Index(name = "idx_rt_zona", columnList = "zona_id")
})
@EntityListeners(TenantEntityListener.class)
public class RegistroTemperatura implements TenantSupport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "zona_id", nullable = false)
    private ZonaAlmacen zona;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal temperatura;

    @Column(precision = 5, scale = 2)
    private BigDecimal humedad;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "fuera_rango", nullable = false)
    private boolean fueraRango = false;

    @Column(length = 300)
    private String observacion;

    @Column(name = "usuario_registro", length = 100)
    private String usuarioRegistro;

    /** MANUAL = formulario; SENSOR = API/gateway */
    @Column(name = "origen", length = 20, nullable = false, columnDefinition = "varchar(20) default 'MANUAL'")
    private String origen = "MANUAL";

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Override public Long getTenantId() { return tenantId; }
    @Override public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ZonaAlmacen getZona() { return zona; }
    public void setZona(ZonaAlmacen zona) { this.zona = zona; }
    public BigDecimal getTemperatura() { return temperatura; }
    public void setTemperatura(BigDecimal temperatura) { this.temperatura = temperatura; }
    public BigDecimal getHumedad() { return humedad; }
    public void setHumedad(BigDecimal humedad) { this.humedad = humedad; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public boolean isFueraRango() { return fueraRango; }
    public void setFueraRango(boolean fueraRango) { this.fueraRango = fueraRango; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen != null ? origen : "MANUAL"; }
}
