package com.farmacia.sistema.export;

import com.farmacia.sistema.domain.empresa.Empresa;
import com.farmacia.sistema.domain.empresa.FacturaSaaS;
import com.farmacia.sistema.domain.empresa.PlanSuscripcion;
import com.farmacia.sistema.domain.empresa.TipoSuscripcion;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Genera un PDF de factura/comprobante SaaS con un diseño más amigable
 * para entregar al cliente como constancia de pago o de cobro.
 */
public final class FacturaSaaSPdfUtil {

    private static final Color PRIMARY = new Color(25, 118, 210);
    private static final Color LIGHT_BG = new Color(245, 248, 252);
    private static final Color BORDER = new Color(210, 210, 210);

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, PRIMARY);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.DARK_GRAY);
    private static final Font TEXT_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
    private static final Font TOTAL_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, Color.WHITE);

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private FacturaSaaSPdfUtil() {}

    public static void export(FacturaSaaS factura,
                              String emisorNombre,
                              String emisorRuc,
                              String emisorDireccion,
                              OutputStream out) {
        Document doc = new Document(PageSize.A4, 36, 36, 40, 40);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Empresa empresa = factura.getEmpresa();
            PlanSuscripcion plan = factura.getPlan();

            // ENCABEZADO (emisor + datos factura)
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{2f, 1.2f});

            // Emisor
            PdfPCell emisorCell = new PdfPCell();
            emisorCell.setBorder(Rectangle.NO_BORDER);
            Paragraph emisorP = new Paragraph();
            emisorP.add(new Phrase(emisorNombre != null && !emisorNombre.isBlank() ? emisorNombre : "NexoERP", TITLE_FONT));
            emisorP.add(Chunk.NEWLINE);
            if (emisorRuc != null && !emisorRuc.isBlank()) {
                emisorP.add(new Phrase("RUC: " + emisorRuc, SUBTITLE_FONT));
                emisorP.add(Chunk.NEWLINE);
            }
            if (emisorDireccion != null && !emisorDireccion.isBlank()) {
                emisorP.add(new Phrase(emisorDireccion, SUBTITLE_FONT));
            }
            emisorCell.addElement(emisorP);
            header.addCell(emisorCell);

            // Info factura / comprobante
            PdfPCell facturaCell = new PdfPCell();
            facturaCell.setBorder(Rectangle.NO_BORDER);
            facturaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph right = new Paragraph();
            right.setAlignment(Element.ALIGN_RIGHT);
            // Si el cliente es persona con DNI, mostrar COMPROBANTE en lugar de FACTURA
            String tituloDocumento = "FACTURA SAAS";
            if (empresa != null) {
                String tipoDoc = empresa.getTipoDocumento();
                if (tipoDoc != null && tipoDoc.equalsIgnoreCase("DNI")) {
                    tituloDocumento = "COMPROBANTE SAAS";
                }
            }
            right.add(new Phrase(tituloDocumento, new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY)));
            right.add(Chunk.NEWLINE);
            String nro = factura.getNumeroFactura() != null ? factura.getNumeroFactura() : "F-" + factura.getId();
            right.add(new Phrase("Nº " + nro, new Font(Font.HELVETICA, 11, Font.BOLD, Color.DARK_GRAY)));
            right.add(Chunk.NEWLINE);
            right.add(new Phrase("Fecha emisión: " + formatDate(factura.getFechaEmision()), SUBTITLE_FONT));
            right.add(Chunk.NEWLINE);
            right.add(new Phrase("Vencimiento: " + formatDate(factura.getFechaVencimiento()), SUBTITLE_FONT));

            facturaCell.addElement(right);
            header.addCell(facturaCell);

            doc.add(header);
            doc.add(Chunk.NEWLINE);

            // DATOS DEL CLIENTE
            PdfPTable clienteTable = new PdfPTable(2);
            clienteTable.setWidthPercentage(100);
            clienteTable.setWidths(new float[]{1.5f, 1.5f});

            PdfPCell clienteHeader = new PdfPCell(new Phrase("Cliente", LABEL_FONT));
            clienteHeader.setColspan(2);
            clienteHeader.setBackgroundColor(LIGHT_BG);
            clienteHeader.setBorderColor(BORDER);
            clienteHeader.setPadding(6);
            clienteTable.addCell(clienteHeader);

            PdfPCell clienteLeft = new PdfPCell();
            clienteLeft.setBorderColor(BORDER);
            clienteLeft.setPadding(6);
            if (empresa != null) {
                clienteLeft.addElement(new Phrase(empresa.getNombre(), TEXT_FONT));
                String docLinea = null;
                if (empresa.getRuc() != null && !empresa.getRuc().isBlank()) {
                    String etiqueta = empresa.getTipoDocumento() != null && !empresa.getTipoDocumento().isBlank()
                            ? empresa.getTipoDocumento()
                            : "RUC";
                    docLinea = etiqueta + ": " + empresa.getRuc();
                }
                if (docLinea != null) {
                    clienteLeft.addElement(new Phrase(docLinea, SUBTITLE_FONT));
                }
            }
            clienteTable.addCell(clienteLeft);

            PdfPCell clienteRight = new PdfPCell();
            clienteRight.setBorderColor(BORDER);
            clienteRight.setPadding(6);
            if (empresa != null) {
                if (empresa.getDireccion() != null && !empresa.getDireccion().isBlank()) {
                    clienteRight.addElement(new Phrase("Dirección: " + empresa.getDireccion(), SUBTITLE_FONT));
                }
                if (empresa.getTelefono() != null && !empresa.getTelefono().isBlank()) {
                    clienteRight.addElement(new Phrase("Teléfono: " + empresa.getTelefono(), SUBTITLE_FONT));
                }
            }
            clienteTable.addCell(clienteRight);

            doc.add(clienteTable);
            doc.add(Chunk.NEWLINE);

            // DETALLE (similar al diseño de factura tradicional)
            PdfPTable detalle = new PdfPTable(4);
            detalle.setWidthPercentage(100);
            detalle.setWidths(new float[]{3.5f, 1f, 1.2f, 1.3f});

            addHeaderCell(detalle, "Descripción");
            addHeaderCell(detalle, "Cant.");
            addHeaderCell(detalle, "Precio");
            addHeaderCell(detalle, "Total");

            String desc = "Suscripción NexoERP";
            if (plan != null) {
                desc += " - Plan " + plan.getNombre();
            }
            TipoSuscripcion tipo = empresa != null ? empresa.getTipoSuscripcion() : null;
            if (tipo != null) {
                desc += " (" + tipo.name().toLowerCase() + ")";
            }

            String periodo = formatDate(factura.getPeriodoDesde()) + " - " + formatDate(factura.getPeriodoHasta());
            BigDecimal total = factura.getMonto() != null ? factura.getMonto() : BigDecimal.ZERO;

            // Descripción incluye el periodo en una segunda línea para que el diseño se parezca más al ejemplo
            addBodyCell(detalle, desc + "\nPeriodo: " + periodo, false);
            addBodyCellCenter(detalle, "1", false);
            addBodyCellRight(detalle, formatMoney(total), false);
            addBodyCellRight(detalle, formatMoney(total), false);

            doc.add(detalle);
            doc.add(Chunk.NEWLINE);

            // TOTALES
            PdfPTable totales = new PdfPTable(2);
            totales.setWidthPercentage(40);
            totales.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totales.setWidths(new float[]{1.2f, 1f});

            // total ya calculado arriba

            PdfPCell lblSubtotal = new PdfPCell(new Phrase("Subtotal", LABEL_FONT));
            lblSubtotal.setBorder(Rectangle.NO_BORDER);
            lblSubtotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totales.addCell(lblSubtotal);
            PdfPCell valSubtotal = new PdfPCell(new Phrase(formatMoney(total), TEXT_FONT));
            valSubtotal.setBorder(Rectangle.NO_BORDER);
            valSubtotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totales.addCell(valSubtotal);

            PdfPCell lblTotal = new PdfPCell(new Phrase("TOTAL A PAGAR", LABEL_FONT));
            lblTotal.setBorder(Rectangle.NO_BORDER);
            lblTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            lblTotal.setPaddingTop(4);
            totales.addCell(lblTotal);

            PdfPCell valTotal = new PdfPCell(new Phrase(formatMoney(total), TEXT_FONT));
            valTotal.setBorder(Rectangle.NO_BORDER);
            valTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            valTotal.setPaddingTop(4);
            totales.addCell(valTotal);

            doc.add(totales);
            doc.add(Chunk.NEWLINE);

            // BARRA FINAL TOTAL
            PdfPTable barraTotal = new PdfPTable(1);
            barraTotal.setWidthPercentage(60);
            barraTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            PdfPCell barra = new PdfPCell(new Phrase("TOTAL A PAGAR: " + formatMoney(total), TOTAL_FONT));
            barra.setBackgroundColor(PRIMARY);
            barra.setHorizontalAlignment(Element.ALIGN_RIGHT);
            barra.setPadding(8);
            barra.setBorder(Rectangle.NO_BORDER);
            barraTotal.addCell(barra);
            doc.add(barraTotal);

            doc.add(Chunk.NEWLINE);

            Paragraph nota = new Paragraph("Gracias por su suscripción. Este comprobante es emitido por NexoERP para el servicio SaaS contratado.",
                    new Font(Font.HELVETICA, 8, Font.ITALIC, Color.DARK_GRAY));
            nota.setSpacingBefore(8);
            doc.add(nota);

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de factura SaaS", e);
        } finally {
            doc.close();
        }
    }

    private static void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, LABEL_FONT));
        cell.setBackgroundColor(PRIMARY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBorderColor(BORDER);
        cell.setPhrase(new Phrase(text, new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
        table.addCell(cell);
    }

    private static void addBodyCell(PdfPTable table, String text, boolean alt) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", TEXT_FONT));
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        if (alt) cell.setBackgroundColor(LIGHT_BG);
        table.addCell(cell);
    }

    private static void addBodyCellRight(PdfPTable table, String text, boolean alt) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", TEXT_FONT));
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (alt) cell.setBackgroundColor(LIGHT_BG);
        table.addCell(cell);
    }

    private static void addBodyCellCenter(PdfPTable table, String text, boolean alt) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", TEXT_FONT));
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        if (alt) cell.setBackgroundColor(LIGHT_BG);
        table.addCell(cell);
    }

    private static String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(FMT_DATE);
    }

    private static String formatMoney(BigDecimal monto) {
        if (monto == null) return "0.00";
        return String.format("S/ %,.2f", monto);
    }
}

