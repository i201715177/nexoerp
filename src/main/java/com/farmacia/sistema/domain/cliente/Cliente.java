package com.farmacia.sistema.domain.cliente;

import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "clientes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "numero_documento"}),
        indexes = {
                @Index(name = "idx_cli_tenant", columnList = "tenant_id"),
                @Index(name = "idx_cli_tenant_doc", columnList = "tenant_id, numero_documento")
        })
@EntityListeners(TenantEntityListener.class)
public class Cliente implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String tipoDocumento;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String numeroDocumento;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String nombres;

    @Size(max = 150)
    @Column(length = 150)
    private String apellidos;

    @Size(max = 20)
    @Column(length = 20)
    private String telefono;

    @Email
    @Size(max = 150)
    @Column(length = 150)
    private String email;

    @Size(max = 255)
    @Column(length = 255)
    private String direccion;

    /** Puntos acumulados del programa de fidelización. */
    @Column(nullable = false)
    private Integer puntos = 0;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Integer getPuntos() {
        return puntos;
    }

    public void setPuntos(Integer puntos) {
        this.puntos = puntos;
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

