package com.farmacia.sistema.domain.empresa;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmpresaService {

    private final EmpresaRepository repository;

    public EmpresaService(EmpresaRepository repository) {
        this.repository = repository;
    }

    public List<Empresa> listarTodas() {
        return repository.findAll();
    }

    public List<Empresa> listarTodasConPlan() {
        return repository.findAllWithPlan();
    }

    public java.util.Optional<Empresa> buscarPorNombreExacto(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return java.util.Optional.empty();
        }
        return repository.findByNombreIgnoreCase(nombre.trim());
    }

    public Empresa obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empresa no encontrada"));
    }

    public Empresa obtenerPorIdConPlan(Long id) {
        return repository.findByIdWithPlan(id)
                .orElseThrow(() -> new EntityNotFoundException("Empresa no encontrada"));
    }

    public Empresa empresaPorDefecto() {
        return repository.findFirstByActivaTrueOrderByIdAsc()
                .orElseGet(() -> repository.findByCodigo("TENANT1")
                        .orElseGet(() -> {
                            Empresa e = new Empresa();
                            e.setCodigo("TENANT1");
                            e.setNombre("Farmacia Central");
                            e.setDescripcion("Empresa demo principal");
                            e.setActiva(true);
                            return repository.save(e);
                        }));
    }

    public boolean existeCodigo(String codigo) {
        return repository.findByCodigo(codigo).isPresent();
    }

    public Empresa crear(Empresa empresa) {
        if (existeCodigo(empresa.getCodigo())) {
            throw new IllegalArgumentException("Ya existe una empresa con el código '" + empresa.getCodigo() + "'.");
        }
        return repository.save(empresa);
    }

    public Empresa actualizar(Long id, Empresa datos) {
        Empresa existente = obtenerPorId(id);
        existente.setCodigo(datos.getCodigo());
        existente.setNombre(datos.getNombre());
        existente.setDescripcion(datos.getDescripcion());
        existente.setActiva(datos.isActiva());
        existente.setPlan(datos.getPlan());
        existente.setTipoSuscripcion(datos.getTipoSuscripcion());
        existente.setMaxUsuarios(datos.getMaxUsuarios());
        return repository.save(existente);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}

