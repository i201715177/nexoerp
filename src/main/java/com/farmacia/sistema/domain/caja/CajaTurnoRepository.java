package com.farmacia.sistema.domain.caja;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CajaTurnoRepository extends JpaRepository<CajaTurno, Long> {

    Optional<CajaTurno> findFirstByEstadoOrderByFechaAperturaDesc(String estado);

    Optional<CajaTurno> findFirstByTenantIdAndEstadoOrderByFechaAperturaDesc(Long tenantId, String estado);

    Optional<CajaTurno> findFirstByTenantIdAndSucursalIdAndEstadoOrderByFechaAperturaDesc(Long tenantId, Long sucursalId, String estado);

    List<CajaTurno> findByEstado(String estado);

    List<CajaTurno> findByTenantIdAndSucursalIdOrderByFechaAperturaDesc(Long tenantId, Long sucursalId);

    List<CajaTurno> findByOrderByFechaAperturaDesc();

    List<CajaTurno> findByTenantIdOrderByFechaAperturaDesc(Long tenantId);

    /** Para historial: solo turnos no ocultos. */
    List<CajaTurno> findByTenantIdAndSucursalIdAndOcultoEnHistorialFalseOrderByFechaAperturaDesc(Long tenantId, Long sucursalId);
    List<CajaTurno> findByTenantIdAndOcultoEnHistorialFalseOrderByFechaAperturaDesc(Long tenantId);
    List<CajaTurno> findByOcultoEnHistorialFalseOrderByFechaAperturaDesc();
}
