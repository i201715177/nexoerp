package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.inventario.*;
import com.farmacia.sistema.domain.producto.Producto;
import org.springframework.security.core.context.SecurityContextHolder;
import com.farmacia.sistema.domain.producto.ProductoService;
import com.farmacia.sistema.export.ExcelExportUtil;
import com.farmacia.sistema.export.PdfExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/web/inventario")
public class InventarioWebController {

    private final InventarioService inventarioService;
    private final ProductoService productoService;
    private final com.farmacia.sistema.domain.auditoria.AuditoriaService auditoriaService;

    public InventarioWebController(InventarioService inventarioService,
                                   ProductoService productoService,
                                   com.farmacia.sistema.domain.auditoria.AuditoriaService auditoriaService) {
        this.inventarioService = inventarioService;
        this.productoService = productoService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public String dashboard(Model model) {
        long bajoMinimo = inventarioService.contarProductosBajoMinimo();
        long sobreMaximo = inventarioService.contarProductosSobreMaximo();
        List<LoteProducto> porVencer = inventarioService.lotesPorVencer(30);
        List<Almacen> almacenes = inventarioService.listarAlmacenes();
        int pendientesRecepcion = inventarioService.listarPendientesDeRecepcion(null).size();
        model.addAttribute("productosBajoMinimo", bajoMinimo);
        model.addAttribute("productosSobreMaximo", sobreMaximo);
        model.addAttribute("lotesPorVencer", porVencer.size());
        model.addAttribute("almacenes", almacenes);
        model.addAttribute("pendientesRecepcionCount", pendientesRecepcion);
        return "inventario";
    }

    @GetMapping("/excel")
    public void exportarExcel(HttpServletResponse response) throws Exception {
        List<Producto> productos = productoService.listarTodos();
        String[] headers = {"Código", "Producto", "Stock Actual", "Stock Mín",
                "Stock Máx", "Estado Stock"};
        List<Object[]> filas = new ArrayList<>();
        for (Producto p : productos) {
            String estadoStock = "Normal";
            if (p.getStockActual() != null && p.getStockMinimo() != null
                    && p.getStockActual() <= p.getStockMinimo()) {
                estadoStock = "Bajo mínimo";
            } else if (p.getStockActual() != null && p.getStockMaximo() != null
                    && p.getStockActual() >= p.getStockMaximo()) {
                estadoStock = "Sobre máximo";
            }
            filas.add(new Object[]{
                    p.getCodigo(), p.getNombre(), p.getStockActual(),
                    p.getStockMinimo(), p.getStockMaximo(), estadoStock
            });
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=inventario_" + LocalDate.now() + ".xlsx");
        try (Workbook wb = ExcelExportUtil.crearReporte("Reporte de Inventario",
                "Exportado el " + LocalDate.now(), headers, filas)) {
            wb.write(response.getOutputStream());
        }
    }

    @GetMapping("/pdf")
    public void exportarPdf(HttpServletResponse response) throws Exception {
        List<Producto> productos = productoService.listarTodos();
        String[] headers = {"Código", "Producto", "Stock Actual", "Stock Mín",
                "Stock Máx", "Estado"};
        List<Object[]> filas = new ArrayList<>();
        for (Producto p : productos) {
            String estadoStock = "Normal";
            if (p.getStockActual() != null && p.getStockMinimo() != null
                    && p.getStockActual() <= p.getStockMinimo()) {
                estadoStock = "Bajo mínimo";
            } else if (p.getStockActual() != null && p.getStockMaximo() != null
                    && p.getStockActual() >= p.getStockMaximo()) {
                estadoStock = "Sobre máximo";
            }
            filas.add(new Object[]{
                    p.getCodigo(), p.getNombre(), p.getStockActual(),
                    p.getStockMinimo(), p.getStockMaximo(), estadoStock
            });
        }
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=inventario_" + LocalDate.now() + ".pdf");
        PdfExportUtil.crearReporte(response.getOutputStream(), "Reporte de Inventario",
                "Exportado el " + LocalDate.now(), headers, filas, null);
    }

    @GetMapping("/kardex")
    public String kardex(@RequestParam("productoId") Long productoId, Model model) {
        Producto producto = productoService.obtenerPorId(productoId);
        List<InventarioMovimiento> movimientos = inventarioService.kardexPorProducto(productoId);
        model.addAttribute("producto", producto);
        model.addAttribute("movimientos", movimientos);
        return "inventario-kardex";
    }

    @GetMapping("/ajustes")
    public String ajustes(Model model) {
        model.addAttribute("productos", productoService.listarTodos());
        return "inventario-ajustes";
    }

    @PostMapping("/ajustes")
    public String ejecutarAjuste(@RequestParam("productoId") Long productoId,
                                 @RequestParam("cantidad") int cantidad,
                                 @RequestParam(value = "motivo", required = false) String motivo,
                                 RedirectAttributes ra) {
        try {
            String prodNombre = "";
            int stockAntes = 0;
            try {
                var p = productoService.obtenerPorId(productoId);
                prodNombre = p.getNombre();
                stockAntes = p.getStockActual() != null ? p.getStockActual() : 0;
            } catch (Exception ignored) {}
            inventarioService.ajustar(productoId, cantidad, motivo);
            auditoriaService.registrarAccion("PUT", "Ajuste de inventario",
                    prodNombre + " | Stock antes: " + stockAntes
                            + " → después: " + (stockAntes + cantidad)
                            + " | Cantidad: " + (cantidad >= 0 ? "+" : "") + cantidad
                            + (motivo != null && !motivo.isBlank() ? " | Motivo: " + motivo : ""));
            ra.addFlashAttribute("mensaje", "Ajuste registrado correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/inventario/ajustes";
    }

    @GetMapping("/transferencias")
    public String transferencias(@RequestParam(value = "almacenDestinoId", required = false) Long almacenDestinoId,
                                Model model) {
        model.addAttribute("almacenes", inventarioService.listarAlmacenes());
        model.addAttribute("productos", productoService.listarTodos());
        model.addAttribute("almacenDestinoFiltro", almacenDestinoId);
        try {
            model.addAttribute("pendientesRecepcion", inventarioService.listarPendientesDeRecepcion(almacenDestinoId));
            model.addAttribute("historialTransferencias", inventarioService.listarHistorialTransferencias());
        } catch (Exception e) {
            model.addAttribute("pendientesRecepcion", List.of());
            model.addAttribute("historialTransferencias", List.of());
            model.addAttribute("errorTransferencias", "No se pudo cargar el historial de transferencias. Si es la primera vez, reinicie la aplicación para crear la tabla.");
        }
        return "inventario-transferencias";
    }

    @PostMapping("/transferencias")
    public String ejecutarTransferencia(@RequestParam("almacenOrigenId") Long almacenOrigenId,
                                        @RequestParam("almacenDestinoId") Long almacenDestinoId,
                                        @RequestParam("productoId") Long productoId,
                                        @RequestParam("cantidad") int cantidad,
                                        @RequestParam(value = "referencia", required = false) String referencia,
                                        RedirectAttributes ra) {
        try {
            String usuario = SecurityContextHolder.getContext().getAuthentication() != null
                    && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                    ? SecurityContextHolder.getContext().getAuthentication().getName() : "Sistema";
            String prodNombre = "";
            try { prodNombre = productoService.obtenerPorId(productoId).getNombre(); } catch (Exception ignored) {}
            inventarioService.transferir(almacenOrigenId, almacenDestinoId, productoId, cantidad, referencia, usuario);
            auditoriaService.registrarAccion("POST", "Transferencia de inventario (enviada)",
                    prodNombre + " | Cantidad: " + cantidad
                            + " | Almacén origen #" + almacenOrigenId + " → destino #" + almacenDestinoId + ". Pendiente de confirmación en destino.");
            ra.addFlashAttribute("mensaje", "Transferencia enviada. El almacén destino debe confirmar la recepción cuando reciba la mercancía.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/inventario/transferencias";
    }

    @PostMapping("/transferencias/{id}/confirmar-recepcion")
    public String confirmarRecepcion(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            String usuario = SecurityContextHolder.getContext().getAuthentication() != null
                    && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                    ? SecurityContextHolder.getContext().getAuthentication().getName() : "Sistema";
            inventarioService.confirmarRecepcion(id, usuario);
            ra.addFlashAttribute("mensaje", "Recepción confirmada. El producto ya está disponible en este almacén.");
        } catch (IllegalArgumentException | jakarta.persistence.EntityNotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/inventario/transferencias";
    }

    @PostMapping("/transferencias/{id}/eliminar")
    public String eliminarTransferencia(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            inventarioService.eliminarTransferencia(id);
            ra.addFlashAttribute("mensaje", "Transferencia eliminada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/inventario/transferencias";
    }

    @PostMapping("/transferencias/{id}/actualizar")
    public String actualizarTransferencia(@PathVariable("id") Long id,
                                         @RequestParam("cantidad") int cantidad,
                                         @RequestParam(value = "referencia", required = false) String referencia,
                                         RedirectAttributes ra) {
        try {
            inventarioService.actualizarTransferencia(id, cantidad, referencia != null ? referencia : "");
            ra.addFlashAttribute("mensaje", "Transferencia actualizada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/inventario/transferencias";
    }

    @GetMapping("/transferencias/{id}/comprobante")
    public String comprobanteTransferencia(@PathVariable("id") Long id, Model model) {
        com.farmacia.sistema.domain.inventario.Transferencia t = inventarioService.obtenerTransferenciaPorId(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Transferencia no encontrada"));
        model.addAttribute("transferencia", t);
        return "inventario-transferencia-comprobante";
    }

    @GetMapping("/lotes")
    public String lotes(@RequestParam(value = "productoId", required = false) String productoIdStr,
                        @RequestParam(value = "numeroLote", required = false) String filtroNumeroLote,
                        Model model) {
        String filtroLote = (filtroNumeroLote != null) ? filtroNumeroLote.trim() : "";
        model.addAttribute("filtroNumeroLote", filtroLote);

        Long productoId = null;
        if (productoIdStr != null && !productoIdStr.isBlank()) {
            try {
                productoId = Long.parseLong(productoIdStr.trim());
            } catch (NumberFormatException ignored) {}
        }
        model.addAttribute("productoIdSeleccionado", productoId);
        model.addAttribute("productos", productoService.listarTodos());

        final Long productoIdFiltro = productoId;
        // Lotes por vencer: filtrar por producto (si hay uno elegido) y por número de lote
        List<LoteProducto> porVencer = inventarioService.lotesPorVencer(60);
        if (productoIdFiltro != null) {
            porVencer = porVencer.stream()
                    .filter(l -> l.getProducto() != null && productoIdFiltro.equals(l.getProducto().getId()))
                    .collect(Collectors.toList());
        }
        if (!filtroLote.isBlank()) {
            String f = filtroLote.toLowerCase();
            porVencer = porVencer.stream()
                    .filter(l -> l.getNumeroLote() != null && l.getNumeroLote().toLowerCase().contains(f))
                    .collect(Collectors.toList());
        }
        model.addAttribute("lotesPorVencer", porVencer);

        // Lotes vencidos (fecha &lt; hoy): mismo filtro por producto y por número de lote
        List<LoteProducto> vencidos = inventarioService.lotesVencidos();
        if (productoIdFiltro != null) {
            vencidos = vencidos.stream()
                    .filter(l -> l.getProducto() != null && productoIdFiltro.equals(l.getProducto().getId()))
                    .collect(Collectors.toList());
        }
        if (!filtroLote.isBlank()) {
            String f = filtroLote.toLowerCase();
            vencidos = vencidos.stream()
                    .filter(l -> l.getNumeroLote() != null && l.getNumeroLote().toLowerCase().contains(f))
                    .collect(Collectors.toList());
        }
        model.addAttribute("lotesVencidos", vencidos);

        // Lotes del producto seleccionado (tabla izquierda)
        if (productoId != null) {
            List<LoteProducto> lotes = inventarioService.lotesPorProducto(productoId);
            if (!filtroLote.isBlank()) {
                String f = filtroLote.toLowerCase();
                lotes = lotes.stream()
                        .filter(l -> l.getNumeroLote() != null && l.getNumeroLote().toLowerCase().contains(f))
                        .collect(Collectors.toList());
            }
            model.addAttribute("lotesProducto", lotes);
        }
        return "inventario-lotes";
    }

    @PostMapping("/lotes")
    public String registrarLote(@RequestParam("productoId") Long productoId,
                                @RequestParam(value = "numeroLote", required = false) String numeroLote,
                                @RequestParam("fechaVencimiento") LocalDate fechaVencimiento,
                                @RequestParam("cantidad") int cantidad,
                                @RequestParam(value = "numeroLoteFiltro", required = false) String filtroNumeroLote,
                                RedirectAttributes ra) {
        try {
            String prodNombre = "";
            try { prodNombre = productoService.obtenerPorId(productoId).getNombre(); } catch (Exception ignored) {}
            LoteProducto lote = inventarioService.registrarLote(productoId, numeroLote, fechaVencimiento, cantidad);
            String numLote = lote.getNumeroLote() != null ? lote.getNumeroLote() : "";
            auditoriaService.registrarCreacion("lote", prodNombre,
                    "Lote: " + numLote + " | Vence: " + fechaVencimiento + " | Cantidad: " + cantidad);
            ra.addFlashAttribute("mensaje", "Lote registrado. Nº " + numLote);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return construirRedirectLotes(String.valueOf(productoId), filtroNumeroLote);
    }

    @PostMapping("/lotes/eliminar")
    public String eliminarLotes(@RequestParam(value = "loteIds", required = false) List<Long> loteIds,
                                @RequestParam(value = "productoId", required = false) String productoIdStr,
                                @RequestParam(value = "numeroLote", required = false) String filtroNumeroLote,
                                RedirectAttributes ra) {
        try {
            if (loteIds == null || loteIds.isEmpty()) {
                ra.addFlashAttribute("error", "Seleccione al menos un lote para eliminar.");
                return construirRedirectLotes(productoIdStr, filtroNumeroLote);
            }
            for (Long id : loteIds) {
                inventarioService.eliminarLote(id);
            }
            auditoriaService.registrarEliminacion("lote(s)", String.valueOf(loteIds.size()) + " lote(s)", "IDs: " + loteIds);
            ra.addFlashAttribute("mensaje", "Lote(s) eliminado(s).");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return construirRedirectLotes(productoIdStr, filtroNumeroLote);
    }

    @PostMapping("/lotes/actualizar")
    public String actualizarLote(@RequestParam("loteId") Long loteId,
                                @RequestParam("fechaVencimiento") LocalDate fechaVencimiento,
                                @RequestParam("cantidad") int cantidad,
                                @RequestParam(value = "productoId", required = false) String productoIdStr,
                                @RequestParam(value = "numeroLote", required = false) String filtroNumeroLote,
                                RedirectAttributes ra) {
        try {
            inventarioService.actualizarLote(loteId, fechaVencimiento, cantidad);
            ra.addFlashAttribute("mensaje", "Lote actualizado.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return construirRedirectLotes(productoIdStr, filtroNumeroLote);
    }

    private String construirRedirectLotes(String productoIdStr, String filtroNumeroLote) {
        StringBuilder q = new StringBuilder("redirect:/web/inventario/lotes");
        List<String> params = new ArrayList<>();
        if (productoIdStr != null && !productoIdStr.isBlank()) {
            params.add("productoId=" + productoIdStr.trim());
        }
        if (filtroNumeroLote != null && !filtroNumeroLote.isBlank()) {
            try {
                params.add("numeroLote=" + URLEncoder.encode(filtroNumeroLote.trim(), StandardCharsets.UTF_8.toString()));
            } catch (UnsupportedEncodingException e) {
                params.add("numeroLote=" + filtroNumeroLote.trim().replace("&", "%26"));
            }
        }
        if (!params.isEmpty()) {
            q.append("?").append(String.join("&", params));
        }
        return q.toString();
    }
}
