package com.farmacia.sistema.export;

import com.farmacia.sistema.domain.empresa.Empresa;
import com.farmacia.sistema.domain.venta.PagoVenta;
import com.farmacia.sistema.domain.venta.Venta;
import com.farmacia.sistema.domain.venta.VentaItem;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera comprobantes de venta en PDF:
 * - Boleta: diseño tipo comprobante de pago (cabecera, emisor, cliente, detalle, APROBADO).
 * - Factura (cliente con RUC): plantilla tipo factura formal (FACTURAR A, ítems, subtotal/IGV/total, pie con contacto).
 */
public final class ComprobantePdfUtil {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("hh:mm a");

    private static final Color COLOR_HEADER = new Color(30, 64, 175);
    private static final Color COLOR_HEADER_VERDE = new Color(22, 163, 74);
    private static final Color COLOR_APROBADO = new Color(22, 163, 74);
    private static final Color COLOR_ANULADO = new Color(220, 38, 38);
    private static final Color COLOR_FONDO_SECCION = new Color(248, 250, 252);
    private static final Color COLOR_BORDE = new Color(226, 232, 240);
    private static final Color COLOR_TEXTO_MONTO = new Color(22, 163, 74);
    private static final Color COLOR_PIE = new Color(100, 116, 139);
    private static final Color COLOR_TITULO_FACTURA = new Color(30, 64, 175);
    /** Diseño factura: encabezado y pie gris oscuro (#2C2E34) */
    private static final Color COLOR_FACTURA_GRIS = new Color(44, 46, 52);
    /** Acento logo factura (#FDC830) */
    private static final Color COLOR_FACTURA_AMARILLO = new Color(253, 200, 48);

    private static final Font FONT_HEADER = new Font(Font.HELVETICA, 14, Font.BOLD, Color.WHITE);
    private static final Font FONT_ETIQUETA = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
    private static final Font FONT_VALOR = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);
    private static final Font FONT_NORMAL = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
    private static final Font FONT_PEQUEÑO = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);
    private static final Font FONT_APROBADO = new Font(Font.HELVETICA, 12, Font.BOLD, Color.WHITE);
    private static final Font FONT_ANULADO = new Font(Font.HELVETICA, 12, Font.BOLD, Color.WHITE);
    private static final Font FONT_TITULO_FACTURA = new Font(Font.HELVETICA, 22, Font.BOLD, COLOR_TITULO_FACTURA);

    private ComprobantePdfUtil() {}

    public static void generarComprobante(OutputStream out, Empresa empresa, Venta venta) {
        Document doc = new Document(PageSize.A5, 24, 24, 20, 20);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            boolean esFactura = "FAC".equals(venta.getTipoComprobante());
            if (esFactura) {
                doc.addTitle("Factura");
                generarFacturaPlantilla(doc, empresa, venta);
                return;
            }

            doc.addTitle("Comprobante");
            // ----- Flujo Boleta / Comprobante -----
            boolean anulada = "ANULADA".equals(venta.getEstado());
            String comprobanteStr = comprobanteCompleto(venta);
            String fecha = venta.getFechaHora() != null ? venta.getFechaHora().format(FMT_FECHA) : "";
            String hora = venta.getFechaHora() != null ? venta.getFechaHora().format(FMT_HORA) : "";
            BigDecimal total = venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO;
            String nombreCliente = venta.getNombreClienteVenta() != null ? venta.getNombreClienteVenta().trim() : "Público general";
            String docCliente = "";
            if (venta.getCliente() != null) {
                String tipo = venta.getCliente().getTipoDocumento() != null ? venta.getCliente().getTipoDocumento() : "DNI";
                String num = venta.getCliente().getNumeroDocumento() != null ? venta.getCliente().getNumeroDocumento() : "";
                docCliente = tipo + ": " + num;
            }

            PdfPTable barraHeader = new PdfPTable(1);
            barraHeader.setWidthPercentage(100);
            barraHeader.setSpacingAfter(0);
            PdfPCell cellHeader = new PdfPCell(new Phrase("COMPROBANTE", FONT_HEADER));
            cellHeader.setBackgroundColor(COLOR_HEADER);
            cellHeader.setPadding(12);
            cellHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellHeader.setBorder(Rectangle.NO_BORDER);
            cellHeader.setBorderColorBottom(COLOR_HEADER_VERDE);
            cellHeader.setBorderWidthBottom(3);
            barraHeader.addCell(cellHeader);
            doc.add(barraHeader);
            doc.add(new Paragraph(" "));

            PdfPTable filaDatos = new PdfPTable(3);
            filaDatos.setWidthPercentage(100);
            filaDatos.setWidths(new float[]{1.2f, 1.2f, 1.6f});
            filaDatos.setSpacingAfter(8);
            añadirCeldaDato(filaDatos, "Fecha", fecha);
            añadirCeldaDato(filaDatos, "Hora", hora);
            añadirCeldaDato(filaDatos, "N° Comprobante", comprobanteStr);
            doc.add(filaDatos);

            PdfPTable tablaEmisor = new PdfPTable(1);
            tablaEmisor.setWidthPercentage(100);
            tablaEmisor.setSpacingAfter(6);
            String nomEmpresa = empresa.getNombre() != null ? empresa.getNombre() : "Farmacia";
            añadirCeldaSeccion(tablaEmisor, "Emisor:", nomEmpresa);
            if (empresa.getRuc() != null && !empresa.getRuc().isBlank()) {
                añadirCeldaSeccion(tablaEmisor, null, "RUC: " + empresa.getRuc());
            }
            if (empresa.getDireccion() != null && !empresa.getDireccion().isBlank()) {
                añadirCeldaSeccion(tablaEmisor, null, empresa.getDireccion());
            }
            if (empresa.getTelefono() != null && !empresa.getTelefono().isBlank()) {
                añadirCeldaSeccion(tablaEmisor, null, "Tel: " + empresa.getTelefono());
            }
            doc.add(tablaEmisor);

            PdfPTable tablaCliente = new PdfPTable(1);
            tablaCliente.setWidthPercentage(100);
            tablaCliente.setSpacingAfter(8);
            añadirCeldaSeccion(tablaCliente, "Cliente:", nombreCliente);
            if (!docCliente.isEmpty()) añadirCeldaSeccion(tablaCliente, null, docCliente);
            if (venta.getCliente() != null && venta.getCliente().getDireccion() != null && !venta.getCliente().getDireccion().isBlank()) {
                añadirCeldaSeccion(tablaCliente, null, "Dirección: " + venta.getCliente().getDireccion());
            }
            doc.add(tablaCliente);

            doc.add(new Paragraph("Detalle de productos", new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK)));
            PdfPTable tablaItems = new PdfPTable(5);
            tablaItems.setWidthPercentage(100);
            tablaItems.setWidths(new float[]{0.8f, 3.5f, 1f, 1.2f, 1.5f});
            tablaItems.setSpacingBefore(4);
            tablaItems.setSpacingAfter(6);
            añadirCeldaTabla(tablaItems, "#", true);
            añadirCeldaTabla(tablaItems, "Descripción", true);
            añadirCeldaTabla(tablaItems, "Cantidad", true);
            añadirCeldaTabla(tablaItems, "P. unit.", true);
            añadirCeldaTabla(tablaItems, "Subtotal", true);
            List<VentaItem> itemsB = venta.getItems();
            if (itemsB != null) {
                int n = 1;
                for (VentaItem it : itemsB) {
                    añadirCeldaTabla(tablaItems, String.valueOf(n++), false);
                    String nombreProd = it.getProducto() != null ? it.getProducto().getNombre() : "?";
                    if (nombreProd.length() > 40) nombreProd = nombreProd.substring(0, 37) + "...";
                    añadirCeldaTabla(tablaItems, nombreProd, false);
                    añadirCeldaTabla(tablaItems, String.valueOf(it.getCantidad() != null ? it.getCantidad() : 0), false);
                    añadirCeldaTabla(tablaItems, "S/ " + formatSoles(it.getPrecioUnitario()), false);
                    añadirCeldaTabla(tablaItems, "S/ " + formatSoles(it.getSubtotal()), false);
                }
            }
            doc.add(tablaItems);

            PdfPTable tablaResumen = new PdfPTable(2);
            tablaResumen.setWidthPercentage(100);
            tablaResumen.setWidths(new float[]{1f, 1f});
            tablaResumen.setSpacingAfter(8);
            PdfPCell cellMonto = new PdfPCell(new Phrase("Monto total:", FONT_ETIQUETA));
            cellMonto.setBorder(Rectangle.NO_BORDER);
            cellMonto.setPadding(2);
            tablaResumen.addCell(cellMonto);
            PdfPCell cellMontoValor = new PdfPCell(new Phrase("S/ " + formatSoles(total), new Font(Font.HELVETICA, 12, Font.BOLD, COLOR_TEXTO_MONTO)));
            cellMontoValor.setBorder(Rectangle.NO_BORDER);
            cellMontoValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellMontoValor.setPadding(2);
            tablaResumen.addCell(cellMontoValor);
            añadirFilaResumen(tablaResumen, "Concepto:", "Venta de productos");
            añadirFilaResumen(tablaResumen, "Titular:", nombreCliente);
            if (!docCliente.isEmpty()) añadirFilaResumen(tablaResumen, "Documento:", docCliente);
            List<PagoVenta> pagos = venta.getPagos();
            if (pagos != null && !pagos.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (PagoVenta p : pagos) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(labelMedio(p.getMedioPago())).append(" S/ ").append(formatSoles(p.getMonto()));
                }
                añadirFilaResumen(tablaResumen, "Forma de pago:", sb.toString());
            }
            doc.add(tablaResumen);

            PdfPTable tablaEstado = new PdfPTable(1);
            tablaEstado.setWidthPercentage(100);
            tablaEstado.setSpacingBefore(6);
            tablaEstado.setSpacingAfter(8);
            PdfPCell cellEstado = new PdfPCell(new Phrase(anulada ? "  ANULADO  " : "  APROBADO  ", anulada ? FONT_ANULADO : FONT_APROBADO));
            cellEstado.setBackgroundColor(anulada ? COLOR_ANULADO : COLOR_APROBADO);
            cellEstado.setPadding(10);
            cellEstado.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellEstado.setBorder(Rectangle.NO_BORDER);
            tablaEstado.addCell(cellEstado);
            doc.add(tablaEstado);

            PdfPTable barraPie = new PdfPTable(1);
            barraPie.setWidthPercentage(100);
            barraPie.setSpacingBefore(6);
            PdfPCell cellPieBar = new PdfPCell(new Phrase(" "));
            cellPieBar.setBackgroundColor(COLOR_BORDE);
            cellPieBar.setFixedHeight(8);
            cellPieBar.setBorder(Rectangle.NO_BORDER);
            barraPie.addCell(cellPieBar);
            doc.add(barraPie);
            Paragraph mensajePie = new Paragraph("Transacción realizada exitosamente. Conserve este comprobante como respaldo de su operación.",
                    new Font(Font.HELVETICA, 8, Font.NORMAL, COLOR_PIE));
            mensajePie.setAlignment(Element.ALIGN_CENTER);
            mensajePie.setSpacingBefore(6);
            doc.add(mensajePie);
            Paragraph nexoSistema = new Paragraph("Documento generado por NexoERP", new Font(Font.HELVETICA, 6, Font.ITALIC, Color.LIGHT_GRAY));
            nexoSistema.setAlignment(Element.ALIGN_CENTER);
            doc.add(nexoSistema);

        } catch (DocumentException e) {
            throw new RuntimeException("Error generando comprobante PDF", e);
        } finally {
            doc.close();
        }
    }

    /**
     * Genera el PDF de Factura con plantilla idéntica al modelo: encabezado gris oscuro (FACTURA + logo/empresa),
     * barra con Nº factura y fechas, FACTURAR A + fechas, tabla ítems, pie gris con totales y detalle de pago.
     */
    private static void generarFacturaPlantilla(Document doc, Empresa empresa, Venta venta) throws DocumentException {
        boolean anulada = "ANULADA".equals(venta.getEstado());
        String comprobanteStr = comprobanteCompleto(venta);
        String fecha = venta.getFechaHora() != null ? venta.getFechaHora().format(FMT_FECHA) : "";
        String fechaVenc = fecha;
        try {
            if (venta.getFechaHora() != null) {
                fechaVenc = venta.getFechaHora().plusDays(8).format(FMT_FECHA);
            }
        } catch (Exception ignored) {}
        BigDecimal total = venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO;
        java.math.RoundingMode rm = java.math.RoundingMode.HALF_UP;
        BigDecimal baseImponible = total.divide(new BigDecimal("1.18"), 2, rm);
        BigDecimal igv = total.subtract(baseImponible);

        String nomEmpresa = (empresa.getNombre() != null && !empresa.getNombre().isBlank())
                ? empresa.getNombre().trim().toUpperCase() : "EMPRESA S.A.";
        String dirEmpresa = empresa.getDireccion() != null ? empresa.getDireccion() : "";
        String telEmpresa = empresa.getTelefono() != null ? empresa.getTelefono() : "";
        String rucEmpresa = empresa.getRuc() != null ? empresa.getRuc() : "";
        String nombreCliente = venta.getNombreClienteVenta() != null ? venta.getNombreClienteVenta().trim() : "";
        String rucCliente = "";
        String dirCliente = "";
        String telCliente = "";
        String soloNombreCliente = "";
        if (venta.getCliente() != null) {
            String nombres = venta.getCliente().getNombres() != null ? venta.getCliente().getNombres().trim() : "";
            String apellidos = venta.getCliente().getApellidos() != null ? venta.getCliente().getApellidos().trim() : "";
            soloNombreCliente = (nombres + " " + apellidos).trim();
            if (soloNombreCliente.isEmpty()) soloNombreCliente = nombreCliente;
            rucCliente = venta.getCliente().getNumeroDocumento() != null ? venta.getCliente().getNumeroDocumento() : "";
            dirCliente = venta.getCliente().getDireccion() != null ? venta.getCliente().getDireccion() : "";
            telCliente = venta.getCliente().getTelefono() != null ? venta.getCliente().getTelefono() : "";
        } else {
            soloNombreCliente = nombreCliente.replaceAll("\\s*\\(RUC\\s+[0-9]+\\)\\s*$", "").replaceAll("\\s*\\(DNI\\s+[0-9]+\\)\\s*$", "").trim();
            if (soloNombreCliente.isEmpty()) soloNombreCliente = nombreCliente;
        }

        Font fontBlancoBold = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        Font fontBlanco = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.WHITE);
        Font fontGrisEtiqueta = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);

        // ----- Encabezado: fondo gris oscuro. Izq: FACTURA. Der: logo (amarillo) + EMPRESA S.A. + dirección + teléfono -----
        PdfPTable encabezado = new PdfPTable(2);
        encabezado.setWidthPercentage(100);
        encabezado.setWidths(new float[]{1f, 1.4f});
        encabezado.setSpacingAfter(0);
        PdfPCell cellTitulo = new PdfPCell(new Phrase("FACTURA", new Font(Font.HELVETICA, 22, Font.BOLD, Color.WHITE)));
        cellTitulo.setBackgroundColor(COLOR_FACTURA_GRIS);
        cellTitulo.setBorder(Rectangle.NO_BORDER);
        cellTitulo.setPadding(20);
        cellTitulo.setVerticalAlignment(Element.ALIGN_TOP);
        encabezado.addCell(cellTitulo);
        PdfPTable tablaEmpresa = new PdfPTable(2);
        tablaEmpresa.setWidthPercentage(100);
        tablaEmpresa.setWidths(new float[]{0.2f, 1f});
        PdfPCell cellLogo = new PdfPCell(new Phrase("Z", new Font(Font.HELVETICA, 18, Font.BOLD, COLOR_FACTURA_GRIS)));
        cellLogo.setBackgroundColor(COLOR_FACTURA_AMARILLO);
        cellLogo.setBorder(Rectangle.NO_BORDER);
        cellLogo.setPadding(10);
        cellLogo.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tablaEmpresa.addCell(cellLogo);
        StringBuilder empText = new StringBuilder(nomEmpresa).append("\n");
        if (!dirEmpresa.isEmpty()) empText.append(dirEmpresa).append("\n");
        if (!telEmpresa.isEmpty()) empText.append("Teléfono: ").append(telEmpresa);
        PdfPCell cellEmpresaDatos = new PdfPCell(new Phrase(empText.toString().trim(), fontBlanco));
        cellEmpresaDatos.setBackgroundColor(COLOR_FACTURA_GRIS);
        cellEmpresaDatos.setBorder(Rectangle.NO_BORDER);
        cellEmpresaDatos.setPadding(8);
        cellEmpresaDatos.setVerticalAlignment(Element.ALIGN_TOP);
        tablaEmpresa.addCell(cellEmpresaDatos);
        PdfPCell wrapEmpresa = new PdfPCell(tablaEmpresa);
        wrapEmpresa.setBackgroundColor(COLOR_FACTURA_GRIS);
        wrapEmpresa.setBorder(Rectangle.NO_BORDER);
        wrapEmpresa.setPadding(12);
        wrapEmpresa.setHorizontalAlignment(Element.ALIGN_RIGHT);
        encabezado.addCell(wrapEmpresa);
        doc.add(encabezado);

        // ----- Barra: Factura A: FCT-xxx (izq) | Fechas una debajo de la otra (der) -----
        PdfPTable barraInfo = new PdfPTable(2);
        barraInfo.setWidthPercentage(100);
        barraInfo.setWidths(new float[]{1f, 1f});
        barraInfo.setSpacingAfter(0);
        PdfPCell cellNumFact = new PdfPCell(new Phrase("Factura A: " + comprobanteStr, fontBlancoBold));
        cellNumFact.setBackgroundColor(COLOR_FACTURA_GRIS);
        cellNumFact.setBorder(Rectangle.NO_BORDER);
        cellNumFact.setPadding(10);
        barraInfo.addCell(cellNumFact);
        Paragraph pBarraFechas = new Paragraph();
        pBarraFechas.add(new Chunk("FECHA EMISIÓN: ", new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(220, 220, 220))));
        pBarraFechas.add(new Chunk(fecha + "\n", fontBlancoBold));
        pBarraFechas.add(new Chunk("FECHA VENCIMIENTO: ", new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(220, 220, 220))));
        pBarraFechas.add(new Chunk(fechaVenc, fontBlancoBold));
        PdfPCell cellFechas = new PdfPCell(pBarraFechas);
        cellFechas.setBackgroundColor(COLOR_FACTURA_GRIS);
        cellFechas.setBorder(Rectangle.NO_BORDER);
        cellFechas.setPadding(10);
        cellFechas.setHorizontalAlignment(Element.ALIGN_RIGHT);
        barraInfo.addCell(cellFechas);
        doc.add(barraInfo);

        // ----- Sección blanca: solo FACTURAR A (nombre, RUC, Dirección, teléfono). Fechas ya van arriba, no se repiten -----
        PdfPTable seccionCliente = new PdfPTable(1);
        seccionCliente.setWidthPercentage(100);
        seccionCliente.setSpacingAfter(8);
        PdfPCell cellFacturarA = new PdfPCell();
        cellFacturarA.setBorder(Rectangle.NO_BORDER);
        cellFacturarA.setPadding(4);
        Paragraph pCliente = new Paragraph();
        pCliente.add(new Chunk("FACTURAR A:\n", fontGrisEtiqueta));
        pCliente.add(new Chunk((soloNombreCliente.isEmpty() ? "—" : soloNombreCliente) + "\n", new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK)));
        if (!rucCliente.isEmpty()) pCliente.add(new Chunk("RUC: " + rucCliente + "\n", FONT_NORMAL));
        if (!dirCliente.isEmpty()) pCliente.add(new Chunk("Dirección: " + dirCliente + "\n", FONT_NORMAL));
        if (!telCliente.isEmpty()) pCliente.add(new Chunk("Número: " + telCliente, FONT_NORMAL));
        cellFacturarA.addElement(pCliente);
        seccionCliente.addCell(cellFacturarA);
        doc.add(seccionCliente);

        // ----- Tabla ítems: cabecera gris oscuro (Descripción, Cantidad, Precio unitario, Total). Cantidad centrada -----
        PdfPTable tablaItems = new PdfPTable(4);
        tablaItems.setWidthPercentage(100);
        tablaItems.setWidths(new float[]{3f, 0.9f, 1.2f, 1.2f});
        tablaItems.setSpacingBefore(2);
        tablaItems.setSpacingAfter(4);
        añadirCeldaFacturaHeader(tablaItems, "Descripción", Element.ALIGN_LEFT);
        añadirCeldaFacturaHeader(tablaItems, "Cantidad", Element.ALIGN_CENTER);
        añadirCeldaFacturaHeader(tablaItems, "Precio unitario", Element.ALIGN_CENTER);
        añadirCeldaFacturaHeader(tablaItems, "Total", Element.ALIGN_RIGHT);
        List<VentaItem> items = venta.getItems();
        if (items != null) {
            for (VentaItem it : items) {
                String nombreProd = it.getProducto() != null ? it.getProducto().getNombre() : "?";
                if (nombreProd.length() > 45) nombreProd = nombreProd.substring(0, 42) + "...";
                añadirCeldaFacturaFila(tablaItems, nombreProd, false);
                añadirCeldaFacturaFila(tablaItems, String.valueOf(it.getCantidad() != null ? it.getCantidad() : 0), true);
                añadirCeldaFacturaFila(tablaItems, "S/ " + formatSoles(it.getPrecioUnitario()), true);
                añadirCeldaFacturaFila(tablaItems, "S/ " + formatSoles(it.getSubtotal()), true);
            }
        }
        doc.add(tablaItems);

        // ----- Pie gris oscuro: izq = Detalle de pago (lo que registró el sistema). der = Subtotal, IVA, Total + método de pago -----
        String detallePagoTexto = textoDetallePagoDesdeVenta(venta);
        PdfPTable pieTotales = new PdfPTable(2);
        pieTotales.setWidthPercentage(100);
        pieTotales.setWidths(new float[]{1.2f, 1f});
        pieTotales.setSpacingAfter(0);
        PdfPCell cellPago = new PdfPCell();
        cellPago.setBackgroundColor(COLOR_FACTURA_GRIS);
        cellPago.setBorder(Rectangle.NO_BORDER);
        cellPago.setPadding(16);
        Paragraph pPago = new Paragraph();
        pPago.add(new Chunk("DETALLE DE PAGO\n", new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(200, 200, 200))));
        pPago.add(new Chunk(detallePagoTexto.isEmpty() ? "—" : detallePagoTexto, fontBlanco));
        cellPago.addElement(pPago);
        pieTotales.addCell(cellPago);
        PdfPTable tablaTotalesDerecha = new PdfPTable(2);
        tablaTotalesDerecha.setWidthPercentage(100);
        tablaTotalesDerecha.setWidths(new float[]{1f, 1f});
        añadirFilaTotalPie(tablaTotalesDerecha, "Subtotal", "S/ " + formatSoles(baseImponible), fontBlanco);
        añadirFilaTotalPie(tablaTotalesDerecha, "IVA (18%)", "S/ " + formatSoles(igv), fontBlanco);
        añadirFilaTotalPie(tablaTotalesDerecha, "Total a pagar", "S/ " + formatSoles(total), new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE));
        Paragraph pMetodo = new Paragraph();
        pMetodo.add(new Chunk("MÉTODO DE PAGO\n", new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(200, 200, 200))));
        pMetodo.add(new Chunk(detallePagoTexto.isEmpty() ? "—" : detallePagoTexto, fontBlanco));
        pMetodo.setSpacingBefore(14);
        PdfPCell cellTotales = new PdfPCell();
        cellTotales.setBackgroundColor(COLOR_FACTURA_GRIS);
        cellTotales.setBorder(Rectangle.NO_BORDER);
        cellTotales.setPadding(16);
        cellTotales.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellTotales.addElement(tablaTotalesDerecha);
        cellTotales.addElement(pMetodo);
        pieTotales.addCell(cellTotales);
        doc.add(pieTotales);

        if (anulada) {
            PdfPTable tablaAnul = new PdfPTable(1);
            tablaAnul.setWidthPercentage(100);
            tablaAnul.setSpacingBefore(6);
            PdfPCell cellAnul = new PdfPCell(new Phrase("  ANULADO  ", FONT_ANULADO));
            cellAnul.setBackgroundColor(COLOR_ANULADO);
            cellAnul.setPadding(8);
            cellAnul.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellAnul.setBorder(Rectangle.NO_BORDER);
            tablaAnul.addCell(cellAnul);
            doc.add(tablaAnul);
        }

        // ----- Gracias por su preferencia (gris oscuro, itálica) -----
        PdfPTable pieGracias = new PdfPTable(1);
        pieGracias.setWidthPercentage(100);
        PdfPCell cellGracias = new PdfPCell(new Phrase("Gracias por su preferencia", new Font(Font.HELVETICA, 11, Font.ITALIC, Color.WHITE)));
        cellGracias.setBackgroundColor(COLOR_FACTURA_GRIS);
        cellGracias.setBorder(Rectangle.NO_BORDER);
        cellGracias.setPadding(12);
        pieGracias.addCell(cellGracias);
        doc.add(pieGracias);
    }

    private static void añadirCeldaFacturaHeader(PdfPTable tabla, String texto, int alignment) {
        PdfPCell c = new PdfPCell(new Phrase(texto, new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
        c.setBackgroundColor(COLOR_FACTURA_GRIS);
        c.setPadding(8);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(alignment);
        tabla.addCell(c);
    }

    /** Construye el texto de detalle/método de pago desde los pagos registrados en la venta. */
    private static String textoDetallePagoDesdeVenta(Venta venta) {
        List<PagoVenta> pagos = venta.getPagos();
        if (pagos == null || pagos.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (PagoVenta p : pagos) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(labelMedio(p.getMedioPago())).append(" S/ ").append(formatSoles(p.getMonto()));
        }
        return sb.toString();
    }

    private static void añadirCeldaFacturaFila(PdfPTable tabla, String texto, boolean alinearDerecha) {
        PdfPCell c = new PdfPCell(new Phrase(texto, new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK)));
        c.setPadding(6);
        c.setBorderColor(COLOR_BORDE);
        if (alinearDerecha) c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(c);
    }

    private static void añadirFilaTotalPie(PdfPTable tabla, String etiqueta, String valor, Font fontValor) {
        PdfPCell c1 = new PdfPCell(new Phrase(etiqueta, new Font(Font.HELVETICA, 9, Font.NORMAL, Color.WHITE)));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(2);
        tabla.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(valor, fontValor));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(2);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(c2);
    }

    private static void celdaSeccionFactura(PdfPTable tabla, String etiqueta, String valor) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(etiqueta + "\n", new Font(Font.HELVETICA, 7, Font.NORMAL, Color.GRAY)));
        p.add(new Chunk(valor, new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK)));
        PdfPCell c = new PdfPCell(p);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(2);
        tabla.addCell(c);
    }

    private static String comprobanteCompleto(Venta venta) {
        String tipo = venta.getTipoComprobante() != null ? venta.getTipoComprobante() : "BOL";
        String serie = venta.getSerieComprobante() != null ? venta.getSerieComprobante() : "001";
        String numero = venta.getNumeroComprobante() != null ? venta.getNumeroComprobante() : String.valueOf(venta.getId());
        return tipo + "-" + serie + "-" + numero;
    }

    private static void añadirCeldaDato(PdfPTable tabla, String etiqueta, String valor) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(etiqueta + ": ", FONT_ETIQUETA));
        p.add(new Chunk(valor, FONT_VALOR));
        PdfPCell c = new PdfPCell(p);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(4);
        tabla.addCell(c);
    }

    private static void añadirCeldaSeccion(PdfPTable tabla, String titulo, String texto) {
        PdfPCell cell;
        if (titulo != null) {
            cell = new PdfPCell(new Phrase(titulo + " " + texto, new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK)));
        } else {
            cell = new PdfPCell(new Phrase(texto, FONT_PEQUEÑO));
        }
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        cell.setBackgroundColor(COLOR_FONDO_SECCION);
        tabla.addCell(cell);
    }

    private static void añadirCeldaTabla(PdfPTable tabla, String texto, boolean header) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, header ? new Font(Font.HELVETICA, 8, Font.BOLD, Color.BLACK) : FONT_PEQUEÑO));
        cell.setPadding(5);
        cell.setBorderColor(COLOR_BORDE);
        if (header) cell.setBackgroundColor(COLOR_FONDO_SECCION);
        tabla.addCell(cell);
    }

    private static void añadirFilaResumen(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell c1 = new PdfPCell(new Phrase(etiqueta, FONT_ETIQUETA));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(2);
        tabla.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(valor, FONT_NORMAL));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(2);
        tabla.addCell(c2);
    }

    private static String formatSoles(BigDecimal valor) {
        if (valor == null) return "0.00";
        return String.format("%,.2f", valor);
    }

    private static String labelMedio(String medio) {
        if (medio == null) return "Efectivo";
        return switch (medio.toUpperCase()) {
            case "TARJETA" -> "Tarjeta";
            case "TRANSFERENCIA" -> "Transferencia";
            case "YAPE" -> "Yape";
            case "PLIN" -> "Plin";
            default -> "Efectivo";
        };
    }
}
