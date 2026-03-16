package com.farmacia.sistema.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@EnableScheduling
public class BackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);
    private static final String BACKUP_DIR = "backups";
    private static final int MAX_BACKUPS = 30;

    private final DataSource dataSource;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    public BackupScheduler(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void backupAutomatico() {
        ejecutarBackup();
    }

    public String ejecutarBackup() {
        try {
            Path backupPath = Paths.get(BACKUP_DIR);
            Files.createDirectories(backupPath);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "nexoerp_backup_" + timestamp + ".sql";
            Path filePath = backupPath.resolve(fileName);

            if (datasourceUrl.contains("h2")) {
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("SCRIPT TO '" + filePath.toAbsolutePath().toString().replace("\\", "/") + "'");
                }
            } else {
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("SCRIPT TO '" + filePath.toAbsolutePath().toString().replace("\\", "/") + "'");
                }
            }

            limpiarBackupsAntiguos(backupPath);

            log.info("Backup completado: {}", filePath.toAbsolutePath());
            return filePath.toAbsolutePath().toString();

        } catch (Exception e) {
            log.error("Error al crear backup: {}", e.getMessage(), e);
            return null;
        }
    }

    private void limpiarBackupsAntiguos(Path backupPath) {
        try {
            File[] backups = backupPath.toFile().listFiles((dir, name) -> name.startsWith("nexoerp_backup_") && name.endsWith(".sql"));
            if (backups != null && backups.length > MAX_BACKUPS) {
                java.util.Arrays.sort(backups, java.util.Comparator.comparingLong(File::lastModified));
                for (int i = 0; i < backups.length - MAX_BACKUPS; i++) {
                    if (backups[i].delete()) {
                        log.info("Backup antiguo eliminado: {}", backups[i].getName());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("No se pudieron limpiar backups antiguos: {}", e.getMessage());
        }
    }
}
