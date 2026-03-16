package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.sunat.SunatConsultaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sunat")
public class SunatApiController {

    private final SunatConsultaService sunatConsultaService;

    public SunatApiController(SunatConsultaService sunatConsultaService) {
        this.sunatConsultaService = sunatConsultaService;
    }

    @GetMapping("/consultar")
    public ResponseEntity<Map<String, String>> consultar(
            @RequestParam("tipo") String tipo,
            @RequestParam("numero") String numero) {

        numero = numero != null ? numero.trim().replaceAll("\\D", "") : "";

        String tipoNorm = tipo != null ? tipo.trim().toUpperCase() : "";
        // RUT se trata como RUC (11 dígitos)
        if ("RUT".equals(tipoNorm)) tipoNorm = "RUC";

        Map<String, String> resultado;
        if ("RUC".equals(tipoNorm)) {
            resultado = sunatConsultaService.consultarRuc(numero);
        } else if ("DNI".equals(tipoNorm)) {
            resultado = sunatConsultaService.consultarDni(numero);
        } else {
            resultado = Map.of("error", "Tipo de documento no soportado. Use DNI, RUC o RUT.");
        }

        if (resultado.containsKey("error")) {
            return ResponseEntity.badRequest().body(resultado);
        }
        return ResponseEntity.ok(resultado);
    }
}
