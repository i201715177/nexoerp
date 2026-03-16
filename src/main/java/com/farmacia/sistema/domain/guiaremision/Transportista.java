package com.farmacia.sistema.domain.guiaremision;

import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "transportistas", indexes = {
        @Index(name = "idx_transp_tenant", columnList = "tenant_id")
})
@EntityListeners(TenantEntityListener.class)
public class Transportista implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ruc", length = 11)
    private String ruc;

    @NotBlank
    @Column(name = "nombre", length = 200, nullable = false)
    private String nombre;

    @Column(name = "conductor_dni", length = 8)
    private String conductorDni;

    @Column(name = "conductor_nombre", length = 200)
    private String conductorNombre;

    @Column(name = "conductor_licencia", length = 20)
    private String conductorLicencia;

    @Column(name = "placa_vehiculo", length = 15)
    private String placaVehiculo;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getConductorDni() { return conductorDni; }
    public void setConductorDni(String conductorDni) { this.conductorDni = conductorDni; }
    public String getConductorNombre() { return conductorNombre; }
    public void setConductorNombre(String conductorNombre) { this.conductorNombre = conductorNombre; }
    public String getConductorLicencia() { return conductorLicencia; }
    public void setConductorLicencia(String conductorLicencia) { this.conductorLicencia = conductorLicencia; }
    public String getPlacaVehiculo() { return placaVehiculo; }
    public void setPlacaVehiculo(String placaVehiculo) { this.placaVehiculo = placaVehiculo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    @Override public Long getTenantId() { return tenantId; }
    @Override public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
