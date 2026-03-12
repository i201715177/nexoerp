package com.farmacia.sistema.domain.venta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotaCreditoRepository extends JpaRepository<NotaCredito, Long> {

    List<NotaCredito> findByVentaIdOrderByFechaDesc(Long ventaId);
}
