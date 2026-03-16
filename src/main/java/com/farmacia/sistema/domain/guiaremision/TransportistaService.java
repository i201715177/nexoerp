package com.farmacia.sistema.domain.guiaremision;

import com.farmacia.sistema.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransportistaService {

    private final TransportistaRepository repository;

    public TransportistaService(TransportistaRepository repository) {
        this.repository = repository;
    }

    public List<Transportista> listarActivos() {
        Long tid = TenantContext.getTenantId();
        return repository.findByTenantIdAndActivoTrueOrderByNombreAsc(tid);
    }

    public Transportista obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transportista no encontrado"));
    }

    @Transactional
    public Transportista guardar(Transportista transportista) {
        return repository.save(transportista);
    }

    @Transactional
    public Transportista actualizar(Long id, Transportista datos) {
        Transportista t = obtenerPorId(id);
        t.setRuc(datos.getRuc());
        t.setNombre(datos.getNombre());
        t.setConductorDni(datos.getConductorDni());
        t.setConductorNombre(datos.getConductorNombre());
        t.setConductorLicencia(datos.getConductorLicencia());
        t.setPlacaVehiculo(datos.getPlacaVehiculo());
        t.setTelefono(datos.getTelefono());
        return repository.save(t);
    }

    @Transactional
    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
