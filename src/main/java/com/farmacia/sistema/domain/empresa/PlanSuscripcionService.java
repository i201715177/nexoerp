package com.farmacia.sistema.domain.empresa;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PlanSuscripcionService {

    private final PlanSuscripcionRepository repository;

    public PlanSuscripcionService(PlanSuscripcionRepository repository) {
        this.repository = repository;
    }

    public List<PlanSuscripcion> listarActivos() {
        return repository.findByActivoTrueOrderByPrecioMensualAsc();
    }

    public List<PlanSuscripcion> listarTodos() {
        return repository.findAll();
    }

    public PlanSuscripcion obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado"));
    }

    public PlanSuscripcion obtenerPorCodigo(String codigo) {
        return repository.findByCodigo(codigo)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado: " + codigo));
    }

    public PlanSuscripcion crear(PlanSuscripcion plan) {
        if (repository.findByCodigo(plan.getCodigo()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un plan con el código '" + plan.getCodigo() + "'.");
        }
        return repository.save(plan);
    }

    public PlanSuscripcion actualizar(Long id, PlanSuscripcion datos) {
        PlanSuscripcion existente = obtenerPorId(id);
        existente.setNombre(datos.getNombre());
        existente.setDescripcion(datos.getDescripcion());
        existente.setPrecioMensual(datos.getPrecioMensual());
        existente.setPrecioAnual(datos.getPrecioAnual());
        existente.setMaxUsuarios(datos.getMaxUsuarios());
        existente.setActivo(datos.isActivo());
        return repository.save(existente);
    }

    /** Devuelve el límite de usuarios para este plan (null = sin límite). */
    public Integer getMaxUsuarios(PlanSuscripcion plan) {
        return plan != null ? plan.getMaxUsuarios() : null;
    }
}
