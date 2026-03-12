package com.farmacia.sistema.domain.venta;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SequenceComprobanteRepository extends JpaRepository<SequenceComprobante, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SequenceComprobante s WHERE s.id = :id")
    java.util.Optional<SequenceComprobante> findAndLockById(@org.springframework.data.repository.query.Param("id") Long id);
}
