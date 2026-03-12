package com.farmacia.sistema.domain.venta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VentaItemRepository extends JpaRepository<VentaItem, Long> {

    @Query("SELECT i FROM VentaItem i JOIN FETCH i.producto WHERE i.venta.id IN :ventaIds")
    List<VentaItem> findByVentaIdInWithProducto(@Param("ventaIds") List<Long> ventaIds);
}
