package com.farmacia.sistema.domain.merma;

import com.farmacia.sistema.domain.digemid.DigemidService;
import com.farmacia.sistema.domain.inventario.AlmacenRepository;
import com.farmacia.sistema.domain.inventario.InventarioService;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoRepository;
import com.farmacia.sistema.domain.producto.ProductoService;
import com.farmacia.sistema.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MermaService {

    private final MermaRepository mermaRepository;
    private final ProductoService productoService;
    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;
    private final DigemidService digemidService;
    private final AlmacenRepository almacenRepository;

    public MermaService(MermaRepository mermaRepository,
                        ProductoService productoService,
                        ProductoRepository productoRepository,
                        InventarioService inventarioService,
                        DigemidService digemidService,
                        AlmacenRepository almacenRepository) {
        this.mermaRepository = mermaRepository;
        this.productoService = productoService;
        this.productoRepository = productoRepository;
        this.inventarioService = inventarioService;
        this.digemidService = digemidService;
        this.almacenRepository = almacenRepository;
    }

    private Long tenantId() {
        return TenantContext.getTenantId();
    }

    private String generarNumeroMerma() {
        String prefijo = "MER-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        long count = mermaRepository.countByTenantId(tenantId()) + 1;
        return prefijo + String.format("%04d", count);
    }

    public List<Merma> listarTodas() {
        Long tid = tenantId();
        if (tid == null) return List.of();
        return mermaRepository.findByTenantIdOrderByFechaRegistroDesc(tid);
    }

    public Merma obtenerPorId(Long id) {
        return mermaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Merma no encontrada"));
    }

    public Merma registrarMerma(Long productoId, Long almacenId, int cantidad, String tipoMerma,
                                 String motivo, String usuarioRegistro, String observaciones,
                                 String lote, String responsableAutorizado,
                                 String aprobacionQF, String actaDestruccion, String numeroReporte) {
        Producto producto = productoService.obtenerPorId(productoId);
        int stockActual = producto.getStockActual() != null ? producto.getStockActual() : 0;
        if (stockActual < cantidad) {
            throw new IllegalArgumentException("Stock insuficiente para " + producto.getNombre() + ". Disponible: " + stockActual + ", solicitado: " + cantidad);
        }

        int stockDespues = stockActual - cantidad;

        Merma m = new Merma();
        m.setNumeroMerma(generarNumeroMerma());
        m.setProducto(producto);
        if (almacenId != null) {
            m.setAlmacen(almacenRepository.findById(almacenId).orElse(null));
        }
        m.setLote(lote != null && !lote.isBlank() ? lote.trim() : producto.getLote());
        m.setFechaVencimientoProducto(producto.getFechaVencimiento());
        m.setCantidad(cantidad);
        m.setStockAntes(stockActual);
        m.setStockDespues(stockDespues);
        m.setTipoMerma(tipoMerma != null && !tipoMerma.isBlank() ? tipoMerma : "OTRO");
        m.setMotivo(motivo);
        m.setObservaciones(observaciones);
        m.setUsuarioRegistro(usuarioRegistro);
        m.setCostoEstimado(producto.getCostoUnitario() != null ? producto.getCostoUnitario().multiply(BigDecimal.valueOf(cantidad)) : null);
        m.setEsControlado(producto.isRequiereReceta());

        if (producto.isRequiereReceta()) {
            m.setResponsableAutorizado(responsableAutorizado);
            m.setAprobacionQuimicoFarmaceutico(aprobacionQF);
            m.setActaDestruccion(actaDestruccion);
            m.setNumeroReporte(numeroReporte);
        }

        m = mermaRepository.save(m);

        productoService.actualizarStock(productoId, stockDespues);
        producto.setStockActual(stockDespues);
        inventarioService.registrarAjuste(producto, -cantidad, "Merma " + m.getNumeroMerma() + ": " + (motivo != null ? motivo : m.getTipoMerma()));

        if (producto.isRequiereReceta()) {
            digemidService.registrarSalidaControlada(productoId, almacenId, cantidad, "SALIDA_MERMA",
                    "Merma " + m.getNumeroMerma() + ": " + (motivo != null ? motivo : m.getTipoMerma()), null, null, m.getId());
        }
        return m;
    }

    /** Backward-compatible overload */
    public Merma registrarMerma(Long productoId, Long almacenId, int cantidad, String tipoMerma, String motivo, String usuarioRegistro) {
        return registrarMerma(productoId, almacenId, cantidad, tipoMerma, motivo, usuarioRegistro, null, null, null, null, null, null);
    }

    public Merma guardar(Merma merma) {
        return mermaRepository.save(merma);
    }

    public List<Merma> listarPorProducto(Long productoId) {
        return mermaRepository.findByProductoIdOrderByFechaRegistroDesc(productoId);
    }

    public List<Merma> listarEntreFechas(LocalDateTime desde, LocalDateTime hasta) {
        Long tid = tenantId();
        if (tid == null) return List.of();
        return mermaRepository.findByTenantIdAndFechaRegistroBetweenOrderByFechaRegistroDesc(tid, desde, hasta);
    }

    public List<Merma> listarPorTipo(String tipoMerma) {
        Long tid = tenantId();
        if (tid == null) return List.of();
        return mermaRepository.findByTenantIdAndTipoMermaOrderByFechaRegistroDesc(tid, tipoMerma);
    }

    // --- Reportes ---

    public List<Map<String, Object>> reportePorProducto() {
        Long tid = tenantId();
        if (tid == null) return List.of();
        List<Merma> todas = mermaRepository.findByTenantIdOrderByFechaRegistroDesc(tid);
        Map<Long, List<Merma>> porProducto = todas.stream()
                .filter(m -> m.getProducto() != null)
                .collect(Collectors.groupingBy(m -> m.getProducto().getId()));

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map.Entry<Long, List<Merma>> entry : porProducto.entrySet()) {
            List<Merma> mermas = entry.getValue();
            Producto p = mermas.get(0).getProducto();
            int totalMerma = mermas.stream().mapToInt(m -> m.getCantidad() != null ? m.getCantidad() : 0).sum();
            BigDecimal costoTotal = mermas.stream()
                    .map(m -> m.getCostoEstimado() != null ? m.getCostoEstimado() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int stockActual = p.getStockActual() != null ? p.getStockActual() : 0;
            double porcentajePerdida = (stockActual + totalMerma) > 0
                    ? (totalMerma * 100.0) / (stockActual + totalMerma) : 0;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("producto", p);
            item.put("totalMermas", mermas.size());
            item.put("totalCantidad", totalMerma);
            item.put("costoTotal", costoTotal);
            item.put("porcentajePerdida", BigDecimal.valueOf(porcentajePerdida).setScale(2, RoundingMode.HALF_UP));
            resultado.add(item);
        }
        resultado.sort((a, b) -> Integer.compare((int) b.get("totalCantidad"), (int) a.get("totalCantidad")));
        return resultado;
    }

    // --- Alertas ---

    public List<Producto> alertasProximoVencer(int dias) {
        Long tid = tenantId();
        if (tid == null) return List.of();
        LocalDate limite = LocalDate.now().plusDays(dias);
        return productoRepository.findByTenantId(tid).stream()
                .filter(p -> p.getFechaVencimiento() != null && !p.getFechaVencimiento().isAfter(limite)
                        && p.getStockActual() != null && p.getStockActual() > 0)
                .toList();
    }

    public List<Map<String, Object>> alertasExcesoMermasPorLote(int umbral) {
        Long tid = tenantId();
        if (tid == null) return List.of();
        List<Merma> todas = mermaRepository.findByTenantIdOrderByFechaRegistroDesc(tid);
        Map<String, List<Merma>> porLote = todas.stream()
                .filter(m -> m.getLote() != null && !m.getLote().isBlank())
                .collect(Collectors.groupingBy(Merma::getLote));

        List<Map<String, Object>> alertas = new ArrayList<>();
        for (Map.Entry<String, List<Merma>> entry : porLote.entrySet()) {
            int total = entry.getValue().stream().mapToInt(m -> m.getCantidad() != null ? m.getCantidad() : 0).sum();
            if (total >= umbral) {
                Map<String, Object> a = new HashMap<>();
                a.put("lote", entry.getKey());
                a.put("totalMerma", total);
                a.put("registros", entry.getValue().size());
                alertas.add(a);
            }
        }
        return alertas;
    }

    public Map<String, Object> resumenMermas() {
        Long tid = tenantId();
        if (tid == null) return Map.of();
        List<Merma> todas = mermaRepository.findByTenantIdOrderByFechaRegistroDesc(tid);
        int totalRegistros = todas.size();
        int totalUnidades = todas.stream().mapToInt(m -> m.getCantidad() != null ? m.getCantidad() : 0).sum();
        BigDecimal costoTotal = todas.stream()
                .map(m -> m.getCostoEstimado() != null ? m.getCostoEstimado() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long proximosVencer = alertasProximoVencer(30).size();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalRegistros", totalRegistros);
        r.put("totalUnidades", totalUnidades);
        r.put("costoTotal", costoTotal);
        r.put("proximosVencer", proximosVencer);
        r.put("excesoLotes", alertasExcesoMermasPorLote(10).size());
        return r;
    }
}
