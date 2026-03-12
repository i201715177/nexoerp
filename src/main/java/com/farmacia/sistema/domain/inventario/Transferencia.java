package com.farmacia.sistema.domain.inventario;

import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Transferencia entre almacenes en dos fases: ENVIADO (origen descontado)
 * hasta que el destino confirma RECIBIDO (se suma en destino).
 */
@Entity
@Table(name = "transferencias", indexes = {
        @Index(name = "idx_trans_tenant", columnList = "tenant_id"),
        @Index(name = "idx_trans_estado_destino", columnList = "tenant_id, estado, almacen_destino_id")
})
@EntityListeners(TenantEntityListener.class)
public class Transferencia implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "almacen_origen_id", nullable = false)
    private Almacen almacenOrigen;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "almacen_destino_id", nullable = false)
    private Almacen almacenDestino;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @NotNull
    @Column(nullable = false)
    private Integer cantidad;

    @Column(length = 255)
    private String referencia;

    /** ENVIADO = en tránsito; RECIBIDO = confirmado en destino */
    @NotNull
    @Column(nullable = false, length = 20)
    private String estado = "ENVIADO";

    @NotNull
    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio = LocalDateTime.now();

    @Column(name = "usuario_envio", length = 100)
    private String usuarioEnvio;

    @Column(name = "fecha_recepcion")
    private LocalDateTime fechaRecepcion;

    @Column(name = "usuario_recepcion", length = 100)
    private String usuarioRecepcion;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Almacen getAlmacenOrigen() { return almacenOrigen; }
    public void setAlmacenOrigen(Almacen almacenOrigen) { this.almacenOrigen = almacenOrigen; }
    public Almacen getAlmacenDestino() { return almacenDestino; }
    public void setAlmacenDestino(Almacen almacenDestino) { this.almacenDestino = almacenDestino; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }
    public String getUsuarioEnvio() { return usuarioEnvio; }
    public void setUsuarioEnvio(String usuarioEnvio) { this.usuarioEnvio = usuarioEnvio; }
    public LocalDateTime getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(LocalDateTime fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }
    public String getUsuarioRecepcion() { return usuarioRecepcion; }
    public void setUsuarioRecepcion(String usuarioRecepcion) { this.usuarioRecepcion = usuarioRecepcion; }

    @Override
    public Long getTenantId() { return tenantId; }
    @Override
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
