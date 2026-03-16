package com.farmacia.sistema.domain.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByTenantIdOrderByNombreAsc(Long tenantId);
    List<Categoria> findByTenantIdAndActivoTrueOrderByNombreAsc(Long tenantId);
}
