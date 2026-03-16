package com.farmacia.sistema.domain.cotizacion;

import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoService;
import com.farmacia.sistema.domain.cliente.Cliente;
import com.farmacia.sistema.domain.cliente.ClienteService;
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
public class CotizacionService {

    private final CotizacionRepository repository;
    private final ProductoService productoService;
    private final ClienteService clienteService;

    public CotizacionService(CotizacionRepository repository, ProductoService productoService,
                             ClienteService clienteService) {
        this.repository = repository;
        this.productoService = productoService;
        this.clienteService = clienteService;
    }

    public List<Cotizacion> listar() {
        return repository.findByTenantIdOrderByFechaDesc(TenantContext.getTenantId());
    }

    public Cotizacion obtenerPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Cotización no encontrada"));
    }

    private String generarNumero() {
        long count = repository.countByTenantId(TenantContext.getTenantId()) + 1;
        String prefix = "COT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        return prefix + String.format("%04d", count);
    }

    public Cotizacion crear(Long clienteId, String nombreCliente, LocalDate vigenciaHasta,
                            String observaciones, String usuario,
                            List<Long> productoIds, List<Integer> cantidades,
                            List<BigDecimal> precios, List<BigDecimal> descuentos) {
        Cotizacion c = new Cotizacion();
        c.setNumero(generarNumero());
        c.setFecha(LocalDateTime.now());
        c.setVigenciaHasta(vigenciaHasta);
        c.setObservaciones(observaciones);
        c.setUsuarioRegistro(usuario);
        c.setEstado("PENDIENTE");

        if (clienteId != null) {
            Cliente cl = clienteService.obtenerPorId(clienteId);
            c.setCliente(cl);
            c.setNombreCliente(cl.getNombres() + (cl.getApellidos() != null ? " " + cl.getApellidos() : ""));
        } else {
            c.setNombreCliente(nombreCliente);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descTotal = BigDecimal.ZERO;

        for (int i = 0; i < productoIds.size(); i++) {
            Producto p = productoService.obtenerPorId(productoIds.get(i));
            CotizacionItem item = new CotizacionItem();
            item.setCotizacion(c);
            item.setProducto(p);
            item.setCantidad(cantidades.get(i));
            item.setPrecioUnitario(precios != null && precios.size() > i ? precios.get(i) : p.getPrecioVenta());
            BigDecimal desc = descuentos != null && descuentos.size() > i ? descuentos.get(i) : BigDecimal.ZERO;
            item.setDescuento(desc);
            BigDecimal sub = item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())).subtract(desc);
            item.setSubtotal(sub);
            c.getItems().add(item);
            subtotal = subtotal.add(sub.add(desc));
            descTotal = descTotal.add(desc);
        }

        c.setSubtotal(subtotal);
        c.setDescuentoTotal(descTotal);
        c.setTotal(subtotal.subtract(descTotal));

        return repository.save(c);
    }

    public void cambiarEstado(Long id, String nuevoEstado) {
        Cotizacion c = obtenerPorId(id);
        c.setEstado(nuevoEstado);
        repository.save(c);
    }

    public long countPendientes() {
        return repository.countByTenantIdAndEstado(TenantContext.getTenantId(), "PENDIENTE");
    }
}
