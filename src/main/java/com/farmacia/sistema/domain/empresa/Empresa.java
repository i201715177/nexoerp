package com.farmacia.sistema.domain.empresa;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "empresas")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "tipo_documento", length = 10)
    private String tipoDocumento; // RUC, DNI, etc.

    @Column(length = 20)
    private String ruc;

    @Column(length = 255)
    private String direccion;

    @Column(length = 30)
    private String telefono;

    /** Correo del dueño/contacto de la empresa: para enviar recordatorios de vencimiento de suscripción. */
    @Column(name = "email_contacto", length = 255)
    private String emailContacto;

    @Column(nullable = false)
    private boolean activa = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private PlanSuscripcion plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_suscripcion", length = 20)
    private TipoSuscripcion tipoSuscripcion;

    /** Override del límite del plan para esta empresa. Null = usar límite del plan. */
    @Column(name = "max_usuarios")
    private Integer maxUsuarios;

    // ── Facturación electrónica SUNAT (por empresa/tenant) ──

    @Lob
    @Column(name = "certificado_pfx")
    private byte[] certificadoPfx;

    @Column(name = "certificado_password", length = 255)
    private String certificadoPassword;

    @Column(name = "sol_usuario", length = 50)
    private String solUsuario;

    @Column(name = "sol_password", length = 100)
    private String solPassword;

    @Column(name = "sunat_habilitado", nullable = false)
    private boolean sunatHabilitado = false;

    @Column(name = "sunat_modo", length = 20)
    private String sunatModo = "DEMO";

    @Column(name = "serie_factura", length = 10)
    private String serieFactura = "F001";

    @Column(name = "serie_boleta", length = 10)
    private String serieBoleta = "B001";

    @Column(name = "serie_nota_credito", length = 10)
    private String serieNotaCredito = "FC01";

    @Column(name = "serie_nota_debito", length = 10)
    private String serieNotaDebito = "FD01";

    @Column(name = "serie_guia_remision", length = 10)
    private String serieGuiaRemision = "T001";

    @Column(name = "certificado_nombre_archivo", length = 255)
    private String certificadoNombreArchivo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmailContacto() { return emailContacto; }
    public void setEmailContacto(String emailContacto) { this.emailContacto = emailContacto; }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public PlanSuscripcion getPlan() { return plan; }
    public void setPlan(PlanSuscripcion plan) { this.plan = plan; }

    public TipoSuscripcion getTipoSuscripcion() { return tipoSuscripcion; }
    public void setTipoSuscripcion(TipoSuscripcion tipoSuscripcion) { this.tipoSuscripcion = tipoSuscripcion; }

    public Integer getMaxUsuarios() { return maxUsuarios; }
    public void setMaxUsuarios(Integer maxUsuarios) { this.maxUsuarios = maxUsuarios; }

    /** Límite efectivo: maxUsuarios de empresa si está definido, sino del plan; null = sin límite. */
    public Integer getMaxUsuariosEfectivo() {
        if (maxUsuarios != null) return maxUsuarios;
        return plan != null ? plan.getMaxUsuarios() : null;
    }

    public byte[] getCertificadoPfx() { return certificadoPfx; }
    public void setCertificadoPfx(byte[] certificadoPfx) { this.certificadoPfx = certificadoPfx; }
    public String getCertificadoPassword() { return certificadoPassword; }
    public void setCertificadoPassword(String certificadoPassword) { this.certificadoPassword = certificadoPassword; }
    public String getSolUsuario() { return solUsuario; }
    public void setSolUsuario(String solUsuario) { this.solUsuario = solUsuario; }
    public String getSolPassword() { return solPassword; }
    public void setSolPassword(String solPassword) { this.solPassword = solPassword; }
    public boolean isSunatHabilitado() { return sunatHabilitado; }
    public void setSunatHabilitado(boolean sunatHabilitado) { this.sunatHabilitado = sunatHabilitado; }
    public String getSunatModo() { return sunatModo; }
    public void setSunatModo(String sunatModo) { this.sunatModo = sunatModo; }
    public String getSerieFactura() { return serieFactura; }
    public void setSerieFactura(String serieFactura) { this.serieFactura = serieFactura; }
    public String getSerieBoleta() { return serieBoleta; }
    public void setSerieBoleta(String serieBoleta) { this.serieBoleta = serieBoleta; }
    public String getSerieNotaCredito() { return serieNotaCredito; }
    public void setSerieNotaCredito(String serieNotaCredito) { this.serieNotaCredito = serieNotaCredito; }
    public String getSerieNotaDebito() { return serieNotaDebito; }
    public void setSerieNotaDebito(String serieNotaDebito) { this.serieNotaDebito = serieNotaDebito; }
    public String getSerieGuiaRemision() { return serieGuiaRemision; }
    public void setSerieGuiaRemision(String serieGuiaRemision) { this.serieGuiaRemision = serieGuiaRemision; }
    public String getCertificadoNombreArchivo() { return certificadoNombreArchivo; }
    public void setCertificadoNombreArchivo(String certificadoNombreArchivo) { this.certificadoNombreArchivo = certificadoNombreArchivo; }

    public boolean tieneCertificadoConfigurado() {
        return certificadoPfx != null && certificadoPfx.length > 0
                && certificadoPassword != null && !certificadoPassword.isBlank();
    }
}

