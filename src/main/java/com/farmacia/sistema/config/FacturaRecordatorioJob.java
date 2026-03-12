package com.farmacia.sistema.config;

import com.farmacia.sistema.domain.empresa.FacturaSaaS;
import com.farmacia.sistema.domain.empresa.FacturaSaaSService;
import com.farmacia.sistema.notification.NotificacionFacturaSaasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class FacturaRecordatorioJob {

    private static final Logger log = LoggerFactory.getLogger(FacturaRecordatorioJob.class);

    private final FacturaSaaSService facturaSaaSService;
    private final NotificacionFacturaSaasService notificacionFacturaSaasService;
    private final int diasRecordatorio;

    public FacturaRecordatorioJob(FacturaSaaSService facturaSaaSService,
                                  NotificacionFacturaSaasService notificacionFacturaSaasService,
                                  @Value("${app.saas.facturacion.dias-recordatorio:3}") int diasRecordatorio) {
        this.facturaSaaSService = facturaSaaSService;
        this.notificacionFacturaSaasService = notificacionFacturaSaasService;
        this.diasRecordatorio = diasRecordatorio;
    }

    /**
     * Cada día a las 09:00 envía recordatorios por correo de facturas PENDIENTE
     * que vencen en los próximos N días (configurable).
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void enviarRecordatoriosVencimiento() {
        LocalDate hoy = LocalDate.now();
        LocalDate hasta = hoy.plusDays(diasRecordatorio);
        List<FacturaSaaS> proximas = facturaSaaSService.pendientesPorVencerEntre(hoy, hasta);

        if (proximas.isEmpty()) {
            return;
        }

        for (FacturaSaaS f : proximas) {
            notificacionFacturaSaasService.enviarRecordatorioVencimiento(f);
        }

        log.info("FacturaRecordatorioJob: procesadas {} factura(s) para recordatorio de vencimiento (correo).", proximas.size());
    }
}

