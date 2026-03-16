package com.farmacia.sistema.domain.listaprecio;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ListaPrecioDetalleRepository extends JpaRepository<ListaPrecioDetalle, Long> {
    List<ListaPrecioDetalle> findByListaPrecioId(Long listaPrecioId);
    List<ListaPrecioDetalle> findByProductoId(Long productoId);
}
