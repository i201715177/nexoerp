package com.farmacia.sistema.domain.reclamacion;

import com.farmacia.sistema.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class ReclamacionService {

    private final ReclamacionRepository repository;

    public ReclamacionService(ReclamacionRepository repository) {
        this.repository = repository;
    }

    public List<Reclamacion> listar() {
        return repository.findByTenantIdOrderByFechaDesc(TenantContext.getTenantId());
    }

    public Reclamacion obtenerPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Reclamación no encontrada"));
    }

    private String generarNumero() {
        long count = repository.countByTenantId(TenantContext.getTenantId()) + 1;
        return "REC-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy")) + "-" + String.format("%05d", count);
    }

    public Reclamacion crear(String tipo, String clienteNombre, String clienteDocumento,
                             String clienteTelefono, String clienteEmail, String clienteDireccion,
                             String detalle, String productoServicio, String montoReclamado, String usuario) {
        Reclamacion r = new Reclamacion();
        r.setNumero(generarNumero());
        r.setFecha(LocalDateTime.now());
        r.setTipo(tipo);
        r.setClienteNombre(clienteNombre);
        r.setClienteDocumento(clienteDocumento);
        r.setClienteTelefono(clienteTelefono);
        r.setClienteEmail(clienteEmail);
        r.setClienteDireccion(clienteDireccion);
        r.setDetalle(detalle);
        r.setProductoServicio(productoServicio);
        r.setMontoReclamado(montoReclamado);
        r.setEstado("RECIBIDO");
        r.setUsuarioRegistro(usuario);
        return repository.save(r);
    }

    public void responder(Long id, String respuesta, String nuevoEstado) {
        Reclamacion r = obtenerPorId(id);
        r.setRespuesta(respuesta);
        r.setEstado(nuevoEstado);
        r.setFechaRespuesta(LocalDateTime.now());
        repository.save(r);
    }

    public long countPendientes() {
        Long tid = TenantContext.getTenantId();
        return repository.countByTenantIdAndEstado(tid, "RECIBIDO")
                + repository.countByTenantIdAndEstado(tid, "EN_PROCESO");
    }
}
