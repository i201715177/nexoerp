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
    private final String dniApiToken;
    private final String dniApiUrl;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public SunatConsultaService(
            @Value("${app.sunat.api.token:}") String apiToken,
            @Value("${app.sunat.api.url:https://api.decolecta.com/v1}") String apiBaseUrl,
            @Value("${app.dni.api.token:}") String dniApiToken,
            @Value("${app.dni.api.url:}") String dniApiUrl) {
        this.apiToken = apiToken;
        this.apiBaseUrl = apiBaseUrl;
        this.dniApiToken = dniApiToken;
        this.dniApiUrl = dniApiUrl;
    }

    public Map<String, String> consultarRuc(String ruc) {
        Map<String, String> resultado = new LinkedHashMap<>();
        if (ruc == null || !ruc.matches("\\d{11}")) {
            resultado.put("error", "El RUC debe tener exactamente 11 dígitos.");
            return resultado;
        }
        if (apiToken == null || apiToken.isBlank()) {
            resultado.put("error", "Token de API SUNAT no configurado.");
            return resultado;
        }
        try {
            String url = apiBaseUrl + "/sunat/ruc/full?numero=" + ruc;
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
                String razonSocial = txt(json, "razon_social");
                String direccion = txt(json, "direccion");
                String distrito = txt(json, "distrito");
                String provincia = txt(json, "provincia");
                String departamento = txt(json, "departamento");

                StringBuilder ubicacion = new StringBuilder();
                if (!distrito.isEmpty()) ubicacion.append(distrito);
                if (!provincia.isEmpty()) {
                    if (ubicacion.length() > 0) ubicacion.append(" - ");
                    ubicacion.append(provincia);
                }
                if (!departamento.isEmpty()) {
                    if (ubicacion.length() > 0) ubicacion.append(" - ");
                    ubicacion.append(departamento);
                }
                String direccionCompleta = direccion;
                if (ubicacion.length() > 0 && !direccionCompleta.isEmpty()) {
                    direccionCompleta = direccionCompleta.trim() + ", " + ubicacion;
                } else if (ubicacion.length() > 0) {
                    direccionCompleta = ubicacion.toString();
                }

                resultado.put("tipoDocumento", "RUC");
                resultado.put("numeroDocumento", ruc);
                resultado.put("razonSocial", razonSocial);
                resultado.put("direccion", direccionCompleta.trim());
                resultado.put("estado", txt(json, "estado"));
                resultado.put("condicion", txt(json, "condicion"));
                resultado.put("departamento", departamento);
                resultado.put("provincia", provincia);
                resultado.put("distrito", distrito);
                resultado.put("ubigeo", txt(json, "ubigeo"));
                resultado.put("tipo", txt(json, "tipo"));
                resultado.put("actividadEconomica", txt(json, "actividad_economica"));
                resultado.put("numeroTrabajadores", txt(json, "numero_trabajadores"));
                resultado.put("esAgenteRetencion", txt(json, "es_agente_retencion"));
                resultado.put("esBuenContribuyente", txt(json, "es_buen_contribuyente"));
            } else {
                log.warn("SUNAT RUC respondió {}: {}", response.statusCode(), response.body());
                resultado.put("error", "No se encontró información para el RUC " + ruc + ".");
            }
        } catch (Exception e) {
            log.error("Error consultando SUNAT para RUC {}: {}", ruc, e.getMessage());
            resultado.put("error", "Error al consultar SUNAT. Intente de nuevo.");
        }
        return resultado;
    }

    public Map<String, String> consultarDni(String dni) {
        Map<String, String> resultado = new LinkedHashMap<>();
        if (dni == null || !dni.matches("\\d{8}")) {
            resultado.put("error", "El DNI debe tener exactamente 8 dígitos.");
            return resultado;
        }

        // Proveedor 1: Decolecta (puede estar descontinuado para DNI)
        resultado = consultarDniDecolecta(dni);
        if (!resultado.containsKey("error")) {
            return resultado;
        }

        // Proveedor 2: API alternativa configurada (consultasperu, apiperu, etc.)
        if (dniApiToken != null && !dniApiToken.isBlank() && dniApiUrl != null && !dniApiUrl.isBlank()) {
            log.info("Decolecta DNI falló, intentando proveedor alternativo...");
            resultado = consultarDniAlternativo(dni);
            if (!resultado.containsKey("error")) {
                return resultado;
            }
        }

        // Si ninguno funciona, devolver error descriptivo
        resultado.clear();
        resultado.put("error",
                "El servicio de consulta DNI (RENIEC) no está disponible actualmente. " +
                "La API fue descontinuada por normativa de protección de datos. " +
                "Ingrese los datos del proveedor manualmente.");
        return resultado;
    }

    private Map<String, String> consultarDniDecolecta(String dni) {
        Map<String, String> resultado = new LinkedHashMap<>();
        if (apiToken == null || apiToken.isBlank()) {
            resultado.put("error", "Token no configurado.");
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
            log.debug("Decolecta DNI respondió {}: {}", response.statusCode(), response.body());

            if (response.statusCode() == 200) {
                JsonNode json = mapper.readTree(response.body());
                return parsearDniResponse(json, dni);
            } else {
                log.warn("Decolecta DNI respondió {}: {}", response.statusCode(), response.body());
                resultado.put("error", "Decolecta DNI no disponible (código " + response.statusCode() + ").");
            }
        } catch (Exception e) {
            log.error("Error consultando Decolecta DNI {}: {}", dni, e.getMessage());
            resultado.put("error", "Error de conexión con Decolecta.");
        }
        return resultado;
    }

    private Map<String, String> consultarDniAlternativo(String dni) {
        Map<String, String> resultado = new LinkedHashMap<>();
        try {
            String bodyJson = mapper.writeValueAsString(Map.of("dni", dni));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(dniApiUrl))
                    .header("Authorization", "Bearer " + dniApiToken)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("API DNI alternativa respondió {}: {}", response.statusCode(), response.body());

            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode json = root.has("data") ? root.get("data") : root;
                return parsearDniResponse(json, dni);
            } else {
                log.warn("API DNI alternativa respondió {}: {}", response.statusCode(), response.body());
                resultado.put("error", "API alternativa DNI no disponible.");
            }
        } catch (Exception e) {
            log.error("Error consultando API alternativa DNI {}: {}", dni, e.getMessage());
            resultado.put("error", "Error de conexión con API alternativa.");
        }
        return resultado;
    }

    private Map<String, String> parsearDniResponse(JsonNode json, String dni) {
        Map<String, String> resultado = new LinkedHashMap<>();

        String nombres = txt(json, "nombres");
        String apPaterno = txt(json, "apellido_paterno", "apellidoPaterno", "surname");
        String apMaterno = txt(json, "apellido_materno", "apellidoMaterno");
        String nombreCompleto = txt(json, "nombre_completo", "nombre", "full_name", "name");

        if (nombreCompleto.isEmpty()) {
            nombreCompleto = (apPaterno + " " + apMaterno + " " + nombres).trim();
        }

        if (nombreCompleto.isEmpty()) {
            resultado.put("error", "No se encontraron datos para el DNI " + dni + ".");
            return resultado;
        }

        String direccion = txt(json, "direccion", "address");
        String departamento = txt(json, "departamento", "department");
        String provincia = txt(json, "provincia", "province");
        String distrito = txt(json, "distrito", "district");

        StringBuilder ubicacion = new StringBuilder();
        if (!distrito.isEmpty()) ubicacion.append(distrito);
        if (!provincia.isEmpty()) {
            if (ubicacion.length() > 0) ubicacion.append(" - ");
            ubicacion.append(provincia);
        }
        if (!departamento.isEmpty()) {
            if (ubicacion.length() > 0) ubicacion.append(" - ");
            ubicacion.append(departamento);
        }
        if (ubicacion.length() > 0 && !direccion.isEmpty()) {
            direccion = direccion.trim() + ", " + ubicacion;
        } else if (ubicacion.length() > 0) {
            direccion = ubicacion.toString();
        }

        resultado.put("tipoDocumento", "DNI");
        resultado.put("numeroDocumento", dni);
        resultado.put("nombres", nombres);
        resultado.put("apellidoPaterno", apPaterno);
        resultado.put("apellidoMaterno", apMaterno);
        resultado.put("razonSocial", nombreCompleto);
        resultado.put("direccion", direccion);
        resultado.put("departamento", departamento);
        resultado.put("provincia", provincia);
        resultado.put("distrito", distrito);
        resultado.put("ubigeo", txt(json, "ubigeo"));

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
