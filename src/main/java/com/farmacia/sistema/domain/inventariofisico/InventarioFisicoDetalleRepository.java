package com.farmacia.sistema.domain.inventariofisico;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventarioFisicoDetalleRepository extends JpaRepository<InventarioFisicoDetalle, Long> {
    List<InventarioFisicoDetalle> findByInventarioFisicoId(Long inventarioFisicoId);
}
