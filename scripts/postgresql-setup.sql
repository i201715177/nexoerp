-- ============================================================================
-- SCRIPT COMPLETO POSTGRESQL - Sistema Farmacia SaaS Multi-Tenant
-- ============================================================================
-- Ejecutar como superusuario (postgres) en psql o pgAdmin
-- Paso 1: Crear usuario y base de datos (ejecutar como postgres)
-- Paso 2: Conectarse a la BD 'farmacia' y ejecutar el resto
-- ============================================================================

-- ============================================================================
-- PASO 1: CREAR USUARIO Y BASE DE DATOS (ejecutar como superusuario postgres)
-- ============================================================================
-- Si ya existe, comentar estas lineas:
CREATE USER farmacia WITH PASSWORD 'farmacia123';
CREATE DATABASE farmacia OWNER farmacia ENCODING 'UTF8';
GRANT ALL PRIVILEGES ON DATABASE farmacia TO farmacia;

-- Conectarse a la base de datos farmacia:
-- \c farmacia

-- Dar permisos en el schema public
GRANT ALL ON SCHEMA public TO farmacia;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO farmacia;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO farmacia;

-- ============================================================================
-- PASO 2: CREAR TABLAS (ejecutar conectado a BD 'farmacia' como usuario 'farmacia')
-- ============================================================================

-- Tabla de empresas (tenants) - NO tiene tenant_id, es global
CREATE TABLE IF NOT EXISTS empresas (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(30) NOT NULL,
    nombre          VARCHAR(150) NOT NULL,
    descripcion     VARCHAR(255),
    activa          BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_empresas_codigo UNIQUE (codigo)
);

-- Usuarios del sistema
CREATE TABLE IF NOT EXISTS usuarios (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(80) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    nombre_completo VARCHAR(150),
    rol             VARCHAR(30) NOT NULL DEFAULT 'VENDEDOR',
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id       BIGINT NOT NULL,
    CONSTRAINT uk_usuarios_username UNIQUE (username)
);

-- Sucursales
CREATE TABLE IF NOT EXISTS sucursales (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(20) NOT NULL,
    nombre          VARCHAR(100) NOT NULL,
    direccion       VARCHAR(255),
    activa          BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id       BIGINT NOT NULL,
    CONSTRAINT uk_sucursales_codigo UNIQUE (codigo)
);

-- Almacenes
CREATE TABLE IF NOT EXISTS almacenes (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(20) NOT NULL,
    nombre          VARCHAR(100) NOT NULL,
    principal       BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id       BIGINT NOT NULL,
    sucursal_id     BIGINT,
    CONSTRAINT uk_almacenes_codigo UNIQUE (codigo),
    CONSTRAINT fk_almacen_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales(id)
);

-- Productos
CREATE TABLE IF NOT EXISTS productos (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(50) NOT NULL,
    nombre          VARCHAR(200) NOT NULL,
    descripcion     VARCHAR(500),
    laboratorio     VARCHAR(100) NOT NULL,
    presentacion    VARCHAR(50) NOT NULL,
    categoria       VARCHAR(100),
    marca           VARCHAR(100),
    unidad_medida   VARCHAR(30),
    codigo_barras   VARCHAR(100),
    imagen_url      VARCHAR(255),
    precio_venta    NUMERIC(12,2) NOT NULL CHECK (precio_venta >= 0),
    costo_unitario  NUMERIC(12,2),
    stock_actual    INTEGER NOT NULL CHECK (stock_actual >= 0),
    stock_minimo    INTEGER NOT NULL CHECK (stock_minimo >= 0),
    stock_maximo    INTEGER CHECK (stock_maximo >= 0),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id       BIGINT NOT NULL,
    CONSTRAINT uk_productos_codigo UNIQUE (codigo)
);

-- Clientes
CREATE TABLE IF NOT EXISTS clientes (
    id                  BIGSERIAL PRIMARY KEY,
    tipo_documento      VARCHAR(20) NOT NULL,
    numero_documento    VARCHAR(20) NOT NULL,
    nombres             VARCHAR(150) NOT NULL,
    apellidos           VARCHAR(150),
    telefono            VARCHAR(20),
    email               VARCHAR(150),
    direccion           VARCHAR(255),
    puntos              INTEGER NOT NULL DEFAULT 0,
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id           BIGINT NOT NULL,
    CONSTRAINT uk_clientes_doc UNIQUE (numero_documento)
);

-- Proveedores
CREATE TABLE IF NOT EXISTS proveedores (
    id                  BIGSERIAL PRIMARY KEY,
    tipo_documento      VARCHAR(20) NOT NULL,
    numero_documento    VARCHAR(20) NOT NULL,
    razon_social        VARCHAR(200) NOT NULL,
    contacto            VARCHAR(150),
    telefono            VARCHAR(20),
    email               VARCHAR(150),
    direccion           VARCHAR(255),
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id           BIGINT NOT NULL,
    CONSTRAINT uk_proveedores_doc UNIQUE (numero_documento)
);

-- Caja / Turnos
CREATE TABLE IF NOT EXISTS caja_turnos (
    id              BIGSERIAL PRIMARY KEY,
    fecha_apertura  TIMESTAMP(6) NOT NULL,
    fecha_cierre    TIMESTAMP(6),
    monto_inicial   NUMERIC(12,2) NOT NULL,
    monto_cierre    NUMERIC(12,2),
    estado          VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',
    observaciones   VARCHAR(255),
    usuario         VARCHAR(50),
    nombre_vendedor VARCHAR(100),
    oculto_en_historial BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id       BIGINT NOT NULL,
    sucursal_id     BIGINT,
    CONSTRAINT fk_caja_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales(id)
);

-- Ventas
CREATE TABLE IF NOT EXISTS ventas (
    id                  BIGSERIAL PRIMARY KEY,
    cliente_id          BIGINT NOT NULL,
    nombre_cliente_venta VARCHAR(255),
    fecha_hora          TIMESTAMP(6) NOT NULL,
    subtotal            NUMERIC(12,2) NOT NULL,
    descuento_total     NUMERIC(12,2) NOT NULL DEFAULT 0,
    total               NUMERIC(12,2) NOT NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'EMITIDA',
    tipo_comprobante    VARCHAR(10),
    serie_comprobante   VARCHAR(10),
    numero_comprobante  VARCHAR(20),
    estado_sunat        VARCHAR(20),
    caja_turno_id       BIGINT,
    tenant_id           BIGINT NOT NULL,
    CONSTRAINT fk_venta_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_venta_caja FOREIGN KEY (caja_turno_id) REFERENCES caja_turnos(id)
);

-- Items de venta
CREATE TABLE IF NOT EXISTS venta_items (
    id              BIGSERIAL PRIMARY KEY,
    venta_id        BIGINT NOT NULL,
    producto_id     BIGINT NOT NULL,
    cantidad        INTEGER NOT NULL CHECK (cantidad >= 1),
    precio_unitario NUMERIC(12,2) NOT NULL CHECK (precio_unitario >= 0),
    descuento       NUMERIC(12,2),
    subtotal        NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0),
    CONSTRAINT fk_ventaitem_venta FOREIGN KEY (venta_id) REFERENCES ventas(id),
    CONSTRAINT fk_ventaitem_prod FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- Pagos de venta
CREATE TABLE IF NOT EXISTS pagos_venta (
    id          BIGSERIAL PRIMARY KEY,
    venta_id    BIGINT NOT NULL,
    medio_pago  VARCHAR(30) NOT NULL,
    monto       NUMERIC(12,2) NOT NULL CHECK (monto >= 0),
    CONSTRAINT fk_pagoventa_venta FOREIGN KEY (venta_id) REFERENCES ventas(id)
);

-- Notas de credito
CREATE TABLE IF NOT EXISTS notas_credito (
    id          BIGSERIAL PRIMARY KEY,
    venta_id    BIGINT NOT NULL,
    numero      VARCHAR(20) NOT NULL,
    fecha       TIMESTAMP(6) NOT NULL,
    total       NUMERIC(12,2) NOT NULL,
    motivo      VARCHAR(255),
    estado      VARCHAR(20) NOT NULL DEFAULT 'EMITIDA',
    CONSTRAINT fk_nc_venta FOREIGN KEY (venta_id) REFERENCES ventas(id)
);

-- Ordenes de compra
CREATE TABLE IF NOT EXISTS ordenes_compra (
    id                  BIGSERIAL PRIMARY KEY,
    proveedor_id        BIGINT NOT NULL,
    fecha_emision       TIMESTAMP(6) NOT NULL,
    fecha_esperada      DATE,
    estado              VARCHAR(20) DEFAULT 'EMITIDA',
    numero_documento    VARCHAR(100),
    subtotal            NUMERIC(12,2) NOT NULL DEFAULT 0,
    total               NUMERIC(12,2) NOT NULL DEFAULT 0,
    observaciones       VARCHAR(255),
    tenant_id           BIGINT NOT NULL,
    CONSTRAINT fk_oc_prov FOREIGN KEY (proveedor_id) REFERENCES proveedores(id)
);

-- Items de orden de compra
CREATE TABLE IF NOT EXISTS orden_compra_items (
    id              BIGSERIAL PRIMARY KEY,
    orden_compra_id BIGINT NOT NULL,
    producto_id     BIGINT NOT NULL,
    cantidad        INTEGER NOT NULL CHECK (cantidad >= 1),
    precio_unitario NUMERIC(12,2) NOT NULL CHECK (precio_unitario >= 0),
    subtotal        NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0),
    CONSTRAINT fk_oci_orden FOREIGN KEY (orden_compra_id) REFERENCES ordenes_compra(id),
    CONSTRAINT fk_oci_prod FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- Cuentas por pagar
CREATE TABLE IF NOT EXISTS cuentas_pagar (
    id                  BIGSERIAL PRIMARY KEY,
    proveedor_id        BIGINT NOT NULL,
    orden_compra_id     BIGINT,
    fecha_emision       DATE NOT NULL,
    fecha_vencimiento   DATE,
    monto_total         NUMERIC(12,2) NOT NULL,
    saldo_pendiente     NUMERIC(12,2) NOT NULL,
    estado              VARCHAR(20) DEFAULT 'PENDIENTE',
    observaciones       VARCHAR(255),
    tenant_id           BIGINT NOT NULL,
    CONSTRAINT fk_cp_prov FOREIGN KEY (proveedor_id) REFERENCES proveedores(id),
    CONSTRAINT fk_cp_oc FOREIGN KEY (orden_compra_id) REFERENCES ordenes_compra(id)
);

-- Stock por almacen
CREATE TABLE IF NOT EXISTS stock_almacen (
    id          BIGSERIAL PRIMARY KEY,
    almacen_id  BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad    INTEGER NOT NULL DEFAULT 0,
    tenant_id   BIGINT NOT NULL,
    CONSTRAINT uk_stock_alm_prod UNIQUE (almacen_id, producto_id),
    CONSTRAINT fk_stock_almacen FOREIGN KEY (almacen_id) REFERENCES almacenes(id),
    CONSTRAINT fk_stock_prod FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- Movimientos de inventario
CREATE TABLE IF NOT EXISTS inventario_movimientos (
    id              BIGSERIAL PRIMARY KEY,
    producto_id     BIGINT NOT NULL,
    almacen_id      BIGINT,
    fecha           TIMESTAMP(6) NOT NULL,
    tipo            VARCHAR(30) NOT NULL,
    cantidad        INTEGER NOT NULL,
    saldo_despues   INTEGER,
    referencia      VARCHAR(255),
    tenant_id       BIGINT NOT NULL,
    CONSTRAINT fk_invmov_prod FOREIGN KEY (producto_id) REFERENCES productos(id),
    CONSTRAINT fk_invmov_alm FOREIGN KEY (almacen_id) REFERENCES almacenes(id)
);

-- Lotes de producto
CREATE TABLE IF NOT EXISTS lotes_producto (
    id                  BIGSERIAL PRIMARY KEY,
    producto_id         BIGINT NOT NULL,
    numero_lote         VARCHAR(50) NOT NULL,
    fecha_vencimiento   DATE,
    cantidad_actual     INTEGER NOT NULL DEFAULT 0,
    tenant_id           BIGINT NOT NULL,
    CONSTRAINT fk_lote_prod FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- Secuencia de comprobantes
CREATE TABLE IF NOT EXISTS sequence_comprobante (
    id          BIGINT PRIMARY KEY,
    siguiente   BIGINT NOT NULL DEFAULT 1,
    version     BIGINT
);

-- Auditoria
CREATE TABLE IF NOT EXISTS auditoria_acciones (
    id          BIGSERIAL PRIMARY KEY,
    fecha_hora  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario     VARCHAR(100) NOT NULL,
    metodo      VARCHAR(10) NOT NULL,
    url         VARCHAR(255) NOT NULL,
    ip          VARCHAR(255),
    accion      VARCHAR(100),
    detalle     VARCHAR(2000)
);

-- ============================================================================
-- PASO 3: CREAR INDICES PARA RENDIMIENTO
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_usr_tenant ON usuarios(tenant_id);
CREATE INDEX IF NOT EXISTS idx_usr_username ON usuarios(username);

CREATE INDEX IF NOT EXISTS idx_suc_tenant ON sucursales(tenant_id);
CREATE INDEX IF NOT EXISTS idx_suc_tenant_codigo ON sucursales(tenant_id, codigo);

CREATE INDEX IF NOT EXISTS idx_alm_tenant ON almacenes(tenant_id);
CREATE INDEX IF NOT EXISTS idx_alm_tenant_codigo ON almacenes(tenant_id, codigo);

CREATE INDEX IF NOT EXISTS idx_prod_tenant ON productos(tenant_id);
CREATE INDEX IF NOT EXISTS idx_prod_tenant_codigo ON productos(tenant_id, codigo);
CREATE INDEX IF NOT EXISTS idx_prod_tenant_nombre ON productos(tenant_id, nombre);
CREATE INDEX IF NOT EXISTS idx_prod_tenant_cat ON productos(tenant_id, categoria);

CREATE INDEX IF NOT EXISTS idx_cli_tenant ON clientes(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cli_tenant_doc ON clientes(tenant_id, numero_documento);

CREATE INDEX IF NOT EXISTS idx_prov_tenant ON proveedores(tenant_id);
CREATE INDEX IF NOT EXISTS idx_prov_tenant_doc ON proveedores(tenant_id, numero_documento);

CREATE INDEX IF NOT EXISTS idx_caja_tenant ON caja_turnos(tenant_id);
CREATE INDEX IF NOT EXISTS idx_caja_tenant_estado ON caja_turnos(tenant_id, estado);
CREATE INDEX IF NOT EXISTS idx_caja_tenant_fecha ON caja_turnos(tenant_id, fecha_apertura);

CREATE INDEX IF NOT EXISTS idx_venta_tenant ON ventas(tenant_id);
CREATE INDEX IF NOT EXISTS idx_venta_tenant_fecha ON ventas(tenant_id, fecha_hora);
CREATE INDEX IF NOT EXISTS idx_venta_tenant_estado ON ventas(tenant_id, estado);

CREATE INDEX IF NOT EXISTS idx_oc_tenant ON ordenes_compra(tenant_id);
CREATE INDEX IF NOT EXISTS idx_oc_tenant_estado ON ordenes_compra(tenant_id, estado);
CREATE INDEX IF NOT EXISTS idx_oc_tenant_fecha ON ordenes_compra(tenant_id, fecha_emision);

CREATE INDEX IF NOT EXISTS idx_cp_tenant ON cuentas_pagar(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cp_tenant_estado ON cuentas_pagar(tenant_id, estado);
CREATE INDEX IF NOT EXISTS idx_cp_tenant_venc ON cuentas_pagar(tenant_id, fecha_vencimiento);

CREATE INDEX IF NOT EXISTS idx_stock_tenant ON stock_almacen(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_tenant_prod ON stock_almacen(tenant_id, producto_id);

CREATE INDEX IF NOT EXISTS idx_invmov_tenant ON inventario_movimientos(tenant_id);
CREATE INDEX IF NOT EXISTS idx_invmov_tenant_fecha ON inventario_movimientos(tenant_id, fecha);
CREATE INDEX IF NOT EXISTS idx_invmov_tenant_prod ON inventario_movimientos(tenant_id, producto_id);

CREATE INDEX IF NOT EXISTS idx_lote_tenant ON lotes_producto(tenant_id);
CREATE INDEX IF NOT EXISTS idx_lote_tenant_venc ON lotes_producto(tenant_id, fecha_vencimiento);

CREATE INDEX IF NOT EXISTS idx_audit_fecha ON auditoria_acciones(fecha_hora);
CREATE INDEX IF NOT EXISTS idx_audit_usuario ON auditoria_acciones(usuario);

-- ============================================================================
-- PASO 4: DATOS INICIALES - EMPRESAS (TENANTS)
-- ============================================================================

INSERT INTO empresas (id, codigo, nombre, descripcion, activa) VALUES
(1, 'TENANT1', 'Farmacia San Juan', 'Farmacia principal - sede central Lima', TRUE),
(2, 'TENANT2', 'Botica del Norte', 'Cadena de boticas en el norte del pais', TRUE),
(3, 'TENANT3', 'Farmacia Salud Total', 'Farmacia especializada en productos naturales', TRUE);

-- Resetear secuencia
SELECT setval('empresas_id_seq', 3);

-- ============================================================================
-- PASO 5: USUARIOS (password BCrypt = 'admin123')
-- ============================================================================
-- Hash BCrypt para 'admin123': $2a$10$EqKcp1WFKs3ViKBrdSylZOeBNESsSgpCMwfCAuJPyr2BiEMV1lQHe
-- Hash BCrypt para 'vendedor1': $2a$10$YhG5tNvG0eO.w5JzLXqJYeKRfJHr/VJ8NxQRxBFmH0e7G4kJwfmXu

INSERT INTO usuarios (id, username, password, nombre_completo, rol, activo, tenant_id) VALUES
-- Empresa 1: Farmacia San Juan
(1, 'admin', '$2a$10$EqKcp1WFKs3ViKBrdSylZOeBNESsSgpCMwfCAuJPyr2BiEMV1lQHe', 'Administrador General', 'ADMIN', TRUE, 1),
(2, 'carlos_ventas', '$2a$10$EqKcp1WFKs3ViKBrdSylZOeBNESsSgpCMwfCAuJPyr2BiEMV1lQHe', 'Carlos Rodriguez', 'VENDEDOR', TRUE, 1),
(3, 'maria_caja', '$2a$10$EqKcp1WFKs3ViKBrdSylZOeBNESsSgpCMwfCAuJPyr2BiEMV1lQHe', 'Maria Garcia', 'CAJERO', TRUE, 1),
-- Empresa 2: Botica del Norte
(4, 'norte_admin', '$2a$10$EqKcp1WFKs3ViKBrdSylZOeBNESsSgpCMwfCAuJPyr2BiEMV1lQHe', 'Admin Norte', 'ADMIN', TRUE, 2),
(5, 'pedro_ventas', '$2a$10$EqKcp1WFKs3ViKBrdSylZOeBNESsSgpCMwfCAuJPyr2BiEMV1lQHe', 'Pedro Martinez', 'VENDEDOR', TRUE, 2),
-- Empresa 3: Farmacia Salud Total
(6, 'salud_admin', '$2a$10$EqKcp1WFKs3ViKBrdSylZOeBNESsSgpCMwfCAuJPyr2BiEMV1lQHe', 'Admin Salud Total', 'ADMIN', TRUE, 3);

SELECT setval('usuarios_id_seq', 6);

-- ============================================================================
-- PASO 6: SUCURSALES
-- ============================================================================

INSERT INTO sucursales (id, codigo, nombre, direccion, activa, tenant_id) VALUES
-- Empresa 1
(1, 'SUC-PRINCIPAL', 'Sede Central', 'Av. Arequipa 1520, Lince, Lima', TRUE, 1),
(2, 'SUC-SURCO', 'Sucursal Surco', 'Av. Caminos del Inca 340, Surco, Lima', TRUE, 1),
-- Empresa 2
(3, 'SUC-TRUJILLO', 'Sede Trujillo', 'Jr. Pizarro 820, Centro, Trujillo', TRUE, 2),
(4, 'SUC-CHICLAYO', 'Sucursal Chiclayo', 'Av. Balta 456, Centro, Chiclayo', TRUE, 2),
-- Empresa 3
(5, 'SUC-NATURAL', 'Sede Natural', 'Av. La Molina 890, La Molina, Lima', TRUE, 3);

SELECT setval('sucursales_id_seq', 5);

-- ============================================================================
-- PASO 7: ALMACENES
-- ============================================================================

INSERT INTO almacenes (id, codigo, nombre, principal, tenant_id, sucursal_id) VALUES
(1, 'ALM-CENTRAL', 'Almacen Central', TRUE, 1, 1),
(2, 'ALM-SURCO', 'Almacen Surco', FALSE, 1, 2),
(3, 'ALM-TRUJILLO', 'Almacen Trujillo', TRUE, 2, 3),
(4, 'ALM-CHICLAYO', 'Almacen Chiclayo', FALSE, 2, 4),
(5, 'ALM-NATURAL', 'Almacen Natural', TRUE, 3, 5);

SELECT setval('almacenes_id_seq', 5);

-- ============================================================================
-- PASO 8: PROVEEDORES
-- ============================================================================

INSERT INTO proveedores (id, tipo_documento, numero_documento, razon_social, contacto, telefono, email, direccion, activo, tenant_id) VALUES
-- Empresa 1
(1, 'RUC', '20100047218', 'Medifarma S.A.', 'Juan Perez', '014521000', 'ventas@medifarma.com', 'Av. Santa Rosa 350, Santa Anita, Lima', TRUE, 1),
(2, 'RUC', '20100096936', 'Laboratorios Roemmers', 'Ana Castillo', '016121200', 'pedidos@roemmers.com.pe', 'Av. Guardia Civil 1280, San Isidro, Lima', TRUE, 1),
(3, 'RUC', '20504902553', 'Distribuidora Dimexa', 'Roberto Diaz', '017180500', 'ventas@dimexa.com', 'Av. Argentina 4458, Callao', TRUE, 1),
(4, 'RUC', '20100128056', 'Farmindustria S.A.', 'Laura Mendez', '016145600', 'compras@farmindustria.com.pe', 'Av. Defensores del Morro 1277, Chorrillos', TRUE, 1),
-- Empresa 2
(5, 'RUC', '20482527948', 'Drogueria La Victoria', 'Mario Torres', '044234567', 'contacto@drog-lavictoria.com', 'Jr. Junin 580, La Victoria, Trujillo', TRUE, 2),
(6, 'RUC', '20100096936', 'Lab. Roemmers Norte', 'Sandra Rios', '074256789', 'norte@roemmers.com.pe', 'Av. Saenz Pena 1120, Chiclayo', TRUE, 2),
-- Empresa 3
(7, 'RUC', '20511315922', 'Naturista Peru SAC', 'Elena Vargas', '012456780', 'ventas@naturistaperu.com', 'Av. Canada 1230, La Victoria, Lima', TRUE, 3);

SELECT setval('proveedores_id_seq', 7);

-- ============================================================================
-- PASO 9: PRODUCTOS (datos reales de farmacia)
-- ============================================================================

INSERT INTO productos (id, codigo, nombre, descripcion, laboratorio, presentacion, categoria, marca, unidad_medida, codigo_barras, precio_venta, costo_unitario, stock_actual, stock_minimo, stock_maximo, activo, tenant_id) VALUES
-- ===================== EMPRESA 1: Farmacia San Juan =====================
-- Analgesicos
(1,  'PROD-001', 'Paracetamol 500mg', 'Tabletas analgesicas y antipireticas', 'Medifarma', 'Caja x 100 tab', 'Analgesicos', 'Panadol', 'CAJA', '7750075000100', 8.50, 4.20, 500, 50, 1000, TRUE, 1),
(2,  'PROD-002', 'Ibuprofeno 400mg', 'Antiinflamatorio no esteroideo', 'Medifarma', 'Caja x 100 tab', 'Analgesicos', 'Motrin', 'CAJA', '7750075000117', 12.00, 5.80, 350, 40, 800, TRUE, 1),
(3,  'PROD-003', 'Naproxeno 550mg', 'Antiinflamatorio analgesico', 'Roemmers', 'Caja x 20 tab', 'Analgesicos', 'Apronax', 'CAJA', '7750075000124', 18.50, 9.00, 200, 30, 500, TRUE, 1),
(4,  'PROD-004', 'Diclofenaco 50mg', 'Antiinflamatorio potente', 'Farmindustria', 'Caja x 20 tab', 'Analgesicos', 'Voltaren', 'CAJA', '7750075000131', 15.00, 7.50, 280, 35, 600, TRUE, 1),
-- Antibioticos
(5,  'PROD-005', 'Amoxicilina 500mg', 'Antibiotico de amplio espectro', 'Medifarma', 'Caja x 21 cap', 'Antibioticos', 'Amoxil', 'CAJA', '7750075000148', 22.00, 10.50, 150, 25, 400, TRUE, 1),
(6,  'PROD-006', 'Azitromicina 500mg', 'Antibiotico macrolido', 'Roemmers', 'Caja x 3 tab', 'Antibioticos', 'Zithromax', 'CAJA', '7750075000155', 28.50, 14.00, 120, 20, 300, TRUE, 1),
(7,  'PROD-007', 'Ciprofloxacino 500mg', 'Antibiotico fluoroquinolona', 'Farmindustria', 'Caja x 10 tab', 'Antibioticos', 'Cipro', 'CAJA', '7750075000162', 18.00, 8.50, 180, 25, 400, TRUE, 1),
-- Gastricos
(8,  'PROD-008', 'Omeprazol 20mg', 'Inhibidor de bomba de protones', 'Medifarma', 'Caja x 30 cap', 'Gastricos', 'Losec', 'CAJA', '7750075000179', 16.00, 7.00, 400, 50, 800, TRUE, 1),
(9,  'PROD-009', 'Ranitidina 150mg', 'Antihistaminico H2', 'Medifarma', 'Caja x 20 tab', 'Gastricos', 'Zantac', 'CAJA', '7750075000186', 9.50, 4.00, 300, 40, 600, TRUE, 1),
-- Vitaminas
(10, 'PROD-010', 'Vitamina C 500mg', 'Acido ascorbico', 'Medifarma', 'Frasco x 100 tab', 'Vitaminas', 'Redoxon', 'FRASCO', '7750075000193', 25.00, 12.00, 250, 30, 500, TRUE, 1),
(11, 'PROD-011', 'Complejo B', 'Vitaminas del complejo B', 'Roemmers', 'Frasco x 100 tab', 'Vitaminas', 'Neurobion', 'FRASCO', '7750075000209', 32.00, 15.50, 200, 25, 400, TRUE, 1),
-- Dermatologicos
(12, 'PROD-012', 'Crema Clotrimazol 1%', 'Antimicotico topico', 'Farmindustria', 'Tubo x 20g', 'Dermatologicos', 'Canesten', 'TUBO', '7750075000216', 14.00, 6.50, 100, 15, 200, TRUE, 1),
-- Respiratorios
(13, 'PROD-013', 'Salbutamol Inhalador', 'Broncodilatador', 'Roemmers', 'Inhaler 200 dosis', 'Respiratorios', 'Ventolin', 'UNIDAD', '7750075000223', 45.00, 22.00, 80, 10, 150, TRUE, 1),
(14, 'PROD-014', 'Loratadina 10mg', 'Antihistaminico', 'Medifarma', 'Caja x 10 tab', 'Antialergicos', 'Clarityne', 'CAJA', '7750075000230', 12.00, 5.50, 320, 40, 700, TRUE, 1),
-- Higiene
(15, 'PROD-015', 'Alcohol 70% 1L', 'Alcohol para desinfeccion', 'Dimexa', 'Botella 1 litro', 'Higiene', 'Dimexa', 'BOTELLA', '7750075000247', 8.00, 3.50, 200, 30, 500, TRUE, 1),
(16, 'PROD-016', 'Agua Oxigenada 120ml', 'Peroxido de hidrogeno', 'Dimexa', 'Frasco 120ml', 'Higiene', 'Dimexa', 'FRASCO', '7750075000254', 5.50, 2.00, 180, 25, 400, TRUE, 1),
-- Diabetes
(17, 'PROD-017', 'Metformina 850mg', 'Hipoglucemiante oral', 'Medifarma', 'Caja x 30 tab', 'Diabetes', 'Glucophage', 'CAJA', '7750075000261', 18.00, 8.00, 200, 30, 500, TRUE, 1),
-- Cardiovascular
(18, 'PROD-018', 'Losartan 50mg', 'Antihipertensivo', 'Roemmers', 'Caja x 30 tab', 'Cardiovascular', 'Cozaar', 'CAJA', '7750075000278', 22.00, 10.00, 180, 25, 400, TRUE, 1),
(19, 'PROD-019', 'Enalapril 10mg', 'Inhibidor de ECA', 'Farmindustria', 'Caja x 30 tab', 'Cardiovascular', 'Renitec', 'CAJA', '7750075000285', 15.00, 6.50, 220, 30, 500, TRUE, 1),
(20, 'PROD-020', 'Aspirina 100mg', 'Antiagregante plaquetario', 'Medifarma', 'Caja x 30 tab', 'Cardiovascular', 'Aspirina', 'CAJA', '7750075000292', 8.00, 3.00, 400, 50, 800, TRUE, 1),

-- ===================== EMPRESA 2: Botica del Norte =====================
(21, 'NORTE-001', 'Paracetamol 500mg', 'Tabletas analgesicas', 'Medifarma', 'Caja x 100 tab', 'Analgesicos', 'Panadol', 'CAJA', '7750075100100', 9.00, 4.50, 400, 50, 800, TRUE, 2),
(22, 'NORTE-002', 'Amoxicilina 500mg', 'Antibiotico amplio espectro', 'Medifarma', 'Caja x 21 cap', 'Antibioticos', 'Amoxil', 'CAJA', '7750075100117', 23.00, 11.00, 180, 25, 400, TRUE, 2),
(23, 'NORTE-003', 'Omeprazol 20mg', 'Inhibidor bomba protones', 'Medifarma', 'Caja x 30 cap', 'Gastricos', 'Losec', 'CAJA', '7750075100124', 17.00, 7.50, 350, 40, 700, TRUE, 2),
(24, 'NORTE-004', 'Ibuprofeno 400mg', 'Antiinflamatorio', 'Medifarma', 'Caja x 100 tab', 'Analgesicos', 'Motrin', 'CAJA', '7750075100131', 13.00, 6.00, 300, 35, 600, TRUE, 2),
(25, 'NORTE-005', 'Vitamina C 500mg', 'Acido ascorbico', 'Medifarma', 'Frasco x 100 tab', 'Vitaminas', 'Redoxon', 'FRASCO', '7750075100148', 26.00, 12.50, 200, 25, 500, TRUE, 2),
(26, 'NORTE-006', 'Losartan 50mg', 'Antihipertensivo', 'Roemmers', 'Caja x 30 tab', 'Cardiovascular', 'Cozaar', 'CAJA', '7750075100155', 23.00, 10.50, 150, 20, 400, TRUE, 2),
(27, 'NORTE-007', 'Metformina 850mg', 'Hipoglucemiante', 'Medifarma', 'Caja x 30 tab', 'Diabetes', 'Glucophage', 'CAJA', '7750075100162', 19.00, 8.50, 180, 25, 400, TRUE, 2),
(28, 'NORTE-008', 'Azitromicina 500mg', 'Antibiotico macrolido', 'Roemmers', 'Caja x 3 tab', 'Antibioticos', 'Zithromax', 'CAJA', '7750075100179', 30.00, 14.50, 100, 15, 300, TRUE, 2),
(29, 'NORTE-009', 'Diclofenaco Gel 1%', 'Gel antiinflamatorio topico', 'Farmindustria', 'Tubo x 30g', 'Dermatologicos', 'Voltaren', 'TUBO', '7750075100186', 16.00, 7.50, 90, 12, 200, TRUE, 2),
(30, 'NORTE-010', 'Loratadina 10mg', 'Antihistaminico', 'Medifarma', 'Caja x 10 tab', 'Antialergicos', 'Clarityne', 'CAJA', '7750075100193', 13.00, 6.00, 250, 30, 600, TRUE, 2),

-- ===================== EMPRESA 3: Farmacia Salud Total =====================
(31, 'NAT-001', 'Uña de Gato Capsulas', 'Antiinflamatorio natural', 'Naturista Peru', 'Frasco x 100 cap', 'Naturales', 'NaturPeru', 'FRASCO', '7750075200100', 28.00, 12.00, 150, 20, 400, TRUE, 3),
(32, 'NAT-002', 'Maca Gelatinizada', 'Energizante natural', 'Naturista Peru', 'Frasco x 100 cap', 'Naturales', 'NaturPeru', 'FRASCO', '7750075200117', 35.00, 15.00, 200, 25, 500, TRUE, 3),
(33, 'NAT-003', 'Aceite de Sacha Inchi', 'Omega 3 vegetal', 'Naturista Peru', 'Frasco x 250ml', 'Naturales', 'NaturPeru', 'FRASCO', '7750075200124', 42.00, 20.00, 100, 15, 300, TRUE, 3),
(34, 'NAT-004', 'Camu Camu Capsulas', 'Vitamina C natural concentrada', 'Naturista Peru', 'Frasco x 100 cap', 'Vitaminas', 'NaturPeru', 'FRASCO', '7750075200131', 32.00, 14.00, 180, 20, 400, TRUE, 3),
(35, 'NAT-005', 'Paracetamol 500mg', 'Analgesico generico', 'Medifarma', 'Caja x 100 tab', 'Analgesicos', 'Panadol', 'CAJA', '7750075200148', 8.50, 4.20, 250, 30, 500, TRUE, 3);

SELECT setval('productos_id_seq', 35);

-- ============================================================================
-- PASO 10: CLIENTES
-- ============================================================================

INSERT INTO clientes (id, tipo_documento, numero_documento, nombres, apellidos, telefono, email, direccion, puntos, activo, tenant_id) VALUES
-- Empresa 1
(1,  'DNI', '45678901', 'Juan Carlos', 'Perez Lopez', '987654321', 'jperez@gmail.com', 'Av. Brasil 1234, Pueblo Libre, Lima', 120, TRUE, 1),
(2,  'DNI', '12345678', 'Maria Elena', 'Garcia Torres', '976543210', 'mgarcia@hotmail.com', 'Jr. Cusco 567, Jesus Maria, Lima', 85, TRUE, 1),
(3,  'RUC', '20501234567', 'Clinica San Pablo', NULL, '014568900', 'compras@sanpablo.com.pe', 'Av. El Polo 789, Surco, Lima', 450, TRUE, 1),
(4,  'DNI', '87654321', 'Roberto', 'Mendoza Quispe', '965432109', 'rmendoza@gmail.com', 'Calle Las Flores 234, San Borja, Lima', 30, TRUE, 1),
(5,  'CE',  'CE001234', 'Ana Sofia', 'Fernandez', '954321098', 'afernandez@yahoo.com', 'Av. Javier Prado 2345, San Isidro, Lima', 200, TRUE, 1),
(6,  'DNI', '00000000', 'Cliente General', 'Varios', NULL, NULL, NULL, 0, TRUE, 1),
-- Empresa 2
(7,  'DNI', '33445566', 'Luis Alberto', 'Torres Ramirez', '944567890', 'ltorres@gmail.com', 'Jr. Gamarra 456, Centro, Trujillo', 75, TRUE, 2),
(8,  'DNI', '77889900', 'Carmen Rosa', 'Vasquez Diaz', '933456789', 'cvasquez@hotmail.com', 'Av. Larco 890, Trujillo', 50, TRUE, 2),
(9,  'RUC', '20482001234', 'Posta Medica El Sol', NULL, '044345678', 'compras@postaelsol.com', 'Calle Bolivar 123, Chiclayo', 300, TRUE, 2),
(10, 'DNI', '00000001', 'Cliente General', 'Varios', NULL, NULL, NULL, 0, TRUE, 2),
-- Empresa 3
(11, 'DNI', '55667788', 'Andrea', 'Salazar Ramos', '922345678', 'asalazar@gmail.com', 'Av. La Molina 1234, La Molina, Lima', 90, TRUE, 3),
(12, 'DNI', '00000002', 'Cliente General', 'Varios', NULL, NULL, NULL, 0, TRUE, 3);

SELECT setval('clientes_id_seq', 12);

-- ============================================================================
-- PASO 11: CAJA / TURNOS DE EJEMPLO
-- ============================================================================

INSERT INTO caja_turnos (id, fecha_apertura, fecha_cierre, monto_inicial, monto_cierre, estado, observaciones, usuario, tenant_id, sucursal_id) VALUES
(1, '2026-03-01 08:00:00', '2026-03-01 20:00:00', 200.00, 1850.50, 'CERRADO', 'Turno manana completo', 'carlos_ventas', 1, 1),
(2, '2026-03-02 08:00:00', '2026-03-02 20:00:00', 200.00, 2100.00, 'CERRADO', 'Turno normal', 'carlos_ventas', 1, 1),
(3, '2026-03-03 08:00:00', NULL, 200.00, NULL, 'ABIERTO', 'Turno actual', 'carlos_ventas', 1, 1),
-- Empresa 2
(4, '2026-03-01 08:30:00', '2026-03-01 19:30:00', 150.00, 980.00, 'CERRADO', NULL, 'pedro_ventas', 2, 3);

SELECT setval('caja_turnos_id_seq', 4);

-- ============================================================================
-- PASO 12: VENTAS DE EJEMPLO
-- ============================================================================

INSERT INTO ventas (id, cliente_id, nombre_cliente_venta, fecha_hora, subtotal, descuento_total, total, estado, tipo_comprobante, serie_comprobante, numero_comprobante, caja_turno_id, tenant_id) VALUES
-- Empresa 1 - Ventas historicas
(1, 1, 'Juan Carlos Perez Lopez', '2026-03-01 09:15:00', 46.50, 0.00, 46.50, 'EMITIDA', 'BOLETA', 'B001', '00000001', 1, 1),
(2, 2, 'Maria Elena Garcia Torres', '2026-03-01 10:30:00', 90.00, 5.00, 85.00, 'EMITIDA', 'BOLETA', 'B001', '00000002', 1, 1),
(3, 3, 'Clinica San Pablo', '2026-03-01 14:00:00', 550.00, 0.00, 550.00, 'EMITIDA', 'FACTURA', 'F001', '00000001', 1, 1),
(4, 6, 'Cliente General Varios', '2026-03-02 08:45:00', 24.00, 0.00, 24.00, 'EMITIDA', 'BOLETA', 'B001', '00000003', 2, 1),
(5, 4, 'Roberto Mendoza Quispe', '2026-03-02 11:20:00', 66.00, 0.00, 66.00, 'EMITIDA', 'BOLETA', 'B001', '00000004', 2, 1),
-- Empresa 2
(6, 7, 'Luis Alberto Torres Ramirez', '2026-03-01 09:00:00', 62.00, 0.00, 62.00, 'EMITIDA', 'BOLETA', 'B001', '00000001', 4, 2);

SELECT setval('ventas_id_seq', 6);

-- Items de venta
INSERT INTO venta_items (id, venta_id, producto_id, cantidad, precio_unitario, descuento, subtotal) VALUES
-- Venta 1: Paracetamol + Omeprazol
(1, 1, 1, 2, 8.50, 0, 17.00),
(2, 1, 8, 1, 16.00, 0, 16.00),
(3, 1, 14, 1, 12.00, 0, 12.00),
-- Venta 2: Antibioticos
(4, 2, 5, 2, 22.00, 0, 44.00),
(5, 2, 6, 1, 28.50, 0, 28.50),
(6, 2, 10, 1, 25.00, 5.00, 20.00),
-- Venta 3: Pedido clinica (gran volumen)
(7, 3, 1, 20, 8.50, 0, 170.00),
(8, 3, 5, 10, 22.00, 0, 220.00),
(9, 3, 8, 10, 16.00, 0, 160.00),
-- Venta 4
(10, 4, 1, 1, 8.50, 0, 8.50),
(11, 4, 15, 1, 8.00, 0, 8.00),
(12, 4, 16, 1, 5.50, 0, 5.50),
-- Venta 5
(13, 5, 18, 2, 22.00, 0, 44.00),
(14, 5, 19, 1, 15.00, 0, 15.00),
-- Venta 6 (Empresa 2)
(15, 6, 21, 3, 9.00, 0, 27.00),
(16, 6, 23, 2, 17.00, 0, 34.00);

SELECT setval('venta_items_id_seq', 16);

-- Pagos de venta
INSERT INTO pagos_venta (id, venta_id, medio_pago, monto) VALUES
(1, 1, 'EFECTIVO', 46.50),
(2, 2, 'TARJETA', 85.00),
(3, 3, 'TRANSFERENCIA', 550.00),
(4, 4, 'EFECTIVO', 24.00),
(5, 5, 'EFECTIVO', 50.00),
(6, 5, 'YAPE', 16.00),
(7, 6, 'EFECTIVO', 62.00);

SELECT setval('pagos_venta_id_seq', 7);

-- ============================================================================
-- PASO 13: ORDENES DE COMPRA
-- ============================================================================

INSERT INTO ordenes_compra (id, proveedor_id, fecha_emision, fecha_esperada, estado, numero_documento, subtotal, total, observaciones, tenant_id) VALUES
(1, 1, '2026-02-25 10:00:00', '2026-03-02', 'RECIBIDA', 'OC-2026-001', 2100.00, 2478.00, 'Reposicion mensual analgesicos', 1),
(2, 2, '2026-02-28 14:00:00', '2026-03-05', 'EMITIDA', 'OC-2026-002', 1400.00, 1652.00, 'Pedido antibioticos', 1),
(3, 5, '2026-02-26 09:00:00', '2026-03-03', 'RECIBIDA', 'OC-NORTE-001', 950.00, 1121.00, 'Stock inicial', 2);

SELECT setval('ordenes_compra_id_seq', 3);

INSERT INTO orden_compra_items (id, orden_compra_id, producto_id, cantidad, precio_unitario, subtotal) VALUES
(1, 1, 1, 200, 4.20, 840.00),
(2, 1, 2, 100, 5.80, 580.00),
(3, 1, 8, 100, 7.00, 700.00),
(4, 2, 5, 50, 10.50, 525.00),
(5, 2, 6, 30, 14.00, 420.00),
(6, 2, 7, 50, 8.50, 425.00),
(7, 3, 21, 100, 4.50, 450.00),
(8, 3, 22, 50, 11.00, 550.00);

SELECT setval('orden_compra_items_id_seq', 8);

-- ============================================================================
-- PASO 14: CUENTAS POR PAGAR
-- ============================================================================

INSERT INTO cuentas_pagar (id, proveedor_id, orden_compra_id, fecha_emision, fecha_vencimiento, monto_total, saldo_pendiente, estado, observaciones, tenant_id) VALUES
(1, 1, 1, '2026-02-25', '2026-03-25', 2478.00, 0.00, 'PAGADA', 'Pagada al contado en recepcion', 1),
(2, 2, 2, '2026-02-28', '2026-03-30', 1652.00, 1652.00, 'PENDIENTE', 'Credito 30 dias', 1),
(3, 5, 3, '2026-02-26', '2026-03-26', 1121.00, 500.00, 'PENDIENTE', 'Abono parcial realizado', 2);

SELECT setval('cuentas_pagar_id_seq', 3);

-- ============================================================================
-- PASO 15: STOCK POR ALMACEN
-- ============================================================================

INSERT INTO stock_almacen (id, almacen_id, producto_id, cantidad, tenant_id) VALUES
-- Almacen Central (Empresa 1)
(1, 1, 1, 350),  (2, 1, 2, 250),  (3, 1, 3, 150),  (4, 1, 4, 200),
(5, 1, 5, 100),  (6, 1, 6, 80),   (7, 1, 7, 120),  (8, 1, 8, 300),
(9, 1, 9, 200),  (10, 1, 10, 180), (11, 1, 11, 140), (12, 1, 12, 70),
-- Almacen Surco (Empresa 1)
(13, 2, 1, 150), (14, 2, 2, 100), (15, 2, 5, 50),  (16, 2, 8, 100),
-- Almacen Trujillo (Empresa 2)
(17, 3, 21, 250),(18, 3, 22, 120),(19, 3, 23, 220),(20, 3, 24, 180),
-- Almacen Natural (Empresa 3)
(21, 5, 31, 100),(22, 5, 32, 150),(23, 5, 33, 70), (24, 5, 34, 130);

SELECT setval('stock_almacen_id_seq', 24);

-- ============================================================================
-- PASO 16: LOTES DE PRODUCTO
-- ============================================================================

INSERT INTO lotes_producto (id, producto_id, numero_lote, fecha_vencimiento, cantidad_actual, tenant_id) VALUES
(1, 1, 'LOT-2025-001', '2027-06-15', 300, 1),
(2, 1, 'LOT-2025-002', '2027-12-30', 200, 1),
(3, 5, 'LOT-AMX-001', '2027-03-20', 150, 1),
(4, 8, 'LOT-OME-001', '2027-09-10', 400, 1),
(5, 21, 'LOT-NRT-001', '2027-05-25', 400, 2),
(6, 31, 'LOT-NAT-001', '2028-01-15', 150, 3),
(7, 3, 'LOT-NAP-001', '2026-04-10', 50, 1),
(8, 14, 'LOT-LOR-001', '2026-05-20', 80, 1);

SELECT setval('lotes_producto_id_seq', 8);

-- ============================================================================
-- PASO 17: MOVIMIENTOS DE INVENTARIO
-- ============================================================================

INSERT INTO inventario_movimientos (id, producto_id, almacen_id, fecha, tipo, cantidad, saldo_despues, referencia, tenant_id) VALUES
(1, 1, 1, '2026-02-25 10:30:00', 'ENTRADA', 200, 500, 'OC-2026-001', 1),
(2, 2, 1, '2026-02-25 10:35:00', 'ENTRADA', 100, 350, 'OC-2026-001', 1),
(3, 8, 1, '2026-02-25 10:40:00', 'ENTRADA', 100, 400, 'OC-2026-001', 1),
(4, 1, 1, '2026-03-01 09:15:00', 'SALIDA', 2, 498, 'Venta #1', 1),
(5, 8, 1, '2026-03-01 09:15:00', 'SALIDA', 1, 399, 'Venta #1', 1),
(6, 1, 1, '2026-03-01 12:00:00', 'TRANSFERENCIA_SALIDA', 50, 448, 'Transferencia a Surco', 1),
(7, 1, 2, '2026-03-01 12:00:00', 'TRANSFERENCIA_ENTRADA', 50, 150, 'Transferencia desde Central', 1),
(8, 21, 3, '2026-02-26 09:30:00', 'ENTRADA', 100, 400, 'OC-NORTE-001', 2);

SELECT setval('inventario_movimientos_id_seq', 8);

-- ============================================================================
-- PASO 18: SECUENCIA DE COMPROBANTES
-- ============================================================================

INSERT INTO sequence_comprobante (id, siguiente, version) VALUES (1, 7, 0)
ON CONFLICT (id) DO UPDATE SET siguiente = 7;

-- ============================================================================
-- PASO 19: AUDITORIA INICIAL
-- ============================================================================

INSERT INTO auditoria_acciones (id, fecha_hora, usuario, metodo, url, ip, accion, detalle) VALUES
(1, '2026-03-01 08:00:00', 'admin', 'POST', '/web/caja/abrir', '127.0.0.1', 'APERTURA_CAJA', 'Apertura de caja con monto inicial S/200.00'),
(2, '2026-03-01 09:15:00', 'carlos_ventas', 'POST', '/web/ventas', '127.0.0.1', 'NUEVA_VENTA', 'Venta #1 - Total: S/46.50 - Cliente: Juan Carlos Perez'),
(3, '2026-03-01 10:30:00', 'carlos_ventas', 'POST', '/web/ventas', '127.0.0.1', 'NUEVA_VENTA', 'Venta #2 - Total: S/85.00 - Cliente: Maria Elena Garcia'),
(4, '2026-03-01 14:00:00', 'carlos_ventas', 'POST', '/web/ventas', '127.0.0.1', 'NUEVA_VENTA', 'Venta #3 - Total: S/550.00 - Cliente: Clinica San Pablo'),
(5, '2026-03-01 20:00:00', 'admin', 'POST', '/web/caja/cerrar', '127.0.0.1', 'CIERRE_CAJA', 'Cierre de caja - Monto cierre: S/1850.50');

SELECT setval('auditoria_acciones_id_seq', 5);

-- ============================================================================
-- VERIFICACION FINAL
-- ============================================================================

DO $$
DECLARE
    v_empresas INT;
    v_usuarios INT;
    v_productos INT;
    v_clientes INT;
    v_ventas INT;
    v_proveedores INT;
BEGIN
    SELECT COUNT(*) INTO v_empresas FROM empresas;
    SELECT COUNT(*) INTO v_usuarios FROM usuarios;
    SELECT COUNT(*) INTO v_productos FROM productos;
    SELECT COUNT(*) INTO v_clientes FROM clientes;
    SELECT COUNT(*) INTO v_ventas FROM ventas;
    SELECT COUNT(*) INTO v_proveedores FROM proveedores;

    RAISE NOTICE '============================================';
    RAISE NOTICE 'RESUMEN DE DATOS INSERTADOS:';
    RAISE NOTICE '  Empresas:     %', v_empresas;
    RAISE NOTICE '  Usuarios:     %', v_usuarios;
    RAISE NOTICE '  Productos:    %', v_productos;
    RAISE NOTICE '  Clientes:     %', v_clientes;
    RAISE NOTICE '  Proveedores:  %', v_proveedores;
    RAISE NOTICE '  Ventas:       %', v_ventas;
    RAISE NOTICE '============================================';
    RAISE NOTICE 'Script ejecutado correctamente!';
    RAISE NOTICE 'Credenciales de acceso:';
    RAISE NOTICE '  admin / admin123 (Farmacia San Juan)';
    RAISE NOTICE '  norte_admin / admin123 (Botica del Norte)';
    RAISE NOTICE '  salud_admin / admin123 (Farmacia Salud Total)';
    RAISE NOTICE '============================================';
END $$;
