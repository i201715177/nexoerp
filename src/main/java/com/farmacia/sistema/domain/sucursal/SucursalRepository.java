package com.farmacia.sistema.domain.sucursal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SucursalRepository extends JpaRepository<Sucursal, Long> {

    List<Sucursal> findByTenantId(Long tenantId);

    Optional<Sucursal> findFirstByActivaTrueOrderByIdAsc();

    Optional<Sucursal> findFirstByTenantIdAndActivaTrueOrderByIdAsc(Long tenantId);

    Optional<Sucursal> findByCodigo(String codigo);

    Optional<Sucursal> findByTenantIdAndCodigo(Long tenantId, String codigo);

    Optional<Sucursal> findFirstByTenantIdAndCentralTrue(Long tenantId);
}

