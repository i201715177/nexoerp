package com.farmacia.sistema.domain.venta;

import jakarta.persistence.*;

@Entity
@Table(name = "sequence_comprobante")
public class SequenceComprobante {

    @Id
    @Column(name = "id")
    private Long id = 1L;

    @Column(name = "siguiente", nullable = false)
    private Long siguiente = 1L;

    @Version
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSiguiente() {
        return siguiente;
    }

    public void setSiguiente(Long siguiente) {
        this.siguiente = siguiente;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
