package com.farmacia.sistema.domain.empresa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findByCodigo(String codigo);

    Optional<Empresa> findFirstByActivaTrueOrderByIdAsc();

    @Query("SELECT e FROM Empresa e LEFT JOIN FETCH e.plan ORDER BY e.nombre")
    List<Empresa> findAllWithPlan();

    @Query("SELECT e FROM Empresa e LEFT JOIN FETCH e.plan WHERE e.id = :id")
    Optional<Empresa> findByIdWithPlan(@Param("id") Long id);

    Optional<Empresa> findByNombreIgnoreCase(String nombre);
}

