package com.farmacia.sistema.domain.digemid;

import com.farmacia.sistema.domain.inventario.Almacen;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento_controlado",
        indexes = {
                @Index(name = "idx_mov_ctrl_tenant", columnList = "tenant_id"),
                @Index(name = "idx_mov_ctrl_prod_fecha", columnList = "producto_id, fecha"),
                @Index(name = "idx_mov_ctrl_tipo", columnList = "tipo")
        })
@EntityListeners(TenantEntityListener.class)
public class MovimientoControlado implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne
    @JoinColumn(name = "almacen_id")
    private Almacen almacen;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    /** ENTRADA, SALIDA_VENTA, SALIDA_MERMA, AJUSTE_ENTRADA, AJUSTE_SALIDA, TRANSFERENCIA_ENTRADA, TRANSFERENCIA_SALIDA, DESTRUCCION, DEVOLUCION */
    @NotNull
    @Column(nullable = false, length = 30)
    private String tipo;

    @NotNull
    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "saldo_despues")
    private Integer saldoDespues;

    @Column(length = 100)
    private String lote;

    @Column(length = 255)
    private String referencia;

    @Column(name = "venta_id")
    private Long ventaId;

    @Column(name = "registro_receta_id")
    private Long registroRecetaId;

    @Column(name = "merma_id")
    private Long mermaId;

    @Column(name = "usuario_registro", length = 100)
    private String usuarioRegistro;

    @Column(name = "numero_documento", length = 100)
    private String numeroDocumento;

    @Column(length = 500)
    private String motivo;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Almacen getAlmacen() { return almacen; }
    public void setAlmacen(Almacen almacen) { this.almacen = almacen; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public Integer getSaldoDespues() { return saldoDespues; }
    public void setSaldoDespues(Integer saldoDespues) { this.saldoDespues = saldoDespues; }
    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public Long getVentaId() { return ventaId; }
    public void setVentaId(Long ventaId) { this.ventaId = ventaId; }
    public Long getRegistroRecetaId() { return registroRecetaId; }
    public void setRegistroRecetaId(Long registroRecetaId) { this.registroRecetaId = registroRecetaId; }
    public Long getMermaId() { return mermaId; }
    public void setMermaId(Long mermaId) { this.mermaId = mermaId; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    @Override
    public Long getTenantId() { return tenantId; }
    @Override
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
