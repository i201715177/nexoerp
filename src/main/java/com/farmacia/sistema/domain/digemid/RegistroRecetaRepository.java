package com.farmacia.sistema.domain.digemid;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RegistroRecetaRepository extends JpaRepository<RegistroReceta, Long> {

    List<RegistroReceta> findByTenantIdOrderByFechaRegistroDesc(Long tenantId);
    List<RegistroReceta> findByProductoIdOrderByFechaRegistroDesc(Long productoId);
    List<RegistroReceta> findByVentaId(Long ventaId);
    Optional<RegistroReceta> findByTenantIdAndNumeroReceta(Long tenantId, String numeroReceta);
    List<RegistroReceta> findByTenantIdAndFechaRegistroBetweenOrderByFechaRegistroDesc(Long tenantId, LocalDateTime desde, LocalDateTime hasta);

    @Query("SELECT r FROM RegistroReceta r WHERE r.tenantId = :tenantId AND LOWER(TRIM(r.numeroReceta)) = LOWER(TRIM(:numero)) AND r.id <> COALESCE(:excluirId, -1)")
    List<RegistroReceta> findByTenantIdAndNumeroRecetaIgualExcluyendoId(@Param("tenantId") Long tenantId, @Param("numero") String numeroReceta, @Param("excluirId") Long excluirId);
}
