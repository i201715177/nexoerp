package com.farmacia.sistema.domain.empresa;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(name = "planes_suscripcion", indexes = {
        @Index(name = "idx_plan_codigo", columnList = "codigo")
})
public class PlanSuscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioMensual = BigDecimal.ZERO;

    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioAnual = BigDecimal.ZERO;

    /** Límite de usuarios por empresa en este plan. Null = ilimitado. */
    @Column(name = "max_usuarios")
    private Integer maxUsuarios;

    @Column(nullable = false)
    private boolean activo = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecioMensual() { return precioMensual; }
    public void setPrecioMensual(BigDecimal precioMensual) { this.precioMensual = precioMensual; }

    public BigDecimal getPrecioAnual() { return precioAnual; }
    public void setPrecioAnual(BigDecimal precioAnual) { this.precioAnual = precioAnual; }

    public Integer getMaxUsuarios() { return maxUsuarios; }
    public void setMaxUsuarios(Integer maxUsuarios) { this.maxUsuarios = maxUsuarios; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
