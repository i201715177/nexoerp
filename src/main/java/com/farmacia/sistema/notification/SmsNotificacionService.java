package com.farmacia.sistema.notification;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsNotificacionService {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificacionService.class);

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final boolean habilitado;

    public SmsNotificacionService(@Value("${twilio.account-sid:}") String accountSid,
                                  @Value("${twilio.auth-token:}") String authToken,
                                  @Value("${twilio.from-number:}") String fromNumber) {
        this.accountSid = accountSid != null ? accountSid.trim() : "";
        this.authToken = authToken != null ? authToken.trim() : "";
        this.fromNumber = fromNumber != null ? fromNumber.trim() : "";
        this.habilitado = !this.accountSid.isEmpty() && !this.authToken.isEmpty() && !this.fromNumber.isEmpty();
        if (!this.habilitado) {
            log.info("SmsNotificacionService: Twilio no configurado (accountSid/fromNumber vacíos). Los SMS de recordatorio se omitirán.");
        } else {
            Twilio.init(this.accountSid, this.authToken);
        }
    }

    public void enviarSms(String numeroDestino, String texto) {
        if (!habilitado) {
            log.debug("SMS omitido: servicio Twilio no configurado. Destino={}, texto={}", numeroDestino, texto);
            return;
        }
        if (numeroDestino == null || numeroDestino.isBlank()) {
            log.debug("SMS omitido: número de destino vacío.");
            return;
        }
        try {
            Message.creator(
                    new PhoneNumber(numeroDestino.trim()),
                    new PhoneNumber(fromNumber),
                    texto
            ).create();
            log.info("SMS de recordatorio enviado a {}", numeroDestino);
        } catch (Exception e) {
            log.warn("No se pudo enviar SMS a {}: {}", numeroDestino, e.getMessage());
        }
    }
}

