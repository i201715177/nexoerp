package com.farmacia.sistema.domain.bpa;

import com.farmacia.sistema.domain.inventario.Almacen;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "checklist_bpa", indexes = @Index(name = "idx_bpa_tenant", columnList = "tenant_id"))
@EntityListeners(TenantEntityListener.class)
public class ChecklistBPA implements TenantSupport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @ManyToOne @JoinColumn(name = "almacen_id")
    private Almacen almacen;

    @Column(nullable = false)
    private LocalDate fecha;

    /** LIMPIEZA, FUMIGACION, INSPECCION, CAPACITACION */
    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(name = "condicion_piso", nullable = false)
    private boolean condicionPiso = true;

    @Column(name = "condicion_paredes", nullable = false)
    private boolean condicionParedes = true;

    @Column(name = "condicion_techo", nullable = false)
    private boolean condicionTecho = true;

    @Column(name = "iluminacion_adecuada", nullable = false)
    private boolean iluminacionAdecuada = true;

    @Column(name = "ventilacion_adecuada", nullable = false)
    private boolean ventilacionAdecuada = true;

    @Column(name = "productos_ordenados", nullable = false)
    private boolean productosOrdenados = true;

    @Column(name = "separacion_pared", nullable = false)
    private boolean separacionPared = true;

    @Column(name = "productos_vencidos_separados", nullable = false)
    private boolean productosVencidosSeparados = true;

    @Column(name = "extintor_vigente", nullable = false)
    private boolean extintorVigente = true;

    @Column(name = "botiquin_primeros_auxilios", nullable = false)
    private boolean botiquinPrimerosAuxilios = true;

    @Column(name = "resultado_general", length = 20)
    private String resultadoGeneral = "CONFORME";

    @Column(length = 1000)
    private String observaciones;

    @Column(name = "accion_correctiva", length = 500)
    private String accionCorrectiva;

    @Column(name = "fecha_proxima_revision")
    private LocalDate fechaProximaRevision;

    @Column(name = "usuario_registro", length = 100)
    private String usuarioRegistro;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Override public Long getTenantId() { return tenantId; }
    @Override public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Almacen getAlmacen() { return almacen; }
    public void setAlmacen(Almacen almacen) { this.almacen = almacen; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public boolean isCondicionPiso() { return condicionPiso; }
    public void setCondicionPiso(boolean v) { this.condicionPiso = v; }
    public boolean isCondicionParedes() { return condicionParedes; }
    public void setCondicionParedes(boolean v) { this.condicionParedes = v; }
    public boolean isCondicionTecho() { return condicionTecho; }
    public void setCondicionTecho(boolean v) { this.condicionTecho = v; }
    public boolean isIluminacionAdecuada() { return iluminacionAdecuada; }
    public void setIluminacionAdecuada(boolean v) { this.iluminacionAdecuada = v; }
    public boolean isVentilacionAdecuada() { return ventilacionAdecuada; }
    public void setVentilacionAdecuada(boolean v) { this.ventilacionAdecuada = v; }
    public boolean isProductosOrdenados() { return productosOrdenados; }
    public void setProductosOrdenados(boolean v) { this.productosOrdenados = v; }
    public boolean isSeparacionPared() { return separacionPared; }
    public void setSeparacionPared(boolean v) { this.separacionPared = v; }
    public boolean isProductosVencidosSeparados() { return productosVencidosSeparados; }
    public void setProductosVencidosSeparados(boolean v) { this.productosVencidosSeparados = v; }
    public boolean isExtintorVigente() { return extintorVigente; }
    public void setExtintorVigente(boolean v) { this.extintorVigente = v; }
    public boolean isBotiquinPrimerosAuxilios() { return botiquinPrimerosAuxilios; }
    public void setBotiquinPrimerosAuxilios(boolean v) { this.botiquinPrimerosAuxilios = v; }
    public String getResultadoGeneral() { return resultadoGeneral; }
    public void setResultadoGeneral(String v) { this.resultadoGeneral = v; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String v) { this.observaciones = v; }
    public String getAccionCorrectiva() { return accionCorrectiva; }
    public void setAccionCorrectiva(String v) { this.accionCorrectiva = v; }
    public LocalDate getFechaProximaRevision() { return fechaProximaRevision; }
    public void setFechaProximaRevision(LocalDate v) { this.fechaProximaRevision = v; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String v) { this.usuarioRegistro = v; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime v) { this.fechaRegistro = v; }
}
