package com.farmacia.sistema.domain.empresa;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_suscripcion", indexes = {
        @Index(name = "idx_solicitud_fecha", columnList = "fecha_solicitud"),
        @Index(name = "idx_solicitud_estado", columnList = "estado")
})
public class SolicitudSuscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "nombre_contacto", nullable = false, length = 150)
    private String nombreContacto;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String email;

    @Size(max = 30)
    @Column(length = 30)
    private String telefono;

    @NotBlank
    @Column(name = "nombre_empresa", nullable = false, length = 150)
    private String nombreEmpresa;

    @Column(name = "tipo_documento", length = 10)
    private String tipoDocumento; // DNI, RUC, etc.

    @Column(name = "numero_documento", length = 20)
    private String numeroDocumento;

    @Column(name = "plan_deseado", length = 50)
    private String planDeseado;

    @Column(length = 500)
    private String mensaje;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    /** PENDIENTE, CONTACTADO, CONVERTIDA (ya creó empresa), RECHAZADA */
    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombreContacto() { return nombreContacto; }
    public void setNombreContacto(String nombreContacto) { this.nombreContacto = nombreContacto; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getNombreEmpresa() { return nombreEmpresa; }
    public void setNombreEmpresa(String nombreEmpresa) { this.nombreEmpresa = nombreEmpresa; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public String getPlanDeseado() { return planDeseado; }
    public void setPlanDeseado(String planDeseado) { this.planDeseado = planDeseado; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
