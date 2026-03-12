package com.farmacia.sistema.domain.proveedor;

import com.farmacia.sistema.tenant.TenantContext;
import com.farmacia.sistema.util.CelularValidator;
import com.farmacia.sistema.util.DocumentoValidator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProveedorService {

    private final ProveedorRepository repository;

    public ProveedorService(ProveedorRepository repository) {
        this.repository = repository;
    }

    public List<Proveedor> listarTodos() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return repository.findByTenantId(tenantId);
        }
        return repository.findAll();
    }

    public Proveedor obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado"));
    }

    public Proveedor crear(@Valid Proveedor proveedor) {
        normalizarDocumento(proveedor);
        DocumentoValidator.validar(proveedor.getTipoDocumento(), proveedor.getNumeroDocumento());
        CelularValidator.validar(proveedor.getTelefono());
        Long tenantId = TenantContext.getTenantId();
        boolean existe = tenantId != null
                ? repository.existsByTenantIdAndNumeroDocumento(tenantId, proveedor.getNumeroDocumento())
                : repository.existsByNumeroDocumento(proveedor.getNumeroDocumento());
        if (existe) {
            throw new IllegalArgumentException("Ya existe un proveedor con el número de documento especificado");
        }
        return repository.save(proveedor);
    }

    public Proveedor actualizar(Long id, @Valid Proveedor datos) {
        normalizarDocumento(datos);
        DocumentoValidator.validar(datos.getTipoDocumento(), datos.getNumeroDocumento());
        CelularValidator.validar(datos.getTelefono());
        Proveedor existente = obtenerPorId(id);
        existente.setTipoDocumento(datos.getTipoDocumento());
        existente.setNumeroDocumento(datos.getNumeroDocumento());
        existente.setRazonSocial(datos.getRazonSocial());
        existente.setContacto(datos.getContacto());
        existente.setTelefono(datos.getTelefono());
        existente.setEmail(datos.getEmail());
        existente.setDireccion(datos.getDireccion());
        existente.setActivo(datos.isActivo());
        return repository.save(existente);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    private static void normalizarDocumento(Proveedor p) {
        if (p.getTipoDocumento() != null) p.setTipoDocumento(p.getTipoDocumento().trim().toUpperCase());
        if (p.getNumeroDocumento() != null) p.setNumeroDocumento(p.getNumeroDocumento().trim().replaceAll("\\s", ""));
    }
}

