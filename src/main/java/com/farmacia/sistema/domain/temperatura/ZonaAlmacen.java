package com.farmacia.sistema.domain.temperatura;

import com.farmacia.sistema.domain.inventario.Almacen;
import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "zonas_almacen", indexes = @Index(name = "idx_zona_tenant", columnList = "tenant_id"))
@EntityListeners(TenantEntityListener.class)
public class ZonaAlmacen implements TenantSupport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @ManyToOne @JoinColumn(name = "almacen_id")
    private Almacen almacen;

    @Column(name = "temp_minima", precision = 5, scale = 2)
    private BigDecimal tempMinima;

    @Column(name = "temp_maxima", precision = 5, scale = 2)
    private BigDecimal tempMaxima;

    @Column(name = "humedad_minima", precision = 5, scale = 2)
    private BigDecimal humedadMinima;

    @Column(name = "humedad_maxima", precision = 5, scale = 2)
    private BigDecimal humedadMaxima;

    /** true = cadena de frío */
    @Column(name = "requiere_refrigeracion", nullable = false)
    private boolean requiereRefrigeracion = false;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Override public Long getTenantId() { return tenantId; }
    @Override public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Almacen getAlmacen() { return almacen; }
    public void setAlmacen(Almacen almacen) { this.almacen = almacen; }
    public BigDecimal getTempMinima() { return tempMinima; }
    public void setTempMinima(BigDecimal v) { this.tempMinima = v; }
    public BigDecimal getTempMaxima() { return tempMaxima; }
    public void setTempMaxima(BigDecimal v) { this.tempMaxima = v; }
    public BigDecimal getHumedadMinima() { return humedadMinima; }
    public void setHumedadMinima(BigDecimal v) { this.humedadMinima = v; }
    public BigDecimal getHumedadMaxima() { return humedadMaxima; }
    public void setHumedadMaxima(BigDecimal v) { this.humedadMaxima = v; }
    public boolean isRequiereRefrigeracion() { return requiereRefrigeracion; }
    public void setRequiereRefrigeracion(boolean v) { this.requiereRefrigeracion = v; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
