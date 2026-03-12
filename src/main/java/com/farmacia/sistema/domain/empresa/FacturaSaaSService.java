package com.farmacia.sistema.domain.empresa;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FacturaSaaSService {

    private final FacturaSaaSRepository repository;
    private final EmpresaService empresaService;
    private final PlanSuscripcionService planSuscripcionService;

    public FacturaSaaSService(FacturaSaaSRepository repository,
                              EmpresaService empresaService,
                              PlanSuscripcionService planSuscripcionService) {
        this.repository = repository;
        this.empresaService = empresaService;
        this.planSuscripcionService = planSuscripcionService;
    }

    public List<FacturaSaaS> listarTodas() {
        return repository.findAllWithRelations();
    }

    public List<FacturaSaaS> listarPorEmpresa(Long empresaId) {
        return repository.findByEmpresaIdWithRelations(empresaId);
    }

    public FacturaSaaS obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Factura no encontrada"));
    }

    public FacturaSaaS obtenerPorIdConEmpresaYPlan(Long id) {
        return repository.findByIdWithRelations(id)
                .orElseThrow(() -> new EntityNotFoundException("Factura no encontrada"));
    }

    /**
     * Genera una factura para el siguiente periodo de la empresa.
     * Si ya tiene facturas, usa el periodo_hasta de la última + 1 día.
     * Si no tiene, usa hoy como inicio y calcula hasta según tipo (MENSUAL/ANUAL).
     */
    public FacturaSaaS generarSiguientePeriodo(Long empresaId) {
        Empresa empresa = empresaService.obtenerPorId(empresaId);
        if (empresa.getPlan() == null) {
            throw new IllegalArgumentException("La empresa no tiene plan asignado. Asigne un plan antes de generar facturas.");
        }
        PlanSuscripcion plan = empresa.getPlan();
        TipoSuscripcion tipo = empresa.getTipoSuscripcion() != null
                ? empresa.getTipoSuscripcion()
                : TipoSuscripcion.MENSUAL;

        LocalDate periodoDesde;
        LocalDate periodoHasta;
        BigDecimal monto;

        List<FacturaSaaS> anteriores = repository.findByEmpresaIdOrderByPeriodoDesdeDesc(empresaId);
        if (!anteriores.isEmpty()) {
            FacturaSaaS ultima = anteriores.get(0);
            periodoDesde = ultima.getPeriodoHasta().plusDays(1);
            if (tipo == TipoSuscripcion.ANUAL) {
                periodoHasta = periodoDesde.plusYears(1).minusDays(1);
                monto = plan.getPrecioAnual();
            } else {
                periodoHasta = periodoDesde.plusMonths(1).minusDays(1);
                monto = plan.getPrecioMensual();
            }
        } else {
            periodoDesde = LocalDate.now();
            if (tipo == TipoSuscripcion.ANUAL) {
                periodoHasta = periodoDesde.plusYears(1).minusDays(1);
                monto = plan.getPrecioAnual();
            } else {
                periodoHasta = periodoDesde.plusMonths(1).minusDays(1);
                monto = plan.getPrecioMensual();
            }
        }

        FacturaSaaS f = new FacturaSaaS();
        f.setEmpresa(empresa);
        f.setPlan(plan);
        f.setPeriodoDesde(periodoDesde);
        f.setPeriodoHasta(periodoHasta);
        f.setMonto(monto);
        f.setEstado(EstadoFactura.PENDIENTE);
        f.setFechaEmision(LocalDate.now());
        f.setFechaVencimiento(periodoHasta.plusDays(7)); // 7 días después del fin de periodo
        f = repository.save(f);
        f.setNumeroFactura(generarNumeroFactura(f.getFechaEmision()));
        return repository.save(f);
    }

    /** Formato F-YYYY-NNNNN (secuencial por año). */
    private String generarNumeroFactura(LocalDate fechaEmision) {
        int year = fechaEmision.getYear();
        LocalDate inicio = LocalDate.of(year, 1, 1);
        LocalDate fin = LocalDate.of(year, 12, 31);
        long count = repository.countByFechaEmisionBetween(inicio, fin);
        return String.format("F-%d-%05d", year, count);
    }

    public FacturaSaaS marcarComoPagada(Long facturaId, LocalDate fechaPago) {
        FacturaSaaS f = obtenerPorIdConEmpresaYPlan(facturaId);
        if (f.getEstado() == EstadoFactura.PAGADA) {
            throw new IllegalArgumentException("La factura ya está marcada como pagada.");
        }
        if (f.getEstado() == EstadoFactura.CANCELADA) {
            throw new IllegalArgumentException("No se puede marcar como pagada una factura cancelada.");
        }
        f.setEstado(EstadoFactura.PAGADA);
        f.setFechaPago(fechaPago != null ? fechaPago : LocalDate.now());
        return repository.save(f);
    }

    public Optional<FacturaSaaS> ultimaFacturaPorEmpresa(Long empresaId) {
        List<FacturaSaaS> list = repository.findByEmpresaIdOrderByPeriodoDesdeDesc(empresaId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Marca como PAGADA la última factura PENDIENTE de la empresa (la más reciente por periodo).
     * Útil al marcar una solicitud como Convertida: si eliges la empresa, su factura pendiente pasa a Pagada.
     */
    public Optional<FacturaSaaS> marcarUltimaPendienteComoPagada(Long empresaId) {
        List<FacturaSaaS> list = repository.findByEmpresaIdOrderByPeriodoDesdeDesc(empresaId);
        for (FacturaSaaS f : list) {
            if (f.getEstado() == EstadoFactura.PENDIENTE) {
                return Optional.of(marcarComoPagada(f.getId(), LocalDate.now()));
            }
        }
        return Optional.empty();
    }

    /**
     * Cancela una factura SaaS siempre que no esté pagada.
     */
    public FacturaSaaS cancelar(Long facturaId) {
        FacturaSaaS f = obtenerPorIdConEmpresaYPlan(facturaId);
        if (f.getEstado() == EstadoFactura.PAGADA) {
            throw new IllegalArgumentException("No se puede cancelar una factura que ya está pagada.");
        }
        if (f.getEstado() == EstadoFactura.CANCELADA) {
            throw new IllegalArgumentException("La factura ya está cancelada.");
        }
        f.setEstado(EstadoFactura.CANCELADA);
        return repository.save(f);
    }

    /** Cuenta facturas PENDIENTE con fecha vencimiento ya pasada. */
    public long countPendientesVencidas() {
        return repository.findByEstadoAndFechaVencimientoBefore(EstadoFactura.PENDIENTE, LocalDate.now()).size();
    }

    /** Devuelve facturas PENDIENTE con fecha de vencimiento entre dos fechas (incluye extremos). */
    public List<FacturaSaaS> pendientesPorVencerEntre(LocalDate desde, LocalDate hasta) {
        return repository.findByEstadoAndFechaVencimientoBetween(EstadoFactura.PENDIENTE, desde, hasta);
    }

    /** Cuenta facturas PENDIENTE que vencen en los próximos días (incluye hoy). */
    public long countPendientesPorVencer(int dias) {
        LocalDate hasta = LocalDate.now().plusDays(dias);
        List<FacturaSaaS> todas = repository.findAllByOrderByFechaEmisionDesc();
        return todas.stream()
                .filter(f -> f.getEstado() == EstadoFactura.PENDIENTE
                        && f.getFechaVencimiento() != null
                        && !f.getFechaVencimiento().isBefore(LocalDate.now())
                        && !f.getFechaVencimiento().isAfter(hasta))
                .count();
    }

    /** Marca como VENCIDA las facturas PENDIENTE con fechaVencimiento &lt; hoy. Usado por el job programado. */
    public int marcarVencidas() {
        List<FacturaSaaS> list = repository.findByEstadoAndFechaVencimientoBefore(EstadoFactura.PENDIENTE, LocalDate.now());
        for (FacturaSaaS f : list) {
            f.setEstado(EstadoFactura.VENCIDA);
            repository.save(f);
        }
        return list.size();
    }
}
