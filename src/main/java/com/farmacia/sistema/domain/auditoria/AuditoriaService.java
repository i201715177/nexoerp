package com.farmacia.sistema.domain.auditoria;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

@Service
public class AuditoriaService {

    private final AuditoriaAccionRepository repository;

    public AuditoriaService(AuditoriaAccionRepository repository) {
        this.repository = repository;
    }

    public void registrarCreacion(String entidad, String identificador, String detalle) {
        guardar("POST", "Crear " + entidad, identificador, detalle);
    }

    public void registrarActualizacion(String entidad, String identificador, Map<String, String[]> cambios) {
        if (cambios.isEmpty()) return;
        StringJoiner sj = new StringJoiner(" | ");
        cambios.forEach((campo, vals) ->
                sj.add(campo + ": " + vals[0] + " → " + vals[1]));
        guardar("PUT", "Actualizar " + entidad, identificador, sj.toString());
    }

    public void registrarEliminacion(String entidad, String identificador, String detalle) {
        guardar("DELETE", "Eliminar " + entidad, identificador, detalle);
    }

    public void registrarAccion(String metodo, String accion, String detalle) {
        guardar(metodo, accion, "", detalle);
    }

    private void guardar(String metodo, String accion, String identificador, String detalle) {
        AuditoriaAccion a = new AuditoriaAccion();
        a.setUsuario(obtenerUsuario());
        a.setMetodo(metodo);
        a.setUrl(obtenerUrl());
        a.setIp(obtenerIp());
        a.setAccion(accion);

        String textoDetalle = identificador != null ? identificador : "";
        if (detalle != null && !detalle.isEmpty()) {
            if (!textoDetalle.isEmpty()) textoDetalle += " | ";
            textoDetalle += detalle;
        }
        if (textoDetalle.length() > 2000) textoDetalle = textoDetalle.substring(0, 2000);
        a.setDetalle(textoDetalle);
        repository.save(a);
    }

    private String obtenerUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anónimo";
    }

    private String obtenerIp() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) return attrs.getRequest().getRemoteAddr();
        } catch (Exception ignored) {}
        return "N/A";
    }

    private String obtenerUrl() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) return attrs.getRequest().getRequestURI();
        } catch (Exception ignored) {}
        return "N/A";
    }

    /**
     * Compara dos valores; si difieren, los agrega al mapa de cambios.
     */
    public static void cmp(Map<String, String[]> m, String campo, Object antes, Object despues) {
        String a = antes != null ? antes.toString().trim() : "";
        String d = despues != null ? despues.toString().trim() : "";
        if (!a.equals(d)) {
            m.put(campo, new String[]{a.isEmpty() ? "(vacío)" : a, d.isEmpty() ? "(vacío)" : d});
        }
    }

    public static Map<String, String[]> nuevoCambios() {
        return new LinkedHashMap<>();
    }
}
