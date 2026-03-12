package com.farmacia.sistema.domain.venta;

import com.farmacia.sistema.domain.inventario.InventarioService;
import com.farmacia.sistema.domain.producto.Producto;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class NotaCreditoService {

    private final NotaCreditoRepository notaCreditoRepository;
    private final VentaRepository ventaRepository;
    private final InventarioService inventarioService;

    public NotaCreditoService(NotaCreditoRepository notaCreditoRepository,
                              VentaRepository ventaRepository,
                              InventarioService inventarioService) {
        this.notaCreditoRepository = notaCreditoRepository;
        this.ventaRepository = ventaRepository;
        this.inventarioService = inventarioService;
    }

    public List<NotaCredito> listarPorVenta(Long ventaId) {
        return notaCreditoRepository.findByVentaIdOrderByFechaDesc(ventaId);
    }

    public NotaCredito obtenerPorId(Long id) {
        return notaCreditoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nota de crédito no encontrada"));
    }

    public NotaCredito emitir(Long ventaId, String motivo, BigDecimal montoTotal, boolean devolverStock) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada"));

        BigDecimal total = montoTotal != null && montoTotal.compareTo(BigDecimal.ZERO) > 0
                ? montoTotal
                : venta.getTotal();

        String numero = generarNumeroNC();

        NotaCredito nc = new NotaCredito();
        nc.setVenta(venta);
        nc.setNumero(numero);
        nc.setFecha(LocalDateTime.now());
        nc.setTotal(total);
        nc.setMotivo(motivo != null ? motivo : "Devolución");
        nc.setEstado("EMITIDA");

        nc = notaCreditoRepository.save(nc);

        if (devolverStock && venta.getItems() != null) {
            for (VentaItem item : venta.getItems()) {
                Producto p = item.getProducto();
                if (p != null) {
                    int cant = item.getCantidad();
                    p.setStockActual(p.getStockActual() + cant);
                    inventarioService.registrarEntrada(p, cant, "NC " + numero);
                    inventarioService.actualizarStockPrincipal(p.getId(), cant);
                }
            }
        }

        return nc;
    }

    private String generarNumeroNC() {
        long count = notaCreditoRepository.count();
        return "NC-" + String.format("%05d", count + 1);
    }
}
