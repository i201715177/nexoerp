package com.farmacia.sistema.domain.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditoriaAccionRepository extends JpaRepository<AuditoriaAccion, Long> {

    List<AuditoriaAccion> findTop100ByOrderByFechaHoraDesc();

    List<AuditoriaAccion> findTop100ByTenantIdOrderByFechaHoraDesc(Long tenantId);

    List<AuditoriaAccion> findByFechaHoraBetweenOrderByFechaHoraDesc(LocalDateTime desde, LocalDateTime hasta);
}

