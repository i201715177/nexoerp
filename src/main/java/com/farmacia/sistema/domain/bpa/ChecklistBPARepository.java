package com.farmacia.sistema.domain.bpa;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChecklistBPARepository extends JpaRepository<ChecklistBPA, Long> {
    List<ChecklistBPA> findByTenantIdOrderByFechaDesc(Long tenantId);
    long countByTenantId(Long tenantId);
}
