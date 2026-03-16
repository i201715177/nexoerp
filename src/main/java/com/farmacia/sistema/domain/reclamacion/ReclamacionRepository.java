package com.farmacia.sistema.domain.reclamacion;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReclamacionRepository extends JpaRepository<Reclamacion, Long> {
    List<Reclamacion> findByTenantIdOrderByFechaDesc(Long tenantId);
    List<Reclamacion> findByTenantIdAndEstadoOrderByFechaDesc(Long tenantId, String estado);
    long countByTenantId(Long tenantId);
    long countByTenantIdAndEstado(Long tenantId, String estado);
}
