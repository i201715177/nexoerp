package com.farmacia.sistema.export;

import com.farmacia.sistema.domain.empresa.Empresa;
import com.farmacia.sistema.domain.guiaremision.GuiaRemision;
import com.farmacia.sistema.domain.guiaremision.GuiaRemisionItem;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;

public final class GuiaRemisionPdfUtil {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color GRIS_OSCURO = new Color(44, 46, 52);
    private static final Color GRIS_BORDE = new Color(226, 232, 240);
    private static final Font F_TITULO = new Font(Font.HELVETICA, 16, Font.BOLD, Color.WHITE);
    private static final Font F_LABEL = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
    private static final Font F_VALOR = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);
    private static final Font F_NORMAL = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
    private static final Font F_HEADER = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
    private static final Font F_SMALL = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.GRAY);

    private GuiaRemisionPdfUtil() {}

    public static void generar(OutputStream out, Empresa empresa, GuiaRemision guia) {
        Document doc = new Document(PageSize.A4, 30, 30, 25, 25);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.addTitle("Guía de Remisión " + guia.getSerieNumero());

            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1.2f, 1f});
            header.setSpacingAfter(10);

            Paragraph pEmisor = new Paragraph();
            pEmisor.add(new Chunk((empresa != null && empresa.getNombre() != null ? empresa.getNombre().toUpperCase() : "EMPRESA") + "\n",
                    new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE)));
            if (empresa != null && empresa.getRuc() != null)
                pEmisor.add(new Chunk("RUC: " + empresa.getRuc() + "\n", new Font(Font.HELVETICA, 9, Font.NORMAL, Color.WHITE)));
            if (empresa != null && empresa.getDireccion() != null)
                pEmisor.add(new Chunk(empresa.getDireccion(), new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(200, 200, 200))));
            PdfPCell cEmisor = new PdfPCell(pEmisor);
            cEmisor.setBackgroundColor(GRIS_OSCURO);
            cEmisor.setBorder(Rectangle.NO_BORDER);
            cEmisor.setPadding(14);
            header.addCell(cEmisor);

            Paragraph pTitulo = new Paragraph();
            pTitulo.add(new Chunk("GUÍA DE REMISIÓN\nREMITENTE\n", F_TITULO));
            pTitulo.add(new Chunk(guia.getSerieNumero(), new Font(Font.HELVETICA, 12, Font.BOLD, new Color(253, 200, 48))));
            PdfPCell cTitulo = new PdfPCell(pTitulo);
            cTitulo.setBackgroundColor(GRIS_OSCURO);
            cTitulo.setBorder(Rectangle.NO_BORDER);
            cTitulo.setPadding(14);
            cTitulo.setHorizontalAlignment(Element.ALIGN_RIGHT);
            header.addCell(cTitulo);
            doc.add(header);

            PdfPTable info = new PdfPTable(4);
            info.setWidthPercentage(100);
            info.setSpacingAfter(8);
            addDato(info, "Fecha emisión", guia.getFechaEmision() != null ? guia.getFechaEmision().format(FMT) : "");
            addDato(info, "Fecha traslado", guia.getFechaTraslado() != null ? guia.getFechaTraslado().format(FMT) : "");
            addDato(info, "Motivo", guia.getMotivoTraslado() != null ? guia.getMotivoTraslado() : "");
            addDato(info, "Estado", guia.getEstado());
            doc.add(info);

            PdfPTable destino = new PdfPTable(2);
            destino.setWidthPercentage(100);
            destino.setSpacingAfter(8);
            addDato(destino, "Dirección partida", guia.getDireccionPartida() != null ? guia.getDireccionPartida() : "—");
            addDato(destino, "Dirección llegada", guia.getDireccionLlegada() != null ? guia.getDireccionLlegada() : "—");
            doc.add(destino);

            if (guia.getProveedor() != null) {
                PdfPTable prov = new PdfPTable(2);
                prov.setWidthPercentage(100);
                prov.setSpacingAfter(8);
                addDato(prov, "Proveedor", guia.getProveedor().getRazonSocial() != null ? guia.getProveedor().getRazonSocial() : "");
                addDato(prov, "RUC/DNI", guia.getProveedor().getNumeroDocumento() != null ? guia.getProveedor().getNumeroDocumento() : "");
                doc.add(prov);
            }

            PdfPTable transporte = new PdfPTable(4);
            transporte.setWidthPercentage(100);
            transporte.setSpacingAfter(8);
            addDato(transporte, "Transportista", guia.getTransportistaNombre() != null ? guia.getTransportistaNombre() : "—");
            addDato(transporte, "RUC Transp.", guia.getTransportistaRuc() != null ? guia.getTransportistaRuc() : "—");
            addDato(transporte, "Conductor", guia.getConductorNombre() != null ? guia.getConductorNombre() : "—");
            addDato(transporte, "Placa", guia.getPlacaVehiculo() != null ? guia.getPlacaVehiculo() : "—");
            doc.add(transporte);

            doc.add(new Paragraph("Detalle de bienes trasladados", new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK)));

            PdfPTable tablaItems = new PdfPTable(4);
            tablaItems.setWidthPercentage(100);
            tablaItems.setWidths(new float[]{0.5f, 3f, 0.8f, 0.8f});
            tablaItems.setSpacingBefore(4);
            tablaItems.setSpacingAfter(10);
            addHeaderCell(tablaItems, "#");
            addHeaderCell(tablaItems, "Descripción");
            addHeaderCell(tablaItems, "Cantidad");
            addHeaderCell(tablaItems, "Unidad");

            if (guia.getItems() != null) {
                int n = 1;
                for (GuiaRemisionItem item : guia.getItems()) {
                    addBodyCell(tablaItems, String.valueOf(n++));
                    addBodyCell(tablaItems, item.getDescripcion() != null ? item.getDescripcion() :
                            (item.getProducto() != null ? item.getProducto().getNombre() : ""));
                    addBodyCell(tablaItems, String.valueOf(item.getCantidad() != null ? item.getCantidad() : 0));
                    addBodyCell(tablaItems, item.getUnidadMedida() != null ? item.getUnidadMedida() : "UND");
                }
            }
            doc.add(tablaItems);

            if (guia.getObservaciones() != null && !guia.getObservaciones().isBlank()) {
                Paragraph obs = new Paragraph("Observaciones: " + guia.getObservaciones(), F_NORMAL);
                obs.setSpacingAfter(8);
                doc.add(obs);
            }

            Paragraph pie = new Paragraph("Representación impresa de la Guía de Remisión Electrónica | Hash: pendiente de firma digital", F_SMALL);
            pie.setAlignment(Element.ALIGN_CENTER);
            pie.setSpacingBefore(12);
            doc.add(pie);

            Paragraph nexo = new Paragraph("Documento generado por NexoERP", new Font(Font.HELVETICA, 6, Font.ITALIC, Color.LIGHT_GRAY));
            nexo.setAlignment(Element.ALIGN_CENTER);
            doc.add(nexo);

        } catch (DocumentException e) {
            throw new RuntimeException("Error generando PDF de guía de remisión", e);
        } finally {
            doc.close();
        }
    }

    private static void addDato(PdfPTable tabla, String label, String valor) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", F_LABEL));
        p.add(new Chunk(valor, F_VALOR));
        PdfPCell c = new PdfPCell(p);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(GRIS_BORDE);
        c.setPadding(5);
        tabla.addCell(c);
    }

    private static void addHeaderCell(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, F_HEADER));
        c.setBackgroundColor(GRIS_OSCURO);
        c.setPadding(6);
        c.setBorder(Rectangle.NO_BORDER);
        t.addCell(c);
    }

    private static void addBodyCell(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, F_NORMAL));
        c.setPadding(5);
        c.setBorderColor(GRIS_BORDE);
        t.addCell(c);
    }
}
