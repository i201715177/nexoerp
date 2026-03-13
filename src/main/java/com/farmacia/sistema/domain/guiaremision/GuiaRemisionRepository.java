package com.farmacia.sistema.domain.guiaremision;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuiaRemisionRepository extends JpaRepository<GuiaRemision, Long> {
    List<GuiaRemision> findByTenantIdOrderByFechaEmisionDesc(Long tenantId);

    @Query("SELECT g FROM GuiaRemision g LEFT JOIN FETCH g.items LEFT JOIN FETCH g.proveedor LEFT JOIN FETCH g.ordenCompra WHERE g.id = :id")
    Optional<GuiaRemision> findByIdWithDetails(@Param("id") Long id);
}
