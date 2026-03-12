package com.farmacia.sistema.domain.inventario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LoteProductoRepository extends JpaRepository<LoteProducto, Long> {

    List<LoteProducto> findByProductoIdOrderByFechaVencimientoAsc(Long productoId);

    /** Lotes que vencen en los próximos días (por tenant) */
    List<LoteProducto> findByTenantIdAndFechaVencimientoBetweenAndCantidadActualGreaterThan(Long tenantId, LocalDate desde, LocalDate hasta, Integer cantidad);

    /** Lotes que vencen (sin filtro tenant, para compatibilidad) */
    List<LoteProducto> findByFechaVencimientoBetweenAndCantidadActualGreaterThan(LocalDate desde, LocalDate hasta, Integer cantidad);

    /** Lotes ya vencidos (fecha &lt; hoy, con stock) por tenant */
    List<LoteProducto> findByTenantIdAndFechaVencimientoBeforeAndCantidadActualGreaterThan(Long tenantId, LocalDate antesDe, Integer cantidad);
    List<LoteProducto> findByFechaVencimientoBeforeAndCantidadActualGreaterThan(LocalDate antesDe, Integer cantidad);

    /** Para autogenerar número: cuántos lotes hay con ese prefijo hoy */
    int countByTenantIdAndNumeroLoteStartingWith(Long tenantId, String prefix);

    void deleteByProductoId(Long productoId);
}
