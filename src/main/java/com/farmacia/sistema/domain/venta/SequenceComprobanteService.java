package com.farmacia.sistema.domain.venta;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SequenceComprobanteService {

    private final SequenceComprobanteRepository repository;

    public SequenceComprobanteService(SequenceComprobanteRepository repository) {
        this.repository = repository;
    }

    private static final long SEQ_BOL = 1L;
    private static final long SEQ_FAC = 2L;
    private static final long SEQ_NC  = 3L;
    private static final long SEQ_ND  = 4L;
    private static final long SEQ_GR  = 5L;

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
        String tipo = tipoComprobante != null ? tipoComprobante.trim().toUpperCase() : "BOL";
        long id = switch (tipo) {
            case "FAC" -> SEQ_FAC;
            case "NC"  -> SEQ_NC;
            case "ND"  -> SEQ_ND;
            case "GR"  -> SEQ_GR;
            default    -> SEQ_BOL;
        };
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
