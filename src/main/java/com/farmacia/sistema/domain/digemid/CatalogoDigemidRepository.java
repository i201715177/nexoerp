package com.farmacia.sistema.domain.digemid;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CatalogoDigemidRepository extends JpaRepository<CatalogoDigemid, Long> {

    @Query("SELECT c FROM CatalogoDigemid c WHERE " +
           "LOWER(c.principioActivo) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(c.nombreComercial) LIKE LOWER(CONCAT('%', :termino, '%'))")
    List<CatalogoDigemid> buscarPorTermino(@Param("termino") String termino);

    boolean existsByPrincipioActivo(String principioActivo);

    long count();
}
