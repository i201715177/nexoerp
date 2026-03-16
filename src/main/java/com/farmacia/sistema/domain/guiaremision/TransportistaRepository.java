package com.farmacia.sistema.domain.guiaremision;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransportistaRepository extends JpaRepository<Transportista, Long> {

    List<Transportista> findByTenantIdAndActivoTrueOrderByNombreAsc(Long tenantId);

    List<Transportista> findByTenantIdOrderByNombreAsc(Long tenantId);
}
