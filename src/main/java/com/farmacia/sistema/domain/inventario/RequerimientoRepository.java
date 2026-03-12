package com.farmacia.sistema.domain.inventario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RequerimientoRepository extends JpaRepository<Requerimiento, Long> {

    List<Requerimiento> findByTenantIdOrderByFechaSolicitudDesc(Long tenantId);

    List<Requerimiento> findByTenantIdAndSucursalIdOrderByFechaSolicitudDesc(Long tenantId, Long sucursalId);

    @Query("SELECT DISTINCT r FROM Requerimiento r LEFT JOIN FETCH r.items i LEFT JOIN FETCH i.producto WHERE r.tenantId = :tenantId ORDER BY r.fechaSolicitud DESC")
    List<Requerimiento> findByTenantIdOrderByFechaSolicitudDescWithItems(@Param("tenantId") Long tenantId);

    @Query("SELECT DISTINCT r FROM Requerimiento r LEFT JOIN FETCH r.items i LEFT JOIN FETCH i.producto WHERE r.tenantId = :tenantId AND r.sucursal.id = :sucursalId ORDER BY r.fechaSolicitud DESC")
    List<Requerimiento> findByTenantIdAndSucursalIdOrderByFechaSolicitudDescWithItems(@Param("tenantId") Long tenantId, @Param("sucursalId") Long sucursalId);

    /** Últimos requerimientos de esta sucursal creados después de una fecha (para detectar duplicados). */
    @Query("SELECT r FROM Requerimiento r LEFT JOIN FETCH r.items i LEFT JOIN FETCH i.producto WHERE r.tenantId = :tenantId AND r.sucursal.id = :sucursalId AND r.fechaSolicitud >= :desde ORDER BY r.fechaSolicitud DESC")
    List<Requerimiento> findByTenantIdAndSucursalIdAndFechaSolicitudAfterWithItems(@Param("tenantId") Long tenantId, @Param("sucursalId") Long sucursalId, @Param("desde") LocalDateTime desde);
}
