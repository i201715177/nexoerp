package com.farmacia.sistema.domain.devolucion;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DevolucionProveedorRepository extends JpaRepository<DevolucionProveedor, Long> {
    List<DevolucionProveedor> findByTenantIdOrderByFechaDesc(Long tenantId);
    long countByTenantId(Long tenantId);
}
