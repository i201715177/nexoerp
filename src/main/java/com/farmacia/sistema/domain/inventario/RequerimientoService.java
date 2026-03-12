package com.farmacia.sistema.domain.inventario;

import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoService;
import com.farmacia.sistema.domain.sucursal.Sucursal;
import com.farmacia.sistema.domain.sucursal.SucursalService;
import com.farmacia.sistema.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RequerimientoService {

    private final RequerimientoRepository requerimientoRepository;
    private final SucursalService sucursalService;
    private final ProductoService productoService;
    private final InventarioService inventarioService;

    public RequerimientoService(RequerimientoRepository requerimientoRepository,
                                SucursalService sucursalService,
                                ProductoService productoService,
                                InventarioService inventarioService) {
        this.requerimientoRepository = requerimientoRepository;
        this.sucursalService = sucursalService;
        this.productoService = productoService;
        this.inventarioService = inventarioService;
    }

    public List<Requerimiento> listarTodos() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) return List.of();
        return requerimientoRepository.findByTenantIdOrderByFechaSolicitudDescWithItems(tenantId);
    }

    public List<Requerimiento> listarPorSucursal(Long sucursalId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null || sucursalId == null) return List.of();
        return requerimientoRepository.findByTenantIdAndSucursalIdOrderByFechaSolicitudDescWithItems(tenantId, sucursalId);
    }

    public Requerimiento obtenerPorId(Long id) {
        return requerimientoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Requerimiento no encontrado"));
    }

    /** productoIds y cantidades deben tener el mismo tamaño; se ignoran pares con cantidad <= 0. Evita duplicados por doble envío. */
    public Requerimiento crear(Long sucursalId, String observaciones, List<Long> productoIds, List<Integer> cantidades) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new IllegalStateException("No hay tenant en contexto");
        if (productoIds == null || productoIds.isEmpty() || cantidades == null) throw new IllegalArgumentException("Debe indicar al menos un producto y cantidad.");
        Sucursal sucursal = sucursalService.obtenerPorId(sucursalId);

        List<Pair> paresRequest = new ArrayList<>();
        int n = Math.min(productoIds.size(), cantidades.size());
        for (int i = 0; i < n; i++) {
            Long pid = productoIds.get(i);
            Integer c = cantidades.get(i);
            if (pid == null || c == null || c <= 0) continue;
            paresRequest.add(new Pair(pid, c));
        }
        if (paresRequest.isEmpty()) throw new IllegalArgumentException("Debe indicar al menos un producto con cantidad mayor a 0.");
        String firmaRequest = firmaItems(paresRequest);

        LocalDateTime hace45Seg = LocalDateTime.now().minusSeconds(45);
        List<Requerimiento> recientes = requerimientoRepository.findByTenantIdAndSucursalIdAndFechaSolicitudAfterWithItems(tenantId, sucursalId, hace45Seg);
        for (Requerimiento existente : recientes) {
            if (firmaItems(extraerPares(existente)).equals(firmaRequest))
                return existente;
        }

        Requerimiento r = new Requerimiento();
        r.setTenantId(tenantId);
        r.setSucursal(sucursal);
        r.setEstado("PENDIENTE");
        r.setFechaSolicitud(LocalDateTime.now());
        r.setObservaciones(observaciones);
        r = requerimientoRepository.save(r);
        for (Pair par : paresRequest) {
            Producto p = productoService.obtenerPorId(par.productoId);
            RequerimientoItem item = new RequerimientoItem();
            item.setRequerimiento(r);
            item.setProducto(p);
            item.setCantidad(par.cantidad);
            r.getItems().add(item);
        }
        return requerimientoRepository.save(r);
    }

    private static List<Pair> extraerPares(Requerimiento r) {
        if (r.getItems() == null) return List.of();
        return r.getItems().stream()
                .filter(i -> i.getProducto() != null && i.getCantidad() != null && i.getCantidad() > 0)
                .map(i -> new Pair(i.getProducto().getId(), i.getCantidad()))
                .collect(Collectors.toList());
    }

    private static String firmaItems(List<Pair> pares) {
        return pares.stream()
                .sorted(Comparator.comparing(Pair::getProductoId).thenComparing(Pair::getCantidad))
                .map(p -> p.productoId + ":" + p.cantidad)
                .collect(Collectors.joining(","));
    }

    private static class Pair {
        final long productoId;
        final int cantidad;
        Pair(Long productoId, Integer cantidad) {
            this.productoId = productoId != null ? productoId : 0L;
            this.cantidad = cantidad != null ? cantidad : 0;
        }
        long getProductoId() { return productoId; }
        int getCantidad() { return cantidad; }
    }

    public void marcarEnCamino(Long id) {
        Requerimiento r = obtenerPorId(id);
        if (!"PENDIENTE".equals(r.getEstado())) throw new IllegalStateException("Solo se puede marcar en camino un requerimiento PENDIENTE.");
        r.setEstado("EN_CAMINO");
        r.setFechaEnCamino(LocalDateTime.now());
        requerimientoRepository.save(r);
    }

    public void marcarRecibido(Long id) {
        Requerimiento r = obtenerPorId(id);
        if (!"EN_CAMINO".equals(r.getEstado())) throw new IllegalStateException("Solo se puede marcar recibido un requerimiento EN_CAMINO.");
        Long sucursalDestinoId = r.getSucursal() != null ? r.getSucursal().getId() : null;
        if (sucursalDestinoId == null) throw new IllegalStateException("Requerimiento sin sucursal.");

        for (RequerimientoItem item : r.getItems()) {
            if (item.getProducto() != null && item.getCantidad() != null && item.getCantidad() > 0) {
                Producto p = item.getProducto();
                int cantidad = item.getCantidad();
                // Actualizar stock en la sucursal destino
                inventarioService.actualizarStockEnSucursal(sucursalDestinoId, p.getId(), cantidad);
                // Reflejar también la entrada en el stockActual global del producto
                int stockActual = p.getStockActual() != null ? p.getStockActual() : 0;
                p.setStockActual(stockActual + cantidad);
                productoService.actualizarStock(p.getId(), stockActual + cantidad);
            }
        }
        r.setEstado("RECIBIDO");
        r.setFechaRecibido(LocalDateTime.now());
        requerimientoRepository.save(r);
    }
}
