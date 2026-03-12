package com.farmacia.sistema.domain.inventario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {

    List<Transferencia> findByTenantIdAndEstadoOrderByFechaEnvioDesc(Long tenantId, String estado);

    List<Transferencia> findByTenantIdAndAlmacenDestinoIdAndEstadoOrderByFechaEnvioDesc(Long tenantId, Long almacenDestinoId, String estado);

    List<Transferencia> findByTenantIdOrderByFechaEnvioDesc(Long tenantId);

    @Query("SELECT DISTINCT t FROM Transferencia t " +
           "LEFT JOIN FETCH t.almacenOrigen LEFT JOIN FETCH t.almacenDestino LEFT JOIN FETCH t.producto " +
           "WHERE t.tenantId = :tenantId ORDER BY t.fechaEnvio DESC")
    List<Transferencia> findByTenantIdOrderByFechaEnvioDescWithFetch(@Param("tenantId") Long tenantId);

    @Query("SELECT DISTINCT t FROM Transferencia t " +
           "LEFT JOIN FETCH t.almacenOrigen LEFT JOIN FETCH t.almacenDestino LEFT JOIN FETCH t.producto " +
           "WHERE t.tenantId = :tenantId AND t.estado = :estado ORDER BY t.fechaEnvio DESC")
    List<Transferencia> findByTenantIdAndEstadoOrderByFechaEnvioDescWithFetch(@Param("tenantId") Long tenantId, @Param("estado") String estado);

    @Query("SELECT DISTINCT t FROM Transferencia t " +
           "LEFT JOIN FETCH t.almacenOrigen LEFT JOIN FETCH t.almacenDestino LEFT JOIN FETCH t.producto " +
           "WHERE t.tenantId = :tenantId AND t.almacenDestino.id = :destinoId AND t.estado = :estado ORDER BY t.fechaEnvio DESC")
    List<Transferencia> findByTenantIdAndAlmacenDestinoIdAndEstadoOrderByFechaEnvioDescWithFetch(@Param("tenantId") Long tenantId, @Param("destinoId") Long destinoId, @Param("estado") String estado);
}
