package com.farmacia.sistema.domain.cotizacion;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {
    List<Cotizacion> findByTenantIdOrderByFechaDesc(Long tenantId);
    List<Cotizacion> findByTenantIdAndEstadoOrderByFechaDesc(Long tenantId, String estado);
    List<Cotizacion> findByTenantIdAndFechaBetweenOrderByFechaDesc(Long tenantId, LocalDateTime desde, LocalDateTime hasta);
    long countByTenantId(Long tenantId);
    long countByTenantIdAndEstado(Long tenantId, String estado);
}
