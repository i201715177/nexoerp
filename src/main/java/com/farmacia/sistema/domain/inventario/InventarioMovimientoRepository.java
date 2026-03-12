package com.farmacia.sistema.domain.inventario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventarioMovimientoRepository extends JpaRepository<InventarioMovimiento, Long> {

    List<InventarioMovimiento> findByProductoIdOrderByFechaDesc(Long productoId);

    void deleteByProductoId(Long productoId);
}
