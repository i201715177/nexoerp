package com.farmacia.sistema.domain.venta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByTenantId(Long tenantId);

    List<Venta> findByTenantIdOrderByFechaHoraDesc(Long tenantId);

    @EntityGraph(attributePaths = {"pagos", "cliente"})
    @Query("SELECT v FROM Venta v WHERE v.tenantId = :tenantId AND v.estado != 'ANULADA' ORDER BY v.fechaHora DESC")
    List<Venta> findByTenantIdWithPagosOrderByFechaHoraDesc(@Param("tenantId") Long tenantId);

    @EntityGraph(attributePaths = {"pagos", "cliente"})
    @Query("SELECT v FROM Venta v WHERE v.estado != 'ANULADA' ORDER BY v.fechaHora DESC")
    List<Venta> findAllWithPagosOrderByFechaHoraDesc();

    @EntityGraph(attributePaths = {"items", "items.producto", "cliente"})
    @Query("SELECT v FROM Venta v WHERE v.tenantId = :tenantId ORDER BY v.fechaHora DESC")
    List<Venta> findByTenantIdOrderByFechaHoraDescWithItems(@Param("tenantId") Long tenantId);

    Optional<Venta> findFirstByClienteIdOrderByFechaHoraDesc(Long clienteId);

    List<Venta> findByClienteIdOrderByFechaHoraDesc(Long clienteId);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.cajaTurno.id = :cajaTurnoId")
    BigDecimal sumTotalByCajaTurnoId(@Param("cajaTurnoId") Long cajaTurnoId);

    @EntityGraph(attributePaths = {"pagos"})
    @Query("SELECT v FROM Venta v WHERE v.cajaTurno.id = :cajaTurnoId AND v.estado != 'ANULADA'")
    List<Venta> findByCajaTurnoIdWithPagos(@Param("cajaTurnoId") Long cajaTurnoId);

    java.util.List<Venta> findAllByOrderByFechaHoraDesc();

    @EntityGraph(attributePaths = {"items", "items.producto", "cliente"})
    @Query("SELECT v FROM Venta v ORDER BY v.fechaHora DESC")
    java.util.List<Venta> findAllByOrderByFechaHoraDescWithItems();
}

