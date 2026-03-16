# Informe de Módulos y Menús — NexoERP
## Sistema de Gestión para Farmacias y Boticas

**Documento para el cliente** — Descripción de cada menú: qué hace, de dónde salen los datos, cómo funciona y flujo de uso.

---

## 1. DASHBOARD

**¿Qué hace?**  
Pantalla principal con indicadores del negocio: ventas del día, de la semana y del mes; productos con stock bajo; productos por vencer o vencidos; cotizaciones pendientes; reclamaciones abiertas; y gráficos de ventas.

**¿De dónde salen los datos?**  
- **Ventas:** tabla `ventas` (VentaService), filtradas por fecha (hoy, últimos 7 días, últimos 30 días).  
- **Productos:** tabla `productos` (ProductoService). Se calculan: stock bajo (stock actual ≤ stock mínimo), por vencer (fecha vencimiento en 30/60/90 días), vencidos.  
- **Cotizaciones:** CotizacionService (cotizaciones en estado BORRADOR o ENVIADA).  
- **Reclamaciones:** ReclamacionService (reclamaciones no resueltas).

**Flujo:**  
El usuario entra y ve de un vistazo el estado del negocio. Desde aquí puede ir a Ventas, Productos, Requerimientos o Reportes según lo que quiera hacer.

---

## 2. PRODUCTOS

**¿Qué hace?**  
Catálogo de productos: listado con filtros (nombre, categoría, marca, estado de stock), alta/edición de productos, importación desde Excel, exportación Excel/PDF. Incluye datos DIGEMID (principio activo, lista de control, requiere receta, etc.) y detección automática si el producto está en el catálogo DIGEMID.

**¿De dónde salen los datos?**  
- **Listado:** tabla `productos` por tenant (ProductoService.listarTodos()).  
- **Marcas/categorías:** mismos productos, valores distintos para filtros.  
- **Catálogo DIGEMID:** tabla `catalogo_digemid` (CatalogoDigemidService) para la búsqueda al escribir nombre o principio activo.

**Flujo:**  
1. Ver productos → se cargan todos del tenant.  
2. Crear/editar → formulario con datos generales + DIGEMID; al escribir nombre/principio activo se consulta el catálogo DIGEMID y se sugiere clasificación.  
3. Guardar → ProductoService.crear/actualizar → se valida stock mínimo/máximo y código único.  
4. Importar Excel → ProductoImportService lee el archivo y crea/actualiza productos por código.

---

## 3. CLIENTES

**¿Qué hace?**  
Mantenimiento de clientes: documento (DNI/RUC), nombres, teléfono, email, dirección. Historial de compras por cliente. Los datos de RUC se pueden completar consultando SUNAT (API externa).

**¿De dónde salen los datos?**  
- **Listado:** tabla `clientes` por tenant (ClienteService).  
- **RUC:** SunatConsultaService (API Decolecta u otra configurada) para razon social y dirección.  
- **Historial:** VentaService por cliente.

**Flujo:**  
1. Listar clientes → ClienteService.  
2. Nuevo/editar → formulario; opcionalmente consultar RUC para auto-completar.  
3. Historial → ventas donde el cliente es el comprador.

---

## 4. PROVEEDORES

**¿Qué hace?**  
Mantenimiento de proveedores (RUC, razón social, contacto, etc.). Se usan en Compras y en Devoluciones a proveedor. RUC se puede consultar a SUNAT para auto-completar.

**¿De dónde salen los datos?**  
- Tabla `proveedores` por tenant (ProveedorService).  
- RUC: SunatConsultaService (igual que en Clientes).

**Flujo:**  
Alta/edición de proveedor → se usa al crear órdenes de compra y al registrar devoluciones.

---

## 5. COMPRAS

**¿Qué hace?**  
Órdenes de compra a proveedores: crear orden, recibir mercadería (actualiza stock), comparar precios por producto, sugerencias de compra según stock bajo.

**¿De dónde salen los datos?**  
- **Órdenes:** tabla `orden_compra` y detalle (CompraService).  
- **Proveedores:** ProveedorService.  
- **Productos:** ProductoService.  
- **Sugerencias:** productos con stock ≤ stock mínimo (CompraService.sugerenciasCompra).  
- **Comparación:** historial de precios por producto en órdenes (compraService.compararProveedoresPorProducto).

**Flujo:**  
1. Crear orden → elegir proveedor, productos y cantidades → se guarda como PENDIENTE.  
2. Recibir → se confirma recepción; se genera movimiento de inventario (entrada) y se actualiza stock.  
3. Cuentas por pagar quedan registradas en el módulo Finanzas.

---

## 6. COTIZACIONES

**¿Qué hace?**  
Presupuestos o cotizaciones para clientes: crear cotización con ítems (producto, cantidad, precio), enviar, convertir en venta o cerrar. Número automático (COT-00001, …).

**¿De dónde salen los datos?**  
- Tablas `cotizaciones` y `cotizacion_items` (CotizacionService).  
- Clientes: ClienteService.  
- Productos: ProductoService (precios).

**Flujo:**  
1. Nueva cotización → cliente + ítems (producto, cantidad, precio) → estado BORRADOR.  
2. Enviar → estado ENVIADA.  
3. Convertir en venta → se usa la cotización como base para crear una venta.  
4. Cerrar → estado CERRADA/CANCELADA.

---

## 7. VENTAS

**¿Qué hace?**  
Registro de ventas: elegir cliente (opcional), agregar ítems (producto, cantidad, precio), medios de pago, descuentos. Para productos controlados (DIGEMID) se registran datos de receta. Genera comprobante y ticket. Debe haber caja abierta para vender.

**¿De dónde salen los datos?**  
- **Productos:** ProductoService (con stock por sucursal si aplica).  
- **Clientes:** ClienteService.  
- **Caja:** CajaTurnoService (debe existir turno abierto).  
- **Ventas:** tabla `ventas`, `venta_items`, `pagos` (VentaService).  
- **Productos controlados:** DigemidService (registro de receta asociado a la venta).

**Flujo:**  
1. Usuario con caja abierta → agrega ítems y opcionalmente cliente.  
2. Para productos con “requiere receta” se piden datos de receta (médico, paciente, tipo receta).  
3. Al confirmar → se descuenta stock (InventarioService), se registra pago en el turno de caja, y si hay controlados se crea RegistroReceta.  
4. Se puede imprimir ticket o comprobante PDF.

---

## 8. CAJA

**¿Qué hace?**  
Control de caja: apertura y cierre de turno (con monto inicial y monto de cierre), ver ventas del turno, resumen por medio de pago (efectivo, tarjeta, etc.). Cada sucursal puede tener su caja.

**¿De dónde salen los datos?**  
- Tabla `caja_turno` (CajaTurnoService).  
- Ventas del turno: VentaService por cajaTurnoId.  
- Resumen por medio de pago: sumatoria de pagos agrupados por tipo.

**Flujo:**  
1. Apertura → el usuario indica monto inicial → se crea turno ABIERTO.  
2. Durante el día las ventas se asocian a ese turno.  
3. Cierre → se indica monto en caja → se compara con lo esperado (inicial + ventas) y se cierra el turno.

---

## 9. REQUERIMIENTOS

**¿Qué hace?**  
Solicitudes de reposición de productos: por sucursal, se indican productos y cantidades necesarias. Sirve para generar después órdenes de compra o transferencias. El sistema puede sugerir productos con stock bajo.

**¿De dónde salen los datos?**  
- Tabla `requerimientos` y detalle (RequerimientoService).  
- Productos: ProductoService.  
- Sucursales: SucursalService.  
- Stock bajo: productos con stock actual ≤ stock mínimo.

**Flujo:**  
1. Crear requerimiento → sucursal + ítems (producto, cantidad).  
2. Se guarda para uso interno; el usuario puede usar esa lista para compras o transferencias.

---

## 10. FINANZAS

**¿Qué hace?**  
Vista financiera: cuentas por cobrar (ventas a crédito), cuentas por pagar (compras pendientes de pago), utilidad (ventas vs costos), flujo de caja, notas de crédito.

**¿De dónde salen los datos?**  
- FinanzasService: totales y listados de CxC, CxP, utilidad.  
- Ventas: VentaService (totales, por período).  
- Compras/Cuentas por pagar: CompraService, CuentaPagar.  
- Notas de crédito: NotaCreditoService.

**Flujo:**  
Consultar saldos, vencimientos y reportes; registrar pagos de CxP o cobros de CxC según pantallas disponibles; emitir notas de crédito vinculadas a ventas.

---

## 11. INVENTARIO

**¿Qué hace?**  
Panel de inventario: productos bajo mínimo, sobre máximo, lotes por vencer, almacenes, kardex por producto, ajustes de stock, transferencias entre almacenes, recepción de compras. Exportar a Excel/PDF.

**¿De dónde salen los datos?**  
- InventarioService: productos bajo/sobre umbrales, lotes por vencimiento, almacenes, movimientos (kardex).  
- ProductoService: listado de productos.  
- Tablas: `inventario_movimientos`, `stock_almacen`, `lote_producto`, `almacen`.

**Flujo:**  
1. Dashboard inventario → ver indicadores y accesos a kardex, ajustes, transferencias.  
2. Kardex → movimientos (entrada/salida/ajuste) por producto.  
3. Ajuste → motivo + nueva cantidad → se registra movimiento y se actualiza stock.  
4. Transferencia → origen, destino, ítems → descuenta en origen, suma en destino.

---

## 12. INVENTARIO FÍSICO

**¿Qué hace?**  
Conteo físico: se crea un inventario con estado (en curso/cerrado), se cargan conteos por producto (stock sistema vs stock físico), al cerrar se calculan diferencias y se pueden aplicar ajustes automáticos.

**¿De dónde salen los datos?**  
- Tablas `inventario_fisico` e `inventario_fisico_detalle` (InventarioFisicoService).  
- Productos y stock actual: ProductoService, InventarioService.  
- Almacenes: InventarioService.

**Flujo:**  
1. Crear inventario físico → almacén, fecha.  
2. Cargar conteo por producto (stock real).  
3. Cerrar → se calculan diferencias; el usuario puede aprobar ajustes que actualizan el stock.

---

## 13. DIGEMID

**¿Qué hace?**  
Control de productos controlados (DIGEMID): stock por producto/lote, registro de entradas, recetas (manual o al vender), destrucción autorizada, distribución entre establecimientos. Alertas (stock bajo, por vencer, vencidos, exceso de venta). Reportes: movimientos, recetas, psicotrópicos, estupefacientes, lotes, distribución. Kardex especial por producto controlado.

**¿De dónde salen los datos?**  
- Stock controlado: `stock_controlado` (DigemidService).  
- Movimientos: `movimiento_controlado`.  
- Recetas: `registro_receta`.  
- Distribuciones: `distribucion_controlada`.  
- Productos controlados: ProductoService (productos con requiereReceta / tipo controlado).  
- Alertas: cálculos sobre stock, vencimientos y ventas recientes.

**Flujo:**  
1. Registrar entrada → producto, lote, vencimiento, cantidad, almacén → aumenta stock controlado y movimiento.  
2. Al vender producto controlado → se crea RegistroReceta (desde venta o manual).  
3. Destrucción → producto, cantidad, acta → baja stock y registra movimiento.  
4. Distribución → producto, origen, destino, cliente (RUC, etc.) → movimiento entre almacenes/establecimientos.  
5. Reportes y kardex para auditoría y normativa.

---

## 14. MERMAS

**¿Qué hace?**  
Registro de mermas (pérdidas): producto, cantidad, motivo (vencido, dañado, rotura, error inventario, destrucción autorizada, etc.), lote, stock antes/después. Para controlados: responsable, aprobación QF, acta de destrucción. Reportes por producto y por período; acta de destrucción imprimible.

**¿De dónde salen los datos?**  
- Tabla `mermas` (MermaService).  
- Productos: ProductoService.  
- Al registrar merma se descuenta stock (InventarioService) y se registra movimiento en kardex.

**Flujo:**  
1. Registrar merma → producto, cantidad, motivo, lote, observaciones; si es controlado, datos adicionales.  
2. Sistema valida stock, descuenta y guarda movimiento.  
3. Reportes y acta de destrucción para cumplimiento.

---

## 15. TEMPERATURA

**¿Qué hace?**  
Control de almacenamiento: zonas con rango de temperatura/humedad, registro de lecturas. Alertas cuando la lectura sale del rango configurado.

**¿De dónde salen los datos?**  
- Tablas `zona_almacen` y `registro_temperatura` (TemperaturaService).  
- Datos ingresados por el usuario (no hay integración con sensores externos en el código base).

**Flujo:**  
Definir zonas → registrar lecturas periódicas → consultar historial y alertas.

---

## 16. BPA (BUENAS PRÁCTICAS DE ALMACENAMIENTO)

**¿Qué hace?**  
Checklists de Buenas Prácticas de Almacenamiento: crear checklist con ítems (cumple/no cumple), resultado global, código automático. Sirve para auditorías y trazabilidad.

**¿De dónde salen los datos?**  
- Tabla `checklist_bpa` (BpaService).  
- Ítems y resultado calculado según respuestas.

**Flujo:**  
Crear checklist → responder ítems → guardar; consultar listado e imprimir si se necesita.

---

## 17. FACTURACIÓN ELECTRÓNICA

**¿Qué hace?**  
Configuración y emisión de comprobantes electrónicos (facturas, boletas) hacia SUNAT. Incluye configuración de certificado digital y parámetros del emisor. Depende de integración con servicio SUNAT (demo o producción).

**¿De dónde salen los datos?**  
- Empresa/emisor: EmpresaService.  
- Configuración SUNAT y certificado: según implementación (SunatIntegrationService, configuración en BD o archivos).  
- Ventas: para generar el comprobante electrónico asociado.

**Flujo:**  
Configurar emisor y certificado → desde ventas (o proceso batch) se emite comprobante electrónico y se envía a SUNAT según configuración.

---

## 18. GUÍAS DE REMISIÓN

**¿Qué hace?**  
Emisión de guías de remisión para traslado de mercadería (entre almacenes o a clientes). Datos del transportista, origen, destino, ítems.

**¿De dónde salen los datos?**  
- Tablas de guías y detalle (GuiaRemisionService).  
- Productos, almacenes, clientes/transportistas según el flujo.

**Flujo:**  
Crear guía → origen, destino, ítems, transportista → numeración y documento según normativa.

---

## 19. DEVOLUCIONES A PROVEEDOR

**¿Qué hace?**  
Devolución de productos al proveedor: número de devolución, proveedor, ítems (producto, cantidad, motivo, lote). Al enviar la devolución se descuenta el stock. Se puede registrar respuesta del proveedor.

**¿De dónde salen los datos?**  
- Tablas `devolucion_proveedor` y `devolucion_proveedor_items` (DevolucionProveedorService).  
- Proveedores: ProveedorService.  
- Productos y stock: ProductoService, InventarioService.

**Flujo:**  
1. Crear devolución → proveedor + ítems.  
2. Enviar → se descuenta stock y se registra movimiento.  
3. Opcional: registrar respuesta (aceptada/rechazada).

---

## 20. REPORTES

**¿Qué hace?**  
Reportes de negocio: ventas por período, utilidad, cuentas por cobrar/pagar, productos más vendidos, sin rotación, ranking vendedores, márgenes por producto. Filtros por fechas. Exportación Excel/PDF.

**¿De dónde salen los datos?**  
- FinanzasService: totales ventas, utilidad, CxC, CxP.  
- VentaService: ventas por período, por vendedor, ítems más vendidos.  
- ProductoService / movimientos: rotación, márgenes.  
- CompraService: CxP.

**Flujo:**  
Seleccionar período → ver métricas y tablas en pantalla → opcionalmente exportar.

---

## 21. ANALYTICS

**¿Qué hace?**  
Análisis avanzado con gráficos: ventas mensuales, análisis ABC de productos, margen por categoría, etc. (según implementación con Chart.js u otra librería).

**¿De dónde salen los datos?**  
- VentaService.listarEntreFechas y agregaciones.  
- Productos y categorías: ProductoService.  
- Cálculos de margen y ABC en el backend (p. ej. AnalyticsWebController).

**Flujo:**  
Seleccionar rango de fechas → el servidor devuelve datos agregados → el navegador dibuja gráficos.

---

## 22. SUCURSALES

**¿Qué hace?**  
Mantenimiento de sucursales (nombre, dirección, etc.) y opcionalmente inventario/ventas por sucursal. Permite filtrar caja, requerimientos y reportes por sucursal.

**¿De dónde salen los datos?**  
- Tabla `sucursales` (SucursalService).  
- Por sucursal: CajaTurnoService, RequerimientoService, stock si está implementado por sucursal.

**Flujo:**  
Alta/edición de sucursales; los usuarios pueden estar asignados a una sucursal para restringir ventas/caja a esa sucursal.

---

## 23. CATÁLOGOS

**¿Qué hace?**  
Maestros reutilizables: categorías de productos, laboratorios, presentaciones. Se usan al dar de alta productos para mantener datos uniformes.

**¿De dónde salen los datos?**  
- Tablas `categoria`, `laboratorio`, `presentacion` (CatalogoService).  
- Los productos referencian (o copian) estos valores.

**Flujo:**  
Mantenimiento de categorías, laboratorios y presentaciones; al crear/editar producto se pueden elegir de estas listas.

---

## 24. LISTAS DE PRRECIO

**¿Qué hace?**  
Listas de precios por tipo de cliente (ej. mayorista, minorista): se define una lista y se asignan productos con precio. En ventas se puede elegir la lista según el cliente para sugerir precios.

**¿De dónde salen los datos?**  
- Tablas `lista_precio` y `lista_precio_detalle` (ListaPrecioService).  
- Productos: ProductoService.

**Flujo:**  
Crear lista → agregar productos con precio → activar lista; en venta se selecciona cliente y opcionalmente lista para aplicar precios.

---

## 25. USUARIOS

**¿Qué hace?**  
Administración de usuarios del tenant: crear, editar, asignar roles (ADMIN, VENDEDOR, QUIMICO_FARMACEUTICO, etc.) y sucursal. Solo usuarios con rol ADMIN/SAAS_ADMIN.

**¿De dónde salen los datos?**  
- Tabla de usuarios (y roles) del sistema (UsuarioService).  
- Sucursales: SucursalService.  
- Multi-tenant: cada usuario pertenece a un tenant (empresa).

**Flujo:**  
Alta/edición de usuario → roles y sucursal → el usuario inicia sesión y el sistema aplica permisos según rol.

---

## 26. MI EMPRESA (CONFIGURACIÓN)

**¿Qué hace?**  
Datos de la empresa del tenant: nombre, RUC, dirección, teléfono, logo, etc. Usado en comprobantes, reportes y facturación electrónica.

**¿De dónde salen los datos?**  
- Tabla `empresa` (o equivalente por tenant) (EmpresaService.guardarDirecto).  
- Se editan desde esta pantalla y se usan en todo el sistema.

**Flujo:**  
Editar datos → guardar; los cambios se reflejan en tickets, PDFs y configuración SUNAT.

---

## 27. RECLAMACIONES (LIBRO DE RECLAMACIONES)

**¿Qué hace?**  
Libro de reclamaciones según normativa peruana: registro de reclamaciones (cliente, documento, detalle, fecha), número automático, estado (abierta/resuelta) y respuesta.

**¿De dónde salen los datos?**  
- Tabla `reclamaciones` (ReclamacionService).  
- Datos ingresados por el usuario o por el cliente (si hay formulario público).

**Flujo:**  
Registrar reclamación → asignar número → dar respuesta y marcar como resuelta cuando corresponda.

---

## 28. BACKUP

**¿Qué hace?**  
Exportación de datos clave a CSV (productos, clientes, ventas, etc.) para respaldo o análisis externo. No reemplaza el backup de base de datos del Panel Sistema.

**¿De dónde salen los datos?**  
- ProductoService, ClienteService, VentaService, etc.  
- BackupWebController arma los CSV y los devuelve para descarga.

**Flujo:**  
Usuario solicita exportar → se generan archivos CSV → descarga.

---

## 29. PANEL SISTEMA

**¿Qué hace?**  
Panel técnico para administradores: estado del sistema (salud de BD, memoria, disco), listado de cachés, backups de base de datos (crear backup manual, ver últimos), limpiar caché. Solo ADMIN/SAAS_ADMIN.

**¿De dónde salen los datos?**  
- Health checks (Actuator), DataSource, CacheManager.  
- BackupScheduler: ejecuta backup (p. ej. H2 SCRIPT) y lista archivos en carpeta de backups.

**Flujo:**  
Ver estado → crear backup si se desea → limpiar caché si hace falta.

---

## 30. AUDITORÍA

**¿Qué hace?**  
Registro de acciones importantes: quién, cuándo, qué entidad (producto, venta, etc.) y qué cambio (alta, edición, eliminación). Consulta por fecha y tipo para cumplimiento y control.

**¿De dónde salen los datos?**  
- Tabla de auditoría (AuditoriaService).  
- Se alimenta desde interceptores o servicios que registran cambios (productos, ventas, etc.).

**Flujo:**  
Consultar registros por rango de fechas y filtros; exportar si está implementado.

---

## 31. ADMIN SAAS

**¿Qué hace?**  
Solo para rol SAAS_ADMIN: gestión de empresas (tenants), solicitudes de suscripción, facturación SaaS, planes. Permite cambiar de empresa desde el selector del sidebar.

**¿De dónde salen los datos?**  
- EmpresaService, SolicitudSuscripcionService, FacturaSaaSService, PlanSuscripcionService, SaasAdminService.  
- Tablas de empresas, planes, solicitudes, facturación SaaS.

**Flujo:**  
Gestionar empresas, aprobar solicitudes, configurar planes y facturación; el selector de empresa en el menú permite “cambiar de tenant” para operar como otra farmacia.

---

## Resumen de orígenes de datos

| Origen | Uso principal |
|--------|----------------|
| **Base de datos (por tenant)** | Productos, clientes, proveedores, ventas, compras, inventario, DIGEMID, mermas, cotizaciones, caja, requerimientos, reclamaciones, usuarios, empresa, etc. |
| **SUNAT (API externa)** | RUC: razón social, dirección (Clientes, Proveedores). |
| **Catálogo DIGEMID (interno)** | Auto-clasificación de productos al dar de alta (principio activo, lista, receta). |
| **Usuario (entrada manual)** | Todos los formularios; lecturas de temperatura; checklists BPA. |
| **Archivos Excel** | Importación de productos; exportaciones en Reportes, Backup, Inventario. |

---

*Documento generado para NexoERP — Sistema de gestión para farmacias y boticas. Cada menú está descrito en función del código actual del proyecto.*
