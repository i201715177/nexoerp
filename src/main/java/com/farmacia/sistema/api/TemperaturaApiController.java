package com.farmacia.sistema.api;

import com.farmacia.sistema.domain.temperatura.RegistroTemperatura;
import com.farmacia.sistema.domain.temperatura.TemperaturaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * API para recibir lecturas de temperatura desde sensores/gateway.
 * Autenticación por cabecera X-API-Key (clave generada en Temperatura → Integración sensores).
 */
@RestController
@RequestMapping("/api/temperatura")
@CrossOrigin(origins = "*")
public class TemperaturaApiController {

    private static final String HEADER_API_KEY = "X-API-Key";

    private final TemperaturaService temperaturaService;

    public TemperaturaApiController(TemperaturaService temperaturaService) {
        this.temperaturaService = temperaturaService;
    }

    /**
     * Registra una lectura enviada por sensor o gateway.
     * Cabecera obligatoria: X-API-Key: &lt;clave del tenant&gt;
     * Body JSON: { "zonaId": number, "temperatura": number, "humedad": number (opcional), "observacion": string (opcional) }
     */
    @PostMapping(value = "/lectura", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registrarLectura(
            @RequestHeader(value = HEADER_API_KEY, required = false) String apiKey,
            @RequestBody Map<String, Object> body) {

        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Falta cabecera X-API-Key"));
        }

        Long tenantId = temperaturaService.validarApiKeyYObtenerTenantId(apiKey.trim());
        if (tenantId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "API Key inválida"));
        }

        Long zonaId = numberFrom(body, "zonaId");
        BigDecimal temperatura = decimalFrom(body, "temperatura");
        if (zonaId == null || temperatura == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "zonaId y temperatura son obligatorios"));
        }

        BigDecimal humedad = decimalFrom(body, "humedad");
        String observacion = body != null && body.get("observacion") != null
                ? body.get("observacion").toString() : null;

        try {
            RegistroTemperatura r = temperaturaService.registrarDesdeSensor(tenantId, zonaId, temperatura, humedad, observacion);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", r.getId(),
                    "zonaId", r.getZona().getId(),
                    "temperatura", r.getTemperatura(),
                    "humedad", r.getHumedad(),
                    "fueraRango", r.isFueraRango(),
                    "fecha", r.getFecha().toString()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    private static Long numberFrom(Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            try { return Long.parseLong((String) v); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private static BigDecimal decimalFrom(Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number) return BigDecimal.valueOf(((Number) v).doubleValue());
        if (v instanceof String) {
            try { return new BigDecimal((String) v); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}
