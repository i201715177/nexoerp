package com.farmacia.sistema.domain.producto;

import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(name = "productos",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "codigo"}),
        indexes = {
                @Index(name = "idx_prod_tenant", columnList = "tenant_id"),
                @Index(name = "idx_prod_tenant_codigo", columnList = "tenant_id, codigo"),
                @Index(name = "idx_prod_tenant_nombre", columnList = "tenant_id, nombre"),
                @Index(name = "idx_prod_tenant_cat", columnList = "tenant_id, categoria")
        })
@EntityListeners(TenantEntityListener.class)
public class Producto implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String codigo;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String laboratorio;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String presentacion;

    @Column(length = 100)
    private String categoria;

    @Column(length = 100)
    private String marca;

    @Column(length = 30)
    private String unidadMedida;

    @Column(length = 100)
    private String codigoBarras;

    @Column(length = 255)
    private String imagenUrl;

    @NotNull
    @Min(0)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    /** Costo unitario de referencia para reportes de utilidad y margen. Opcional. */
    @Column(precision = 12, scale = 2)
    private BigDecimal costoUnitario;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer stockActual;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer stockMinimo;

    @Min(0)
    @Column
    private Integer stockMaximo;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

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

    public String getLaboratorio() {
        return laboratorio;
    }

    public void setLaboratorio(String laboratorio) {
        this.laboratorio = laboratorio;
    }

    public String getPresentacion() {
        return presentacion;
    }

    public void setPresentacion(String presentacion) {
        this.presentacion = presentacion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public Integer getStockActual() {
        return stockActual;
    }

    public void setStockActual(Integer stockActual) {
        this.stockActual = stockActual;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public Integer getStockMaximo() {
        return stockMaximo;
    }

    public void setStockMaximo(Integer stockMaximo) {
        this.stockMaximo = stockMaximo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public Long getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}

