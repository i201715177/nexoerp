package com.farmacia.sistema.domain.inventariofisico;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventarioFisicoRepository extends JpaRepository<InventarioFisico, Long> {
    List<InventarioFisico> findByTenantIdOrderByFechaDesc(Long tenantId);
    long countByTenantId(Long tenantId);
}
