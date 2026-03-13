package com.farmacia.sistema.domain.venta;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotaDebitoRepository extends JpaRepository<NotaDebito, Long> {
    List<NotaDebito> findByTenantIdOrderByFechaDesc(Long tenantId);
    List<NotaDebito> findByVentaId(Long ventaId);
}
