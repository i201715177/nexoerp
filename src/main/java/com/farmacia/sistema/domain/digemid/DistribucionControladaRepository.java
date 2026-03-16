package com.farmacia.sistema.domain.digemid;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DistribucionControladaRepository extends JpaRepository<DistribucionControlada, Long> {

    List<DistribucionControlada> findByTenantIdOrderByFechaEnvioDesc(Long tenantId);
    List<DistribucionControlada> findByTenantIdAndEstadoOrderByFechaEnvioDesc(Long tenantId, String estado);
    List<DistribucionControlada> findByTenantIdAndAlmacenDestinoIdAndEstadoOrderByFechaEnvioDesc(Long tenantId, Long almacenDestinoId, String estado);
}
