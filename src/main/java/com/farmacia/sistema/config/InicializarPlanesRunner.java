package com.farmacia.sistema.config;

import com.farmacia.sistema.domain.empresa.PlanSuscripcion;
import com.farmacia.sistema.domain.empresa.PlanSuscripcionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(0)
public class InicializarPlanesRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializarPlanesRunner.class);

    private final PlanSuscripcionRepository planSuscripcionRepository;

    public InicializarPlanesRunner(PlanSuscripcionRepository planSuscripcionRepository) {
        this.planSuscripcionRepository = planSuscripcionRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (planSuscripcionRepository.findByCodigo("BASIC").isEmpty()) {
            PlanSuscripcion basic = new PlanSuscripcion();
            basic.setCodigo("BASIC");
            basic.setNombre("Básico");
            basic.setDescripcion("Hasta 3 usuarios");
            basic.setPrecioMensual(new BigDecimal("29.00"));
            basic.setPrecioAnual(new BigDecimal("290.00"));
            basic.setMaxUsuarios(3);
            basic.setActivo(true);
            planSuscripcionRepository.save(basic);
            log.info("Plan BASIC creado.");
        }
        if (planSuscripcionRepository.findByCodigo("PRO").isEmpty()) {
            PlanSuscripcion pro = new PlanSuscripcion();
            pro.setCodigo("PRO");
            pro.setNombre("Profesional");
            pro.setDescripcion("Hasta 15 usuarios");
            pro.setPrecioMensual(new BigDecimal("79.00"));
            pro.setPrecioAnual(new BigDecimal("790.00"));
            pro.setMaxUsuarios(15);
            pro.setActivo(true);
            planSuscripcionRepository.save(pro);
            log.info("Plan PRO creado.");
        }
    }
}
