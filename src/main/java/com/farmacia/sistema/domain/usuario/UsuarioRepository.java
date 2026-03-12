package com.farmacia.sistema.domain.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    List<Usuario> findByTenantIdOrderByUsernameAsc(Long tenantId);

    long countByTenantId(Long tenantId);

    boolean existsByUsername(String username);
}
