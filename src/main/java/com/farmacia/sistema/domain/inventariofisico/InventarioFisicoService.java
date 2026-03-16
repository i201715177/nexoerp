package com.farmacia.sistema.domain.inventariofisico;

import com.farmacia.sistema.domain.inventario.Almacen;
import com.farmacia.sistema.domain.inventario.InventarioService;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoService;
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
public class InventarioFisicoService {

    private final InventarioFisicoRepository repository;
    private final InventarioFisicoDetalleRepository detalleRepository;
    private final ProductoService productoService;
    private final InventarioService inventarioService;

    public InventarioFisicoService(InventarioFisicoRepository repository,
                                   InventarioFisicoDetalleRepository detalleRepository,
                                   ProductoService productoService,
                                   InventarioService inventarioService) {
        this.repository = repository;
        this.detalleRepository = detalleRepository;
        this.productoService = productoService;
        this.inventarioService = inventarioService;
    }

    public List<InventarioFisico> listar() {
        return repository.findByTenantIdOrderByFechaDesc(TenantContext.getTenantId());
    }

    public InventarioFisico obtenerPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Inventario físico no encontrado"));
    }

    private String generarCodigo() {
        long count = repository.countByTenantId(TenantContext.getTenantId()) + 1;
        return "IF-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-" + String.format("%04d", count);
    }

    public InventarioFisico crear(Almacen almacen, String observaciones, String usuario) {
        InventarioFisico inv = new InventarioFisico();
        inv.setCodigo(generarCodigo());
        inv.setFecha(LocalDateTime.now());
        inv.setEstado("ABIERTO");
        inv.setAlmacen(almacen);
        inv.setObservaciones(observaciones);
        inv.setUsuarioRegistro(usuario);

        inv = repository.save(inv);

        List<Producto> productos = productoService.listarTodos();
        for (Producto p : productos) {
            if (!p.isActivo()) continue;
            InventarioFisicoDetalle det = new InventarioFisicoDetalle();
            det.setInventarioFisico(inv);
            det.setProducto(p);
            det.setStockSistema(p.getStockActual());
            detalleRepository.save(det);
        }

        return inv;
    }

    public void registrarConteo(Long detalleId, Integer stockFisico, String observacion) {
        InventarioFisicoDetalle det = detalleRepository.findById(detalleId)
                .orElseThrow(() -> new EntityNotFoundException("Detalle no encontrado"));
        det.setStockFisico(stockFisico);
        det.setDiferencia(stockFisico - det.getStockSistema());
        det.setObservacion(observacion);
        detalleRepository.save(det);
    }

    public void cerrar(Long inventarioFisicoId, String usuario) {
        InventarioFisico inv = obtenerPorId(inventarioFisicoId);
        inv.setEstado("CERRADO");
        inv.setUsuarioCierre(usuario);
        inv.setFechaCierre(LocalDateTime.now());
        repository.save(inv);
    }

    public void aplicarAjustes(Long inventarioFisicoId, String usuario) {
        InventarioFisico inv = obtenerPorId(inventarioFisicoId);
        if (!"CERRADO".equals(inv.getEstado())) {
            throw new IllegalStateException("El inventario debe estar cerrado para aplicar ajustes");
        }

        List<InventarioFisicoDetalle> detalles = detalleRepository.findByInventarioFisicoId(inventarioFisicoId);
        for (InventarioFisicoDetalle det : detalles) {
            if (det.getDiferencia() != null && det.getDiferencia() != 0 && !det.isAjustado()) {
                inventarioService.ajustar(det.getProducto().getId(), det.getDiferencia(),
                        "Ajuste inventario físico " + inv.getCodigo());
                det.setAjustado(true);
                detalleRepository.save(det);
            }
        }

        inv.setEstado("AJUSTADO");
        repository.save(inv);
    }

    public List<InventarioFisicoDetalle> obtenerDetalles(Long inventarioFisicoId) {
        return detalleRepository.findByInventarioFisicoId(inventarioFisicoId);
    }
}
