package com.farmacia.sistema.domain.producto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByTenantId(Long tenantId);

    Optional<Producto> findByCodigo(String codigo);

    Optional<Producto> findByTenantIdAndCodigo(Long tenantId, String codigo);

    Optional<Producto> findByCodigoBarras(String codigoBarras);

    Optional<Producto> findByTenantIdAndCodigoBarras(Long tenantId, String codigoBarras);

    boolean existsByCodigo(String codigo);

    boolean existsByTenantIdAndCodigo(Long tenantId, String codigo);
}

