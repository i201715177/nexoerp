package com.farmacia.sistema.domain.producto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByTenantId(Long tenantId);

    Page<Producto> findByTenantId(Long tenantId, Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE p.tenantId = :tid AND " +
           "(LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.codigo) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.principioActivo) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Producto> buscarPaginado(@Param("tid") Long tenantId, @Param("q") String query, Pageable pageable);

    Optional<Producto> findByCodigo(String codigo);

    Optional<Producto> findByTenantIdAndCodigo(Long tenantId, String codigo);

    Optional<Producto> findByCodigoBarras(String codigoBarras);

    Optional<Producto> findByTenantIdAndCodigoBarras(Long tenantId, String codigoBarras);

    boolean existsByCodigo(String codigo);

    boolean existsByTenantIdAndCodigo(Long tenantId, String codigo);

    List<Producto> findByTenantIdAndRequiereRecetaTrue(Long tenantId);
    List<Producto> findByTenantIdAndTipoProductoControlado(Long tenantId, String tipoProductoControlado);

    long countByTenantId(Long tenantId);
    long countByTenantIdAndActivoTrue(Long tenantId);
    long countByTenantIdAndStockActualLessThanEqual(Long tenantId, Integer stock);
}

