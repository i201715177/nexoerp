package com.farmacia.sistema.domain.empresa;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SolicitudSuscripcionService {

    private final SolicitudSuscripcionRepository repository;

    public SolicitudSuscripcionService(SolicitudSuscripcionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SolicitudSuscripcion> listarTodas() {
        return repository.findAllByOrderByFechaSolicitudDesc();
    }

    public SolicitudSuscripcion guardar(SolicitudSuscripcion s) {
        return repository.save(s);
    }

    public SolicitudSuscripcion cambiarEstado(Long id, String nuevoEstado) {
        SolicitudSuscripcion s = repository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Solicitud no encontrada"));
        s.setEstado(nuevoEstado);
        return repository.save(s);
    }

    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new jakarta.persistence.EntityNotFoundException("Solicitud no encontrada");
        }
        repository.deleteById(id);
    }
}
