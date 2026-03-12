package com.farmacia.sistema.domain.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CuentaPagarRepository extends JpaRepository<CuentaPagar, Long> {

    List<CuentaPagar> findByEstadoOrderByFechaVencimientoAsc(String estado);

    List<CuentaPagar> findByTenantId(Long tenantId);

    List<CuentaPagar> findByTenantIdAndEstadoOrderByFechaVencimientoAsc(Long tenantId, String estado);

    List<CuentaPagar> findByOrdenCompra_Id(Long ordenCompraId);

    @Query("SELECT DISTINCT c FROM CuentaPagar c LEFT JOIN FETCH c.ordenCompra o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.producto WHERE c.tenantId = :tenantId AND c.estado = :estado ORDER BY c.fechaVencimiento ASC")
    List<CuentaPagar> findByTenantIdAndEstadoOrderByFechaVencimientoAscWithOrden(@Param("tenantId") Long tenantId, @Param("estado") String estado);

    @Query("SELECT DISTINCT c FROM CuentaPagar c LEFT JOIN FETCH c.ordenCompra o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.producto WHERE c.tenantId = :tenantId ORDER BY c.fechaVencimiento ASC")
    List<CuentaPagar> findByTenantIdWithOrden(@Param("tenantId") Long tenantId);
}

