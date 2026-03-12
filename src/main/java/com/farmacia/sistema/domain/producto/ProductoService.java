package com.farmacia.sistema.domain.producto;

import com.farmacia.sistema.domain.inventario.InventarioMovimientoRepository;
import com.farmacia.sistema.domain.inventario.LoteProductoRepository;
import com.farmacia.sistema.domain.inventario.StockAlmacenRepository;
import com.farmacia.sistema.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductoService {

    private final ProductoRepository repository;
    private final StockAlmacenRepository stockAlmacenRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final InventarioMovimientoRepository movimientoRepository;

    public ProductoService(ProductoRepository repository,
                           StockAlmacenRepository stockAlmacenRepository,
                           LoteProductoRepository loteProductoRepository,
                           InventarioMovimientoRepository movimientoRepository) {
        this.repository = repository;
        this.stockAlmacenRepository = stockAlmacenRepository;
        this.loteProductoRepository = loteProductoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    public List<Producto> listarTodos() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return repository.findByTenantId(tenantId);
        }
        return repository.findAll();
    }

    /** Marcas distintas (no nulas ni vacías) para filtros, ordenadas. */
    public List<String> listarMarcasDistintas() {
        return listarTodos().stream()
                .map(Producto::getMarca)
                .filter(m -> m != null && !m.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    public Producto obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));
    }

    public Producto crear(@Valid Producto producto) {
        if (producto.getCodigo() == null || producto.getCodigo().isBlank()) {
            producto.setCodigo(generarCodigo());
        } else {
            Long tenantId = TenantContext.getTenantId();
            boolean existe = tenantId != null
                    ? repository.existsByTenantIdAndCodigo(tenantId, producto.getCodigo())
                    : repository.existsByCodigo(producto.getCodigo());
            if (existe) {
                throw new IllegalArgumentException("Ya existe un producto con el código especificado");
            }
        }
        validarStock(producto.getStockActual(), producto.getStockMinimo(), producto.getStockMaximo());
        return repository.save(producto);
    }

    public Producto obtenerPorCodigo(String codigo) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return repository.findByTenantIdAndCodigoBarras(tenantId, codigo)
                    .or(() -> repository.findByTenantIdAndCodigo(tenantId, codigo))
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));
        }
        return repository.findByCodigoBarras(codigo)
                .or(() -> repository.findByCodigo(codigo))
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));
    }

    public Producto actualizar(Long id, @Valid Producto datos) {
        validarStock(datos.getStockActual(), datos.getStockMinimo(), datos.getStockMaximo());
        Producto existente = obtenerPorId(id);
        existente.setNombre(datos.getNombre());
        existente.setDescripcion(datos.getDescripcion());
        existente.setLaboratorio(datos.getLaboratorio());
        existente.setPresentacion(datos.getPresentacion());
        existente.setCategoria(datos.getCategoria());
        existente.setMarca(datos.getMarca());
        existente.setUnidadMedida(datos.getUnidadMedida());
        existente.setCodigoBarras(datos.getCodigoBarras());
        existente.setImagenUrl(datos.getImagenUrl());
        existente.setPrecioVenta(datos.getPrecioVenta());
        existente.setStockActual(datos.getStockActual());
        existente.setStockMinimo(datos.getStockMinimo());
        existente.setStockMaximo(datos.getStockMaximo());
        existente.setActivo(datos.isActivo());
        return repository.save(existente);
    }

    private void validarStock(Integer stockActual, Integer stockMinimo, Integer stockMaximo) {
        if (stockActual != null && stockMinimo != null && stockActual < stockMinimo) {
            throw new IllegalArgumentException(
                    "El stock actual debe ser mayor o igual al stock mínimo. Actual: " + stockActual + ", Mínimo: " + stockMinimo + "."
            );
        }
        if (stockMaximo != null) {
            if (stockMinimo != null && stockMinimo > stockMaximo) {
                throw new IllegalArgumentException(
                        "El stock mínimo no puede ser mayor que el stock máximo. Mínimo: " + stockMinimo + ", Máximo: " + stockMaximo + "."
                );
            }
            if (stockActual != null && stockActual > stockMaximo) {
                throw new IllegalArgumentException(
                        "El stock actual no puede superar el stock máximo. Actual: " + stockActual + ", Máximo: " + stockMaximo + "."
                );
            }
        }
    }

    public void eliminar(Long id) {
        stockAlmacenRepository.deleteByProductoId(id);
        loteProductoRepository.deleteByProductoId(id);
        movimientoRepository.deleteByProductoId(id);
        repository.deleteById(id);
    }

    /** Actualiza solo el stock (para ajustes de inventario; no valida stock mínimo). */
    public void actualizarStock(Long id, int nuevoStock) {
        if (nuevoStock < 0) throw new IllegalArgumentException("El stock no puede ser negativo");
        Producto p = obtenerPorId(id);
        p.setStockActual(nuevoStock);
        repository.save(p);
    }

    private String generarCodigo() {
        Long tenantId = TenantContext.getTenantId();
        long total = tenantId != null
                ? repository.findByTenantId(tenantId).size() + 1
                : repository.count() + 1;
        return "PRD-" + String.format("%05d", total);
    }
}

