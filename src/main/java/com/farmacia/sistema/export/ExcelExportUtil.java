package com.farmacia.sistema.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ExcelExportUtil {

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ExcelExportUtil() {}

    /**
     * Crea un workbook nuevo con una sola hoja llamada "Datos".
     */
    public static Workbook crearReporte(String titulo, String subtitulo,
                                        String[] headers, List<Object[]> filas) {
        XSSFWorkbook wb = new XSSFWorkbook();
        poblarHoja(wb, "Datos", titulo, subtitulo, headers, filas);
        return wb;
    }

    /**
     * Agrega una hoja con diseño profesional completo a un workbook existente.
     */
    public static void agregarHoja(XSSFWorkbook wb, String nombreHoja,
                                   String titulo, String subtitulo,
                                   String[] headers, List<Object[]> filas) {
        poblarHoja(wb, nombreHoja, titulo, subtitulo, headers, filas);
    }

    private static void poblarHoja(XSSFWorkbook wb, String nombreHoja,
                                   String titulo, String subtitulo,
                                   String[] headers, List<Object[]> filas) {
        Sheet sheet = wb.createSheet(nombreHoja);
        int colCount = headers.length;

        CellStyle tituloStyle = estiloTitulo(wb);
        CellStyle subtituloStyle = estiloSubtitulo(wb);
        CellStyle headerStyle = estiloHeader(wb);
        CellStyle dataStyle = estiloData(wb, false);
        CellStyle dataAltStyle = estiloData(wb, true);
        CellStyle moneyStyle = estiloMoney(wb, false);
        CellStyle moneyAltStyle = estiloMoney(wb, true);
        CellStyle dateStyle = estiloDate(wb, false);
        CellStyle dateAltStyle = estiloDate(wb, true);
        CellStyle intStyle = estiloInt(wb, false);
        CellStyle intAltStyle = estiloInt(wb, true);

        int rowIdx = 0;

        Row titleRow = sheet.createRow(rowIdx);
        titleRow.setHeightInPoints(30);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(titulo);
        titleCell.setCellStyle(tituloStyle);
        if (colCount > 1) {
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, colCount - 1));
            for (int c = 1; c < colCount; c++) titleRow.createCell(c).setCellStyle(tituloStyle);
        }
        rowIdx++;

        Row subRow = sheet.createRow(rowIdx);
        subRow.setHeightInPoints(20);
        Cell subCell = subRow.createCell(0);
        subCell.setCellValue(subtitulo);
        subCell.setCellStyle(subtituloStyle);
        if (colCount > 1) {
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, colCount - 1));
            for (int c = 1; c < colCount; c++) subRow.createCell(c).setCellStyle(subtituloStyle);
        }
        rowIdx++;

        sheet.createRow(rowIdx++);

        int headerRowIdx = rowIdx;
        Row hRow = sheet.createRow(rowIdx++);
        hRow.setHeightInPoints(22);
        for (int c = 0; c < colCount; c++) {
            Cell cell = hRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < filas.size(); i++) {
            Object[] fila = filas.get(i);
            Row row = sheet.createRow(rowIdx++);
            boolean alt = (i % 2 == 1);
            for (int c = 0; c < fila.length && c < colCount; c++) {
                Cell cell = row.createCell(c);
                Object val = fila[c];
                if (val == null) {
                    cell.setCellValue("");
                    cell.setCellStyle(alt ? dataAltStyle : dataStyle);
                } else if (val instanceof BigDecimal bd) {
                    cell.setCellValue(bd.doubleValue());
                    cell.setCellStyle(alt ? moneyAltStyle : moneyStyle);
                } else if (val instanceof Double d) {
                    cell.setCellValue(d);
                    cell.setCellStyle(alt ? moneyAltStyle : moneyStyle);
                } else if (val instanceof Number n) {
                    cell.setCellValue(n.doubleValue());
                    cell.setCellStyle(alt ? intAltStyle : intStyle);
                } else if (val instanceof LocalDateTime ldt) {
                    cell.setCellValue(ldt.format(FMT_DATETIME));
                    cell.setCellStyle(alt ? dateAltStyle : dateStyle);
                } else if (val instanceof LocalDate ld) {
                    cell.setCellValue(ld.format(FMT_DATE));
                    cell.setCellStyle(alt ? dateAltStyle : dateStyle);
                } else if (val instanceof Boolean b) {
                    cell.setCellValue(b ? "Sí" : "No");
                    cell.setCellStyle(alt ? dataAltStyle : dataStyle);
                } else {
                    cell.setCellValue(val.toString());
                    cell.setCellStyle(alt ? dataAltStyle : dataStyle);
                }
            }
        }

        sheet.setAutoFilter(new CellRangeAddress(headerRowIdx, headerRowIdx, 0, colCount - 1));
        sheet.createFreezePane(0, headerRowIdx + 1);

        for (int c = 0; c < colCount; c++) {
            sheet.autoSizeColumn(c);
            int w = sheet.getColumnWidth(c);
            if (w < 3500) sheet.setColumnWidth(c, 3500);
            if (w > 15000) sheet.setColumnWidth(c, 15000);
        }
    }

    private static CellStyle estiloTitulo(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{27, 58, 92}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private static CellStyle estiloSubtitulo(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setItalic(true);
        f.setFontHeightInPoints((short) 10);
        f.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private static CellStyle estiloHeader(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 11);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{46, 80, (byte) 144}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(s, BorderStyle.THIN);
        return s;
    }

    private static CellStyle estiloData(XSSFWorkbook wb, boolean alt) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        if (alt) {
            s.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 235, (byte) 241, (byte) 252}, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(s, BorderStyle.THIN);
        return s;
    }

    private static CellStyle estiloMoney(XSSFWorkbook wb, boolean alt) {
        CellStyle s = estiloData(wb, alt);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private static CellStyle estiloInt(XSSFWorkbook wb, boolean alt) {
        CellStyle s = estiloData(wb, alt);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private static CellStyle estiloDate(XSSFWorkbook wb, boolean alt) {
        CellStyle s = estiloData(wb, alt);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private static void setBorders(CellStyle s, BorderStyle style) {
        s.setBorderTop(style);
        s.setBorderBottom(style);
        s.setBorderLeft(style);
        s.setBorderRight(style);
        s.setTopBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        s.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        s.setLeftBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        s.setRightBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
    }
}
