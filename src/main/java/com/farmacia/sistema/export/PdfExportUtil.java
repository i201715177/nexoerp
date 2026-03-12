package com.farmacia.sistema.export;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class PdfExportUtil {

    private static final Color HEADER_BG = new Color(46, 80, 144);
    private static final Color TITLE_BG = new Color(27, 58, 92);
    private static final Color ALT_ROW = new Color(240, 244, 250);
    private static final Color BORDER_COLOR = new Color(180, 180, 180);

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD, Color.WHITE);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 10, Font.ITALIC, Color.GRAY);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font DATA_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);
    private static final Font FOOTER_FONT = new Font(Font.HELVETICA, 7, Font.ITALIC, Color.GRAY);

    private PdfExportUtil() {}

    public static void crearReporte(OutputStream out, String titulo, String subtitulo,
                                    String[] headers, List<Object[]> filas,
                                    float[] anchos) {
        boolean landscape = headers.length > 6;
        Rectangle pageSize = landscape ? PageSize.A4.rotate() : PageSize.A4;
        Document doc = new Document(pageSize, 30, 30, 40, 50);

        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new FooterEvent(titulo));
            doc.open();

            PdfPTable titleBar = new PdfPTable(1);
            titleBar.setWidthPercentage(100);
            PdfPCell titleCell = new PdfPCell(new Phrase(titulo, TITLE_FONT));
            titleCell.setBackgroundColor(TITLE_BG);
            titleCell.setPadding(12);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleBar.addCell(titleCell);
            doc.add(titleBar);

            Paragraph sub = new Paragraph(subtitulo, SUBTITLE_FONT);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingBefore(6);
            sub.setSpacingAfter(12);
            doc.add(sub);

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            if (anchos != null && anchos.length == headers.length) {
                table.setWidths(anchos);
            }
            table.setHeaderRows(1);

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
                cell.setBackgroundColor(HEADER_BG);
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setBorderColor(BORDER_COLOR);
                table.addCell(cell);
            }

            for (int i = 0; i < filas.size(); i++) {
                Object[] fila = filas.get(i);
                boolean alt = (i % 2 == 1);
                for (int c = 0; c < headers.length; c++) {
                    Object val = (c < fila.length) ? fila[c] : null;
                    String texto = formatValue(val);
                    PdfPCell cell = new PdfPCell(new Phrase(texto, DATA_FONT));
                    if (alt) cell.setBackgroundColor(ALT_ROW);
                    cell.setPadding(5);
                    cell.setBorderColor(BORDER_COLOR);
                    if (val instanceof Number) {
                        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    } else if (val instanceof LocalDateTime || val instanceof LocalDate) {
                        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    }
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    table.addCell(cell);
                }
            }

            doc.add(table);

            Paragraph totalLine = new Paragraph(
                    "Total de registros: " + filas.size(),
                    new Font(Font.HELVETICA, 9, Font.BOLD, new Color(80, 80, 80)));
            totalLine.setAlignment(Element.ALIGN_RIGHT);
            totalLine.setSpacingBefore(8);
            doc.add(totalLine);

        } catch (DocumentException e) {
            throw new RuntimeException("Error generando PDF", e);
        } finally {
            doc.close();
        }
    }

    private static String formatValue(Object val) {
        if (val == null) return "";
        if (val instanceof BigDecimal bd) return String.format("%,.2f", bd);
        if (val instanceof Double d) return String.format("%,.2f", d);
        if (val instanceof LocalDateTime ldt) return ldt.format(FMT_DATETIME);
        if (val instanceof LocalDate ld) return ld.format(FMT_DATE);
        if (val instanceof Boolean b) return b ? "Sí" : "No";
        return val.toString();
    }

    static class FooterEvent extends PdfPageEventHelper {
        private final String titulo;
        FooterEvent(String titulo) { this.titulo = titulo; }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable footer = new PdfPTable(3);
            footer.setTotalWidth(document.right() - document.left());
            try {
                footer.setWidths(new float[]{1, 2, 1});
            } catch (DocumentException ignored) {}

            PdfPCell left = new PdfPCell(new Phrase("NexoERP", FOOTER_FONT));
            left.setBorder(Rectangle.TOP);
            left.setBorderColor(BORDER_COLOR);
            left.setPaddingTop(4);
            footer.addCell(left);

            PdfPCell center = new PdfPCell(new Phrase(titulo, FOOTER_FONT));
            center.setHorizontalAlignment(Element.ALIGN_CENTER);
            center.setBorder(Rectangle.TOP);
            center.setBorderColor(BORDER_COLOR);
            center.setPaddingTop(4);
            footer.addCell(center);

            PdfPCell right = new PdfPCell(new Phrase(
                    "Página " + writer.getPageNumber(), FOOTER_FONT));
            right.setHorizontalAlignment(Element.ALIGN_RIGHT);
            right.setBorder(Rectangle.TOP);
            right.setBorderColor(BORDER_COLOR);
            right.setPaddingTop(4);
            footer.addCell(right);

            footer.writeSelectedRows(0, -1,
                    document.left(), document.bottom() - 5, writer.getDirectContent());
        }
    }
}
