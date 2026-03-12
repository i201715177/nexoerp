# Proceso de suscripción y pago – NexoERP SaaS

## Cómo se suscribe un cliente (flujo actual)

Hoy el sistema **no tiene registro público**. El flujo es:

1. **El interesado te contacta** (teléfono, correo, WhatsApp) **o** llena el formulario público **Solicitar suscripción** en la URL:  
   `https://tu-dominio.com/solicitar-suscripcion`  
   Esa solicitud queda guardada y tú la ves en **Admin → Solicitudes** (`/web/admin/solicitudes`).
2. **Tú (administrador)** entras al **Panel Admin SaaS** (`/web/admin/empresas`):
   - Creas la **Empresa** (código, nombre, plan Básico/Profesional, tipo Mensual o Anual).
   - Creas al menos un **usuario** para esa empresa (por ejemplo el dueño o administrador) y le das usuario y contraseña.
3. **Le das al cliente** su usuario y contraseña (y la URL de acceso al sistema) para que entre por **Iniciar sesión** (`/login`).

Así, “suscribirse” significa: **tú das de alta la empresa y el usuario**; el cliente solo usa el sistema con las credenciales que tú le envías.

---

## Cómo sabes que el cliente pagó el alquiler

Hoy **no hay pasarela de pago integrada**. La confirmación es manual:

1. **Tú generas la factura** en **Facturación** (`/web/admin/facturacion`):  
   “Generar factura” → eliges la empresa → se crea la factura del siguiente periodo (con número F-YYYY-NNNNN).
2. **Le envías al cliente** la factura o los datos de pago (transferencia, depósito, etc.) por tu canal habitual (correo, WhatsApp, etc.).  
   *(Opcional: más adelante se puede añadir envío de PDF por correo desde el sistema.)*
3. **El cliente paga** por el medio que acordaron (transferencia, depósito, Yape, Plin, PayPal, etc.).
4. **Tú compruebas el pago** (movimiento en banco, comprobante, notificación de PayPal, etc.).
5. En **Facturación**, en la fila de esa factura, haces clic en **“Marcar como pagada”**.  
   A partir de ahí el sistema considera esa factura pagada y puedes usar el listado y los recordatorios (vencidas / por vencer) para el resto.

Resumen: **sabes que pagó porque tú mismo verificas el pago fuera del sistema y luego marcas la factura como pagada** en el panel.

---

## Opciones para mejorar el flujo

| Qué quieres | Opción | Comentario |
|-------------|--------|------------|
| Que el cliente “pida” el sistema por web | **Formulario “Solicitar suscripción”** (página pública) | **Ya implementado.** URL: `/solicitar-suscripcion`. El interesado deja nombre, empresa, plan deseado, correo/teléfono. Tú ves las solicitudes en **Admin → Solicitudes** y das de alta la empresa + usuario cuando lo decidas. |
| Saber el pago sin depender de revisar banco/correo | **Pasarela de pago** (Stripe, Mercado Pago, etc.) | El cliente paga en línea; la pasarela notifica a tu sistema y se puede marcar la factura como pagada automáticamente. Requiere desarrollo e integración. |
| Enviar la factura por correo | **Generar PDF + envío por email** | Al generar la factura (o desde un botón “Enviar por correo”) el sistema genera el PDF y manda un correo al cliente. Requiere configurar servidor de correo. |

---

## Recomendación práctica

- **Corto plazo:**  
  - Usar el flujo actual: contacto (o solicitud por `/solicitar-suscripcion`) → tú ves la solicitud en **Solicitudes** → creas empresa y usuario en Admin SaaS → generas factura → cliente paga → tú marcas “Marcar como pagada” en Facturación.  
  - La página **“Solicitar suscripción”** ya está activa; comparte el enlace para que los interesados dejen sus datos y tú los gestiones desde **Admin → Solicitudes**.
- **Más adelante:**  
  - Integrar una pasarela de pago si quieres que el pago se refleje automáticamente en el sistema.  
  - Añadir envío de factura por correo (PDF) para profesionalizar la cobranza.

Si quieres, el siguiente paso puede ser implementar solo el **formulario público “Solicitar suscripción”** y una pantalla en el panel admin para ver y gestionar esas solicitudes.
