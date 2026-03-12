package com.farmacia.sistema.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * Añade la columna oculto_en_historial a caja_turnos si no existe,
 * para que bases creadas antes del cambio sigan funcionando sin ejecutar SQL a mano.
 */
@Component
@Order(0)
public class MigrationCajaTurnoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigrationCajaTurnoRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public MigrationCajaTurnoRunner(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tieneColumnaOcultoEnHistorial()) {
                agregarColumnaOcultoEnHistorial();
            }
        } catch (Exception e) {
            log.warn("No se pudo verificar/añadir columna oculto_en_historial: {}", e.getMessage());
        }
    }

    private boolean tieneColumnaOcultoEnHistorial() throws Exception {
        try (var conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();
            String schema = conn.getSchema();
            try (ResultSet rs = meta.getColumns(catalog, schema, "CAJA_TURNOS", "OCULTO_EN_HISTORIAL")) {
                if (rs.next()) return true;
            }
            try (ResultSet rs = meta.getColumns(catalog, schema, "caja_turnos", "oculto_en_historial")) {
                return rs.next();
            }
        }
    }

    private void agregarColumnaOcultoEnHistorial() {
        String url;
        try (var conn = dataSource.getConnection()) {
            url = conn.getMetaData().getURL();
        } catch (Exception e) {
            log.warn("No se pudo obtener URL del datasource: {}", e.getMessage());
            url = "";
        }
        String sql;
        if (url != null && url.toLowerCase().contains(":h2:")) {
            sql = "ALTER TABLE caja_turnos ADD COLUMN oculto_en_historial BOOLEAN NOT NULL DEFAULT FALSE";
        } else if (url != null && url.toLowerCase().contains("postgresql")) {
            sql = "ALTER TABLE caja_turnos ADD COLUMN IF NOT EXISTS oculto_en_historial BOOLEAN NOT NULL DEFAULT FALSE";
        } else {
            sql = "ALTER TABLE caja_turnos ADD COLUMN oculto_en_historial BOOLEAN NOT NULL DEFAULT FALSE";
        }
        try {
            jdbcTemplate.execute(sql);
            log.info("Columna caja_turnos.oculto_en_historial añadida correctamente.");
        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("already exists") || e.getMessage().contains("duplicate column"))) {
                log.debug("Columna oculto_en_historial ya existe.");
            } else {
                throw e;
            }
        }
    }
}
