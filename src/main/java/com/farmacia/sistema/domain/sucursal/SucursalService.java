package com.farmacia.sistema.domain.sucursal;

import com.farmacia.sistema.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SucursalService {

    private final SucursalRepository repository;

    public SucursalService(SucursalRepository repository) {
        this.repository = repository;
    }

    public List<Sucursal> listarTodas() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return repository.findByTenantId(tenantId);
        }
        return repository.findAll();
    }

    public Sucursal obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada"));
    }

    /** Sucursal designada como central (almacén): de aquí se descuenta al confirmar requerimientos. */
    public Optional<Sucursal> obtenerSucursalCentral() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) return Optional.empty();
        return repository.findFirstByTenantIdAndCentralTrue(tenantId);
    }

    public Sucursal sucursalPorDefecto() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return repository.findFirstByTenantIdAndActivaTrueOrderByIdAsc(tenantId)
                    .orElseGet(() -> {
                        Sucursal s = new Sucursal();
                        s.setCodigo("S01");
                        s.setNombre("Sucursal Central");
                        s.setActiva(true);
                        return repository.save(s);
                    });
        }
        return repository.findFirstByActivaTrueOrderByIdAsc()
                .orElseGet(() -> {
                    Sucursal s = new Sucursal();
                    s.setCodigo("S01");
                    s.setNombre("Sucursal Central");
                    s.setActiva(true);
                    return repository.save(s);
                });
    }

    public Sucursal crear(Sucursal sucursal) {
        if (sucursal.isCentral()) desmarcarOtrasCentral(sucursal.getTenantId(), null);
        return repository.save(sucursal);
    }

    public Sucursal actualizar(Long id, Sucursal datos) {
        Sucursal existente = obtenerPorId(id);
        existente.setCodigo(datos.getCodigo());
        existente.setNombre(datos.getNombre());
        existente.setDireccion(datos.getDireccion());
        existente.setActiva(datos.isActiva());
        existente.setCentral(datos.isCentral());
        if (datos.isCentral()) desmarcarOtrasCentral(existente.getTenantId(), id);
        return repository.save(existente);
    }

    private void desmarcarOtrasCentral(Long tenantId, Long excluirId) {
        if (tenantId == null) return;
        repository.findByTenantId(tenantId).stream()
                .filter(s -> !s.getId().equals(excluirId) && s.isCentral())
                .forEach(s -> { s.setCentral(false); repository.save(s); });
    }

    public void marcarComoCentral(Long sucursalId) {
        Sucursal s = obtenerPorId(sucursalId);
        desmarcarOtrasCentral(s.getTenantId(), sucursalId);
        s.setCentral(true);
        repository.save(s);
    }
}

