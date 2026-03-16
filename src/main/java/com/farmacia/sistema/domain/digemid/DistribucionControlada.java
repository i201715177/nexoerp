package com.farmacia.sistema.domain.digemid;

import com.farmacia.sistema.domain.inventario.Almacen;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "distribucion_controlada",
        indexes = {
                @Index(name = "idx_dist_ctrl_tenant", columnList = "tenant_id"),
                @Index(name = "idx_dist_ctrl_fecha", columnList = "fecha_envio"),
                @Index(name = "idx_dist_ctrl_estado", columnList = "estado")
        })
@EntityListeners(TenantEntityListener.class)
public class DistribucionControlada implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "almacen_origen_id", nullable = false)
    private Almacen almacenOrigen;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "almacen_destino_id", nullable = false)
    private Almacen almacenDestino;

    @NotNull
    @Column(nullable = false)
    private Integer cantidad;

    @Column(length = 50)
    private String lote;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio = LocalDateTime.now();

    @Column(name = "fecha_recepcion")
    private LocalDateTime fechaRecepcion;

    @Column(nullable = false, length = 20)
    private String estado = "ENVIADO"; // ENVIADO, RECIBIDO

    @Column(length = 255)
    private String referencia;

    @Column(name = "usuario_envio", length = 100)
    private String usuarioEnvio;

    @Column(name = "usuario_recepcion", length = 100)
    private String usuarioRecepcion;

    @Column(name = "cliente_ruc", length = 20)
    private String clienteRuc;

    @Column(name = "cliente_razon_social", length = 200)
    private String clienteRazonSocial;

    /** FARMACIA, CLINICA, HOSPITAL, DROGUERIA */
    @Column(name = "tipo_establecimiento", length = 30)
    private String tipoEstablecimiento;

    @Column(name = "numero_factura", length = 50)
    private String numeroFactura;

    @Column(name = "numero_guia_remision", length = 50)
    private String numeroGuiaRemision;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Almacen getAlmacenOrigen() { return almacenOrigen; }
    public void setAlmacenOrigen(Almacen almacenOrigen) { this.almacenOrigen = almacenOrigen; }
    public Almacen getAlmacenDestino() { return almacenDestino; }
    public void setAlmacenDestino(Almacen almacenDestino) { this.almacenDestino = almacenDestino; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }
    public LocalDateTime getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(LocalDateTime fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getUsuarioEnvio() { return usuarioEnvio; }
    public void setUsuarioEnvio(String usuarioEnvio) { this.usuarioEnvio = usuarioEnvio; }
    public String getUsuarioRecepcion() { return usuarioRecepcion; }
    public void setUsuarioRecepcion(String usuarioRecepcion) { this.usuarioRecepcion = usuarioRecepcion; }
    public String getClienteRuc() { return clienteRuc; }
    public void setClienteRuc(String clienteRuc) { this.clienteRuc = clienteRuc; }
    public String getClienteRazonSocial() { return clienteRazonSocial; }
    public void setClienteRazonSocial(String clienteRazonSocial) { this.clienteRazonSocial = clienteRazonSocial; }
    public String getTipoEstablecimiento() { return tipoEstablecimiento; }
    public void setTipoEstablecimiento(String tipoEstablecimiento) { this.tipoEstablecimiento = tipoEstablecimiento; }
    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }
    public String getNumeroGuiaRemision() { return numeroGuiaRemision; }
    public void setNumeroGuiaRemision(String numeroGuiaRemision) { this.numeroGuiaRemision = numeroGuiaRemision; }

    @Override
    public Long getTenantId() { return tenantId; }
    @Override
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
