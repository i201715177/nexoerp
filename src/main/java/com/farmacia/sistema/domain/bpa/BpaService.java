package com.farmacia.sistema.domain.bpa;

import com.farmacia.sistema.domain.inventario.InventarioService;
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
public class BpaService {

    private final ChecklistBPARepository repository;
    private final InventarioService inventarioService;

    public BpaService(ChecklistBPARepository repository, InventarioService inventarioService) {
        this.repository = repository;
        this.inventarioService = inventarioService;
    }

    public List<ChecklistBPA> listar() {
        return repository.findByTenantIdOrderByFechaDesc(TenantContext.getTenantId());
    }

    public ChecklistBPA obtenerPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Checklist no encontrado"));
    }

    private String generarCodigo() {
        long count = repository.countByTenantId(TenantContext.getTenantId()) + 1;
        return "BPA-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-" + String.format("%04d", count);
    }

    public ChecklistBPA crear(Long almacenId, String tipo, LocalDate fecha,
                               boolean piso, boolean paredes, boolean techo,
                               boolean iluminacion, boolean ventilacion, boolean ordenados,
                               boolean separacion, boolean vencidosSep, boolean extintor, boolean botiquin,
                               String observaciones, String accionCorrectiva, LocalDate proximaRevision,
                               String usuario) {
        ChecklistBPA c = new ChecklistBPA();
        c.setCodigo(generarCodigo());
        c.setFecha(fecha != null ? fecha : LocalDate.now());
        c.setTipo(tipo);
        if (almacenId != null) {
            c.setAlmacen(inventarioService.obtenerAlmacenPorId(almacenId));
        }
        c.setCondicionPiso(piso);
        c.setCondicionParedes(paredes);
        c.setCondicionTecho(techo);
        c.setIluminacionAdecuada(iluminacion);
        c.setVentilacionAdecuada(ventilacion);
        c.setProductosOrdenados(ordenados);
        c.setSeparacionPared(separacion);
        c.setProductosVencidosSeparados(vencidosSep);
        c.setExtintorVigente(extintor);
        c.setBotiquinPrimerosAuxilios(botiquin);
        c.setObservaciones(observaciones);
        c.setAccionCorrectiva(accionCorrectiva);
        c.setFechaProximaRevision(proximaRevision);
        c.setUsuarioRegistro(usuario);
        c.setFechaRegistro(LocalDateTime.now());

        boolean conforme = piso && paredes && techo && iluminacion && ventilacion
                && ordenados && separacion && vencidosSep && extintor && botiquin;
        c.setResultadoGeneral(conforme ? "CONFORME" : "NO_CONFORME");

        return repository.save(c);
    }
}
