package com.farmacia.sistema.notification;

import com.farmacia.sistema.domain.empresa.SolicitudSuscripcion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificacionSolicitudService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionSolicitudService.class);

    private final JavaMailSender mailSender;
    private final String emailDestino;
    private final String mailFrom;

    public NotificacionSolicitudService(JavaMailSender mailSender,
                                        @Value("${app.notification.solicitud-email:}") String emailDestino,
                                        @Value("${spring.mail.username:}") String mailFrom) {
        this.mailSender = mailSender;
        this.emailDestino = emailDestino == null ? "" : emailDestino.trim();
        this.mailFrom = mailFrom == null ? "" : mailFrom.trim();
    }

    /**
     * Envía un correo al administrador cuando se registra una nueva solicitud de suscripción.
     * Si el correo no está configurado (MAIL_USERNAME/MAIL_PASSWORD vacíos) no hace nada y solo registra en log.
     */
    public void notificarNuevaSolicitud(SolicitudSuscripcion s) {
        if (emailDestino.isEmpty()) {
            log.debug("Notificación de solicitud omitida: no hay correo configurado (NOTIFICATION_EMAIL)");
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            if (!mailFrom.isEmpty()) {
                msg.setFrom(mailFrom);
            }
            msg.setTo(emailDestino);
            msg.setSubject("[NexoERP] Nueva solicitud de suscripción: " + s.getNombreEmpresa());
            msg.setText(buildCuerpo(s));
            mailSender.send(msg);
            log.info("Notificación de solicitud enviada a {}", emailDestino);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de notificación. Error completo:", e);
        }
    }

    private String buildCuerpo(SolicitudSuscripcion s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Se ha recibido una nueva solicitud de suscripción en NexoERP.\n\n");
        sb.append("Contacto: ").append(s.getNombreContacto()).append("\n");
        sb.append("Correo: ").append(s.getEmail()).append("\n");
        if (s.getTelefono() != null && !s.getTelefono().isBlank()) {
            sb.append("Teléfono: ").append(s.getTelefono()).append("\n");
        }
        sb.append("Empresa: ").append(s.getNombreEmpresa()).append("\n");
        if (s.getTipoDocumento() != null && s.getNumeroDocumento() != null && !s.getNumeroDocumento().isBlank()) {
            sb.append("Documento: ").append(s.getTipoDocumento()).append(" ").append(s.getNumeroDocumento()).append("\n");
        }
        if (s.getPlanDeseado() != null && !s.getPlanDeseado().isBlank()) {
            sb.append("Plan de interés: ").append(s.getPlanDeseado()).append("\n");
        }
        if (s.getMensaje() != null && !s.getMensaje().isBlank()) {
            sb.append("Mensaje: ").append(s.getMensaje()).append("\n");
        }
        sb.append("\nEntra al panel Admin → Solicitudes para gestionarla.");
        return sb.toString();
    }
}
