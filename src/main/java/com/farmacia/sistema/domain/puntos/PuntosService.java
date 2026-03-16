package com.farmacia.sistema.domain.puntos;

import com.farmacia.sistema.domain.cliente.Cliente;
import com.farmacia.sistema.domain.cliente.ClienteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PuntosService {

    private final PuntoMovimientoRepository repository;
    private final ClienteService clienteService;

    /** Cada S/10 de compra = 1 punto */
    private static final BigDecimal SOLES_POR_PUNTO = BigDecimal.TEN;
    /** Cada punto vale S/0.50 al canjear */
    private static final BigDecimal VALOR_PUNTO = new BigDecimal("0.50");

    public PuntosService(PuntoMovimientoRepository repository, ClienteService clienteService) {
        this.repository = repository;
        this.clienteService = clienteService;
    }

    public List<PuntoMovimiento> historialCliente(Long clienteId) {
        return repository.findByClienteIdOrderByFechaDesc(clienteId);
    }

    public int acumular(Long clienteId, BigDecimal montoVenta, String referencia) {
        int puntosGanados = montoVenta.divideToIntegralValue(SOLES_POR_PUNTO).intValue();
        if (puntosGanados <= 0) return 0;

        Cliente c = clienteService.obtenerPorId(clienteId);

        PuntoMovimiento pm = new PuntoMovimiento();
        pm.setCliente(c);
        pm.setTipo("ACUMULACION");
        pm.setPuntos(puntosGanados);
        pm.setReferencia(referencia);
        pm.setFecha(LocalDateTime.now());
        repository.save(pm);

        c.setPuntos(c.getPuntos() + puntosGanados);
        clienteService.guardar(c);

        return puntosGanados;
    }

    public BigDecimal canjear(Long clienteId, int puntos, String referencia) {
        Cliente c = clienteService.obtenerPorId(clienteId);
        if (c.getPuntos() < puntos) {
            throw new IllegalArgumentException("El cliente no tiene suficientes puntos. Disponible: " + c.getPuntos());
        }

        PuntoMovimiento pm = new PuntoMovimiento();
        pm.setCliente(c);
        pm.setTipo("CANJE");
        pm.setPuntos(-puntos);
        pm.setReferencia(referencia);
        pm.setFecha(LocalDateTime.now());
        repository.save(pm);

        c.setPuntos(c.getPuntos() - puntos);
        clienteService.guardar(c);

        return VALOR_PUNTO.multiply(BigDecimal.valueOf(puntos));
    }

    public BigDecimal getValorPunto() { return VALOR_PUNTO; }
    public BigDecimal getSolesPorPunto() { return SOLES_POR_PUNTO; }
}
