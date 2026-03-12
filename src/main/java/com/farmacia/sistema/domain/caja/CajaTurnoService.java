package com.farmacia.sistema.domain.caja;

import com.farmacia.sistema.domain.sucursal.SucursalService;
import com.farmacia.sistema.domain.venta.PagoVenta;
import com.farmacia.sistema.domain.venta.Venta;
import com.farmacia.sistema.domain.venta.VentaRepository;
import com.farmacia.sistema.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class CajaTurnoService {

    private final CajaTurnoRepository cajaTurnoRepository;
    private final VentaRepository ventaRepository;
    private final SucursalService sucursalService;

    public CajaTurnoService(CajaTurnoRepository cajaTurnoRepository,
                            VentaRepository ventaRepository,
                            SucursalService sucursalService) {
        this.cajaTurnoRepository = cajaTurnoRepository;
        this.ventaRepository = ventaRepository;
        this.sucursalService = sucursalService;
    }

    public BigDecimal getTotalVentasEnTurno(Long cajaTurnoId) {
        if (cajaTurnoId == null) return BigDecimal.ZERO;
        BigDecimal sum = ventaRepository.sumTotalByCajaTurnoId(cajaTurnoId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    /**
     * Resumen de pagos del turno agrupados por medio de pago: cantidad de operaciones y monto total por medio.
     */
    public List<Map<String, Object>> getResumenPagosPorMedio(Long cajaTurnoId) {
        if (cajaTurnoId == null) return List.of();
        List<Venta> ventas = ventaRepository.findByCajaTurnoIdWithPagos(cajaTurnoId);
        Map<String, long[]> agrupado = new LinkedHashMap<>(); // medio -> [cantidad, total*100 para evitar decimales]
        for (Venta v : ventas) {
            if (v.getPagos() == null) continue;
            for (PagoVenta p : v.getPagos()) {
                String medio = p.getMedioPago() != null ? p.getMedioPago().trim().toUpperCase() : "EFECTIVO";
                String label = labelMedio(medio);
                agrupado.putIfAbsent(label, new long[]{0, 0});
                agrupado.get(label)[0]++;
                agrupado.get(label)[1] += (p.getMonto() != null ? p.getMonto().multiply(BigDecimal.valueOf(100)).longValue() : 0);
            }
        }
        String[] orden = {"Efectivo", "Tarjeta", "Transferencia", "Yape", "Plin"};
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (String medio : orden) {
            if (!agrupado.containsKey(medio)) continue;
            long[] datos = agrupado.get(medio);
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("medio", medio);
            fila.put("cantidad", datos[0]);
            fila.put("total", BigDecimal.valueOf(datos[1]).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP));
            resultado.add(fila);
        }
        return resultado;
    }

    private static String labelMedio(String medio) {
        if (medio == null) return "Efectivo";
        return switch (medio.toUpperCase()) {
            case "TARJETA" -> "Tarjeta";
            case "TRANSFERENCIA" -> "Transferencia";
            case "YAPE" -> "Yape";
            case "PLIN" -> "Plin";
            default -> "Efectivo";
        };
    }

    public List<CajaTurno> listarTodos() {
        return listarPorSucursal(null);
    }

    /** Lista turnos visibles en el historial (excluye los ocultos). La auditoría y las ventas no se modifican al ocultar. */
    public List<CajaTurno> listarPorSucursal(Long sucursalId) {
        Long tenantId = TenantContext.getTenantId();
        if (sucursalId != null && tenantId != null) {
            return cajaTurnoRepository.findByTenantIdAndSucursalIdAndOcultoEnHistorialFalseOrderByFechaAperturaDesc(tenantId, sucursalId);
        }
        if (tenantId != null) {
            return cajaTurnoRepository.findByTenantIdAndOcultoEnHistorialFalseOrderByFechaAperturaDesc(tenantId);
        }
        return cajaTurnoRepository.findByOcultoEnHistorialFalseOrderByFechaAperturaDesc();
    }

    public CajaTurno obtenerPorId(Long id) {
        return cajaTurnoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Turno de caja no encontrado"));
    }

    public Optional<CajaTurno> obtenerCajaAbierta() {
        return obtenerCajaAbierta(null);
    }

    /** Obtiene la caja abierta. Si sucursalId != null, solo de esa sucursal. */
    public Optional<CajaTurno> obtenerCajaAbierta(Long sucursalId) {
        Long tenantId = TenantContext.getTenantId();
        if (sucursalId != null) {
            if (tenantId != null) {
                return cajaTurnoRepository.findFirstByTenantIdAndSucursalIdAndEstadoOrderByFechaAperturaDesc(tenantId, sucursalId, "ABIERTO");
            }
            return Optional.empty();
        }
        if (tenantId != null) {
            return cajaTurnoRepository.findFirstByTenantIdAndEstadoOrderByFechaAperturaDesc(tenantId, "ABIERTO");
        }
        return cajaTurnoRepository.findFirstByEstadoOrderByFechaAperturaDesc("ABIERTO");
    }

    public CajaTurno abrirCaja(BigDecimal montoInicial) {
        return abrirCaja(montoInicial, null, null);
    }

    public CajaTurno abrirCaja(BigDecimal montoInicial, Long sucursalId) {
        return abrirCaja(montoInicial, sucursalId, null);
    }

    public CajaTurno abrirCaja(BigDecimal montoInicial, Long sucursalId, String nombreVendedor) {
        var sucursal = sucursalId != null ? sucursalService.obtenerPorId(sucursalId) : sucursalService.sucursalPorDefecto();
        Long sid = sucursal != null ? sucursal.getId() : null;
        if (obtenerCajaAbierta(sid).isPresent()) {
            throw new IllegalStateException("Ya existe una caja abierta en esta sucursal. Cierre el turno actual antes de abrir otro.");
        }
        CajaTurno turno = new CajaTurno();
        turno.setFechaApertura(LocalDateTime.now());
        turno.setMontoInicial(montoInicial != null ? montoInicial : BigDecimal.ZERO);
        turno.setEstado("ABIERTO");
        turno.setSucursal(sucursal);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonimo";
        turno.setUsuario(usuario);
        String vendedor = (nombreVendedor != null && !nombreVendedor.isBlank()) ? nombreVendedor.trim() : usuario;
        turno.setNombreVendedor(vendedor);
        return cajaTurnoRepository.save(turno);
    }

    public CajaTurno cerrarCaja(Long turnoId, BigDecimal montoCierre, String observaciones) {
        CajaTurno turno = obtenerPorId(turnoId);
        if (!"ABIERTO".equals(turno.getEstado())) {
            throw new IllegalStateException("El turno no está abierto");
        }
        turno.setFechaCierre(LocalDateTime.now());
        turno.setMontoCierre(montoCierre != null ? montoCierre : turno.getMontoInicial());
        turno.setObservaciones(observaciones);
        turno.setEstado("CERRADO");
        return cajaTurnoRepository.save(turno);
    }

    /**
     * Oculta el turno del historial (solo deja de mostrarse en la lista).
     * Las ventas y la auditoría no se borran; los datos reales siguen en el sistema.
     * Solo se permite para turnos ya cerrados.
     */
    public void ocultarDelHistorial(Long turnoId) {
        CajaTurno turno = obtenerPorId(turnoId);
        if ("ABIERTO".equals(turno.getEstado())) {
            throw new IllegalStateException("Solo se puede eliminar del historial un turno ya cerrado.");
        }
        turno.setOcultoEnHistorial(true);
        cajaTurnoRepository.save(turno);
    }
}
