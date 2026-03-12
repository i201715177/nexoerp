-- Migración: añadir columna oculto_en_historial a caja_turnos (eliminar del historial sin borrar datos).
-- Ejecutar solo si la columna aún no existe (error "Column OCULTO_EN_HISTORIAL not found").

-- H2 (desarrollo, BD en ./data/farmacia):
ALTER TABLE caja_turnos ADD COLUMN oculto_en_historial BOOLEAN NOT NULL DEFAULT FALSE;

-- PostgreSQL (si usas perfil postgres; comentar la línea H2 y descomentar la siguiente):
-- ALTER TABLE caja_turnos ADD COLUMN IF NOT EXISTS oculto_en_historial BOOLEAN NOT NULL DEFAULT FALSE;
