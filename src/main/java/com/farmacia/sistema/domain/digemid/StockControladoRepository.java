package com.farmacia.sistema.domain.digemid;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockControladoRepository extends JpaRepository<StockControlado, Long> {

    List<StockControlado> findByTenantId(Long tenantId);
    List<StockControlado> findByProductoId(Long productoId);
    List<StockControlado> findByTenantIdAndProductoId(Long tenantId, Long productoId);
    Optional<StockControlado> findByProductoIdAndAlmacenIdAndLote(Long productoId, Long almacenId, String lote);
    List<StockControlado> findByTenantIdAndFechaVencimientoBetween(Long tenantId, LocalDate desde, LocalDate hasta);
    List<StockControlado> findByTenantIdAndFechaVencimientoBeforeAndCantidadGreaterThan(Long tenantId, LocalDate fecha, int cantidad);
}
