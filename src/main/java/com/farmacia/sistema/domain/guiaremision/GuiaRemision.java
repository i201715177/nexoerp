package com.farmacia.sistema.domain.guiaremision;

import com.farmacia.sistema.domain.compra.OrdenCompra;
import com.farmacia.sistema.domain.proveedor.Proveedor;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "guias_remision", indexes = {
        @Index(name = "idx_gr_tenant", columnList = "tenant_id"),
        @Index(name = "idx_gr_tenant_fecha", columnList = "tenant_id, fecha_emision")
})
@EntityListeners(TenantEntityListener.class)
public class GuiaRemision implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "serie", length = 10, nullable = false)
    private String serie = "T001";

    @NotNull
    @Column(nullable = false, length = 20)
    private String numero;

    @NotNull
    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "fecha_traslado")
    private LocalDate fechaTraslado;

    @ManyToOne
    @JoinColumn(name = "orden_compra_id")
    private OrdenCompra ordenCompra;

    @ManyToOne
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    /** REMITENTE o TRANSPORTISTA */
    @Column(name = "motivo_traslado", length = 50)
    private String motivoTraslado = "COMPRA";

    @Column(name = "direccion_partida", length = 255)
    private String direccionPartida;

    @Column(name = "direccion_llegada", length = 255)
    private String direccionLlegada;

    @Column(name = "transportista_ruc", length = 11)
    private String transportistaRuc;

    @Column(name = "transportista_nombre", length = 200)
    private String transportistaNombre;

    @Column(name = "conductor_dni", length = 8)
    private String conductorDni;

    @Column(name = "conductor_nombre", length = 200)
    private String conductorNombre;

    @Column(name = "conductor_licencia", length = 20)
    private String conductorLicencia;

    @Column(name = "placa_vehiculo", length = 15)
    private String placaVehiculo;

    @Column(name = "peso_total", length = 30)
    private String pesoTotal;

    @Column(name = "numero_bultos")
    private Integer numeroBultos;

    @Column(length = 255)
    private String observaciones;

    @Column(length = 20, nullable = false)
    private String estado = "EMITIDA";

    @Column(name = "estado_sunat", length = 20)
    private String estadoSunat = "PENDIENTE";

    @OneToMany(mappedBy = "guiaRemision", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GuiaRemisionItem> items = new ArrayList<>();

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }
    public LocalDate getFechaTraslado() { return fechaTraslado; }
    public void setFechaTraslado(LocalDate fechaTraslado) { this.fechaTraslado = fechaTraslado; }
    public OrdenCompra getOrdenCompra() { return ordenCompra; }
    public void setOrdenCompra(OrdenCompra ordenCompra) { this.ordenCompra = ordenCompra; }
    public Proveedor getProveedor() { return proveedor; }
    public void setProveedor(Proveedor proveedor) { this.proveedor = proveedor; }
    public String getMotivoTraslado() { return motivoTraslado; }
    public void setMotivoTraslado(String motivoTraslado) { this.motivoTraslado = motivoTraslado; }
    public String getDireccionPartida() { return direccionPartida; }
    public void setDireccionPartida(String direccionPartida) { this.direccionPartida = direccionPartida; }
    public String getDireccionLlegada() { return direccionLlegada; }
    public void setDireccionLlegada(String direccionLlegada) { this.direccionLlegada = direccionLlegada; }
    public String getTransportistaRuc() { return transportistaRuc; }
    public void setTransportistaRuc(String transportistaRuc) { this.transportistaRuc = transportistaRuc; }
    public String getTransportistaNombre() { return transportistaNombre; }
    public void setTransportistaNombre(String transportistaNombre) { this.transportistaNombre = transportistaNombre; }
    public String getConductorDni() { return conductorDni; }
    public void setConductorDni(String conductorDni) { this.conductorDni = conductorDni; }
    public String getConductorNombre() { return conductorNombre; }
    public void setConductorNombre(String conductorNombre) { this.conductorNombre = conductorNombre; }
    public String getConductorLicencia() { return conductorLicencia; }
    public void setConductorLicencia(String conductorLicencia) { this.conductorLicencia = conductorLicencia; }
    public String getPlacaVehiculo() { return placaVehiculo; }
    public void setPlacaVehiculo(String placaVehiculo) { this.placaVehiculo = placaVehiculo; }
    public String getPesoTotal() { return pesoTotal; }
    public void setPesoTotal(String pesoTotal) { this.pesoTotal = pesoTotal; }
    public Integer getNumeroBultos() { return numeroBultos; }
    public void setNumeroBultos(Integer numeroBultos) { this.numeroBultos = numeroBultos; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getEstadoSunat() { return estadoSunat; }
    public void setEstadoSunat(String estadoSunat) { this.estadoSunat = estadoSunat; }
    public List<GuiaRemisionItem> getItems() { return items; }
    public void setItems(List<GuiaRemisionItem> items) { this.items = items; }
    @Override public Long getTenantId() { return tenantId; }
    @Override public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getSerieNumero() {
        return (serie != null ? serie : "T001") + "-" + (numero != null ? numero : "");
    }
}
