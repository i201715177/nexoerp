package com.farmacia.sistema.domain.producto;

import com.farmacia.sistema.tenant.TenantEntityListener;
import com.farmacia.sistema.tenant.TenantSupport;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    // --- Campos DIGEMID (productos controlados) ---
    @Column(name = "principio_activo", length = 200)
    private String principioActivo;
    @Column(length = 100)
    private String concentracion;
    @Column(name = "forma_farmaceutica", length = 100)
    private String formaFarmaceutica;
    @Column(name = "registro_sanitario", length = 50)
    private String registroSanitario;
    @Column(length = 50)
    private String lote;
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;
    /** ESTUPEFACIENTE, PSICOTROPICO, SUJETO_FISCALIZACION */
    @Column(name = "tipo_producto_controlado", length = 30)
    private String tipoProductoControlado;
    @Column(name = "tipo_control_digemid", length = 30)
    private String tipoControlDigemid;
    /** LISTA_I, LISTA_II, LISTA_III, LISTA_IV */
    @Column(name = "lista_control", length = 30)
    private String listaControl;
    @Column(name = "control_stock_especial", nullable = false)
    private boolean controlStockEspecial = false;
    @Column(name = "requiere_receta", nullable = false)
    private boolean requiereReceta = false;
    @Column(name = "tipo_receta", length = 30)
    private String tipoReceta;  // RECETA_SIMPLE, RECETA_RETENIDA, etc.

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

    public String getPrincipioActivo() { return principioActivo; }
    public void setPrincipioActivo(String principioActivo) { this.principioActivo = principioActivo; }
    public String getConcentracion() { return concentracion; }
    public void setConcentracion(String concentracion) { this.concentracion = concentracion; }
    public String getFormaFarmaceutica() { return formaFarmaceutica; }
    public void setFormaFarmaceutica(String formaFarmaceutica) { this.formaFarmaceutica = formaFarmaceutica; }
    public String getRegistroSanitario() { return registroSanitario; }
    public void setRegistroSanitario(String registroSanitario) { this.registroSanitario = registroSanitario; }
    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public String getTipoProductoControlado() { return tipoProductoControlado; }
    public void setTipoProductoControlado(String tipoProductoControlado) { this.tipoProductoControlado = tipoProductoControlado; }
    public String getTipoControlDigemid() { return tipoControlDigemid; }
    public void setTipoControlDigemid(String tipoControlDigemid) { this.tipoControlDigemid = tipoControlDigemid; }
    public String getListaControl() { return listaControl; }
    public void setListaControl(String listaControl) { this.listaControl = listaControl; }
    public boolean isControlStockEspecial() { return controlStockEspecial; }
    public void setControlStockEspecial(boolean controlStockEspecial) { this.controlStockEspecial = controlStockEspecial; }
    public boolean isRequiereReceta() { return requiereReceta; }
    public void setRequiereReceta(boolean requiereReceta) { this.requiereReceta = requiereReceta; }
    public String getTipoReceta() { return tipoReceta; }
    public void setTipoReceta(String tipoReceta) { this.tipoReceta = tipoReceta; }

    @Override
    public Long getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}

