package com.farmacia.sistema.domain.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    @Query("SELECT o FROM OrdenCompra o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.producto LEFT JOIN FETCH o.proveedor WHERE o.id = :id")
    Optional<OrdenCompra> findByIdWithItems(@Param("id") Long id);

    List<OrdenCompra> findAllByOrderByFechaEmisionDesc();

    List<OrdenCompra> findByTenantIdOrderByFechaEmisionDesc(Long tenantId);

    @Query("SELECT DISTINCT o FROM OrdenCompra o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.producto WHERE o.tenantId = :tenantId ORDER BY o.fechaEmision DESC")
    List<OrdenCompra> findByTenantIdOrderByFechaEmisionDescWithItems(@Param("tenantId") Long tenantId);

    List<OrdenCompra> findByFechaEmisionBetweenOrderByFechaEmisionDesc(LocalDateTime desde, LocalDateTime hasta);
}

