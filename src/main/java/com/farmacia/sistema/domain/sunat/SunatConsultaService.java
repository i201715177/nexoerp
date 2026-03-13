package com.farmacia.sistema.domain.sunat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SunatConsultaService {

    private static final Logger log = LoggerFactory.getLogger(SunatConsultaService.class);

    private final String apiToken;
    private final String apiBaseUrl;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public SunatConsultaService(
            @Value("${app.sunat.api.token:}") String apiToken,
            @Value("${app.sunat.api.url:https://api.decolecta.com/v1}") String apiBaseUrl) {
        this.apiToken = apiToken;
        this.apiBaseUrl = apiBaseUrl;
    }

    /**
     * Consulta RUC en SUNAT vía DECOLECTA API.
     * Endpoint: GET /sunat/ruc?numero=XXXXXXXXXXX
     */
    public Map<String, String> consultarRuc(String ruc) {
        Map<String, String> resultado = new LinkedHashMap<>();
        if (ruc == null || !ruc.matches("\\d{11}")) {
            resultado.put("error", "El RUC debe tener exactamente 11 dígitos.");
            return resultado;
        }
        if (apiToken == null || apiToken.isBlank()) {
            resultado.put("error", "Token de API SUNAT no configurado. Configure app.sunat.api.token o la variable de entorno SUNAT_API_TOKEN.");
            return resultado;
        }
        try {
            String url = apiBaseUrl + "/sunat/ruc?numero=" + ruc;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = mapper.readTree(response.body());
                resultado.put("tipoDocumento", "RUC");
                resultado.put("numeroDocumento", ruc);
                resultado.put("razonSocial", txt(json, "razon_social"));
                resultado.put("direccion", txt(json, "direccion"));
                resultado.put("estado", txt(json, "estado"));
                resultado.put("condicion", txt(json, "condicion"));
                resultado.put("departamento", txt(json, "departamento"));
                resultado.put("provincia", txt(json, "provincia"));
                resultado.put("distrito", txt(json, "distrito"));
            } else {
                log.warn("DECOLECTA SUNAT respondió {}: {}", response.statusCode(), response.body());
                resultado.put("error", "No se encontró información para el RUC " + ruc + ".");
            }
        } catch (Exception e) {
            log.error("Error consultando SUNAT para RUC {}: {}", ruc, e.getMessage());
            resultado.put("error", "Error al consultar SUNAT. Intente de nuevo en unos segundos.");
        }
        return resultado;
    }

    /**
     * Consulta DNI en RENIEC vía DECOLECTA API.
     * Endpoint: GET /reniec/dni?numero=XXXXXXXX
     * Nota: este servicio puede tener restricciones por normativa de datos personales.
     */
    public Map<String, String> consultarDni(String dni) {
        Map<String, String> resultado = new LinkedHashMap<>();
        if (dni == null || !dni.matches("\\d{8}")) {
            resultado.put("error", "El DNI debe tener exactamente 8 dígitos.");
            return resultado;
        }
        if (apiToken == null || apiToken.isBlank()) {
            resultado.put("error", "Token de API no configurado. Configure app.sunat.api.token o la variable de entorno SUNAT_API_TOKEN.");
            return resultado;
        }
        try {
            String url = apiBaseUrl + "/reniec/dni?numero=" + dni;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = mapper.readTree(response.body());
                String nombres = txt(json, "nombres");
                String apPaterno = txt(json, "apellido_paterno", "apellidoPaterno");
                String apMaterno = txt(json, "apellido_materno", "apellidoMaterno");
                String nombreCompleto = (apPaterno + " " + apMaterno + " " + nombres).trim();

                resultado.put("tipoDocumento", "DNI");
                resultado.put("numeroDocumento", dni);
                resultado.put("nombres", nombres);
                resultado.put("apellidoPaterno", apPaterno);
                resultado.put("apellidoMaterno", apMaterno);
                resultado.put("razonSocial", nombreCompleto);
            } else {
                log.warn("DECOLECTA RENIEC respondió {}: {}", response.statusCode(), response.body());
                resultado.put("error", "No se encontró información para el DNI " + dni + ". El servicio RENIEC puede tener restricciones.");
            }
        } catch (Exception e) {
            log.error("Error consultando RENIEC para DNI {}: {}", dni, e.getMessage());
            resultado.put("error", "Error al consultar RENIEC. Intente de nuevo en unos segundos.");
        }
        return resultado;
    }

    private String txt(JsonNode json, String... campos) {
        for (String campo : campos) {
            if (json.has(campo) && !json.get(campo).isNull()) {
                String valor = json.get(campo).asText("").trim();
                if (!valor.isEmpty()) return valor;
            }
        }
        return "";
    }
}
