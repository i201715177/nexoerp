package com.farmacia.sistema.domain.inventario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {

    Optional<Almacen> findFirstByPrincipalTrue();

    Optional<Almacen> findFirstByTenantIdAndPrincipalTrue(Long tenantId);

    List<Almacen> findByOrderByPrincipalDescNombreAsc();

    List<Almacen> findByTenantIdOrderByPrincipalDescNombreAsc(Long tenantId);

    List<Almacen> findBySucursal_Id(Long sucursalId);
}
