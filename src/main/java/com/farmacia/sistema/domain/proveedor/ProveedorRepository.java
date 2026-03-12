package com.farmacia.sistema.domain.proveedor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    List<Proveedor> findByTenantId(Long tenantId);

    boolean existsByNumeroDocumento(String numeroDocumento);

    boolean existsByTenantIdAndNumeroDocumento(Long tenantId, String numeroDocumento);
}

