package com.farmacia.sistema.domain.temperatura;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ZonaAlmacenRepository extends JpaRepository<ZonaAlmacen, Long> {
    List<ZonaAlmacen> findByTenantIdOrderByNombreAsc(Long tenantId);
    List<ZonaAlmacen> findByTenantIdAndActivoTrueOrderByNombreAsc(Long tenantId);
}
