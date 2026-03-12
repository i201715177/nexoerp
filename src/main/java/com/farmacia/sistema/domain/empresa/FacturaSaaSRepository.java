package com.farmacia.sistema.domain.empresa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FacturaSaaSRepository extends JpaRepository<FacturaSaaS, Long> {

    List<FacturaSaaS> findByEmpresaIdOrderByPeriodoDesdeDesc(Long empresaId);

    List<FacturaSaaS> findAllByOrderByFechaEmisionDesc();

    @Query("SELECT f FROM FacturaSaaS f JOIN FETCH f.empresa JOIN FETCH f.plan ORDER BY f.fechaEmision DESC")
    List<FacturaSaaS> findAllWithRelations();

    @Query("SELECT f FROM FacturaSaaS f JOIN FETCH f.empresa JOIN FETCH f.plan WHERE f.empresa.id = :empresaId ORDER BY f.periodoDesde DESC")
    List<FacturaSaaS> findByEmpresaIdWithRelations(@Param("empresaId") Long empresaId);

    @Query("SELECT f FROM FacturaSaaS f JOIN FETCH f.empresa JOIN FETCH f.plan WHERE f.id = :id")
    Optional<FacturaSaaS> findByIdWithRelations(@Param("id") Long id);

    long countByFechaEmisionBetween(LocalDate inicio, LocalDate fin);

    List<FacturaSaaS> findByEstadoAndFechaVencimientoBefore(EstadoFactura estado, LocalDate fecha);

    List<FacturaSaaS> findByEstadoAndFechaVencimientoBetween(EstadoFactura estado, LocalDate desde, LocalDate hasta);
}
