package com.farmacia.sistema.domain.temperatura;

import com.farmacia.sistema.domain.inventario.InventarioService;
import com.farmacia.sistema.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@Transactional
public class TemperaturaService {

    private static final String ORIGEN_MANUAL = "MANUAL";
    private static final String ORIGEN_SENSOR = "SENSOR";
    private static final String USUARIO_SENSOR = "SENSOR";

    private final RegistroTemperaturaRepository registroRepo;
    private final ZonaAlmacenRepository zonaRepo;
    private final ConfiguracionTemperaturaApiRepository configApiRepo;
    private final InventarioService inventarioService;
    private final SecureRandom random = new SecureRandom();

    public TemperaturaService(RegistroTemperaturaRepository registroRepo, ZonaAlmacenRepository zonaRepo,
                              ConfiguracionTemperaturaApiRepository configApiRepo,
                              InventarioService inventarioService) {
        this.registroRepo = registroRepo;
        this.zonaRepo = zonaRepo;
        this.configApiRepo = configApiRepo;
        this.inventarioService = inventarioService;
    }

    // ── Zonas ──
    public List<ZonaAlmacen> listarZonas() {
        return zonaRepo.findByTenantIdOrderByNombreAsc(TenantContext.getTenantId());
    }

    public ZonaAlmacen crearZona(String nombre, Long almacenId, BigDecimal tempMin, BigDecimal tempMax,
                                  BigDecimal humMin, BigDecimal humMax, boolean refrigeracion) {
        ZonaAlmacen z = new ZonaAlmacen();
        z.setNombre(nombre);
        if (almacenId != null) {
            z.setAlmacen(inventarioService.obtenerAlmacenPorId(almacenId));
        }
        z.setTempMinima(tempMin);
        z.setTempMaxima(tempMax);
        z.setHumedadMinima(humMin);
        z.setHumedadMaxima(humMax);
        z.setRequiereRefrigeracion(refrigeracion);
        return zonaRepo.save(z);
    }

    // ── Registros ──
    public List<RegistroTemperatura> listarRegistros() {
        return registroRepo.findByTenantIdOrderByFechaDesc(TenantContext.getTenantId());
    }

    public List<RegistroTemperatura> alertasFueraRango() {
        return registroRepo.findByTenantIdAndFueraRangoTrueOrderByFechaDesc(TenantContext.getTenantId());
    }

    public RegistroTemperatura registrar(Long zonaId, BigDecimal temperatura, BigDecimal humedad,
                                          String observacion, String usuario) {
        ZonaAlmacen zona = zonaRepo.findById(zonaId)
                .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada"));

        RegistroTemperatura r = new RegistroTemperatura();
        r.setZona(zona);
        r.setTemperatura(temperatura);
        r.setHumedad(humedad);
        r.setFecha(LocalDateTime.now());
        r.setObservacion(observacion);
        r.setUsuarioRegistro(usuario);
        r.setOrigen(ORIGEN_MANUAL);

        boolean fuera = false;
        if (zona.getTempMinima() != null && temperatura.compareTo(zona.getTempMinima()) < 0) fuera = true;
        if (zona.getTempMaxima() != null && temperatura.compareTo(zona.getTempMaxima()) > 0) fuera = true;
        if (humedad != null && zona.getHumedadMinima() != null && humedad.compareTo(zona.getHumedadMinima()) < 0) fuera = true;
        if (humedad != null && zona.getHumedadMaxima() != null && humedad.compareTo(zona.getHumedadMaxima()) > 0) fuera = true;
        r.setFueraRango(fuera);

        return registroRepo.save(r);
    }

    /** Obtiene la clave de API del tenant; si no existe, la genera y guarda. */
    public String getOrCreateApiKey(Long tenantId) {
        return configApiRepo.findByTenantId(tenantId)
                .map(ConfiguracionTemperaturaApi::getApiKey)
                .orElseGet(() -> generarYGuardarClave(tenantId));
    }

    /** Regenera la clave de API del tenant. */
    public String regenerateApiKey(Long tenantId) {
        configApiRepo.findByTenantId(tenantId).ifPresent(configApiRepo::delete);
        return generarYGuardarClave(tenantId);
    }

    private String generarYGuardarClave(Long tenantId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String key = "nexo_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        ConfiguracionTemperaturaApi c = new ConfiguracionTemperaturaApi();
        c.setTenantId(tenantId);
        c.setApiKey(key);
        c.setFechaGeneracion(LocalDateTime.now());
        configApiRepo.save(c);
        return key;
    }

    /** Valida la API key y devuelve el tenantId si es correcta; null si no. */
    public Long validarApiKeyYObtenerTenantId(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) return null;
        return configApiRepo.findByApiKey(apiKey.trim())
                .map(ConfiguracionTemperaturaApi::getTenantId)
                .orElse(null);
    }

    /** Registra una lectura enviada por sensor/API (origen SENSOR). */
    public RegistroTemperatura registrarDesdeSensor(Long tenantId, Long zonaId, BigDecimal temperatura,
                                                     BigDecimal humedad, String observacion) {
        ZonaAlmacen zona = zonaRepo.findById(zonaId)
                .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada"));
        if (!zona.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("La zona no pertenece a este tenant");
        }
        Long prev = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            RegistroTemperatura r = new RegistroTemperatura();
            r.setZona(zona);
            r.setTemperatura(temperatura);
            r.setHumedad(humedad);
            r.setFecha(LocalDateTime.now());
            r.setObservacion(observacion);
            r.setUsuarioRegistro(USUARIO_SENSOR);
            r.setOrigen(ORIGEN_SENSOR);

            boolean fuera = false;
            if (zona.getTempMinima() != null && temperatura.compareTo(zona.getTempMinima()) < 0) fuera = true;
            if (zona.getTempMaxima() != null && temperatura.compareTo(zona.getTempMaxima()) > 0) fuera = true;
            if (humedad != null && zona.getHumedadMinima() != null && humedad.compareTo(zona.getHumedadMinima()) < 0) fuera = true;
            if (humedad != null && zona.getHumedadMaxima() != null && humedad.compareTo(zona.getHumedadMaxima()) > 0) fuera = true;
            r.setFueraRango(fuera);

            return registroRepo.save(r);
        } finally {
            TenantContext.setTenantId(prev);
        }
    }
}
