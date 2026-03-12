package com.farmacia.sistema.domain.venta;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SequenceComprobanteService {

    private final SequenceComprobanteRepository repository;

    public SequenceComprobanteService(SequenceComprobanteRepository repository) {
        this.repository = repository;
    }

    /** ID de secuencia: 1 = Boleta, 2 = Factura */
    private static final long SEQ_BOL = 1L;
    private static final long SEQ_FAC = 2L;

    @Transactional
    public long getNextNumero() {
        return getNextNumeroPorTipo("BOL");
    }

    /**
     * Obtiene el siguiente número correlativo según tipo de comprobante.
     * BOL/FAC usan series independientes (id 1 y 2 en sequence_comprobante).
     */
    @Transactional
    public long getNextNumeroPorTipo(String tipoComprobante) {
        long id = "FAC".equalsIgnoreCase(tipoComprobante != null ? tipoComprobante.trim() : "") ? SEQ_FAC : SEQ_BOL;
        return repository.findAndLockById(id)
                .map(seq -> {
                    long actual = seq.getSiguiente();
                    seq.setSiguiente(actual + 1);
                    repository.save(seq);
                    return actual;
                })
                .orElseGet(() -> {
                    SequenceComprobante nueva = new SequenceComprobante();
                    nueva.setId(id);
                    nueva.setSiguiente(2L);
                    repository.save(nueva);
                    return 1L;
                });
    }
}
