package com.farmacia.sistema.domain.temperatura;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface RegistroTemperaturaRepository extends JpaRepository<RegistroTemperatura, Long> {
    List<RegistroTemperatura> findByTenantIdOrderByFechaDesc(Long tenantId);
    List<RegistroTemperatura> findByZonaIdOrderByFechaDesc(Long zonaId);
    List<RegistroTemperatura> findByTenantIdAndFechaBetweenOrderByFechaDesc(Long tenantId, LocalDateTime desde, LocalDateTime hasta);
    List<RegistroTemperatura> findByTenantIdAndFueraRangoTrueOrderByFechaDesc(Long tenantId);
}
