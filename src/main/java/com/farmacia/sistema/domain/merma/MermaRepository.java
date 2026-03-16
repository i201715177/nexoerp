package com.farmacia.sistema.domain.merma;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MermaRepository extends JpaRepository<Merma, Long> {

    List<Merma> findByTenantIdOrderByFechaRegistroDesc(Long tenantId);
    List<Merma> findByProductoIdOrderByFechaRegistroDesc(Long productoId);
    List<Merma> findByTenantIdAndFechaRegistroBetweenOrderByFechaRegistroDesc(Long tenantId, LocalDateTime desde, LocalDateTime hasta);
    List<Merma> findByTenantIdAndTipoMermaOrderByFechaRegistroDesc(Long tenantId, String tipoMerma);
    List<Merma> findByTenantIdAndProductoIdOrderByFechaRegistroDesc(Long tenantId, Long productoId);
    long countByTenantId(Long tenantId);
    long countByTenantIdAndLote(Long tenantId, String lote);
}
