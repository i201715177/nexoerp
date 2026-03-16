package com.farmacia.sistema.export;

import com.farmacia.sistema.domain.empresa.Empresa;
import com.farmacia.sistema.domain.venta.PagoVenta;
import com.farmacia.sistema.domain.venta.Venta;
import com.farmacia.sistema.domain.venta.VentaItem;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import com.farmacia.sistema.domain.facturacion.MontoEnLetras;

import java.awt.Color;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
        boolean esFactura = "FAC".equals(venta.getTipoComprobante());
        Document doc = new Document(esFactura ? PageSize.A4 : PageSize.A5, 30, 30, 25, 25);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            if (esFactura) {
                doc.addTitle("Factura Electrónica");
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

            Paragraph montoLetras = new Paragraph(MontoEnLetras.convertir(total),
                    new Font(Font.HELVETICA, 7, Font.ITALIC, COLOR_PIE));
            montoLetras.setAlignment(Element.ALIGN_CENTER);
            montoLetras.setSpacingBefore(4);
            doc.add(montoLetras);

            BigDecimal baseImp = total.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
            BigDecimal igvCalc = total.subtract(baseImp);
            PdfPTable tablaIgv = new PdfPTable(2);
            tablaIgv.setWidthPercentage(60);
            tablaIgv.setSpacingBefore(4);
            tablaIgv.setSpacingAfter(4);
            tablaIgv.setHorizontalAlignment(Element.ALIGN_RIGHT);
            añadirFilaResumen(tablaIgv, "Op. Gravada:", "S/ " + formatSoles(baseImp));
            añadirFilaResumen(tablaIgv, "IGV (18%):", "S/ " + formatSoles(igvCalc));
            añadirFilaResumen(tablaIgv, "Total:", "S/ " + formatSoles(total));
            doc.add(tablaIgv);

            PdfPTable barraPie = new PdfPTable(1);
            barraPie.setWidthPercentage(100);
            barraPie.setSpacingBefore(6);
            PdfPCell cellPieBar = new PdfPCell(new Phrase(" "));
            cellPieBar.setBackgroundColor(COLOR_BORDE);
            cellPieBar.setFixedHeight(8);
            cellPieBar.setBorder(Rectangle.NO_BORDER);
            barraPie.addCell(cellPieBar);
            doc.add(barraPie);

            Paragraph hashRef = new Paragraph("Hash: pendiente de firma digital | Representación impresa del comprobante electrónico",
                    new Font(Font.HELVETICA, 6, Font.NORMAL, COLOR_PIE));
            hashRef.setAlignment(Element.ALIGN_CENTER);
            hashRef.setSpacingBefore(3);
            doc.add(hashRef);

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

    private static void generarFacturaPlantilla(Document doc, Empresa empresa, Venta venta) throws DocumentException {
        boolean anulada = "ANULADA".equals(venta.getEstado());
        String fecha = venta.getFechaHora() != null ? venta.getFechaHora().format(FMT_FECHA) : "";
        String fechaVenc = "";
        try {
            if (venta.getFechaHora() != null) fechaVenc = venta.getFechaHora().plusDays(30).format(FMT_FECHA);
        } catch (Exception ignored) {}
        BigDecimal total = venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO;
        BigDecimal descuento = venta.getDescuentoTotal() != null ? venta.getDescuentoTotal() : BigDecimal.ZERO;
        RoundingMode rm = RoundingMode.HALF_UP;
        BigDecimal baseImponible = total.divide(new BigDecimal("1.18"), 2, rm);
        BigDecimal igv = total.subtract(baseImponible);
        BigDecimal valorVenta = baseImponible;

        String nomEmpresa = empresa.getNombre() != null ? empresa.getNombre().trim().toUpperCase() : "EMPRESA";
        String descripcionEmpresa = empresa.getDescripcion() != null ? empresa.getDescripcion().trim().toUpperCase() : "";
        String dirEmpresa = empresa.getDireccion() != null ? empresa.getDireccion().trim() : "";
        String rucEmpresa = empresa.getRuc() != null ? empresa.getRuc() : "";

        String nombreCliente = "";
        String rucCliente = "";
        String dirCliente = "";
        if (venta.getCliente() != null) {
            String n = venta.getCliente().getNombres() != null ? venta.getCliente().getNombres().trim() : "";
            String a = venta.getCliente().getApellidos() != null ? venta.getCliente().getApellidos().trim() : "";
            nombreCliente = (n + " " + a).trim();
            rucCliente = venta.getCliente().getNumeroDocumento() != null ? venta.getCliente().getNumeroDocumento() : "";
            dirCliente = venta.getCliente().getDireccion() != null ? venta.getCliente().getDireccion() : "";
        }
        if (nombreCliente.isEmpty()) nombreCliente = venta.getNombreClienteVenta() != null ? venta.getNombreClienteVenta().trim() : "";
        if (nombreCliente.isEmpty()) nombreCliente = "CLIENTE GENERAL";

        String serieNum = (venta.getSerieComprobante() != null ? venta.getSerieComprobante() : "E001")
                + "-" + (venta.getNumeroComprobante() != null ? venta.getNumeroComprobante() : "0");

        Font fontNormal = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);
        Font fontBold = new Font(Font.HELVETICA, 8, Font.BOLD, Color.BLACK);
        Font fontBold9 = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);
        Font fontBold10 = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);
        Font fontSmall = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.BLACK);
        Font fontSmallBold = new Font(Font.HELVETICA, 7, Font.BOLD, Color.BLACK);
        Font fontTituloDoc = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);
        Font fontFooter = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.BLACK);

        // === ENCABEZADO: Empresa (izq) | Recuadro FACTURA ELECTRONICA (der) ===
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1.6f, 1f});

        Paragraph empresaInfo = new Paragraph();
        empresaInfo.add(new Chunk(nomEmpresa + "\n", fontBold10));
        if (!descripcionEmpresa.isEmpty() && !descripcionEmpresa.equals(nomEmpresa)) {
            empresaInfo.add(new Chunk(descripcionEmpresa + "\n", fontBold));
        }
        if (!dirEmpresa.isEmpty()) empresaInfo.add(new Chunk(dirEmpresa + "\n", fontNormal));
        PdfPCell cellEmpresa = new PdfPCell(empresaInfo);
        cellEmpresa.setBorder(Rectangle.BOX);
        cellEmpresa.setPadding(10);
        cellEmpresa.setVerticalAlignment(Element.ALIGN_MIDDLE);
        header.addCell(cellEmpresa);

        PdfPCell cellDoc = new PdfPCell();
        cellDoc.setBorder(Rectangle.BOX);
        cellDoc.setPadding(10);
        cellDoc.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellDoc.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph docInfo = new Paragraph();
        docInfo.setAlignment(Element.ALIGN_CENTER);
        docInfo.add(new Chunk("FACTURA ELECTRÓNICA\n", fontTituloDoc));
        docInfo.add(new Chunk("RUC: " + rucEmpresa + "\n", fontBold9));
        docInfo.add(new Chunk(serieNum, fontBold9));
        cellDoc.addElement(docInfo);
        header.addCell(cellDoc);
        doc.add(header);

        doc.add(new Paragraph(" ", new Font(Font.HELVETICA, 4)));

        // === DATOS DEL CLIENTE (campos etiquetados) ===
        PdfPTable datosCliente = new PdfPTable(2);
        datosCliente.setWidthPercentage(100);
        datosCliente.setWidths(new float[]{0.55f, 1.45f});
        agregarFilaDato(datosCliente, "Fecha de Vencimiento", fechaVenc, fontBold, fontNormal);
        agregarFilaDato(datosCliente, "Fecha de Emisión", fecha, fontBold, fontNormal);
        agregarFilaDato(datosCliente, "Señor(es)", nombreCliente.toUpperCase(), fontBold, fontNormal);
        agregarFilaDato(datosCliente, "RUC", rucCliente.isEmpty() ? "—" : rucCliente, fontBold, fontNormal);
        agregarFilaDato(datosCliente, "Dirección del Cliente", dirCliente.isEmpty() ? "—" : dirCliente, fontBold, fontNormal);
        agregarFilaDato(datosCliente, "Tipo de Moneda", "NUEVOS SOLES", fontBold, fontNormal);
        agregarFilaDato(datosCliente, "Observación", "", fontBold, fontNormal);
        doc.add(datosCliente);

        doc.add(new Paragraph(" ", new Font(Font.HELVETICA, 4)));

        // === TABLA DE ITEMS ===
        PdfPTable tablaItems = new PdfPTable(5);
        tablaItems.setWidthPercentage(100);
        tablaItems.setWidths(new float[]{0.8f, 1f, 1f, 2.5f, 1.2f});

        String[] headers = {"Cantidad", "Unidad Medida", "Código", "Descripción", "Valor Unitario"};
        for (String h : headers) {
            PdfPCell ch = new PdfPCell(new Phrase(h, fontSmallBold));
            ch.setBorder(Rectangle.BOX);
            ch.setPadding(4);
            ch.setBackgroundColor(new Color(240, 240, 240));
            ch.setHorizontalAlignment(Element.ALIGN_CENTER);
            tablaItems.addCell(ch);
        }

        List<VentaItem> items = venta.getItems();
        if (items != null) {
            for (VentaItem it : items) {
                int cant = it.getCantidad() != null ? it.getCantidad() : 0;
                String unidad = "UNIDAD";
                if (it.getProducto() != null && it.getProducto().getUnidadMedida() != null) {
                    unidad = it.getProducto().getUnidadMedida().toUpperCase();
                }
                String codigo = "";
                if (it.getProducto() != null && it.getProducto().getCodigo() != null) {
                    codigo = it.getProducto().getCodigo();
                }
                String desc = it.getProducto() != null ? it.getProducto().getNombre() : "?";
                BigDecimal precioUnit = it.getPrecioUnitario() != null ? it.getPrecioUnitario() : BigDecimal.ZERO;

                celdaItemSunat(tablaItems, String.format("%.2f", (double) cant), Element.ALIGN_CENTER, fontSmall);
                celdaItemSunat(tablaItems, unidad, Element.ALIGN_CENTER, fontSmall);
                celdaItemSunat(tablaItems, codigo, Element.ALIGN_CENTER, fontSmall);
                celdaItemSunat(tablaItems, desc, Element.ALIGN_LEFT, fontSmall);
                celdaItemSunat(tablaItems, formatSoles(precioUnit), Element.ALIGN_RIGHT, fontSmall);
            }
        }
        doc.add(tablaItems);

        doc.add(new Paragraph(" ", new Font(Font.HELVETICA, 2)));

        // === SECCIÓN INFERIOR: Monto en letras (izq) + Totales (der) ===
        PdfPTable seccionInferior = new PdfPTable(2);
        seccionInferior.setWidthPercentage(100);
        seccionInferior.setWidths(new float[]{1.4f, 1f});

        Paragraph izqInfo = new Paragraph();
        izqInfo.add(new Chunk("Valor de Venta de Operaciones Gratuitas : S/. 0.00\n\n", fontSmall));
        izqInfo.add(new Chunk("SON: " + MontoEnLetras.convertir(total).toUpperCase() + "\n", fontBold));
        PdfPCell cellIzq = new PdfPCell(izqInfo);
        cellIzq.setBorder(Rectangle.BOX);
        cellIzq.setPadding(8);
        cellIzq.setVerticalAlignment(Element.ALIGN_BOTTOM);
        seccionInferior.addCell(cellIzq);

        PdfPTable totales = new PdfPTable(2);
        totales.setWidthPercentage(100);
        totales.setWidths(new float[]{1.2f, 0.8f});
        agregarFilaTotalSunat(totales, "Sub Total Ventas :", "S/. " + formatSoles(valorVenta), fontSmall, fontSmall);
        agregarFilaTotalSunat(totales, "Anticipos :", "S/. 0", fontSmall, fontSmall);
        agregarFilaTotalSunat(totales, "Descuentos :", "S/. " + formatSoles(descuento), fontSmall, fontSmall);
        agregarFilaTotalSunat(totales, "Valor Venta :", "S/. " + formatSoles(valorVenta), fontSmall, fontBold);
        agregarFilaTotalSunat(totales, "ISC :", "S/. 0.00", fontSmall, fontSmall);
        agregarFilaTotalSunat(totales, "IGV :", "S/. " + formatSoles(igv), fontSmall, fontBold);
        agregarFilaTotalSunat(totales, "Otros Cargos :", "S/. 0.00", fontSmall, fontSmall);
        agregarFilaTotalSunat(totales, "Otros Tributos :", "S/. 0.00", fontSmall, fontSmall);
        agregarFilaTotalSunat(totales, "Importe Total :", "S/. " + formatSoles(total), fontSmallBold, fontBold);
        PdfPCell cellTotales = new PdfPCell(totales);
        cellTotales.setBorder(Rectangle.BOX);
        cellTotales.setPadding(4);
        seccionInferior.addCell(cellTotales);
        doc.add(seccionInferior);

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

        doc.add(new Paragraph(" ", new Font(Font.HELVETICA, 6)));

        // === PIE SUNAT ===
        PdfPTable pieSunat = new PdfPTable(1);
        pieSunat.setWidthPercentage(100);
        Paragraph pieTxt = new Paragraph(
                "Esta es una representación impresa de la factura electrónica, generada en el Sistema de SUNAT. " +
                "Puede verificarla utilizando su clave SOL", fontFooter);
        pieTxt.setAlignment(Element.ALIGN_CENTER);
        PdfPCell cellPie = new PdfPCell(pieTxt);
        cellPie.setBorder(Rectangle.BOX);
        cellPie.setPadding(8);
        cellPie.setHorizontalAlignment(Element.ALIGN_CENTER);
        pieSunat.addCell(cellPie);
        doc.add(pieSunat);

        Paragraph nexo = new Paragraph("Documento generado por NexoERP",
                new Font(Font.HELVETICA, 6, Font.ITALIC, Color.GRAY));
        nexo.setAlignment(Element.ALIGN_CENTER);
        nexo.setSpacingBefore(4);
        doc.add(nexo);
    }

    private static void agregarFilaDato(PdfPTable tabla, String etiqueta, String valor, Font fEtiqueta, Font fValor) {
        PdfPCell c1 = new PdfPCell(new Phrase(etiqueta + " :", fEtiqueta));
        c1.setBorder(Rectangle.BOX);
        c1.setPadding(3);
        tabla.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(valor, fValor));
        c2.setBorder(Rectangle.BOX);
        c2.setPadding(3);
        tabla.addCell(c2);
    }

    private static void celdaItemSunat(PdfPTable tabla, String texto, int align, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setBorder(Rectangle.BOX);
        c.setPadding(3);
        c.setHorizontalAlignment(align);
        tabla.addCell(c);
    }

    private static void agregarFilaTotalSunat(PdfPTable tabla, String etiqueta, String valor, Font fEtiq, Font fVal) {
        PdfPCell c1 = new PdfPCell(new Phrase(etiqueta, fEtiq));
        c1.setBorder(Rectangle.BOX);
        c1.setPadding(2);
        c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(valor, fVal));
        c2.setBorder(Rectangle.BOX);
        c2.setPadding(2);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(c2);
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
