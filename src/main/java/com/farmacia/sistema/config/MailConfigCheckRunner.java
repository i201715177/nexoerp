package com.farmacia.sistema.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Al arranque, comprueba si el correo está configurado para enviar notificaciones.
 * Si falta MAIL_PASSWORD, los correos no se enviarán (solicitudes de suscripción, recordatorios SaaS).
 */
@Component
@Order(1)
public class MailConfigCheckRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MailConfigCheckRunner.class);

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Override
    public void run(ApplicationArguments args) {
        if (mailPassword == null || mailPassword.isBlank()) {
            log.warn("========================================");
            log.warn("CORREO NO CONFIGURADO: MAIL_PASSWORD no está definido.");
            log.warn("Los correos (solicitudes de suscripción, recordatorios SaaS) NO se enviarán.");
            log.warn("========================================");
        } else if (mailUsername != null && !mailUsername.isBlank()) {
            log.info("Correo configurado: host={}, usuario={}, password={}****",
                    mailHost, mailUsername,
                    mailPassword.length() > 4 ? mailPassword.substring(0, 4) : "****");
        }
    }
}
