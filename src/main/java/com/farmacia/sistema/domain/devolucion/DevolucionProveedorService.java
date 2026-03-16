package com.farmacia.sistema.domain.devolucion;

import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoService;
import com.farmacia.sistema.domain.proveedor.Proveedor;
import com.farmacia.sistema.domain.proveedor.ProveedorService;
import com.farmacia.sistema.domain.inventario.InventarioService;
import com.farmacia.sistema.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class DevolucionProveedorService {

    private final DevolucionProveedorRepository repository;
    private final ProductoService productoService;
    private final ProveedorService proveedorService;
    private final InventarioService inventarioService;

    public DevolucionProveedorService(DevolucionProveedorRepository repository,
                                      ProductoService productoService,
                                      ProveedorService proveedorService,
                                      InventarioService inventarioService) {
        this.repository = repository;
        this.productoService = productoService;
        this.proveedorService = proveedorService;
        this.inventarioService = inventarioService;
    }

    public List<DevolucionProveedor> listar() {
        return repository.findByTenantIdOrderByFechaDesc(TenantContext.getTenantId());
    }

    public DevolucionProveedor obtenerPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Devolución no encontrada"));
    }

    private String generarNumero() {
        long count = repository.countByTenantId(TenantContext.getTenantId()) + 1;
        return "DEV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-" + String.format("%04d", count);
    }

    public DevolucionProveedor crear(Long proveedorId, String motivo, String observaciones, String usuario,
                                     List<Long> productoIds, List<Integer> cantidades, List<String> lotes) {
        Proveedor prov = proveedorService.obtenerPorId(proveedorId);

        DevolucionProveedor dev = new DevolucionProveedor();
        dev.setNumero(generarNumero());
        dev.setProveedor(prov);
        dev.setFecha(LocalDateTime.now());
        dev.setMotivo(motivo);
        dev.setObservaciones(observaciones);
        dev.setUsuarioRegistro(usuario);
        dev.setEstado("PENDIENTE");

        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < productoIds.size(); i++) {
            Producto p = productoService.obtenerPorId(productoIds.get(i));
            DevolucionProveedorItem item = new DevolucionProveedorItem();
            item.setDevolucion(dev);
            item.setProducto(p);
            item.setCantidad(cantidades.get(i));
            item.setCostoUnitario(p.getCostoUnitario());
            item.setSubtotal(p.getCostoUnitario().multiply(BigDecimal.valueOf(cantidades.get(i))));
            if (lotes != null && lotes.size() > i) item.setLote(lotes.get(i));
            dev.getItems().add(item);
            total = total.add(item.getSubtotal());
        }

        dev.setTotal(total);
        return repository.save(dev);
    }

    public void enviar(Long id) {
        DevolucionProveedor dev = obtenerPorId(id);
        dev.setEstado("ENVIADA");
        for (DevolucionProveedorItem item : dev.getItems()) {
            inventarioService.ajustar(item.getProducto().getId(), -item.getCantidad(),
                    "Devolución a proveedor " + dev.getNumero());
        }
        repository.save(dev);
    }

    public void registrarRespuesta(Long id, String estado, String notaCredito) {
        DevolucionProveedor dev = obtenerPorId(id);
        dev.setEstado(estado);
        dev.setNotaCreditoProveedor(notaCredito);
        repository.save(dev);
    }
}
