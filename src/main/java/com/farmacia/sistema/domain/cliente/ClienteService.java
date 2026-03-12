package com.farmacia.sistema.domain.cliente;

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
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    public List<Cliente> listarTodos() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return repository.findByTenantId(tenantId);
        }
        return repository.findAll();
    }

    public Cliente obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));
    }

    public Cliente crear(@Valid Cliente cliente) {
        normalizarDocumento(cliente);
        DocumentoValidator.validar(cliente.getTipoDocumento(), cliente.getNumeroDocumento());
        CelularValidator.validar(cliente.getTelefono());
        Long tenantId = TenantContext.getTenantId();
        boolean existe = tenantId != null
                ? repository.existsByTenantIdAndNumeroDocumento(tenantId, cliente.getNumeroDocumento())
                : repository.existsByNumeroDocumento(cliente.getNumeroDocumento());
        if (existe) {
            throw new IllegalArgumentException("Ya existe un cliente con el número de documento especificado");
        }
        return repository.save(cliente);
    }

    public Cliente actualizar(Long id, @Valid Cliente datos) {
        normalizarDocumento(datos);
        DocumentoValidator.validar(datos.getTipoDocumento(), datos.getNumeroDocumento());
        CelularValidator.validar(datos.getTelefono());
        Cliente existente = obtenerPorId(id);
        existente.setTipoDocumento(datos.getTipoDocumento());
        existente.setNumeroDocumento(datos.getNumeroDocumento());
        existente.setNombres(datos.getNombres());
        existente.setApellidos(datos.getApellidos());
        existente.setTelefono(datos.getTelefono());
        existente.setEmail(datos.getEmail());
        existente.setDireccion(datos.getDireccion());
        existente.setActivo(datos.isActivo());
        return repository.save(existente);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    private static void normalizarDocumento(Cliente c) {
        if (c.getTipoDocumento() != null) c.setTipoDocumento(c.getTipoDocumento().trim().toUpperCase());
        if (c.getNumeroDocumento() != null) c.setNumeroDocumento(c.getNumeroDocumento().trim().replaceAll("\\s", ""));
    }
}

