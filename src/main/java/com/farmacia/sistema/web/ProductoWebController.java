package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.auditoria.AuditoriaService;
import com.farmacia.sistema.domain.inventario.InventarioService;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.security.TenantUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.farmacia.sistema.domain.producto.ProductoImportService;
import com.farmacia.sistema.domain.producto.ProductoService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.farmacia.sistema.export.ExcelExportUtil;
import com.farmacia.sistema.export.PdfExportUtil;
import org.apache.poi.ss.usermodel.Workbook;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.farmacia.sistema.domain.auditoria.AuditoriaService.cmp;
import static com.farmacia.sistema.domain.auditoria.AuditoriaService.nuevoCambios;

@Controller
@RequestMapping("/web/productos")
public class ProductoWebController {

    private static final Logger log = LoggerFactory.getLogger(ProductoWebController.class);

    private final ProductoService productoService;
    private final InventarioService inventarioService;
    private final AuditoriaService auditoriaService;
    private final ProductoImportService importService;

    public ProductoWebController(ProductoService productoService,
                                 InventarioService inventarioService,
                                 AuditoriaService auditoriaService,
                                 ProductoImportService importService) {
        this.productoService = productoService;
        this.inventarioService = inventarioService;
        this.auditoriaService = auditoriaService;
        this.importService = importService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "q", required = false) String filtroNombre,
                         @RequestParam(value = "editId", required = false) Long editId,
                         @RequestParam(value = "error", required = false) String error,
                         Model model) {
        if ("sin_permiso".equals(error)) {
            model.addAttribute("error", "No tiene permiso para acceder a esa opción.");
        }
        List<Producto> productos = productoService.listarTodos();
        if (productos == null) {
            productos = List.of();
        }
        if (filtroNombre != null && !filtroNombre.isBlank()) {
            String criterio = filtroNombre.toLowerCase(Locale.ROOT);
            productos = productos.stream()
                    .filter(p -> p != null && p.getNombre() != null &&
                            p.getNombre().toLowerCase(Locale.ROOT).contains(criterio))
                    .toList();
        }

        Producto productoForm;
        try {
            productoForm = (editId != null)
                    ? productoService.obtenerPorId(editId)
                    : new Producto();
        } catch (Exception e) {
            productoForm = new Producto();
        }

        // Para vendedor con sucursal: mostrar stock de su sucursal en lugar del global
        Map<Long, Integer> stockPorProducto = null;
        Long sucursalIdUsuario = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof TenantUserDetails tud) {
            sucursalIdUsuario = tud.getSucursalId();
        }
        if (sucursalIdUsuario != null) {
            Map<com.farmacia.sistema.domain.producto.Producto, Integer> stockSucursal = inventarioService.stockPorSucursal(sucursalIdUsuario);
            stockPorProducto = new HashMap<>();
            for (Map.Entry<com.farmacia.sistema.domain.producto.Producto, Integer> e : stockSucursal.entrySet()) {
                if (e.getKey() != null && e.getKey().getId() != null) {
                    stockPorProducto.put(e.getKey().getId(), e.getValue());
                }
            }
        }
        if (stockPorProducto != null) {
            model.addAttribute("stockPorProducto", stockPorProducto);
        }

        // KPIs coherentes con lo que ve el usuario en la grilla
        int total = productos.size();
        int stockBajo = 0;
        int activos = 0;
        int sobreMax = 0;
        for (Producto p : productos) {
            if (p == null) continue;
            Integer stock = null;
            if (stockPorProducto != null) {
                stock = stockPorProducto.get(p.getId());
            }
            if (stock == null) {
                stock = p.getStockActual();
            }
            Integer min = p.getStockMinimo();
            Integer max = p.getStockMaximo();
            if (p.isActivo()) {
                activos++;
            }
            if (stock != null && min != null && stock < min) {
                stockBajo++;
            }
            if (stock != null && max != null && stock > max) {
                sobreMax++;
            }
        }
        model.addAttribute("kpiTotalProductos", total);
        model.addAttribute("kpiStockBajo", stockBajo);
        model.addAttribute("kpiActivos", activos);
        model.addAttribute("kpiSobreMax", sobreMax);

        model.addAttribute("productos", productos);
        model.addAttribute("producto", productoForm);
        model.addAttribute("editando", editId != null);
        model.addAttribute("filtroNombre", filtroNombre);
        // Valores por defecto para mensajes de importación (evitan null en la plantilla)
        if (!model.containsAttribute("importExito")) model.addAttribute("importExito", (String) null);
        if (!model.containsAttribute("importError")) model.addAttribute("importError", (String) null);
        if (!model.containsAttribute("importErrores")) model.addAttribute("importErrores", List.<String>of());
        return "productos";
    }

    @PostMapping
    public String crear(@ModelAttribute("producto") @Valid Producto producto,
                        BindingResult bindingResult,
                        Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productos", productoService.listarTodos());
            model.addAttribute("editando", producto.getId() != null);
            model.addAttribute("filtroNombre", "");
            model.addAttribute("errorProducto", "Revise los campos obligatorios marcados con *.");
            return "productos";
        }

        try {
            if (producto.getId() == null) {
                Producto guardado = productoService.crear(producto);
                inventarioService.asegurarStockInicialEnPrincipal(guardado);
                auditoriaService.registrarCreacion("producto", guardado.getNombre(),
                        "Código: " + guardado.getCodigo()
                                + " | Laboratorio: " + guardado.getLaboratorio()
                                + " | Precio: " + guardado.getPrecioVenta()
                                + " | Stock: " + guardado.getStockActual());
            } else {
                Producto anterior = productoService.obtenerPorId(producto.getId());
                Map<String, String[]> cambios = nuevoCambios();
                cmp(cambios, "Nombre", anterior.getNombre(), producto.getNombre());
                cmp(cambios, "Laboratorio", anterior.getLaboratorio(), producto.getLaboratorio());
                cmp(cambios, "Presentación", anterior.getPresentacion(), producto.getPresentacion());
                cmp(cambios, "Categoría", anterior.getCategoria(), producto.getCategoria());
                cmp(cambios, "Marca", anterior.getMarca(), producto.getMarca());
                cmp(cambios, "Precio", anterior.getPrecioVenta(), producto.getPrecioVenta());
                cmp(cambios, "Costo", anterior.getCostoUnitario(), producto.getCostoUnitario());
                cmp(cambios, "Stock actual", anterior.getStockActual(), producto.getStockActual());
                cmp(cambios, "Stock mínimo", anterior.getStockMinimo(), producto.getStockMinimo());
                cmp(cambios, "Stock máximo", anterior.getStockMaximo(), producto.getStockMaximo());
                cmp(cambios, "Activo", anterior.isActivo(), producto.isActivo());

                productoService.actualizar(producto.getId(), producto);
                inventarioService.asegurarStockInicialEnPrincipal(producto);
                auditoriaService.registrarActualizacion("producto", anterior.getNombre(), cambios);
            }
        } catch (Exception e) {
            model.addAttribute("productos", productoService.listarTodos());
            model.addAttribute("editando", producto.getId() != null);
            model.addAttribute("filtroNombre", "");
            model.addAttribute("errorProducto", e.getMessage() != null ? e.getMessage() : "Error al guardar el producto.");
            return "productos";
        }
        return "redirect:/web/productos";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Producto p = productoService.obtenerPorId(id);
            auditoriaService.registrarEliminacion("producto", p.getNombre(),
                    "Código: " + p.getCodigo()
                            + " | Laboratorio: " + p.getLaboratorio()
                            + " | Precio: " + p.getPrecioVenta());
            productoService.eliminar(id);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("No se pudo eliminar producto id={}: restricción de integridad referencial", id);
            ra.addFlashAttribute("errorProducto",
                    "No se puede eliminar este producto porque tiene ventas, compras u otros registros asociados. "
                  + "Puede desactivarlo en su lugar.");
            return "redirect:/web/productos";
        } catch (Exception e) {
            log.error("Error al eliminar producto id={}", id, e);
            ra.addFlashAttribute("errorProducto", "Error al eliminar el producto: " + e.getMessage());
            return "redirect:/web/productos";
        }
        return "redirect:/web/productos";
    }

    @GetMapping("/excel/plantilla")
    public void descargarPlantilla(HttpServletResponse response) throws Exception {
        byte[] data = importService.generarPlantilla();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plantilla_productos.xlsx");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    @GetMapping("/excel/plantilla-abarrotes-2000")
    public void descargarPlantillaAbarrotes2000(HttpServletResponse response) {
        try {
            byte[] data = importService.generarPlantillaAbarrotes2000();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=abarrotes_2000_registros.xlsx");
            response.setContentLength(data.length);
            response.getOutputStream().write(data);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("Error al generar Excel Abarrotes 2000", e);
            throw new RuntimeException("No se pudo generar el Excel. Revisa el log del servidor.", e);
        }
    }

    @GetMapping("/excel/plantilla-ferreteria-2000")
    public void descargarPlantillaFerreteria2000(HttpServletResponse response) {
        try {
            byte[] data = importService.generarPlantillaFerreteria2000();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ferreteria_2000_registros.xlsx");
            response.setContentLength(data.length);
            response.getOutputStream().write(data);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("Error al generar Excel Ferreteria 2000", e);
            throw new RuntimeException("No se pudo generar el Excel. Revisa el log del servidor.", e);
        }
    }

    @GetMapping("/excel/plantilla-farmacia-2000")
    public void descargarPlantillaFarmacia2000(HttpServletResponse response) {
        try {
            byte[] data = importService.generarPlantillaFarmacia2000();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=farmacia_2000_registros.xlsx");
            response.setContentLength(data.length);
            response.getOutputStream().write(data);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("Error al generar Excel Farmacia 2000", e);
            throw new RuntimeException("No se pudo generar el Excel. Revisa el log del servidor.", e);
        }
    }

    @GetMapping("/excel/exportar")
    public void exportarExcel(HttpServletResponse response) throws Exception {
        List<Producto> productos = productoService.listarTodos();
        byte[] data = importService.generarExportacion(productos);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=productos_catalogo.xlsx");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    @GetMapping("/pdf")
    public void exportarPdf(HttpServletResponse response) throws Exception {
        List<Producto> productos = productoService.listarTodos();
        String[] headers = {"Código", "Nombre", "Laboratorio", "Categoría", "Marca",
                "Presentación", "Precio Venta", "Costo", "Stock", "Stock Mín", "Activo"};
        List<Object[]> filas = new ArrayList<>();
        for (Producto p : productos) {
            filas.add(new Object[]{
                    p.getCodigo(), p.getNombre(), p.getLaboratorio(), p.getCategoria(),
                    p.getMarca(), p.getPresentacion(), p.getPrecioVenta(),
                    p.getCostoUnitario(), p.getStockActual(), p.getStockMinimo(), p.isActivo()
            });
        }
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=productos_" + LocalDate.now() + ".pdf");
        PdfExportUtil.crearReporte(response.getOutputStream(), "Catálogo de Productos",
                "Exportado el " + LocalDate.now(), headers, filas, null);
    }

    @GetMapping("/excel/profesional")
    public void exportarExcelProfesional(HttpServletResponse response) throws Exception {
        List<Producto> productos = productoService.listarTodos();
        String[] headers = {"Código", "Nombre", "Laboratorio", "Categoría", "Marca",
                "Presentación", "Precio Venta", "Costo", "Stock", "Stock Mín", "Stock Máx", "Activo"};
        List<Object[]> filas = new ArrayList<>();
        for (Producto p : productos) {
            filas.add(new Object[]{
                    p.getCodigo(), p.getNombre(), p.getLaboratorio(), p.getCategoria(),
                    p.getMarca(), p.getPresentacion(), p.getPrecioVenta(),
                    p.getCostoUnitario(), p.getStockActual(), p.getStockMinimo(),
                    p.getStockMaximo(), p.isActivo()
            });
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=productos_" + LocalDate.now() + ".xlsx");
        try (Workbook wb = ExcelExportUtil.crearReporte("Catálogo de Productos",
                "Exportado el " + LocalDate.now(), headers, filas)) {
            wb.write(response.getOutputStream());
        }
    }

    @PostMapping("/excel/importar")
    public String importarExcel(@RequestParam("archivo") MultipartFile archivo,
                                RedirectAttributes ra) {
        try {
            ProductoImportService.ImportResult result = importService.importarDesdeExcel(archivo);

            StringBuilder msg = new StringBuilder();
            msg.append("Importacion completada: ");
            msg.append(result.getCreados()).append(" creados, ");
            msg.append(result.getActualizados()).append(" actualizados.");

            if (result.hasErrors()) {
                msg.append(" Errores: ").append(result.getErrores().size()).append(".");
                ra.addFlashAttribute("importErrores", result.getErrores());
            }

            ra.addFlashAttribute("importExito", msg.toString());
            auditoriaService.registrarCreacion("importacion_excel", "Productos",
                    "Creados: " + result.getCreados()
                            + " | Actualizados: " + result.getActualizados()
                            + " | Errores: " + result.getErrores().size());

            log.info("Importacion Excel: {} creados, {} actualizados, {} errores",
                    result.getCreados(), result.getActualizados(), result.getErrores().size());

        } catch (Exception e) {
            log.error("Error en importacion Excel: {}", e.getMessage(), e);
            ra.addFlashAttribute("importError", "Error al importar: " + e.getMessage());
        }
        return "redirect:/web/productos";
    }
}
