package com.farmacia.sistema.domain.listaprecio;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ListaPrecioRepository extends JpaRepository<ListaPrecio, Long> {
    List<ListaPrecio> findByTenantIdOrderByNombreAsc(Long tenantId);
    List<ListaPrecio> findByTenantIdAndActivoTrueOrderByNombreAsc(Long tenantId);
    List<ListaPrecio> findByTenantIdAndTipoClienteAndActivoTrue(Long tenantId, String tipoCliente);
}
