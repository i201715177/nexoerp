package com.farmacia.sistema.notification;

import com.farmacia.sistema.domain.empresa.Empresa;
import com.farmacia.sistema.domain.empresa.FacturaSaaS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class NotificacionFacturaSaasService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionFacturaSaasService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaMailSender mailSender;
    private final String copiaAdmin;
    private final String mailFrom;

    public NotificacionFacturaSaasService(JavaMailSender mailSender,
                                          @Value("${app.notification.solicitud-email:}") String copiaAdmin,
                                          @Value("${spring.mail.username:}") String mailFrom) {
        this.mailSender = mailSender;
        this.copiaAdmin = copiaAdmin == null ? "" : copiaAdmin.trim();
        this.mailFrom = mailFrom == null ? "" : mailFrom.trim();
    }

    /**
     * Envía un correo de recordatorio de vencimiento de factura SaaS al correo de la empresa
     * (si está configurado) y opcionalmente en copia al administrador.
     */
    public void enviarRecordatorioVencimiento(FacturaSaaS f) {
        Empresa e = f.getEmpresa();
        if (e == null) {
            return;
        }

        // Prioridad: correo del dueño (emailContacto) → descripción si contiene @ → copia admin
        String emailDestino = null;
        if (e.getEmailContacto() != null && !e.getEmailContacto().isBlank() && e.getEmailContacto().contains("@")) {
            emailDestino = e.getEmailContacto().trim();
        }
        if (emailDestino == null && e.getDescripcion() != null && e.getDescripcion().contains("@")) {
            emailDestino = e.getDescripcion().trim();
        }
        if (emailDestino == null || emailDestino.isBlank()) {
            if (copiaAdmin.isEmpty()) {
                log.debug("Recordatorio SaaS omitido: empresa '{}' sin correo de dueño y sin copia admin.", e.getNombre());
                return;
            }
            emailDestino = copiaAdmin;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            if (!mailFrom.isEmpty()) {
                msg.setFrom(mailFrom);
            }
            msg.setTo(emailDestino);
            if (!copiaAdmin.isEmpty() && !copiaAdmin.equalsIgnoreCase(emailDestino)) {
                msg.setCc(copiaAdmin);
            }
            String nro = f.getNumeroFactura() != null ? f.getNumeroFactura() : "F-" + f.getId();
            msg.setSubject("[NexoERP] Recordatorio de vencimiento factura SaaS " + nro);
            msg.setText(buildCuerpo(f));
            mailSender.send(msg);
            log.info("Recordatorio de factura SaaS enviado a {}", emailDestino);
        } catch (Exception ex) {
            log.warn("No se pudo enviar recordatorio de factura SaaS a {}: {}", emailDestino, ex.getMessage());
        }
    }

    private String buildCuerpo(FacturaSaaS f) {
        Empresa e = f.getEmpresa();
        StringBuilder sb = new StringBuilder();
        sb.append("Hola");
        if (e != null && e.getNombre() != null) {
            sb.append(" ").append(e.getNombre());
        }
        sb.append(",\n\n");
        sb.append("Le recordamos que su factura de suscripción SaaS está próxima a vencer.\n\n");
        String nro = f.getNumeroFactura() != null ? f.getNumeroFactura() : "F-" + f.getId();
        sb.append("Factura: ").append(nro).append("\n");
        if (f.getPeriodoDesde() != null && f.getPeriodoHasta() != null) {
            sb.append("Periodo: ").append(f.getPeriodoDesde().format(FMT))
                    .append(" - ").append(f.getPeriodoHasta().format(FMT)).append("\n");
        }
        if (f.getFechaVencimiento() != null) {
            sb.append("Fecha de vencimiento: ").append(f.getFechaVencimiento().format(FMT)).append("\n");
        }
        if (f.getMonto() != null) {
            sb.append("Monto: S/ ").append(String.format("%,.2f", f.getMonto())).append("\n");
        }
        sb.append("\nSi ya realizó el pago, puede ignorar este mensaje.\n");
        sb.append("De lo contrario, por favor regularice el pago para mantener activo su acceso a NexoERP.\n\n");
        sb.append("Atentamente,\n");
        sb.append("Equipo NexoERP");
        return sb.toString();
    }
}

