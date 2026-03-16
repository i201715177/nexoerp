package com.farmacia.sistema.domain.puntos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PuntoMovimientoRepository extends JpaRepository<PuntoMovimiento, Long> {
    List<PuntoMovimiento> findByClienteIdOrderByFechaDesc(Long clienteId);
    List<PuntoMovimiento> findByTenantIdOrderByFechaDesc(Long tenantId);
    @Query("SELECT COALESCE(SUM(pm.puntos), 0) FROM PuntoMovimiento pm WHERE pm.cliente.id = :clienteId")
    int sumPuntosByClienteId(@Param("clienteId") Long clienteId);
}
