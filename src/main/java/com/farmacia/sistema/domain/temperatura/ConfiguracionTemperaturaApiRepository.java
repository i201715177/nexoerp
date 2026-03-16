package com.farmacia.sistema.domain.temperatura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionTemperaturaApiRepository extends JpaRepository<ConfiguracionTemperaturaApi, Long> {

    Optional<ConfiguracionTemperaturaApi> findByTenantId(Long tenantId);

    Optional<ConfiguracionTemperaturaApi> findByApiKey(String apiKey);
}
