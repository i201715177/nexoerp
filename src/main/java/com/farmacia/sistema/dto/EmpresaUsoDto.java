package com.farmacia.sistema.dto;

import java.math.BigDecimal;

public class EmpresaUsoDto {

    private Long id;
    private String codigo;
    private String nombre;
    private boolean activa;
    private long clientes;
    private long productos;
    private long sucursales;
    private long ventas;
    private BigDecimal montoVentas;
    private long usuarios;
    private Integer maxUsuarios;
    private String planNombre;
    private String planCodigo;
    private String emailContacto;

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

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public long getClientes() {
        return clientes;
    }

    public void setClientes(long clientes) {
        this.clientes = clientes;
    }

    public long getProductos() {
        return productos;
    }

    public void setProductos(long productos) {
        this.productos = productos;
    }

    public long getSucursales() {
        return sucursales;
    }

    public void setSucursales(long sucursales) {
        this.sucursales = sucursales;
    }

    public long getVentas() {
        return ventas;
    }

    public void setVentas(long ventas) {
        this.ventas = ventas;
    }

    public BigDecimal getMontoVentas() {
        return montoVentas;
    }

    public void setMontoVentas(BigDecimal montoVentas) {
        this.montoVentas = montoVentas;
    }

    public long getUsuarios() { return usuarios; }
    public void setUsuarios(long usuarios) { this.usuarios = usuarios; }

    public Integer getMaxUsuarios() { return maxUsuarios; }
    public void setMaxUsuarios(Integer maxUsuarios) { this.maxUsuarios = maxUsuarios; }

    public String getPlanNombre() { return planNombre; }
    public void setPlanNombre(String planNombre) { this.planNombre = planNombre; }

    public String getPlanCodigo() { return planCodigo; }
    public void setPlanCodigo(String planCodigo) { this.planCodigo = planCodigo; }

    public String getEmailContacto() { return emailContacto; }
    public void setEmailContacto(String emailContacto) { this.emailContacto = emailContacto; }
}

