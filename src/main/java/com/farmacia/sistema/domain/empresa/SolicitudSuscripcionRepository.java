package com.farmacia.sistema.domain.empresa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudSuscripcionRepository extends JpaRepository<SolicitudSuscripcion, Long> {

    List<SolicitudSuscripcion> findAllByOrderByFechaSolicitudDesc();
}
