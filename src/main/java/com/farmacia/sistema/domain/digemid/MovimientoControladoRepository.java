package com.farmacia.sistema.domain.digemid;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoControladoRepository extends JpaRepository<MovimientoControlado, Long> {

    List<MovimientoControlado> findByTenantId(Long tenantId);
    List<MovimientoControlado> findByProductoIdOrderByFechaDesc(Long productoId);
    List<MovimientoControlado> findByTenantIdAndProductoIdOrderByFechaDesc(Long tenantId, Long productoId);
    List<MovimientoControlado> findByTenantIdAndFechaBetweenOrderByFechaDesc(Long tenantId, LocalDateTime desde, LocalDateTime hasta);
    List<MovimientoControlado> findByVentaId(Long ventaId);
    List<MovimientoControlado> findByRegistroRecetaId(Long registroRecetaId);
    List<MovimientoControlado> findByMermaId(Long mermaId);
    List<MovimientoControlado> findByTenantIdAndTipoAndFechaBetweenOrderByFechaDesc(Long tenantId, String tipo, LocalDateTime desde, LocalDateTime hasta);
    List<MovimientoControlado> findByTenantIdAndProducto_TipoProductoControladoAndFechaBetweenOrderByFechaDesc(Long tenantId, String tipoProductoControlado, LocalDateTime desde, LocalDateTime hasta);
}
