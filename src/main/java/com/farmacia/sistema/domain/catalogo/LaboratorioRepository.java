package com.farmacia.sistema.domain.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LaboratorioRepository extends JpaRepository<Laboratorio, Long> {
    List<Laboratorio> findByTenantIdOrderByNombreAsc(Long tenantId);
    List<Laboratorio> findByTenantIdAndActivoTrueOrderByNombreAsc(Long tenantId);
}
