package com.farmacia.sistema.web;

import com.farmacia.sistema.config.BackupScheduler;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Controller
@RequestMapping("/web/sistema")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR')")
public class SystemWebController {

    private final BackupScheduler backupScheduler;
    private final CacheManager cacheManager;
    private final DataSource dataSource;
    private final HealthEndpoint healthEndpoint;

    public SystemWebController(BackupScheduler backupScheduler, CacheManager cacheManager,
                               DataSource dataSource, HealthEndpoint healthEndpoint) {
        this.backupScheduler = backupScheduler;
        this.cacheManager = cacheManager;
        this.dataSource = dataSource;
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping
    public String dashboard(Model model) {
        Runtime rt = Runtime.getRuntime();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        info.put("maxMemoryMb", rt.maxMemory() / (1024 * 1024));
        info.put("usedMemoryMb", (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024));
        info.put("freeMemoryMb", rt.freeMemory() / (1024 * 1024));
        info.put("processors", rt.availableProcessors());
        info.put("uptimeMs", java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime());

        try (Connection conn = dataSource.getConnection()) {
            info.put("database", conn.getMetaData().getDatabaseProductName() + " " + conn.getMetaData().getDatabaseProductVersion());
            info.put("dbUrl", conn.getMetaData().getURL());
        } catch (Exception e) {
            info.put("database", "Error: " + e.getMessage());
        }

        Status healthStatus = healthEndpoint.health().getStatus();
        info.put("healthStatus", healthStatus.getCode());

        model.addAttribute("sysInfo", info);
        model.addAttribute("cacheNames", cacheManager.getCacheNames());
        model.addAttribute("backups", listarBackups());
        return "sistema";
    }

    @PostMapping("/backup")
    public String crearBackup(RedirectAttributes ra) {
        String path = backupScheduler.ejecutarBackup();
        if (path != null) {
            ra.addFlashAttribute("mensajeSistema", "Backup creado exitosamente: " + path);
        } else {
            ra.addFlashAttribute("errorSistema", "Error al crear backup. Ver logs.");
        }
        return "redirect:/web/sistema";
    }

    @PostMapping("/cache/limpiar")
    public String limpiarCache(RedirectAttributes ra) {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
        ra.addFlashAttribute("mensajeSistema", "Todas las cachés han sido limpiadas.");
        return "redirect:/web/sistema";
    }

    private List<Map<String, Object>> listarBackups() {
        List<Map<String, Object>> backups = new ArrayList<>();
        try {
            Path backupPath = Paths.get("backups");
            if (Files.exists(backupPath)) {
                File[] files = backupPath.toFile().listFiles((dir, name) -> name.endsWith(".sql"));
                if (files != null) {
                    Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
                    for (File f : files) {
                        Map<String, Object> b = new LinkedHashMap<>();
                        b.put("nombre", f.getName());
                        b.put("tamanoKb", f.length() / 1024);
                        b.put("fecha", LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault()));
                        backups.add(b);
                    }
                }
            }
        } catch (Exception ignored) {}
        return backups;
    }
}
