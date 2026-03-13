package com.farmacia.sistema.domain.guiaremision;

import com.farmacia.sistema.domain.compra.OrdenCompra;
import com.farmacia.sistema.domain.compra.OrdenCompraItem;
import com.farmacia.sistema.domain.venta.SequenceComprobanteService;
import com.farmacia.sistema.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class GuiaRemisionService {

    private final GuiaRemisionRepository repository;
    private final SequenceComprobanteService sequenceService;

    public GuiaRemisionService(GuiaRemisionRepository repository,
                               SequenceComprobanteService sequenceService) {
        this.repository = repository;
        this.sequenceService = sequenceService;
    }

    public List<GuiaRemision> listarTodas() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return repository.findByTenantIdOrderByFechaEmisionDesc(tenantId);
        }
        return repository.findAll();
    }

    public GuiaRemision obtenerPorId(Long id) {
        return repository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Guía de remisión no encontrada"));
    }

    public GuiaRemision crearDesdeOrdenCompra(OrdenCompra orden, GuiaRemision guia) {
        guia.setOrdenCompra(orden);
        guia.setProveedor(orden.getProveedor());
        guia.setFechaEmision(LocalDateTime.now());
        guia.setSerie("T001");
        guia.setNumero(String.format("%08d", sequenceService.getNextNumeroPorTipo("GR")));
        guia.setEstado("EMITIDA");
        guia.setEstadoSunat("PENDIENTE");

        if (guia.getItems().isEmpty() && orden.getItems() != null) {
            for (OrdenCompraItem oci : orden.getItems()) {
                GuiaRemisionItem gri = new GuiaRemisionItem();
                gri.setGuiaRemision(guia);
                gri.setProducto(oci.getProducto());
                gri.setCantidad(oci.getCantidad());
                gri.setUnidadMedida("UND");
                gri.setDescripcion(oci.getProducto() != null ? oci.getProducto().getNombre() : "");
                guia.getItems().add(gri);
            }
        }

        return repository.save(guia);
    }

    public GuiaRemision crear(GuiaRemision guia) {
        guia.setFechaEmision(LocalDateTime.now());
        guia.setSerie("T001");
        guia.setNumero(String.format("%08d", sequenceService.getNextNumeroPorTipo("GR")));
        guia.setEstado("EMITIDA");
        guia.setEstadoSunat("PENDIENTE");
        return repository.save(guia);
    }

    public void anular(Long id) {
        GuiaRemision gr = obtenerPorId(id);
        gr.setEstado("ANULADA");
        repository.save(gr);
    }
}
