package com.farmacia.sistema.domain.digemid;

import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "registro_receta",
        indexes = {
                @Index(name = "idx_reg_receta_tenant", columnList = "tenant_id"),
                @Index(name = "idx_reg_receta_numero", columnList = "numero_receta"),
                @Index(name = "idx_reg_receta_fecha", columnList = "fecha_registro"),
                @Index(name = "idx_reg_receta_venta", columnList = "venta_id")
        })
@EntityListeners(TenantEntityListener.class)
public class RegistroReceta implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_receta", nullable = false, length = 100)
    private String numeroReceta;

    @Column(name = "nombre_medico", length = 200)
    private String nombreMedico;

    @Column(name = "cmp_medico", length = 20)
    private String cmpMedico;

    @Column(name = "especialidad_medico", length = 100)
    private String especialidadMedico;

    @Column(name = "nombre_paciente", length = 200)
    private String nombrePaciente;

    @Column(name = "documento_paciente", length = 20)
    private String documentoPaciente;

    @Column(name = "direccion_paciente", length = 300)
    private String direccionPaciente;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "cantidad_prescrita")
    private Integer cantidadPrescrita;

    @Column(name = "cantidad_vendida", nullable = false)
    private Integer cantidad = 1;

    @Column(name = "fecha_emision_receta")
    private LocalDate fechaEmisionReceta;

    @NotNull
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Column(name = "venta_id")
    private Long ventaId;

    @Column(name = "tipo_receta", length = 30)
    private String tipoReceta;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumeroReceta() { return numeroReceta; }
    public void setNumeroReceta(String numeroReceta) { this.numeroReceta = numeroReceta; }
    public String getNombreMedico() { return nombreMedico; }
    public void setNombreMedico(String nombreMedico) { this.nombreMedico = nombreMedico; }
    public String getCmpMedico() { return cmpMedico; }
    public void setCmpMedico(String cmpMedico) { this.cmpMedico = cmpMedico; }
    public String getEspecialidadMedico() { return especialidadMedico; }
    public void setEspecialidadMedico(String especialidadMedico) { this.especialidadMedico = especialidadMedico; }
    public String getNombrePaciente() { return nombrePaciente; }
    public void setNombrePaciente(String nombrePaciente) { this.nombrePaciente = nombrePaciente; }
    public String getDocumentoPaciente() { return documentoPaciente; }
    public void setDocumentoPaciente(String documentoPaciente) { this.documentoPaciente = documentoPaciente; }
    public String getDireccionPaciente() { return direccionPaciente; }
    public void setDireccionPaciente(String direccionPaciente) { this.direccionPaciente = direccionPaciente; }
    public Integer getCantidadPrescrita() { return cantidadPrescrita; }
    public void setCantidadPrescrita(Integer cantidadPrescrita) { this.cantidadPrescrita = cantidadPrescrita; }
    public LocalDate getFechaEmisionReceta() { return fechaEmisionReceta; }
    public void setFechaEmisionReceta(LocalDate fechaEmisionReceta) { this.fechaEmisionReceta = fechaEmisionReceta; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public Long getVentaId() { return ventaId; }
    public void setVentaId(Long ventaId) { this.ventaId = ventaId; }
    public String getTipoReceta() { return tipoReceta; }
    public void setTipoReceta(String tipoReceta) { this.tipoReceta = tipoReceta; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    @Override
    public Long getTenantId() { return tenantId; }
    @Override
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
