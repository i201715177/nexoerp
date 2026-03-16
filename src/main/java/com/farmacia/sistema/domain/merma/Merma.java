package com.farmacia.sistema.domain.merma;

import com.farmacia.sistema.domain.inventario.Almacen;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mermas",
        indexes = {
                @Index(name = "idx_merma_tenant", columnList = "tenant_id"),
                @Index(name = "idx_merma_fecha", columnList = "fecha_registro"),
                @Index(name = "idx_merma_producto", columnList = "producto_id"),
                @Index(name = "idx_merma_tipo", columnList = "tipo_merma"),
                @Index(name = "idx_merma_numero", columnList = "numero_merma")
        })
@EntityListeners(TenantEntityListener.class)
public class Merma implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_merma", length = 30, unique = true)
    private String numeroMerma;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne
    @JoinColumn(name = "almacen_id")
    private Almacen almacen;

    @Column(length = 50)
    private String lote;

    @Column(name = "fecha_vencimiento_producto")
    private LocalDate fechaVencimientoProducto;

    @NotNull
    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "stock_antes")
    private Integer stockAntes;

    @Column(name = "stock_despues")
    private Integer stockDespues;

    /** PRODUCTO_VENCIDO, PRODUCTO_DANADO, ROTURA, ERROR_INVENTARIO, DESTRUCCION_AUTORIZADA, DETERIORO, EXTRAVIO, OTRO */
    @Column(name = "tipo_merma", nullable = false, length = 30)
    private String tipoMerma = "OTRO";

    @Column(length = 500)
    private String motivo;

    @Column(length = 500)
    private String observaciones;

    @NotNull
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Column(name = "costo_estimado", precision = 12, scale = 2)
    private BigDecimal costoEstimado;

    @Column(name = "usuario_registro", length = 100)
    private String usuarioRegistro;

    @Column(name = "es_controlado")
    private Boolean esControlado = false;

    @Column(name = "responsable_autorizado", length = 200)
    private String responsableAutorizado;

    @Column(name = "aprobacion_quimico_farmaceutico", length = 200)
    private String aprobacionQuimicoFarmaceutico;

    @Column(name = "acta_destruccion", length = 100)
    private String actaDestruccion;

    @Column(name = "numero_reporte", length = 50)
    private String numeroReporte;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumeroMerma() { return numeroMerma; }
    public void setNumeroMerma(String numeroMerma) { this.numeroMerma = numeroMerma; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Almacen getAlmacen() { return almacen; }
    public void setAlmacen(Almacen almacen) { this.almacen = almacen; }
    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }
    public LocalDate getFechaVencimientoProducto() { return fechaVencimientoProducto; }
    public void setFechaVencimientoProducto(LocalDate fechaVencimientoProducto) { this.fechaVencimientoProducto = fechaVencimientoProducto; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public Integer getStockAntes() { return stockAntes; }
    public void setStockAntes(Integer stockAntes) { this.stockAntes = stockAntes; }
    public Integer getStockDespues() { return stockDespues; }
    public void setStockDespues(Integer stockDespues) { this.stockDespues = stockDespues; }
    public String getTipoMerma() { return tipoMerma; }
    public void setTipoMerma(String tipoMerma) { this.tipoMerma = tipoMerma; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public BigDecimal getCostoEstimado() { return costoEstimado; }
    public void setCostoEstimado(BigDecimal costoEstimado) { this.costoEstimado = costoEstimado; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
    public Boolean getEsControlado() { return esControlado; }
    public void setEsControlado(Boolean esControlado) { this.esControlado = esControlado; }
    public String getResponsableAutorizado() { return responsableAutorizado; }
    public void setResponsableAutorizado(String responsableAutorizado) { this.responsableAutorizado = responsableAutorizado; }
    public String getAprobacionQuimicoFarmaceutico() { return aprobacionQuimicoFarmaceutico; }
    public void setAprobacionQuimicoFarmaceutico(String aprobacionQuimicoFarmaceutico) { this.aprobacionQuimicoFarmaceutico = aprobacionQuimicoFarmaceutico; }
    public String getActaDestruccion() { return actaDestruccion; }
    public void setActaDestruccion(String actaDestruccion) { this.actaDestruccion = actaDestruccion; }
    public String getNumeroReporte() { return numeroReporte; }
    public void setNumeroReporte(String numeroReporte) { this.numeroReporte = numeroReporte; }

    @Override
    public Long getTenantId() { return tenantId; }
    @Override
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
