package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.compra.CompraService;
import com.farmacia.sistema.domain.compra.CuentaPagar;
import com.farmacia.sistema.domain.compra.OrdenCompra;
import com.farmacia.sistema.domain.producto.ProductoService;
import com.farmacia.sistema.domain.proveedor.ProveedorService;
import com.farmacia.sistema.export.ExcelExportUtil;
import com.farmacia.sistema.export.PdfExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/web/compras")
@PreAuthorize("hasAnyRole('ADMIN', 'SAAS_ADMIN')")
public class CompraWebController {

    private final CompraService compraService;
    private final ProveedorService proveedorService;
    private final ProductoService productoService;
    private final com.farmacia.sistema.domain.auditoria.AuditoriaService auditoriaService;

    public CompraWebController(CompraService compraService,
                               ProveedorService proveedorService,
                               ProductoService productoService,
                               com.farmacia.sistema.domain.auditoria.AuditoriaService auditoriaService) {
        this.compraService = compraService;
        this.proveedorService = proveedorService;
        this.productoService = productoService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "productoComparacionId", required = false) Long productoComparacionId,
                         Model model) {
        List<OrdenCompra> ordenes = compraService.listarOrdenes();
        model.addAttribute("ordenes", ordenes);
        model.addAttribute("proveedores", proveedorService.listarTodos());
        model.addAttribute("productos", productoService.listarTodos());
        model.addAttribute("productoComparacionId", productoComparacionId);
        model.addAttribute("comparacionProveedores",
                compraService.compararProveedoresPorProducto(productoComparacionId));
        model.addAttribute("sugerenciasCompra", compraService.sugerenciasCompra());
        return "compras";
    }

    @PostMapping
    public String crearOrden(@RequestParam("proveedorId") Long proveedorId,
                             @RequestParam("productoId") Long productoId,
                             @RequestParam("cantidad") int cantidad,
                             @RequestParam("precioUnitario") BigDecimal precioUnitario,
                             @RequestParam(value = "fechaEsperada", required = false) String fechaEsperadaStr,
                             @RequestParam(value = "observaciones", required = false) String observaciones,
                             RedirectAttributes ra) {
        try {
            LocalDate fechaEsperada = null;
            if (fechaEsperadaStr != null && !fechaEsperadaStr.isBlank()) {
                fechaEsperada = LocalDate.parse(fechaEsperadaStr);
            }
            compraService.crearOrden(proveedorId, productoId, cantidad, precioUnitario, fechaEsperada, observaciones);
            String prodNombre = "";
            try { prodNombre = productoService.obtenerPorId(productoId).getNombre(); } catch (Exception ignored) {}
            String provNombre = "";
            try { provNombre = proveedorService.obtenerPorId(proveedorId).getRazonSocial(); } catch (Exception ignored) {}
            auditoriaService.registrarCreacion("orden de compra", prodNombre,
                    "Proveedor: " + provNombre
                            + " | Cantidad: " + cantidad
                            + " | P.Unit: " + precioUnitario
                            + " | Total: " + precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
            ra.addFlashAttribute("mensaje", "Orden de compra registrada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/compras";
    }

    @PostMapping("/recibir")
    public String recibir(@RequestParam("ordenId") Long ordenId,
                          RedirectAttributes ra) {
        try {
            compraService.recibirOrden(ordenId);
            auditoriaService.registrarAccion("PUT", "Recibir orden de compra",
                    "Orden #" + ordenId + " marcada como recibida e ingresada a inventario");
            ra.addFlashAttribute("mensaje", "Orden marcada como recibida y mercadería ingresada a inventario.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/compras";
    }

    @PostMapping("/eliminar")
    public String eliminar(@RequestParam("ordenId") Long ordenId, RedirectAttributes ra) {
        try {
            compraService.eliminarOrden(ordenId);
            auditoriaService.registrarAccion("DELETE", "Eliminar orden de compra", "Orden #" + ordenId + " eliminada");
            ra.addFlashAttribute("mensaje", "Orden de compra eliminada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/compras";
    }

    @PostMapping("/actualizar")
    public String actualizar(@RequestParam("ordenId") Long ordenId,
                             @RequestParam("proveedorId") Long proveedorId,
                             @RequestParam("productoId") Long productoId,
                             @RequestParam("cantidad") int cantidad,
                             @RequestParam("precioUnitario") BigDecimal precioUnitario,
                             @RequestParam(value = "fechaEsperada", required = false) String fechaEsperadaStr,
                             @RequestParam(value = "observaciones", required = false) String observaciones,
                             RedirectAttributes ra) {
        try {
            LocalDate fechaEsperada = null;
            if (fechaEsperadaStr != null && !fechaEsperadaStr.isBlank()) {
                fechaEsperada = LocalDate.parse(fechaEsperadaStr);
            }
            compraService.actualizarOrden(ordenId, proveedorId, productoId, cantidad, precioUnitario, fechaEsperada, observaciones);
            auditoriaService.registrarAccion("PUT", "Modificar orden de compra", "Orden #" + ordenId + " actualizada");
            ra.addFlashAttribute("mensaje", "Orden de compra actualizada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/compras";
    }

    @GetMapping("/cuentas-pagar")
    public String cuentasPorPagar(Model model) {
        List<CuentaPagar> pendientes = compraService.listarCuentasPendientes();
        List<CuentaPagar> todas = compraService.listarTodasCuentas();
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("todas", todas);
        return "cuentas-pagar";
    }

    @GetMapping("/excel")
    public void exportarExcel(HttpServletResponse response) throws Exception {
        List<OrdenCompra> ordenes = compraService.listarOrdenes();
        String[] headers = {"ID", "Proveedor", "Fecha Emisión", "Fecha Esperada",
                "Estado", "Subtotal", "Total", "Observaciones"};
        List<Object[]> filas = new ArrayList<>();
        for (OrdenCompra o : ordenes) {
            filas.add(new Object[]{
                    o.getId(),
                    o.getProveedor() != null ? o.getProveedor().getRazonSocial() : "",
                    o.getFechaEmision(), o.getFechaEsperada(),
                    o.getEstado(), o.getSubtotal(), o.getTotal(), o.getObservaciones()
            });
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=compras_" + LocalDate.now() + ".xlsx");
        try (Workbook wb = ExcelExportUtil.crearReporte("Órdenes de Compra",
                "Exportado el " + LocalDate.now(), headers, filas)) {
            wb.write(response.getOutputStream());
        }
    }

    @GetMapping("/pdf")
    public void exportarPdf(HttpServletResponse response) throws Exception {
        List<OrdenCompra> ordenes = compraService.listarOrdenes();
        String[] headers = {"ID", "Proveedor", "Fecha Emisión", "Estado", "Total"};
        List<Object[]> filas = new ArrayList<>();
        for (OrdenCompra o : ordenes) {
            filas.add(new Object[]{
                    o.getId(),
                    o.getProveedor() != null ? o.getProveedor().getRazonSocial() : "",
                    o.getFechaEmision(), o.getEstado(), o.getTotal()
            });
        }
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=compras_" + LocalDate.now() + ".pdf");
        PdfExportUtil.crearReporte(response.getOutputStream(), "Órdenes de Compra",
                "Exportado el " + LocalDate.now(), headers, filas,
                new float[]{1, 3, 2, 1.5f, 2});
    }

    @GetMapping("/cuentas-pagar/excel")
    public void exportarCuentasPagarExcel(HttpServletResponse response) throws Exception {
        List<CuentaPagar> cuentas = compraService.listarTodasCuentas();
        String[] headers = {"ID", "Proveedor", "Fecha Emisión", "Fecha Vencimiento",
                "Monto Total", "Saldo Pendiente", "Estado"};
        List<Object[]> filas = new ArrayList<>();
        for (CuentaPagar c : cuentas) {
            filas.add(new Object[]{
                    c.getId(),
                    c.getProveedor() != null ? c.getProveedor().getRazonSocial() : "",
                    c.getFechaEmision(), c.getFechaVencimiento(),
                    c.getMontoTotal(), c.getSaldoPendiente(), c.getEstado()
            });
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=cuentas_pagar_" + LocalDate.now() + ".xlsx");
        try (Workbook wb = ExcelExportUtil.crearReporte("Cuentas por Pagar",
                "Exportado el " + LocalDate.now(), headers, filas)) {
            wb.write(response.getOutputStream());
        }
    }

    @GetMapping("/cuentas-pagar/pdf")
    public void exportarCuentasPagarPdf(HttpServletResponse response) throws Exception {
        List<CuentaPagar> cuentas = compraService.listarTodasCuentas();
        String[] headers = {"ID", "Proveedor", "F. Emisión", "F. Vencimiento",
                "Monto Total", "Saldo Pend.", "Estado"};
        List<Object[]> filas = new ArrayList<>();
        for (CuentaPagar c : cuentas) {
            filas.add(new Object[]{
                    c.getId(),
                    c.getProveedor() != null ? c.getProveedor().getRazonSocial() : "",
                    c.getFechaEmision(), c.getFechaVencimiento(),
                    c.getMontoTotal(), c.getSaldoPendiente(), c.getEstado()
            });
        }
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=cuentas_pagar_" + LocalDate.now() + ".pdf");
        PdfExportUtil.crearReporte(response.getOutputStream(), "Cuentas por Pagar",
                "Exportado el " + LocalDate.now(), headers, filas, null);
    }

    @PostMapping("/cuentas-pagar/pagar")
    public String pagarCuenta(@RequestParam("cuentaId") Long cuentaId,
                              RedirectAttributes ra) {
        try {
            compraService.marcarCuentaPagada(cuentaId);
            auditoriaService.registrarAccion("PUT", "Pagar cuenta por pagar",
                    "Cuenta #" + cuentaId + " marcada como pagada");
            ra.addFlashAttribute("mensaje", "Cuenta por pagar marcada como pagada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/compras/cuentas-pagar";
    }

    @PostMapping("/cuentas-pagar/eliminar")
    public String eliminarCuenta(@RequestParam("cuentaId") Long cuentaId,
                                 RedirectAttributes ra) {
        try {
            compraService.eliminarCuenta(cuentaId);
            auditoriaService.registrarAccion("DELETE", "Eliminar cuenta por pagar", "Cuenta #" + cuentaId + " eliminada");
            ra.addFlashAttribute("mensaje", "Cuenta por pagar eliminada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/compras/cuentas-pagar";
    }
}

