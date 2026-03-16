package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.compra.CompraService;
import com.farmacia.sistema.domain.compra.OrdenCompra;
import com.farmacia.sistema.domain.compra.OrdenCompraItem;
import com.farmacia.sistema.domain.empresa.Empresa;
import com.farmacia.sistema.domain.empresa.EmpresaService;
import com.farmacia.sistema.domain.guiaremision.GuiaRemision;
import com.farmacia.sistema.domain.guiaremision.GuiaRemisionService;
import com.farmacia.sistema.domain.guiaremision.Transportista;
import com.farmacia.sistema.domain.guiaremision.TransportistaService;
import com.farmacia.sistema.domain.proveedor.Proveedor;
import com.farmacia.sistema.domain.proveedor.ProveedorService;
import com.farmacia.sistema.export.GuiaRemisionPdfUtil;
import com.farmacia.sistema.tenant.TenantContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/web/guias-remision")
public class GuiaRemisionController {

    private final GuiaRemisionService guiaRemisionService;
    private final CompraService compraService;
    private final ProveedorService proveedorService;
    private final EmpresaService empresaService;
    private final TransportistaService transportistaService;

    public GuiaRemisionController(GuiaRemisionService guiaRemisionService,
                                  CompraService compraService,
                                  ProveedorService proveedorService,
                                  EmpresaService empresaService,
                                  TransportistaService transportistaService) {
        this.guiaRemisionService = guiaRemisionService;
        this.compraService = compraService;
        this.proveedorService = proveedorService;
        this.empresaService = empresaService;
        this.transportistaService = transportistaService;
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
                OrdenCompra orden = compraService.obtenerOrdenConItems(ordenCompraId);
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

    // ===================== API TRANSPORTISTAS =====================

    @GetMapping("/api/transportistas")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> listarTransportistas() {
        List<Transportista> lista = transportistaService.listarActivos();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Transportista t : lista) {
            result.add(transportistaToMap(t));
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/transportistas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearTransportista(@RequestBody Map<String, String> body) {
        try {
            Transportista t = new Transportista();
            t.setRuc(body.getOrDefault("ruc", ""));
            t.setNombre(body.getOrDefault("nombre", ""));
            t.setConductorDni(body.getOrDefault("conductorDni", ""));
            t.setConductorNombre(body.getOrDefault("conductorNombre", ""));
            t.setConductorLicencia(body.getOrDefault("conductorLicencia", ""));
            t.setPlacaVehiculo(body.getOrDefault("placaVehiculo", ""));
            t.setTelefono(body.getOrDefault("telefono", ""));
            t = transportistaService.guardar(t);
            return ResponseEntity.ok(transportistaToMap(t));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/api/transportistas/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarTransportista(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            Transportista datos = new Transportista();
            datos.setRuc(body.getOrDefault("ruc", ""));
            datos.setNombre(body.getOrDefault("nombre", ""));
            datos.setConductorDni(body.getOrDefault("conductorDni", ""));
            datos.setConductorNombre(body.getOrDefault("conductorNombre", ""));
            datos.setConductorLicencia(body.getOrDefault("conductorLicencia", ""));
            datos.setPlacaVehiculo(body.getOrDefault("placaVehiculo", ""));
            datos.setTelefono(body.getOrDefault("telefono", ""));
            Transportista t = transportistaService.actualizar(id, datos);
            return ResponseEntity.ok(transportistaToMap(t));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/transportistas/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarTransportista(@PathVariable Long id) {
        try {
            transportistaService.eliminar(id);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> transportistaToMap(Transportista t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("ruc", t.getRuc() != null ? t.getRuc() : "");
        m.put("nombre", t.getNombre() != null ? t.getNombre() : "");
        m.put("conductorDni", t.getConductorDni() != null ? t.getConductorDni() : "");
        m.put("conductorNombre", t.getConductorNombre() != null ? t.getConductorNombre() : "");
        m.put("conductorLicencia", t.getConductorLicencia() != null ? t.getConductorLicencia() : "");
        m.put("placaVehiculo", t.getPlacaVehiculo() != null ? t.getPlacaVehiculo() : "");
        m.put("telefono", t.getTelefono() != null ? t.getTelefono() : "");
        return m;
    }

    // ===================== API ORDEN COMPRA =====================

    @GetMapping("/api/orden/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerDatosOrden(@PathVariable Long id) {
        try {
            OrdenCompra orden = compraService.obtenerOrdenConItems(id);
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("id", orden.getId());
            datos.put("estado", orden.getEstado());
            datos.put("total", orden.getTotal());
            datos.put("observaciones", orden.getObservaciones());

            if (orden.getProveedor() != null) {
                Proveedor p = orden.getProveedor();
                Map<String, String> prov = new LinkedHashMap<>();
                prov.put("razonSocial", p.getRazonSocial() != null ? p.getRazonSocial() : "");
                prov.put("tipoDocumento", p.getTipoDocumento() != null ? p.getTipoDocumento() : "");
                prov.put("numeroDocumento", p.getNumeroDocumento() != null ? p.getNumeroDocumento() : "");
                prov.put("direccion", p.getDireccion() != null ? p.getDireccion() : "");
                prov.put("telefono", p.getTelefono() != null ? p.getTelefono() : "");
                prov.put("email", p.getEmail() != null ? p.getEmail() : "");
                prov.put("contacto", p.getContacto() != null ? p.getContacto() : "");
                datos.put("proveedor", prov);
            }

            Long tid = TenantContext.getTenantId();
            Empresa empresa = tid != null ? empresaService.obtenerPorId(tid) : empresaService.empresaPorDefecto();
            datos.put("direccionLlegada", empresa.getDireccion() != null ? empresa.getDireccion() : "");
            datos.put("empresaNombre", empresa.getNombre());

            List<Map<String, Object>> itemsList = new ArrayList<>();
            if (orden.getItems() != null) {
                for (OrdenCompraItem item : orden.getItems()) {
                    Map<String, Object> it = new LinkedHashMap<>();
                    it.put("productoNombre", item.getProducto() != null ? item.getProducto().getNombre() : "");
                    it.put("cantidad", item.getCantidad());
                    it.put("precioUnitario", item.getPrecioUnitario());
                    it.put("subtotal", item.getSubtotal());
                    itemsList.add(it);
                }
            }
            datos.put("items", itemsList);
            datos.put("totalItems", itemsList.size());

            return ResponseEntity.ok(datos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
