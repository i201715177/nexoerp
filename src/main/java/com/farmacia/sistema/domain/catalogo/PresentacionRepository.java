package com.farmacia.sistema.domain.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PresentacionRepository extends JpaRepository<Presentacion, Long> {
    List<Presentacion> findByTenantIdOrderByNombreAsc(Long tenantId);
    List<Presentacion> findByTenantIdAndActivoTrueOrderByNombreAsc(Long tenantId);
}
