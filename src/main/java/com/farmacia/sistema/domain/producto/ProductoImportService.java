package com.farmacia.sistema.domain.producto;

import com.farmacia.sistema.tenant.TenantContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ProductoImportService {

    private static final Logger log = LoggerFactory.getLogger(ProductoImportService.class);

    private static final Set<String> HINT_TEXTS = Set.of(
            "auto si vacio", "auto si vacío", "obligatorio", "opcional"
    );

    private static final String[] HEADERS = {
            "Codigo", "Nombre", "Descripcion", "Laboratorio", "Presentacion",
            "Categoria", "Marca", "Unidad Medida", "Codigo Barras",
            "Precio Venta", "Costo Unitario", "Stock Actual", "Stock Minimo", "Stock Maximo"
    };

    private final ProductoRepository repository;

    public ProductoImportService(ProductoRepository repository) {
        this.repository = repository;
    }

    public byte[] generarPlantilla() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Productos");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle requiredStyle = wb.createCellStyle();
            Font reqFont = wb.createFont();
            reqFont.setItalic(true);
            reqFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            requiredStyle.setFont(reqFont);

            Row hintRow = sheet.createRow(1);
            String[] hints = {
                    "Auto si vacio", "Obligatorio", "Opcional", "Obligatorio", "Obligatorio",
                    "Opcional", "Opcional", "Opcional", "Opcional",
                    "Obligatorio", "Opcional", "Obligatorio", "Obligatorio", "Opcional"
            };
            for (int i = 0; i < hints.length; i++) {
                Cell cell = hintRow.createCell(i);
                cell.setCellValue(hints[i]);
                cell.setCellStyle(requiredStyle);
            }

            Object[][] ejemplos = {
                    {"", "Paracetamol 500mg", "Tabletas analgesicas", "Medifarma", "Caja x 100 tab", "Analgesicos", "Panadol", "CAJA", "7750075000100", 8.50, 4.20, 500, 50, 1000},
                    {"", "Ibuprofeno 400mg", "Antiinflamatorio", "Medifarma", "Caja x 100 tab", "Analgesicos", "Motrin", "CAJA", "7750075000117", 12.00, 5.80, 350, 40, 800},
                    {"", "Amoxicilina 500mg", "Antibiotico amplio espectro", "Medifarma", "Caja x 21 cap", "Antibioticos", "Amoxil", "CAJA", "7750075000148", 22.00, 10.50, 150, 25, 400},
                    {"", "Omeprazol 20mg", "Inhibidor bomba protones", "Medifarma", "Caja x 30 cap", "Gastricos", "Losec", "CAJA", "7750075000179", 16.00, 7.00, 400, 50, 800},
                    {"", "Vitamina C 500mg", "Acido ascorbico", "Medifarma", "Frasco x 100 tab", "Vitaminas", "Redoxon", "FRASCO", "7750075000193", 25.00, 12.00, 250, 30, 500},
            };

            for (int r = 0; r < ejemplos.length; r++) {
                Row row = sheet.createRow(r + 2);
                Object[] data = ejemplos[r];
                for (int c = 0; c < data.length; c++) {
                    Cell cell = row.createCell(c);
                    if (data[c] instanceof Number num) {
                        cell.setCellValue(num.doubleValue());
                    } else {
                        cell.setCellValue(String.valueOf(data[c]));
                    }
                }
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3500) sheet.setColumnWidth(i, 3500);
            }

            Sheet instrSheet = wb.createSheet("Instrucciones");
            String[] instrucciones = {
                    "INSTRUCCIONES DE USO:",
                    "",
                    "1. Llene los datos en la hoja 'Productos' desde la fila 3 (las filas 1 y 2 son encabezado y guia).",
                    "2. Los campos obligatorios son: Nombre, Laboratorio, Presentacion, Precio Venta, Stock Actual, Stock Minimo.",
                    "3. Si deja el campo 'Codigo' vacio, el sistema generara uno automaticamente.",
                    "4. Si un codigo ya existe en el sistema, ese producto se ACTUALIZARA (no se duplica).",
                    "5. Los campos de stock deben ser numeros enteros >= 0.",
                    "6. El precio de venta debe ser un numero decimal >= 0.",
                    "7. Puede borrar las filas de ejemplo antes de ingresar sus datos.",
                    "8. Guarde el archivo como .xlsx y subase al sistema.",
                    "",
                    "CAMPOS:",
                    "- Codigo: Codigo unico del producto (se autogenera si se deja vacio)",
                    "- Nombre: Nombre completo del medicamento (obligatorio)",
                    "- Descripcion: Descripcion adicional",
                    "- Laboratorio: Nombre del laboratorio fabricante (obligatorio)",
                    "- Presentacion: Forma de presentacion, ej: 'Caja x 20 tab' (obligatorio)",
                    "- Categoria: Grupo terapeutico, ej: 'Analgesicos', 'Antibioticos'",
                    "- Marca: Marca comercial",
                    "- Unidad Medida: CAJA, FRASCO, TUBO, UNIDAD, BOTELLA, etc.",
                    "- Codigo Barras: Codigo de barras EAN/UPC",
                    "- Precio Venta: Precio de venta al publico (obligatorio)",
                    "- Costo Unitario: Costo de compra unitario",
                    "- Stock Actual: Cantidad actual en inventario (obligatorio)",
                    "- Stock Minimo: Alerta cuando baje de esta cantidad (obligatorio)",
                    "- Stock Maximo: Limite maximo de inventario",
            };
            for (int i = 0; i < instrucciones.length; i++) {
                Row row = instrSheet.createRow(i);
                Cell cell = row.createCell(0);
                cell.setCellValue(instrucciones[i]);
                if (i == 0) {
                    CellStyle titleStyle = wb.createCellStyle();
                    Font titleFont = wb.createFont();
                    titleFont.setBold(true);
                    titleFont.setFontHeightInPoints((short) 14);
                    titleStyle.setFont(titleFont);
                    cell.setCellStyle(titleStyle);
                }
            }
            instrSheet.setColumnWidth(0, 25000);

            wb.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Genera un Excel con 2000 registros de productos de abarrotes listos para importar.
     * Formato compatible con la importación del sistema (mismas columnas que la plantilla).
     */
    public byte[] generarPlantillaAbarrotes2000() throws IOException {
        try (Workbook wb = new SXSSFWorkbook(200); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Productos");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle hintStyle = wb.createCellStyle();
            Font hintFont = wb.createFont();
            hintFont.setItalic(true);
            hintFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            hintStyle.setFont(hintFont);
            Row hintRow = sheet.createRow(1);
            String[] hints = {
                    "Auto si vacio", "Obligatorio", "Opcional", "Obligatorio", "Obligatorio",
                    "Opcional", "Opcional", "Opcional", "Opcional",
                    "Obligatorio", "Opcional", "Obligatorio", "Obligatorio", "Opcional"
            };
            for (int i = 0; i < hints.length; i++) {
                Cell cell = hintRow.createCell(i);
                cell.setCellValue(hints[i]);
                cell.setCellStyle(hintStyle);
            }

            String[] categorias = {"Abarrotes", "Granos y pastas", "Enlatados", "Lacteos", "Bebidas", "Snacks", "Limpieza", "Cuidado personal", "Dulces", "Conservas"};
            String[] marcas = {"Gloria", "Alicorp", "San Fernando", "Laive", "Coca-Cola", "Arcor", "Donofrio", "Molitalia", "Primor", "Frugos", "Kraft", "Nestle", "Unilever", "Aje", "Generica"};
            String[] presentaciones = {"Bolsa 1 kg", "Bolsa 500 g", "Paquete 1 kg", "Lata 400 g", "Botella 1 L", "Caja 12 und", "Frasco 500 g", "Sobre 200 g", "Unidad", "Pack 6 und", "Bolsa 2 kg", "Litro", "Botella 500 ml", "Bolsa 250 g", "Caja 24 und"};
            String[] unidades = {"BOLSA", "LATA", "BOTELLA", "CAJA", "FRASCO", "PAQUETE", "UNIDAD", "LITRO", "SOBRE", "PACK"};

            String[][] productosBase = {
                    {"Arroz", "Arroz blanco grado 2", "Granos y pastas"},
                    {"Azucar", "Azucar blanca refinada", "Abarrotes"},
                    {"Aceite", "Aceite vegetal para cocina", "Abarrotes"},
                    {"Fideos", "Pasta tipo tallarin", "Granos y pastas"},
                    {"Atun", "Atun en conserva", "Enlatados"},
                    {"Leche", "Leche entera UHT", "Lacteos"},
                    {"Galletas", "Galletas dulces", "Snacks"},
                    {"Cafe", "Cafe instantaneo", "Bebidas"},
                    {"Te", "Infusion en sobre", "Bebidas"},
                    {"Harina", "Harina de trigo", "Abarrotes"},
                    {"Frijol", "Frijol seco", "Granos y pastas"},
                    {"Lenteja", "Lenteja partida", "Granos y pastas"},
                    {"Garbanzo", "Garbanzo seco", "Granos y pastas"},
                    {"Avena", "Avena en hojuelas", "Abarrotes"},
                    {"Sardina", "Sardina en aceite", "Enlatados"},
                    {"Mayonesa", "Mayonesa", "Conservas"},
                    {"Salsa tomate", "Salsa de tomate", "Conservas"},
                    {"Jabon", "Jabon de lavar", "Limpieza"},
                    {"Detergente", "Detergente en polvo", "Limpieza"},
                    {"Shampoo", "Shampoo", "Cuidado personal"},
                    {"Pasta dental", "Pasta dental", "Cuidado personal"},
                    {"Chocolate", "Tableta de chocolate", "Dulces"},
                    {"Caramelo", "Caramelos surtidos", "Dulces"},
                    {"Gaseosa", "Bebida gaseosa", "Bebidas"},
                    {"Jugo", "Jugo en sobre", "Bebidas"},
                    {"Agua mineral", "Agua mineral sin gas", "Bebidas"},
                    {"Sal", "Sal refinada", "Abarrotes"},
                    {"Vinagre", "Vinagre blanco", "Abarrotes"},
                    {"Menestras", "Mezcla de menestras", "Granos y pastas"},
                    {"Conserva pollo", "Pollo en conserva", "Enlatados"},
                    {"Queso", "Queso fresco", "Lacteos"},
                    {"Yogurt", "Yogurt natural", "Lacteos"},
                    {"Mantequilla", "Mantequilla", "Lacteos"},
                    {"Pan", "Pan de molde", "Abarrotes"},
                    {"Bizcocho", "Bizcocho", "Snacks"},
                    {"Chifle", "Chifle platanito", "Snacks"},
                    {"Papas fritas", "Snack de papa", "Snacks"},
                    {"Cereal", "Cereal en caja", "Abarrotes"},
                    {"Miel", "Miel de abeja", "Abarrotes"},
                    {"Mermelada", "Mermelada de fruta", "Conservas"},
                    {"Palillos", "Palillos de dientes", "Abarrotes"},
                    {"Fosforos", "Caja de fosforos", "Abarrotes"},
                    {"Velas", "Vela para emergencias", "Abarrotes"},
                    {"Lavavajillas", "Detergente liquido", "Limpieza"},
                    {"Papel higienico", "Papel higienico", "Limpieza"},
                    {"Servilletas", "Servilletas", "Abarrotes"},
                    {"Acondicionador", "Acondicionador", "Cuidado personal"},
                    {"Desodorante", "Desodorante", "Cuidado personal"},
                    {"Crema dental", "Crema dental", "Cuidado personal"},
                    {"Jabon tocador", "Jabon de tocador", "Cuidado personal"},
                    {"Leche condensada", "Leche condensada", "Lacteos"},
                    {"Leche evaporada", "Leche evaporada", "Lacteos"},
                    {"Cafe molido", "Cafe molido", "Bebidas"},
            };

            int rowNum = 2;
            for (int n = 0; n < 2000; n++) {
                String[] base = productosBase[n % productosBase.length];
                int var = n / productosBase.length;
                String nombre = base[0] + " " + (var > 0 ? (presentaciones[var % presentaciones.length]) : presentaciones[n % presentaciones.length]);
                String desc = base[1];
                String cat = base[2];
                String lab = marcas[n % marcas.length];
                String pres = presentaciones[(n + 3) % presentaciones.length];
                String marca = marcas[(n + 7) % marcas.length];
                String um = unidades[n % unidades.length];
                long codBarNum = 50000000000L + n;
                String codBar = "77" + String.format("%011d", codBarNum);
                double precio = 1.50 + (n % 50) * 0.50 + (n % 10) * 2;
                if (precio > 45) precio = 3 + (n % 25);
                double costo = Math.round(precio * (0.5 + (n % 30) / 100.0) * 100) / 100.0;
                int stock = 20 + (n % 180);
                int stockMin = 5 + (n % 15);
                int stockMax = 100 + (n % 200);
                if (costo <= 0) costo = precio * 0.6;

                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue("");
                row.createCell(1).setCellValue(nombre);
                row.createCell(2).setCellValue(desc);
                row.createCell(3).setCellValue(lab);
                row.createCell(4).setCellValue(pres);
                row.createCell(5).setCellValue(cat);
                row.createCell(6).setCellValue(marca);
                row.createCell(7).setCellValue(um);
                row.createCell(8).setCellValue(codBar);
                row.createCell(9).setCellValue(precio);
                row.createCell(10).setCellValue(costo);
                row.createCell(11).setCellValue(stock);
                row.createCell(12).setCellValue(stockMin);
                row.createCell(13).setCellValue(stockMax);
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.setColumnWidth(i, 4000);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Genera un Excel con 2000 registros de productos de ferretería listos para importar.
     */
    public byte[] generarPlantillaFerreteria2000() throws IOException {
        try (Workbook wb = new SXSSFWorkbook(200); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Productos");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle hintStyle = wb.createCellStyle();
            Font hintFont = wb.createFont();
            hintFont.setItalic(true);
            hintFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            hintStyle.setFont(hintFont);
            Row hintRow = sheet.createRow(1);
            String[] hints = {
                    "Auto si vacio", "Obligatorio", "Opcional", "Obligatorio", "Obligatorio",
                    "Opcional", "Opcional", "Opcional", "Opcional",
                    "Obligatorio", "Opcional", "Obligatorio", "Obligatorio", "Opcional"
            };
            for (int i = 0; i < hints.length; i++) {
                Cell cell = hintRow.createCell(i);
                cell.setCellValue(hints[i]);
                cell.setCellStyle(hintStyle);
            }

            String[] marcas = {"Truper", "Tramontina", "Bosch", "Skil", "Black+Decker", "Ferguar", "Inca", "Gamma", "Sodimac", "Maestro", "Indeco", "Conex", "Volt", "Luminox", "Generica"};
            String[] presentaciones = {"Unidad", "Caja 10 und", "Caja 50 und", "Bolsa 1 kg", "Bolsa 5 kg", "Galon 4 L", "Tarro 1 L", "Rollo 100 m", "Paquete 6 und", "Set 12 und", "Fardo 20 und", "Tubo", "Metro", "Caja 100 und", "Pack 24 und"};
            String[] unidades = {"UNIDAD", "CAJA", "BOLSA", "GALON", "TARRO", "ROLLO", "PAQUETE", "SET", "METRO", "TUBO"};

            String[][] productosBase = {
                    {"Tornillo", "Tornillo para madera", "Tornilleria"},
                    {"Clavo", "Clavo punta Paris", "Tornilleria"},
                    {"Tuerca", "Tuerca hexagonal", "Tornilleria"},
                    {"Pernos", "Perno con tuerca", "Tornilleria"},
                    {"Pintura", "Pintura latex lavable", "Pinturas"},
                    {"Brocha", "Brocha para pintar", "Herramientas"},
                    {"Rodillo", "Rodillo pintor", "Herramientas"},
                    {"Cable", "Cable electrico flexible", "Electricidad"},
                    {"Foco", "Foco LED", "Electricidad"},
                    {"Interruptor", "Interruptor simple", "Electricidad"},
                    {"Cinta aislar", "Cinta aisladora", "Electricidad"},
                    {"Martillo", "Martillo carpintero", "Herramientas"},
                    {"Destornillador", "Destornillador plano", "Herramientas"},
                    {"Alicate", "Alicate universal", "Herramientas"},
                    {"Llave inglesa", "Llave ajustable", "Herramientas"},
                    {"Serrucho", "Serrucho de mano", "Herramientas"},
                    {"Lija", "Lija para madera", "Abrasivos"},
                    {"Silicona", "Silicona sellador", "Adhesivos"},
                    {"Pegamento", "Pegamento contacto", "Adhesivos"},
                    {"Cemento", "Cemento gris", "Construccion"},
                    {"Arena", "Arena gruesa", "Construccion"},
                    {"Ladrillo", "Ladrillo king kong", "Construccion"},
                    {"Tuberia PVC", "Tuberia PVC 1/2", "Plomeria"},
                    {"Codo PVC", "Codo 90 grados PVC", "Plomeria"},
                    {"Cinta teflon", "Cinta para roscas", "Plomeria"},
                    {"Llave lavatorio", "Llave mezcladora", "Plomeria"},
                    {"Filtro agua", "Filtro purificador", "Plomeria"},
                    {"Bomba agua", "Bomba sumergible", "Plomeria"},
                    {"Manguera", "Manguera jardin", "Jardin"},
                    {"Regadera", "Regadera metal", "Jardin"},
                    {"Guante", "Guante trabajo", "Seguridad"},
                    {"Lente seguridad", "Lentes protectores", "Seguridad"},
                    {"Mascarilla", "Mascarilla respirador", "Seguridad"},
                    {"Casco", "Casco obrero", "Seguridad"},
                    {"Taladro", "Taladro electrico", "Herramientas electricas"},
                    {"Sierra circular", "Sierra circular mano", "Herramientas electricas"},
                    {"Pulidora", "Pulidora angular", "Herramientas electricas"},
                    {"Caladora", "Caladora electrica", "Herramientas electricas"},
                    {"Atornillador", "Atornillador inalambrico", "Herramientas electricas"},
                    {"Extension", "Cable extension", "Electricidad"},
                    {"Multiconector", "Multiconector 3 salidas", "Electricidad"},
                    {"Fusible", "Fusible cartucho", "Electricidad"},
                    {"Caja registro", "Caja electrica", "Electricidad"},
                    {"Candado", "Candado seguridad", "Cerraduras"},
                    {"Bisagra", "Bisagra 4 pulgadas", "Cerraduras"},
                    {"Cerradura", "Cerradura embutir", "Cerraduras"},
                    {"Manija", "Manija puerta", "Cerraduras"},
                    {"Cadenas", "Cadena seguridad", "Cerraduras"},
                    {"Cerrajeria", "Juego cerrajeria", "Cerraduras"},
                    {"Escuadra", "Escuadra metalica", "Fijaciones"},
                    {"Soporte", "Soporte estante", "Fijaciones"},
                    {"Ancla", "Ancla expansion", "Fijaciones"},
                    {"Taco", "Taco plastico", "Fijaciones"},
                    {"Cinta metrica", "Cinta metrica 5 m", "Medicion"},
                    {"Nivel", "Nivel burbuja", "Medicion"},
                    {"Escalera", "Escalera aluminio", "Accesorios"},
                    {"Carretilla", "Carretilla obra", "Accesorios"},
                    {"Balde", "Balde plastico", "Accesorios"},
            };

            int rowNum = 2;
            for (int n = 0; n < 2000; n++) {
                String[] base = productosBase[n % productosBase.length];
                int var = n / productosBase.length;
                String nombre = base[0] + " " + (var > 0 ? presentaciones[var % presentaciones.length] : presentaciones[n % presentaciones.length]);
                String desc = base[1];
                String cat = base[2];
                String lab = marcas[n % marcas.length];
                String pres = presentaciones[(n + 3) % presentaciones.length];
                String marca = marcas[(n + 7) % marcas.length];
                String um = unidades[n % unidades.length];
                long codBarNum = 60000000000L + n;
                String codBar = "77" + String.format("%011d", codBarNum);
                double precio = 2.00 + (n % 45) * 0.80 + (n % 8) * 3;
                if (precio > 80) precio = 5 + (n % 40);
                double costo = Math.round(precio * (0.5 + (n % 35) / 100.0) * 100) / 100.0;
                if (costo <= 0) costo = precio * 0.6;
                int stock = 15 + (n % 120);
                int stockMin = 5 + (n % 12);
                int stockMax = 80 + (n % 150);

                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue("");
                row.createCell(1).setCellValue(nombre);
                row.createCell(2).setCellValue(desc);
                row.createCell(3).setCellValue(lab);
                row.createCell(4).setCellValue(pres);
                row.createCell(5).setCellValue(cat);
                row.createCell(6).setCellValue(marca);
                row.createCell(7).setCellValue(um);
                row.createCell(8).setCellValue(codBar);
                row.createCell(9).setCellValue(precio);
                row.createCell(10).setCellValue(costo);
                row.createCell(11).setCellValue(stock);
                row.createCell(12).setCellValue(stockMin);
                row.createCell(13).setCellValue(stockMax);
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.setColumnWidth(i, 4000);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Genera un Excel con 2000 registros de productos de farmacia listos para importar.
     */
    public byte[] generarPlantillaFarmacia2000() throws IOException {
        try (Workbook wb = new SXSSFWorkbook(200); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Productos");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle hintStyle = wb.createCellStyle();
            Font hintFont = wb.createFont();
            hintFont.setItalic(true);
            hintFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            hintStyle.setFont(hintFont);
            Row hintRow = sheet.createRow(1);
            String[] hints = {
                    "Auto si vacio", "Obligatorio", "Opcional", "Obligatorio", "Obligatorio",
                    "Opcional", "Opcional", "Opcional", "Opcional",
                    "Obligatorio", "Opcional", "Obligatorio", "Obligatorio", "Opcional"
            };
            for (int i = 0; i < hints.length; i++) {
                Cell cell = hintRow.createCell(i);
                cell.setCellValue(hints[i]);
                cell.setCellStyle(hintStyle);
            }

            String[] marcas = {"Bayer", "Pfizer", "Sanofi", "Roche", "Novartis", "GSK", "Medifarma", "Mintlab", "Pharmetica", "Eurofarma", "Bagó", "Lilly", "Merck", "Johnson", "Generica"};
            String[] presentaciones = {"Caja x 10 tab", "Caja x 20 tab", "Caja x 30 tab", "Caja x 100 tab", "Frasco x 60 ml", "Frasco x 120 ml", "Tubo x 30 g", "Sobre x 5 g", "Blister x 10 cap", "Caja x 21 cap", "Caja x 14 tab", "Frasco x 500 ml", "Ampolla", "Jeringa", "Caja x 50 und"};
            String[] unidades = {"CAJA", "FRASCO", "TUBO", "SOBRE", "BLISTER", "AMPOLLA", "UNIDAD", "BOTELLA", "POMADA", "JERINGA"};

            String[][] productosBase = {
                    {"Paracetamol", "Analgesico y antipiretico", "Analgesicos"},
                    {"Ibuprofeno", "Antiinflamatorio no esteroideo", "Analgesicos"},
                    {"Amoxicilina", "Antibiotico amplio espectro", "Antibioticos"},
                    {"Omeprazol", "Inhibidor bomba de protones", "Gastricos"},
                    {"Loratadina", "Antihistaminico", "Antialergicos"},
                    {"Diclofenaco", "Antiinflamatorio", "Analgesicos"},
                    {"Metformina", "Antidiabetico oral", "Antidiabeticos"},
                    {"Losartan", "Antihipertensivo", "Cardiovascular"},
                    {"Enalapril", "Inhibidor ECA", "Cardiovascular"},
                    {"Atorvastatina", "Hipolipemiante", "Cardiovascular"},
                    {"Amlodipino", "Bloqueante canales calcio", "Cardiovascular"},
                    {"Salbutamol", "Broncodilatador", "Respiratorio"},
                    {"Dexametasona", "Corticoide", "Corticoides"},
                    {"Prednisona", "Corticoide oral", "Corticoides"},
                    {"Ranitidina", "Antihistaminico H2", "Gastricos"},
                    {"Metoclopramida", "Antiemetico", "Gastricos"},
                    {"Hidroxido aluminio", "Antiacido", "Gastricos"},
                    {"Vitamina C", "Acido ascorbico", "Vitaminas"},
                    {"Vitamina D", "Colecalciferol", "Vitaminas"},
                    {"Vitamina B12", "Cianocobalamina", "Vitaminas"},
                    {"Complejo B", "Vitaminas del grupo B", "Vitaminas"},
                    {"Hierro", "Sulfato ferroso", "Suplementos"},
                    {"Acido folico", "Suplemento prenatal", "Suplementos"},
                    {"Calcio", "Carbonato de calcio", "Suplementos"},
                    {"Magnesio", "Citrato de magnesio", "Suplementos"},
                    {"Zinc", "Sulfato de zinc", "Suplementos"},
                    {"Omega 3", "Acidos grasos", "Suplementos"},
                    {"Propolis", "Extracto propoleo", "Naturales"},
                    {"Jarabe tos", "Antitusivo", "Respiratorio"},
                    {"Descongestionante", "Pseudofedrina", "Respiratorio"},
                    {"Antigripal", "Combinacion gripal", "Respiratorio"},
                    {"Naproxeno", "AINE", "Analgesicos"},
                    {"Ketoprofeno", "Antiinflamatorio", "Analgesicos"},
                    {"Tramadol", "Analgesico opioide", "Analgesicos"},
                    {"Clonazepam", "Ansiolitico", "Neurologico"},
                    {"Sertralina", "Antidepresivo ISRS", "Neurologico"},
                    {"Fluoxetina", "Antidepresivo", "Neurologico"},
                    {"Diazepam", "Ansiolitico", "Neurologico"},
                    {"Gabapentina", "Anticonvulsivo", "Neurologico"},
                    {"Carbamazepina", "Anticonvulsivo", "Neurologico"},
                    {"Metronidazol", "Antibiotico antiparasitario", "Antibioticos"},
                    {"Azitromicina", "Antibiotico macrolido", "Antibioticos"},
                    {"Ciprofloxacino", "Antibiotico fluoroquinolona", "Antibioticos"},
                    {"Amoxicilina clavulanico", "Antibiotico combinado", "Antibioticos"},
                    {"Gentamicina", "Antibiotico topico", "Antibioticos"},
                    {"Clotrimazol", "Antimicotico", "Antimicoticos"},
                    {"Fluconazol", "Antimicotico sistemico", "Antimicoticos"},
                    {"Nistatina", "Antimicotico", "Antimicoticos"},
                    {"Hidrocortisona", "Corticoide topico", "Dermatologico"},
                    {"Betametasona", "Corticoide crema", "Dermatologico"},
                    {"Mupirocina", "Antibiotico topico", "Dermatologico"},
                    {"Alcohol", "Alcohol etilico 70", "Antisepticos"},
                    {"Yodo", "Povidona yodada", "Antisepticos"},
                    {"Agua oxigenada", "Peroxido hidrogeno", "Antisepticos"},
                    {"Gasas", "Gasas esteriles", "Curacion"},
                    {"Vendas", "Venda elastica", "Curacion"},
                    {"Tiritas", "Apósitos adhesivos", "Curacion"},
                    {"Termometro", "Termometro digital", "Insumos"},
                    {"Jeringa", "Jeringa desechable", "Insumos"},
                    {"Guantes", "Guantes latex", "Insumos"},
                    {"Mascarilla", "Mascarilla quirurgica", "Insumos"},
                    {"Condones", "Preservativo", "Salud sexual"},
                    {"Anticonceptivo", "Anticonceptivo oral", "Salud sexual"},
                    {"Shampoo anticaspa", "Anticaspa", "Cuidado personal"},
                    {"Crema corporal", "Hidratante corporal", "Cuidado personal"},
                    {"Protector solar", "FPS 50", "Cuidado personal"},
                    {"Repelente", "Repelente insectos", "Cuidado personal"},
            };

            int rowNum = 2;
            for (int n = 0; n < 2000; n++) {
                String[] base = productosBase[n % productosBase.length];
                int var = n / productosBase.length;
                String nombre = base[0] + " " + (var > 0 ? presentaciones[var % presentaciones.length] : presentaciones[n % presentaciones.length]);
                String desc = base[1];
                String cat = base[2];
                String lab = marcas[n % marcas.length];
                String pres = presentaciones[(n + 3) % presentaciones.length];
                String marca = marcas[(n + 7) % marcas.length];
                String um = unidades[n % unidades.length];
                long codBarNum = 70000000000L + n;
                String codBar = "77" + String.format("%011d", codBarNum);
                double precio = 3.50 + (n % 40) * 0.80 + (n % 12) * 2.5;
                if (precio > 65) precio = 5 + (n % 35);
                double costo = Math.round(precio * (0.45 + (n % 40) / 100.0) * 100) / 100.0;
                if (costo <= 0) costo = precio * 0.55;
                int stock = 25 + (n % 150);
                int stockMin = 10 + (n % 20);
                int stockMax = 120 + (n % 200);

                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue("");
                row.createCell(1).setCellValue(nombre);
                row.createCell(2).setCellValue(desc);
                row.createCell(3).setCellValue(lab);
                row.createCell(4).setCellValue(pres);
                row.createCell(5).setCellValue(cat);
                row.createCell(6).setCellValue(marca);
                row.createCell(7).setCellValue(um);
                row.createCell(8).setCellValue(codBar);
                row.createCell(9).setCellValue(precio);
                row.createCell(10).setCellValue(costo);
                row.createCell(11).setCellValue(stock);
                row.createCell(12).setCellValue(stockMin);
                row.createCell(13).setCellValue(stockMax);
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.setColumnWidth(i, 4000);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generarExportacion(List<Producto> productos) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Productos");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            String[] exportHeaders = {"Codigo", "Nombre", "Descripcion", "Laboratorio", "Presentacion",
                    "Categoria", "Marca", "Unidad Medida", "Codigo Barras",
                    "Precio Venta", "Costo Unitario", "Stock Actual", "Stock Minimo", "Stock Maximo", "Activo"};
            for (int i = 0; i < exportHeaders.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(exportHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Producto p : productos) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getCodigo() != null ? p.getCodigo() : "");
                row.createCell(1).setCellValue(p.getNombre() != null ? p.getNombre() : "");
                row.createCell(2).setCellValue(p.getDescripcion() != null ? p.getDescripcion() : "");
                row.createCell(3).setCellValue(p.getLaboratorio() != null ? p.getLaboratorio() : "");
                row.createCell(4).setCellValue(p.getPresentacion() != null ? p.getPresentacion() : "");
                row.createCell(5).setCellValue(p.getCategoria() != null ? p.getCategoria() : "");
                row.createCell(6).setCellValue(p.getMarca() != null ? p.getMarca() : "");
                row.createCell(7).setCellValue(p.getUnidadMedida() != null ? p.getUnidadMedida() : "");
                row.createCell(8).setCellValue(p.getCodigoBarras() != null ? p.getCodigoBarras() : "");
                row.createCell(9).setCellValue(p.getPrecioVenta() != null ? p.getPrecioVenta().doubleValue() : 0);
                row.createCell(10).setCellValue(p.getCostoUnitario() != null ? p.getCostoUnitario().doubleValue() : 0);
                row.createCell(11).setCellValue(p.getStockActual() != null ? p.getStockActual() : 0);
                row.createCell(12).setCellValue(p.getStockMinimo() != null ? p.getStockMinimo() : 0);
                row.createCell(13).setCellValue(p.getStockMaximo() != null ? p.getStockMaximo() : 0);
                row.createCell(14).setCellValue(p.isActivo() ? "SI" : "NO");
            }

            for (int i = 0; i < exportHeaders.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3500) sheet.setColumnWidth(i, 3500);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    public ImportResult importarDesdeExcel(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo esta vacio");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new IllegalArgumentException("Solo se aceptan archivos Excel (.xlsx o .xls)");
        }

        ImportResult result = new ImportResult();
        importCounter = -1;

        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {

            Sheet sheet = wb.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            for (int i = 2; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                try {
                    Producto producto = parseRow(row, i + 1);
                    guardarProducto(producto, result);
                } catch (Exception e) {
                    result.addError("Fila " + (i + 1) + ": " + e.getMessage());
                    log.warn("Error importando fila {}: {}", i + 1, e.getMessage());
                }
            }
        }

        log.info("Importacion completada: {} creados, {} actualizados, {} errores",
                result.getCreados(), result.getActualizados(), result.getErrores().size());
        return result;
    }

    private boolean isHintText(String value) {
        return value != null && HINT_TEXTS.contains(value.toLowerCase().trim());
    }

    private String cleanValue(String value) {
        if (value == null) return null;
        if (isHintText(value)) return null;
        return value.trim().isEmpty() ? null : value.trim();
    }

    private Producto parseRow(Row row, int rowNum) {
        String codigo = cleanValue(getStringValue(row, 0));
        String nombre = cleanValue(getStringValue(row, 1));
        String descripcion = cleanValue(getStringValue(row, 2));
        String laboratorio = cleanValue(getStringValue(row, 3));
        String presentacion = cleanValue(getStringValue(row, 4));
        String categoria = cleanValue(getStringValue(row, 5));
        String marca = cleanValue(getStringValue(row, 6));
        String unidadMedida = cleanValue(getStringValue(row, 7));
        String codigoBarras = cleanValue(getStringValue(row, 8));
        BigDecimal precioVenta = getDecimalValue(row, 9);
        BigDecimal costoUnitario = getDecimalValue(row, 10);
        Integer stockActual = getIntValue(row, 11);
        Integer stockMinimo = getIntValue(row, 12);
        Integer stockMaximo = getIntValue(row, 13);

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (laboratorio == null || laboratorio.isBlank()) {
            throw new IllegalArgumentException("El laboratorio es obligatorio");
        }
        if (presentacion == null || presentacion.isBlank()) {
            throw new IllegalArgumentException("La presentacion es obligatoria");
        }
        if (precioVenta == null || precioVenta.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio de venta es obligatorio y debe ser >= 0");
        }
        if (stockActual == null || stockActual < 0) {
            throw new IllegalArgumentException("El stock actual es obligatorio y debe ser >= 0");
        }
        if (stockMinimo == null || stockMinimo < 0) {
            throw new IllegalArgumentException("El stock minimo es obligatorio y debe ser >= 0");
        }

        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setLaboratorio(laboratorio);
        p.setPresentacion(presentacion);
        p.setCategoria(categoria);
        p.setMarca(marca);
        p.setUnidadMedida(unidadMedida);
        p.setCodigoBarras(codigoBarras);
        p.setPrecioVenta(precioVenta);
        p.setCostoUnitario(costoUnitario);
        p.setStockActual(stockActual);
        p.setStockMinimo(stockMinimo);
        p.setStockMaximo(stockMaximo);
        p.setActivo(true);
        return p;
    }

    private void guardarProducto(Producto datos, ImportResult result) {
        Long tenantId = TenantContext.getTenantId();
        if (datos.getCodigo() != null && !datos.getCodigo().isBlank()) {
            boolean existe = tenantId != null
                    ? repository.existsByTenantIdAndCodigo(tenantId, datos.getCodigo())
                    : repository.existsByCodigo(datos.getCodigo());
            if (existe) {
                Optional<Producto> existente = tenantId != null
                        ? repository.findByTenantIdAndCodigo(tenantId, datos.getCodigo())
                        : repository.findByCodigo(datos.getCodigo());
                if (existente.isPresent()) {
                    Producto p = existente.get();
                    p.setNombre(datos.getNombre());
                    p.setDescripcion(datos.getDescripcion());
                    p.setLaboratorio(datos.getLaboratorio());
                    p.setPresentacion(datos.getPresentacion());
                    p.setCategoria(datos.getCategoria());
                    p.setMarca(datos.getMarca());
                    p.setUnidadMedida(datos.getUnidadMedida());
                    p.setCodigoBarras(datos.getCodigoBarras());
                    p.setPrecioVenta(datos.getPrecioVenta());
                    p.setCostoUnitario(datos.getCostoUnitario());
                    p.setStockActual(datos.getStockActual());
                    p.setStockMinimo(datos.getStockMinimo());
                    p.setStockMaximo(datos.getStockMaximo());
                    repository.save(p);
                    result.incrementActualizados();
                    return;
                }
            }
        } else {
            datos.setCodigo(generarCodigo());
        }
        repository.save(datos);
        result.incrementCreados();
    }

    private long importCounter = -1;

    private String generarCodigo() {
        Long tenantId = TenantContext.getTenantId();
        if (importCounter < 0) {
            importCounter = tenantId != null
                    ? repository.findByTenantId(tenantId).size() + 1
                    : repository.count() + 1;
        }
        String codigo;
        do {
            codigo = "PRD-" + String.format("%05d", importCounter);
            importCounter++;
        } while (tenantId != null
                ? repository.existsByTenantIdAndCodigo(tenantId, codigo)
                : repository.existsByCodigo(codigo));
        return codigo;
    }

    private boolean isRowEmpty(Row row) {
        boolean allEmpty = true;
        boolean allHints = true;
        for (int c = 0; c < 14; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getStringValue(row, c);
                if (val != null && !val.isBlank()) {
                    allEmpty = false;
                    if (!isHintText(val)) {
                        allHints = false;
                    }
                }
            }
        }
        return allEmpty || allHints;
    }

    private String getStringValue(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim().isEmpty() ? null : cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private BigDecimal getDecimalValue(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> {
                String val = cell.getStringCellValue().trim();
                yield val.isEmpty() ? null : new BigDecimal(val.replace(",", "."));
            }
            default -> null;
        };
    }

    private Integer getIntValue(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                String val = cell.getStringCellValue().trim();
                yield val.isEmpty() ? null : Integer.parseInt(val);
            }
            default -> null;
        };
    }

    public static class ImportResult {
        private int creados = 0;
        private int actualizados = 0;
        private final List<String> errores = new ArrayList<>();

        public void incrementCreados() { creados++; }
        public void incrementActualizados() { actualizados++; }
        public void addError(String error) { errores.add(error); }

        public int getCreados() { return creados; }
        public int getActualizados() { return actualizados; }
        public List<String> getErrores() { return errores; }
        public int getTotal() { return creados + actualizados; }
        public boolean hasErrors() { return !errores.isEmpty(); }
    }
}
