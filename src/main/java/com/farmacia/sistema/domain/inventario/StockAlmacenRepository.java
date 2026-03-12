package com.farmacia.sistema.domain.inventario;

import com.farmacia.sistema.domain.producto.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface StockAlmacenRepository extends JpaRepository<StockAlmacen, Long> {

    Optional<StockAlmacen> findByAlmacenIdAndProductoId(Long almacenId, Long productoId);

    List<StockAlmacen> findByProductoId(Long productoId);

    List<StockAlmacen> findByAlmacenId(Long almacenId);

    List<StockAlmacen> findByAlmacen_Sucursal_Id(Long sucursalId);

    void deleteByProductoId(Long productoId);
}
