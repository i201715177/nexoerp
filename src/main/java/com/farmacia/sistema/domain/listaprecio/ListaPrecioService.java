package com.farmacia.sistema.domain.listaprecio;

import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoService;
import com.farmacia.sistema.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ListaPrecioService {

    private final ListaPrecioRepository repository;
    private final ListaPrecioDetalleRepository detalleRepo;
    private final ProductoService productoService;

    public ListaPrecioService(ListaPrecioRepository repository, ListaPrecioDetalleRepository detalleRepo,
                              ProductoService productoService) {
        this.repository = repository;
        this.detalleRepo = detalleRepo;
        this.productoService = productoService;
    }

    public List<ListaPrecio> listar() {
        return repository.findByTenantIdOrderByNombreAsc(TenantContext.getTenantId());
    }

    public ListaPrecio obtenerPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Lista de precios no encontrada"));
    }

    public ListaPrecio crear(String nombre, String tipoCliente, Double descuento,
                             LocalDate fechaInicio, LocalDate fechaFin) {
        ListaPrecio lp = new ListaPrecio();
        lp.setNombre(nombre);
        lp.setTipoCliente(tipoCliente);
        lp.setDescuentoPorcentaje(descuento);
        lp.setFechaInicio(fechaInicio);
        lp.setFechaFin(fechaFin);
        return repository.save(lp);
    }

    public void agregarProducto(Long listaId, Long productoId, BigDecimal precioEspecial,
                                 Double descuentoPct, Integer cantidadMinima) {
        ListaPrecio lp = obtenerPorId(listaId);
        Producto p = productoService.obtenerPorId(productoId);
        ListaPrecioDetalle det = new ListaPrecioDetalle();
        det.setListaPrecio(lp);
        det.setProducto(p);
        det.setPrecioEspecial(precioEspecial);
        det.setDescuentoPorcentaje(descuentoPct);
        det.setCantidadMinima(cantidadMinima);
        detalleRepo.save(det);
    }

    public void toggle(Long id) {
        ListaPrecio lp = obtenerPorId(id);
        lp.setActivo(!lp.isActivo());
        repository.save(lp);
    }

    public List<ListaPrecioDetalle> obtenerDetalles(Long listaId) {
        return detalleRepo.findByListaPrecioId(listaId);
    }

    public void eliminarDetalle(Long detalleId) {
        detalleRepo.deleteById(detalleId);
    }
}
