package com.farmacia.sistema.domain.empresa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanSuscripcionRepository extends JpaRepository<PlanSuscripcion, Long> {

    Optional<PlanSuscripcion> findByCodigo(String codigo);

    List<PlanSuscripcion> findByActivoTrueOrderByPrecioMensualAsc();
}
