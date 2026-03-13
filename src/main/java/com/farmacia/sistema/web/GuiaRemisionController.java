package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.compra.CompraService;
import com.farmacia.sistema.domain.compra.OrdenCompra;
import com.farmacia.sistema.domain.empresa.Empresa;
import com.farmacia.sistema.domain.empresa.EmpresaService;
import com.farmacia.sistema.domain.guiaremision.GuiaRemision;
import com.farmacia.sistema.domain.guiaremision.GuiaRemisionService;
import com.farmacia.sistema.domain.proveedor.Proveedor;
import com.farmacia.sistema.domain.proveedor.ProveedorService;
import com.farmacia.sistema.export.GuiaRemisionPdfUtil;
import com.farmacia.sistema.tenant.TenantContext;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/web/guias-remision")
public class GuiaRemisionController {

    private final GuiaRemisionService guiaRemisionService;
    private final CompraService compraService;
    private final ProveedorService proveedorService;
    private final EmpresaService empresaService;

    public GuiaRemisionController(GuiaRemisionService guiaRemisionService,
                                  CompraService compraService,
                                  ProveedorService proveedorService,
                                  EmpresaService empresaService) {
        this.guiaRemisionService = guiaRemisionService;
        this.compraService = compraService;
        this.proveedorService = proveedorService;
        this.empresaService = empresaService;
    }

    @GetMapping
    public String listar(Model model) {
        List<GuiaRemision> guias = guiaRemisionService.listarTodas();
        List<OrdenCompra> ordenes = compraService.listarOrdenes();
        List<Proveedor> proveedores = proveedorService.listarTodos();

        model.addAttribute("guias", guias);
        model.addAttribute("ordenes", ordenes);
        model.addAttribute("proveedores", proveedores);
        return "guias-remision";
    }

    @PostMapping
    public String crear(@RequestParam(value = "ordenCompraId", required = false) Long ordenCompraId,
                         @RequestParam(value = "proveedorId", required = false) Long proveedorId,
                         @RequestParam(value = "motivoTraslado", required = false) String motivoTraslado,
                         @RequestParam(value = "direccionPartida", required = false) String direccionPartida,
                         @RequestParam(value = "direccionLlegada", required = false) String direccionLlegada,
                         @RequestParam(value = "fechaTraslado", required = false) String fechaTraslado,
                         @RequestParam(value = "transportistaRuc", required = false) String transportistaRuc,
                         @RequestParam(value = "transportistaNombre", required = false) String transportistaNombre,
                         @RequestParam(value = "conductorDni", required = false) String conductorDni,
                         @RequestParam(value = "conductorNombre", required = false) String conductorNombre,
                         @RequestParam(value = "conductorLicencia", required = false) String conductorLicencia,
                         @RequestParam(value = "placaVehiculo", required = false) String placaVehiculo,
                         @RequestParam(value = "pesoTotal", required = false) String pesoTotal,
                         @RequestParam(value = "numeroBultos", required = false) Integer numeroBultos,
                         @RequestParam(value = "observaciones", required = false) String observaciones,
                         RedirectAttributes ra) {
        try {
            GuiaRemision guia = new GuiaRemision();
            guia.setMotivoTraslado(motivoTraslado != null ? motivoTraslado : "COMPRA");
            guia.setDireccionPartida(direccionPartida);
            guia.setDireccionLlegada(direccionLlegada);
            if (fechaTraslado != null && !fechaTraslado.isBlank()) {
                guia.setFechaTraslado(LocalDate.parse(fechaTraslado));
            }
            guia.setTransportistaRuc(transportistaRuc);
            guia.setTransportistaNombre(transportistaNombre);
            guia.setConductorDni(conductorDni);
            guia.setConductorNombre(conductorNombre);
            guia.setConductorLicencia(conductorLicencia);
            guia.setPlacaVehiculo(placaVehiculo);
            guia.setPesoTotal(pesoTotal);
            guia.setNumeroBultos(numeroBultos);
            guia.setObservaciones(observaciones);

            if (ordenCompraId != null) {
                OrdenCompra orden = compraService.obtenerOrdenPorId(ordenCompraId);
                guia.setDireccionPartida(orden.getProveedor() != null ? orden.getProveedor().getDireccion() : direccionPartida);
                Long tid = TenantContext.getTenantId();
                Empresa empresa = tid != null ? empresaService.obtenerPorId(tid) : null;
                if (empresa != null && (direccionLlegada == null || direccionLlegada.isBlank())) {
                    guia.setDireccionLlegada(empresa.getDireccion());
                }
                guiaRemisionService.crearDesdeOrdenCompra(orden, guia);
            } else {
                if (proveedorId != null) {
                    Proveedor prov = proveedorService.obtenerPorId(proveedorId);
                    guia.setProveedor(prov);
                }
                guiaRemisionService.crear(guia);
            }

            ra.addFlashAttribute("mensaje", "Guía de remisión " + guia.getSerieNumero() + " registrada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/web/guias-remision";
    }

    @PostMapping("/anular")
    public String anular(@RequestParam("guiaId") Long guiaId, RedirectAttributes ra) {
        try {
            guiaRemisionService.anular(guiaId);
            ra.addFlashAttribute("mensaje", "Guía de remisión anulada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/web/guias-remision";
    }

    @GetMapping("/{id}/pdf")
    public void descargarPdf(@PathVariable Long id, HttpServletResponse response) {
        try {
            GuiaRemision guia = guiaRemisionService.obtenerPorId(id);
            Long tid = TenantContext.getTenantId();
            Empresa empresa = tid != null ? empresaService.obtenerPorId(tid) : empresaService.empresaPorDefecto();
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition",
                    "inline; filename=GR-" + guia.getSerieNumero() + ".pdf");
            GuiaRemisionPdfUtil.generar(response.getOutputStream(), empresa, guia);
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de guía de remisión", e);
        }
    }
}
