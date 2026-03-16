package com.farmacia.sistema.api;

import com.farmacia.sistema.domain.empresa.SolicitudSuscripcion;
import com.farmacia.sistema.domain.empresa.SolicitudSuscripcionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final SolicitudSuscripcionService solicitudService;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public NotificacionController(SolicitudSuscripcionService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<Map<String, Object>> pendientes() {
        long count = solicitudService.contarPendientes();
        List<SolicitudSuscripcion> recientes = solicitudService.listarUltimasPendientes();

        List<Map<String, String>> items = recientes.stream().map(s -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("id", String.valueOf(s.getId()));
            m.put("empresa", s.getNombreEmpresa());
            m.put("contacto", s.getNombreContacto());
            m.put("fecha", s.getFechaSolicitud() != null ? s.getFechaSolicitud().format(FMT) : "");
            m.put("plan", s.getPlanDeseado() != null ? s.getPlanDeseado() : "");
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("count", count);
        resp.put("items", items);
        return ResponseEntity.ok(resp);
    }
}
