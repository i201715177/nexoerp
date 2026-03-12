package com.farmacia.sistema.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import javax.sql.DataSource;

/**
 * Añade la columna es_central a la tabla sucursales si no existe (H2).
 * Se ejecuta tras crear el EntityManagerFactory (y el ddl-auto de Hibernate) y antes de atender peticiones.
 */
@Component
@DependsOn("entityManagerFactory")
public class SucursalEsCentralMigration {

    private static final Logger log = LoggerFactory.getLogger(SucursalEsCentralMigration.class);

    private final DataSource dataSource;

    public SucursalEsCentralMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void addColumnIfMissing() {
        String url;
        try {
            url = dataSource.getConnection().getMetaData().getURL();
        } catch (Exception e) {
            return;
        }
        if (url == null || !url.startsWith("jdbc:h2:")) {
            return;
        }
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try {
            Integer exists = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'SUCURSALES' AND COLUMN_NAME = 'ES_CENTRAL'",
                    Integer.class);
            if (exists != null && exists == 0) {
                jdbc.execute("ALTER TABLE sucursales ADD COLUMN es_central BOOLEAN DEFAULT FALSE NOT NULL");
                log.info("Columna sucursales.es_central creada correctamente.");
            }
        } catch (Exception e) {
            log.warn("Migración es_central (comprobación o ALTER): {}", e.getMessage());
        }
    }
}
