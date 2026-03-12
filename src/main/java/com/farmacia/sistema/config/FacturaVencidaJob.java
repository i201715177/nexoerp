package com.farmacia.sistema.config;

import com.farmacia.sistema.domain.empresa.FacturaSaaSService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FacturaVencidaJob {

    private static final Logger log = LoggerFactory.getLogger(FacturaVencidaJob.class);

    private final FacturaSaaSService facturaSaaSService;

    public FacturaVencidaJob(FacturaSaaSService facturaSaaSService) {
        this.facturaSaaSService = facturaSaaSService;
    }

    /** Cada día a la 01:00, marca como VENCIDA las facturas PENDIENTE con fecha de vencimiento pasada. */
    @Scheduled(cron = "0 0 1 * * ?")
    public void marcarFacturasVencidas() {
        int n = facturaSaaSService.marcarVencidas();
        if (n > 0) {
            log.info("FacturaVencidaJob: {} factura(s) marcada(s) como vencida(s).", n);
        }
    }
}
