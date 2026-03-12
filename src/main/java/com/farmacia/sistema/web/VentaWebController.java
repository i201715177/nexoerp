package com.farmacia.sistema.web;

import com.farmacia.sistema.api.venta.CrearVentaRequest;
import com.farmacia.sistema.api.venta.ItemVentaRequest;
import com.farmacia.sistema.api.venta.PagoRequest;
import com.farmacia.sistema.domain.caja.CajaTurnoService;
import com.farmacia.sistema.domain.cliente.ClienteService;
import com.farmacia.sistema.domain.producto.ProductoService;
import com.farmacia.sistema.domain.venta.NotaCreditoService;
import com.farmacia.sistema.dto.VentaResumenDto;
import com.farmacia.sistema.domain.inventario.InventarioService;
import com.farmacia.sistema.domain.venta.VentaService;
import com.farmacia.sistema.domain.empresa.EmpresaService;
import com.farmacia.sistema.domain.venta.Venta;
import com.farmacia.sistema.security.TenantUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.farmacia.sistema.export.ComprobantePdfUtil;
import com.farmacia.sistema.export.ExcelExportUtil;
import com.farmacia.sistema.export.PdfExportUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Controller
@RequestMapping("/web/ventas")
public class VentaWebController {

    private final VentaService ventaService;
    private final ClienteService clienteService;
    private final ProductoService productoService;
    private final CajaTurnoService cajaTurnoService;
    private final NotaCreditoService notaCreditoService;
    private final EmpresaService empresaService;
    private final InventarioService inventarioService;
    private final com.farmacia.sistema.domain.auditoria.AuditoriaService auditoriaService;

    public VentaWebController(VentaService ventaService,
                              ClienteService clienteService,
                              ProductoService productoService,
                              CajaTurnoService cajaTurnoService,
                              NotaCreditoService notaCreditoService,
                              EmpresaService empresaService,
                              InventarioService inventarioService,
                              com.farmacia.sistema.domain.auditoria.AuditoriaService auditoriaService) {
        this.ventaService = ventaService;
        this.clienteService = clienteService;
        this.productoService = productoService;
        this.cajaTurnoService = cajaTurnoService;
        this.notaCreditoService = notaCreditoService;
        this.empresaService = empresaService;
        this.inventarioService = inventarioService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String formulario(Model model) {
        CrearVentaRequest venta = new CrearVentaRequest();
        ItemVentaRequest item = new ItemVentaRequest();
        venta.setItems(Collections.singletonList(item));

        Optional<com.farmacia.sistema.domain.caja.CajaTurno> cajaAbierta = cajaTurnoService.obtenerCajaAbierta();
        List<com.farmacia.sistema.domain.producto.Producto> productos = productoService.listarTodos();
        Long sucursalIdUsuario = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof TenantUserDetails tud) {
            sucursalIdUsuario = tud.getSucursalId();
        }
        if (sucursalIdUsuario != null) {
            Map<com.farmacia.sistema.domain.producto.Producto, Integer> stockSucursal = inventarioService.stockPorSucursal(sucursalIdUsuario);
            Map<Long, Integer> stockPorProducto = new HashMap<>();
            for (Map.Entry<com.farmacia.sistema.domain.producto.Producto, Integer> e : stockSucursal.entrySet()) {
                if (e.getKey() != null && e.getKey().getId() != null) {
                    stockPorProducto.put(e.getKey().getId(), e.getValue());
                }
            }
            model.addAttribute("stockPorProducto", stockPorProducto);
        }
        model.addAttribute("venta", venta);
        model.addAttribute("clientes", clienteService.listarTodos());
        model.addAttribute("productos", productos);
        model.addAttribute("ventas", ventaService.listarVentasResumenParaWeb());
        model.addAttribute("montoTotalVentas", ventaService.getMontoTotalVendido());
        model.addAttribute("ventasHoyCount", ventaService.getVentasHoyCount());
        model.addAttribute("ventasHoyMonto", ventaService.getVentasHoyMonto());
        model.addAttribute("cajaAbierta", cajaAbierta.orElse(null));
        return "ventas";
    }

    @PostMapping
    public String crear(@ModelAttribute("venta") @Valid CrearVentaRequest venta,
                        BindingResult bindingResult,
                        Model model,
                        HttpServletRequest request) {
        if (venta.getPagos() == null || venta.getPagos().isEmpty()) {
            String medio = request.getParameter("medioPagoUnico");
            String montoStr = request.getParameter("montoPagado");
            if (medio != null || (montoStr != null && !montoStr.isBlank())) {
                List<PagoRequest> pagos = new ArrayList<>();
                PagoRequest p = new PagoRequest();
                p.setMedioPago(medio != null && !medio.isBlank() ? medio : "EFECTIVO");
                try {
                    p.setMonto(montoStr != null && !montoStr.isBlank() ? new BigDecimal(montoStr) : BigDecimal.ZERO);
                } catch (NumberFormatException e) {
                    p.setMonto(BigDecimal.ZERO);
                }
                if (p.getMonto().compareTo(BigDecimal.ZERO) > 0) {
                    pagos.add(p);
                }
                venta.setPagos(pagos);
            }
        }
        Optional<com.farmacia.sistema.domain.caja.CajaTurno> cajaAbierta = cajaTurnoService.obtenerCajaAbierta();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esVendedor = auth != null && auth.getAuthorities() != null
                && auth.getAuthorities().stream().anyMatch(a -> "ROLE_VENDEDOR".equals(a.getAuthority()));
        if (esVendedor) {
            cajaAbierta.ifPresent(caja -> venta.setCajaTurnoId(caja.getId()));
        } else {
            // Para ADMIN / SAAS_ADMIN la venta se considera general (sin sucursal),
            // usando stock global del producto/almacén principal.
            venta.setCajaTurnoId(null);
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("clientes", clienteService.listarTodos());
            model.addAttribute("productos", productoService.listarTodos());
            model.addAttribute("ventas", ventaService.listarVentasResumenParaWeb());
            model.addAttribute("montoTotalVentas", ventaService.getMontoTotalVendido());
            model.addAttribute("ventasHoyCount", ventaService.getVentasHoyCount());
            model.addAttribute("ventasHoyMonto", ventaService.getVentasHoyMonto());
            model.addAttribute("cajaAbierta", cajaAbierta.orElse(null));
            return "ventas";
        }
        try {
            var ventaCreada = ventaService.crearVenta(venta);
            String itemsDesc = "";
            if (venta.getItems() != null) {
                var sb = new StringBuilder();
                for (var it : venta.getItems()) {
                    try {
                        var prod = productoService.obtenerPorId(it.getProductoId());
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(prod.getNombre()).append(" x").append(it.getCantidad());
                    } catch (Exception ignored) {}
                }
                itemsDesc = sb.toString();
            }
            auditoriaService.registrarCreacion("venta", "Venta registrada",
                    "Items: " + (itemsDesc.isEmpty() ? "N/A" : itemsDesc)
                            + " | Medio pago: " + (venta.getPagos() != null && !venta.getPagos().isEmpty()
                            ? venta.getPagos().get(0).getMedioPago() : "N/A"));
            return "redirect:/web/ventas";
        } catch (IllegalArgumentException e) {
            model.addAttribute("clientes", clienteService.listarTodos());
            model.addAttribute("productos", productoService.listarTodos());
            model.addAttribute("ventas", ventaService.listarVentasResumenParaWeb());
            model.addAttribute("montoTotalVentas", ventaService.getMontoTotalVendido());
            model.addAttribute("ventasHoyCount", ventaService.getVentasHoyCount());
            model.addAttribute("ventasHoyMonto", ventaService.getVentasHoyMonto());
            model.addAttribute("cajaAbierta", cajaAbierta.orElse(null));
            model.addAttribute("venta", venta);
            model.addAttribute("error", e.getMessage());
            return "ventas";
        }
    }

    @GetMapping("/excel")
    @Transactional(readOnly = true)
    public void exportarExcel(HttpServletResponse response,
                              @RequestParam(value = "ids", required = false) String idsParam) throws Exception {
        List<VentaResumenDto> ventas = ventaService.listarVentasResumenParaWeb();
        if (idsParam != null && !idsParam.isBlank()) {
            List<Long> ids = Stream.of(idsParam.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                    .map(Long::parseLong).toList();
            if (!ids.isEmpty()) ventas = ventas.stream().filter(v -> ids.contains(v.getId())).toList();
        }
        String[] headers = {"ID", "Tipo", "Número", "Cliente", "Fecha/Hora", "Comprobante", "Items",
                "Productos vendidos", "Descuento", "Total", "Estado", "Pagos"};
        List<Object[]> filas = new ArrayList<>();
        for (VentaResumenDto v : ventas) {
            String tipoLabel = "FAC".equals(v.getTipoComprobante()) ? "Factura" : "Boleta";
            filas.add(new Object[]{
                    v.getId(), tipoLabel, v.getNumeroDocumentoCliente() != null ? v.getNumeroDocumentoCliente() : "",
                    v.getNombreClienteVenta(), v.getFechaHora(),
                    v.getComprobante(), v.getItemsCount(), v.getItemsDetalle() != null ? v.getItemsDetalle() : "",
                    v.getDescuentoTotal(), v.getTotal(), v.getEstado() != null ? v.getEstado() : "EMITIDA", v.getPagosResumen()
            });
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=ventas_" + LocalDate.now() + ".xlsx");
        try (Workbook wb = ExcelExportUtil.crearReporte("Registro de Ventas",
                "Exportado el " + LocalDate.now(), headers, filas)) {
            wb.write(response.getOutputStream());
        }
    }

    @GetMapping("/pdf")
    @Transactional(readOnly = true)
    public void exportarPdf(HttpServletResponse response,
                            @RequestParam(value = "ids", required = false) String idsParam) throws Exception {
        List<VentaResumenDto> ventas = ventaService.listarVentasResumenParaWeb();
        if (idsParam != null && !idsParam.isBlank()) {
            List<Long> ids = Stream.of(idsParam.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                    .map(Long::parseLong).toList();
            if (!ids.isEmpty()) ventas = ventas.stream().filter(v -> ids.contains(v.getId())).toList();
        }
        String[] headers = {"ID", "Tipo", "Número", "Cliente", "Fecha/Hora", "Comprobante",
                "Items", "Productos vendidos", "Descuento", "Total", "Estado"};
        List<Object[]> filas = new ArrayList<>();
        for (VentaResumenDto v : ventas) {
            String tipoLabel = "FAC".equals(v.getTipoComprobante()) ? "Factura" : "Boleta";
            filas.add(new Object[]{
                    v.getId(), tipoLabel, v.getNumeroDocumentoCliente() != null ? v.getNumeroDocumentoCliente() : "",
                    v.getNombreClienteVenta(), v.getFechaHora(),
                    v.getComprobante(), v.getItemsCount(), v.getItemsDetalle() != null ? v.getItemsDetalle() : "",
                    v.getDescuentoTotal(), v.getTotal(), v.getEstado() != null ? v.getEstado() : "EMITIDA"
            });
        }
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=ventas_" + LocalDate.now() + ".pdf");
        PdfExportUtil.crearReporte(response.getOutputStream(), "Registro de Ventas",
                "Exportado el " + LocalDate.now(), headers, filas,
                new float[]{0.8f, 1f, 1.2f, 2.5f, 1.8f, 1.8f, 0.8f, 3f, 1.2f, 1.2f, 1f});
    }

    @GetMapping("/{ventaId}/comprobante")
    @Transactional(readOnly = true)
    public void comprobantePdf(@PathVariable Long ventaId, HttpServletResponse response) throws java.io.IOException {
        Venta venta = ventaService.obtenerPorId(ventaId);
        venta.getItems().size();
        venta.getPagos().size();
        if (venta.getCliente() != null) venta.getCliente().getNombres();

        var empresa = venta.getTenantId() != null
                ? empresaService.obtenerPorId(venta.getTenantId())
                : empresaService.empresaPorDefecto();

        boolean esFactura = "FAC".equals(venta.getTipoComprobante());
        String nombreArchivo = (esFactura ? "factura-" : "comprobante-") + (venta.getNumeroComprobante() != null ? venta.getNumeroComprobante() : ventaId) + ".pdf";
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=\"" + nombreArchivo + "\"");

        ComprobantePdfUtil.generarComprobante(response.getOutputStream(), empresa, venta);
    }

    @GetMapping("/{ventaId}/nota-credito")
    public String formularioNotaCredito(@PathVariable Long ventaId, Model model) {
        model.addAttribute("venta", ventaService.obtenerPorId(ventaId));
        return "nota-credito";
    }

    @PostMapping("/{ventaId}/nota-credito")
    public String emitirNotaCredito(@PathVariable Long ventaId,
                                   @RequestParam(value = "motivo", required = false) String motivo,
                                   @RequestParam(value = "montoTotal", required = false) BigDecimal montoTotal,
                                   @RequestParam(value = "devolverStock", defaultValue = "false") boolean devolverStock,
                                   RedirectAttributes ra) {
        try {
            notaCreditoService.emitir(ventaId, motivo, montoTotal, devolverStock);
            auditoriaService.registrarCreacion("nota de crédito", "Venta #" + ventaId,
                    "Motivo: " + (motivo != null ? motivo : "N/A")
                            + " | Monto: " + montoTotal
                            + " | Devolver stock: " + (devolverStock ? "Sí" : "No"));
            ra.addFlashAttribute("mensaje", "Nota de crédito emitida.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/ventas";
    }

    /**
     * Devolución total: productos vuelven al almacén y la venta queda anulada (boleta anulada).
     */
    @PostMapping("/{ventaId}/devolucion")
    public String registrarDevolucion(@PathVariable Long ventaId, RedirectAttributes ra) {
        try {
            ventaService.anularPorDevolucion(ventaId);
            auditoriaService.registrarCreacion("devolución", "Devolución venta #" + ventaId,
                    "Productos devueltos al almacén, venta anulada.");
            ra.addFlashAttribute("mensaje", "Devolución registrada. Los productos han vuelto al almacén y la boleta quedó anulada.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo registrar la devolución: " + e.getMessage());
        }
        return "redirect:/web/ventas";
    }
}

