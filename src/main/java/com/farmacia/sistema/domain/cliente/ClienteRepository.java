package com.farmacia.sistema.domain.cliente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findByTenantId(Long tenantId);

    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    boolean existsByNumeroDocumento(String numeroDocumento);

    boolean existsByTenantIdAndNumeroDocumento(Long tenantId, String numeroDocumento);
}

