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
import java.util.Locale;

public final class GuiaRemisionPdfUtil {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_LARGO = DateTimeFormatter.ofPattern("dd 'de' MMMM 'del' yyyy", new Locale("es", "PE"));

    private static final Font F_EMPRESA_NOMBRE = new Font(Font.HELVETICA, 12, Font.BOLD, Color.BLACK);
    private static final Font F_EMPRESA_DIR = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
    private static final Font F_RUC_TITULO = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);
    private static final Font F_RUC_NUMERO = new Font(Font.HELVETICA, 13, Font.BOLD, Color.BLACK);
    private static final Font F_GUIA_TITULO = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);
    private static final Font F_LABEL = new Font(Font.HELVETICA, 8, Font.BOLD, Color.BLACK);
    private static final Font F_VALOR = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);
    private static final Font F_SECCION = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);
    private static final Font F_HEADER_COL = new Font(Font.HELVETICA, 8, Font.BOLD, Color.BLACK);
    private static final Font F_BODY = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);
    private static final Font F_PIE = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.GRAY);

    private GuiaRemisionPdfUtil() {}

    public static void generar(OutputStream out, Empresa empresa, GuiaRemision guia) {
        Document doc = new Document(PageSize.A4, 30, 30, 25, 25);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.addTitle("Guía de Remisión " + guia.getSerieNumero());

            agregarEncabezado(doc, empresa, guia);
            agregarDatosGenerales(doc, guia);
            agregarMotivoTraslado(doc, guia);
            agregarDatosBienes(doc, guia);
            agregarDatosTransporte(doc, guia);
            agregarPie(doc, empresa);

        } catch (DocumentException e) {
            throw new RuntimeException("Error generando PDF de guía de remisión", e);
        } finally {
            doc.close();
        }
    }

    private static void agregarEncabezado(Document doc, Empresa empresa, GuiaRemision guia) throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1.3f, 1f});
        header.setSpacingAfter(8);

        PdfPCell cEmisor = new PdfPCell();
        cEmisor.setBorder(Rectangle.BOX);
        cEmisor.setBorderWidth(1.5f);
        cEmisor.setPadding(10);
        cEmisor.setVerticalAlignment(Element.ALIGN_MIDDLE);
        String nombreEmp = empresa != null && empresa.getNombre() != null ? empresa.getNombre().toUpperCase() : "EMPRESA";
        String dirEmp = empresa != null && empresa.getDireccion() != null ? empresa.getDireccion().toUpperCase() : "";
        cEmisor.addElement(new Paragraph(nombreEmp, F_EMPRESA_NOMBRE));
        cEmisor.addElement(new Paragraph(dirEmp, F_EMPRESA_DIR));
        header.addCell(cEmisor);

        PdfPCell cRuc = new PdfPCell();
        cRuc.setBorder(Rectangle.BOX);
        cRuc.setBorderWidth(1.5f);
        cRuc.setPadding(10);
        cRuc.setHorizontalAlignment(Element.ALIGN_CENTER);
        cRuc.setVerticalAlignment(Element.ALIGN_MIDDLE);

        String ruc = empresa != null && empresa.getRuc() != null ? empresa.getRuc() : "";

        Paragraph pLinea1 = new Paragraph("RUC N°:", F_RUC_TITULO);
        pLinea1.setAlignment(Element.ALIGN_CENTER);
        cRuc.addElement(pLinea1);

        Paragraph pLinea2 = new Paragraph(ruc, F_RUC_NUMERO);
        pLinea2.setAlignment(Element.ALIGN_CENTER);
        pLinea2.setSpacingBefore(2);
        cRuc.addElement(pLinea2);

        Paragraph pLinea3 = new Paragraph("GUÍA DE REMISIÓN", F_GUIA_TITULO);
        pLinea3.setAlignment(Element.ALIGN_CENTER);
        pLinea3.setSpacingBefore(4);
        cRuc.addElement(pLinea3);

        Paragraph pLinea4 = new Paragraph("DE REMITENTE", F_GUIA_TITULO);
        pLinea4.setAlignment(Element.ALIGN_CENTER);
        cRuc.addElement(pLinea4);

        Paragraph pNumero = new Paragraph(guia.getSerieNumero(), F_RUC_NUMERO);
        pNumero.setAlignment(Element.ALIGN_CENTER);
        pNumero.setSpacingBefore(4);
        cRuc.addElement(pNumero);

        header.addCell(cRuc);

        doc.add(header);
    }

    private static void agregarDatosGenerales(Document doc, GuiaRemision guia) throws DocumentException {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.2f, 1f});
        tabla.setSpacingAfter(6);

        PdfPCell cIzq = new PdfPCell();
        cIzq.setBorder(Rectangle.BOX);
        cIzq.setBorderWidth(0.5f);
        cIzq.setPadding(6);

        String fechaTraslado = "";
        if (guia.getFechaTraslado() != null) {
            try { fechaTraslado = guia.getFechaTraslado().format(FMT_LARGO).toUpperCase(); }
            catch (Exception e) { fechaTraslado = guia.getFechaTraslado().format(FMT); }
        } else if (guia.getFechaEmision() != null) {
            try { fechaTraslado = guia.getFechaEmision().toLocalDate().format(FMT_LARGO).toUpperCase(); }
            catch (Exception e) { fechaTraslado = guia.getFechaEmision().format(FMT); }
        }
        agregarLinea(cIzq, "Fecha de inicio de traslado: ", fechaTraslado);

        String destinatario = "";
        String rucDest = "";
        if (guia.getProveedor() != null) {
            destinatario = guia.getProveedor().getRazonSocial() != null ? guia.getProveedor().getRazonSocial() : "";
            rucDest = guia.getProveedor().getNumeroDocumento() != null ? guia.getProveedor().getNumeroDocumento() : "";
        } else if (guia.getOrdenCompra() != null && guia.getOrdenCompra().getProveedor() != null) {
            destinatario = guia.getOrdenCompra().getProveedor().getRazonSocial() != null ? guia.getOrdenCompra().getProveedor().getRazonSocial() : "";
            rucDest = guia.getOrdenCompra().getProveedor().getNumeroDocumento() != null ? guia.getOrdenCompra().getProveedor().getNumeroDocumento() : "";
        }
        agregarLinea(cIzq, "Destinatario: ", destinatario.toUpperCase());
        agregarLinea(cIzq, "RUC: ", rucDest);
        agregarLinea(cIzq, "N° Doc. Identidad: ", "");
        tabla.addCell(cIzq);

        PdfPCell cDer = new PdfPCell();
        cDer.setBorder(Rectangle.BOX);
        cDer.setBorderWidth(0.5f);
        cDer.setPadding(6);
        agregarLinea(cDer, "Punto de partida: ", safe(guia.getDireccionPartida()));
        agregarLinea(cDer, "Punto de llegada: ", safe(guia.getDireccionLlegada()));
        tabla.addCell(cDer);

        doc.add(tabla);
    }

    private static void agregarMotivoTraslado(Document doc, GuiaRemision guia) throws DocumentException {
        PdfPTable contenedor = new PdfPTable(1);
        contenedor.setWidthPercentage(100);
        contenedor.setSpacingAfter(6);

        PdfPCell cTitulo = new PdfPCell(new Phrase("Motivo del traslado:", F_SECCION));
        cTitulo.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.TOP);
        cTitulo.setBorderWidth(0.5f);
        cTitulo.setPadding(4);
        contenedor.addCell(cTitulo);

        PdfPTable motivos = new PdfPTable(3);
        motivos.setWidthPercentage(100);
        motivos.setWidths(new float[]{1f, 1.3f, 1.2f});

        String motivoActual = guia.getMotivoTraslado() != null ? guia.getMotivoTraslado().toUpperCase() : "";

        String[][] opciones = {
            {"VENTA", "Venta"},
            {"VENTA_CONFIRMACION", "Venta sujeta a confirmación por el comprador"},
            {"RECOJO", "Recojo de bienes"},
            {"COMPRA", "Compra"},
            {"TRASLADO_ENTRE_ESTABLECIMIENTOS", "Traslado entre establecimientos de la misma empresa"},
            {"IMPORTACION", "Importación"},
            {"CONSIGNACION", "Consignación"},
            {"DEVOLUCION", "Devolución"},
            {"TRASLADO_ZONA_PRIMARIA", "Traslado zona primaria"},
            {"VENTA_CON_ENTREGA", "Venta con entrega a terceros"},
            {"OTROS", "Otros (especificar)"},
            {"TRASLADO_EMISOR_ITINERANTE", "Traslado por emisor itinerantes"}
        };

        for (String[] opc : opciones) {
            boolean marcado = motivoActual.contains(opc[0]);

            Paragraph p = new Paragraph();
            if (marcado) {
                p.add(new Chunk("4 ", new Font(Font.ZAPFDINGBATS, 8, Font.NORMAL, Color.BLACK)));
            } else {
                p.add(new Chunk("o ", new Font(Font.ZAPFDINGBATS, 8, Font.NORMAL, Color.GRAY)));
            }
            p.add(new Chunk(opc[1], F_BODY));
            PdfPCell c = new PdfPCell(p);
            c.setBorder(Rectangle.NO_BORDER);
            c.setPaddingLeft(4);
            c.setPaddingTop(1);
            c.setPaddingBottom(1);
            motivos.addCell(c);
        }

        int restantes = 3 - (opciones.length % 3);
        if (restantes < 3) {
            for (int i = 0; i < restantes; i++) {
                PdfPCell vacia = new PdfPCell(new Phrase(""));
                vacia.setBorder(Rectangle.NO_BORDER);
                motivos.addCell(vacia);
            }
        }

        PdfPCell cMotivos = new PdfPCell(motivos);
        cMotivos.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);
        cMotivos.setBorderWidth(0.5f);
        cMotivos.setPadding(3);
        contenedor.addCell(cMotivos);

        doc.add(contenedor);
    }

    private static void agregarDatosBienes(Document doc, GuiaRemision guia) throws DocumentException {
        Paragraph titulo = new Paragraph("Datos del bien", F_SECCION);
        titulo.setSpacingBefore(4);
        titulo.setSpacingAfter(2);
        doc.add(titulo);

        PdfPTable tablaItems = new PdfPTable(4);
        tablaItems.setWidthPercentage(100);
        tablaItems.setWidths(new float[]{3f, 0.8f, 1f, 0.8f});
        tablaItems.setSpacingAfter(8);

        agregarCeldaHeader(tablaItems, "Descripción:");
        agregarCeldaHeader(tablaItems, "Cantidad:");
        agregarCeldaHeader(tablaItems, "Unidad de medida:");
        agregarCeldaHeader(tablaItems, "Peso:");

        if (guia.getItems() != null && !guia.getItems().isEmpty()) {
            for (GuiaRemisionItem item : guia.getItems()) {
                String desc = item.getDescripcion() != null ? item.getDescripcion() :
                        (item.getProducto() != null ? item.getProducto().getNombre() : "");
                agregarCeldaBody(tablaItems, desc);
                agregarCeldaBody(tablaItems, String.valueOf(item.getCantidad() != null ? item.getCantidad() : 0));
                agregarCeldaBody(tablaItems, item.getUnidadMedida() != null ? item.getUnidadMedida() : "UND");
                agregarCeldaBody(tablaItems, "");
            }
        } else {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 4; j++) {
                    agregarCeldaBody(tablaItems, "");
                }
            }
        }

        doc.add(tablaItems);
    }

    private static void agregarDatosTransporte(Document doc, GuiaRemision guia) throws DocumentException {
        PdfPTable tTransporte = new PdfPTable(2);
        tTransporte.setWidthPercentage(100);
        tTransporte.setWidths(new float[]{1f, 1f});
        tTransporte.setSpacingAfter(8);

        PdfPCell cTransp = new PdfPCell();
        cTransp.setBorder(Rectangle.BOX);
        cTransp.setBorderWidth(0.5f);
        cTransp.setPadding(6);
        cTransp.addElement(new Paragraph("Datos del transportista:", F_SECCION));

        PdfPTable tDetTransp = new PdfPTable(2);
        tDetTransp.setWidthPercentage(100);
        tDetTransp.setWidths(new float[]{1f, 1.5f});

        agregarCeldaDato(tDetTransp, "RUC:", safe(guia.getTransportistaRuc()));
        agregarCeldaDato(tDetTransp, "Denominación, apellidos y nombres:", safe(guia.getTransportistaNombre()));

        cTransp.addElement(tDetTransp);
        tTransporte.addCell(cTransp);

        PdfPCell cUnidad = new PdfPCell();
        cUnidad.setBorder(Rectangle.BOX);
        cUnidad.setBorderWidth(0.5f);
        cUnidad.setPadding(6);
        cUnidad.addElement(new Paragraph("Datos de la unidad de Transporte y conductor:", F_SECCION));

        PdfPTable tDetUnidad = new PdfPTable(2);
        tDetUnidad.setWidthPercentage(100);
        tDetUnidad.setWidths(new float[]{1f, 1f});

        agregarCeldaDato(tDetUnidad, "Marca y placa:", safe(guia.getPlacaVehiculo()));
        agregarCeldaDato(tDetUnidad, "Licencia de conducir:", safe(guia.getConductorLicencia()));
        agregarCeldaDato(tDetUnidad, "DNI conductor:", safe(guia.getConductorDni()));
        agregarCeldaDato(tDetUnidad, "Nombre conductor:", safe(guia.getConductorNombre()));

        cUnidad.addElement(tDetUnidad);
        tTransporte.addCell(cUnidad);

        doc.add(tTransporte);

        if (guia.getObservaciones() != null && !guia.getObservaciones().isBlank()) {
            Paragraph obs = new Paragraph("Observaciones: " + guia.getObservaciones(), F_BODY);
            obs.setSpacingAfter(6);
            doc.add(obs);
        }
    }

    private static void agregarPie(Document doc, Empresa empresa) throws DocumentException {
        Paragraph sep = new Paragraph(" ");
        sep.setSpacingBefore(10);
        doc.add(sep);

        PdfPTable pie = new PdfPTable(1);
        pie.setWidthPercentage(60);
        pie.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell cPie = new PdfPCell();
        cPie.setBorder(Rectangle.NO_BORDER);
        cPie.setPadding(3);

        String nombreEmp = empresa != null && empresa.getNombre() != null ? empresa.getNombre().toUpperCase() : "EMPRESA";
        String rucEmp = empresa != null && empresa.getRuc() != null ? empresa.getRuc() : "";
        cPie.addElement(new Paragraph("Imprenta:", F_PIE));
        cPie.addElement(new Paragraph(nombreEmp + " RUC:", F_PIE));
        cPie.addElement(new Paragraph(rucEmp, F_PIE));
        cPie.addElement(new Paragraph("Representación impresa de la Guía de Remisión Electrónica", F_PIE));
        cPie.addElement(new Paragraph("Hash: pendiente de firma digital", F_PIE));
        pie.addCell(cPie);

        doc.add(pie);

        Paragraph nexo = new Paragraph("Documento generado por NexoERP", new Font(Font.HELVETICA, 6, Font.ITALIC, Color.LIGHT_GRAY));
        nexo.setAlignment(Element.ALIGN_CENTER);
        nexo.setSpacingBefore(4);
        doc.add(nexo);
    }

    private static void agregarLinea(PdfPCell cell, String label, String valor) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label, F_LABEL));
        p.add(new Chunk(valor, F_VALOR));
        p.setSpacingAfter(2);
        cell.addElement(p);
    }

    private static void agregarCeldaHeader(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, F_HEADER_COL));
        c.setBackgroundColor(new Color(240, 240, 240));
        c.setPadding(5);
        c.setBorder(Rectangle.BOX);
        c.setBorderWidth(0.5f);
        t.addCell(c);
    }

    private static void agregarCeldaBody(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, F_BODY));
        c.setPadding(4);
        c.setBorder(Rectangle.BOX);
        c.setBorderWidth(0.5f);
        c.setMinimumHeight(18);
        t.addCell(c);
    }

    private static void agregarCeldaDato(PdfPTable t, String label, String valor) {
        PdfPCell cLabel = new PdfPCell(new Phrase(label, F_LABEL));
        cLabel.setBorder(Rectangle.BOX);
        cLabel.setBorderWidth(0.5f);
        cLabel.setPadding(4);
        t.addCell(cLabel);

        PdfPCell cValor = new PdfPCell(new Phrase(valor, F_VALOR));
        cValor.setBorder(Rectangle.BOX);
        cValor.setBorderWidth(0.5f);
        cValor.setPadding(4);
        t.addCell(cValor);
    }

    private static String safe(String val) {
        return val != null ? val : "";
    }
}
