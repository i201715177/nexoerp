package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.inventario.Almacen;
import com.farmacia.sistema.domain.inventario.InventarioService;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.sucursal.Sucursal;
import com.farmacia.sistema.domain.sucursal.SucursalService;
import com.farmacia.sistema.domain.venta.Venta;
import com.farmacia.sistema.domain.venta.VentaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/web/sucursales")
@PreAuthorize("hasAnyRole('ADMIN', 'SAAS_ADMIN')")
public class SucursalWebController {

    private final SucursalService sucursalService;
    private final InventarioService inventarioService;
    private final VentaService ventaService;
    private final com.farmacia.sistema.domain.auditoria.AuditoriaService auditoriaService;

    public SucursalWebController(SucursalService sucursalService,
                                 InventarioService inventarioService,
                                 VentaService ventaService,
                                 com.farmacia.sistema.domain.auditoria.AuditoriaService auditoriaService) {
        this.sucursalService = sucursalService;
        this.inventarioService = inventarioService;
        this.ventaService = ventaService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "editId", required = false) Long editId,
                         Model model) {
        List<Sucursal> sucursales = sucursalService.listarTodas();
        model.addAttribute("sucursales", sucursales);
        if (editId != null) {
            Sucursal sucursalEdit = sucursalService.obtenerPorId(editId);
            model.addAttribute("sucursalEdit", sucursalEdit);
        }
        return "sucursales";
    }

    @PostMapping
    public String guardar(@RequestParam(value = "id", required = false) Long id,
                          @RequestParam("codigo") String codigo,
                          @RequestParam("nombre") String nombre,
                          @RequestParam(value = "direccion", required = false) String direccion,
                          @RequestParam(value = "central", defaultValue = "false") boolean central) {
        if (id == null) {
            // Crear nueva sucursal
            Sucursal s = new Sucursal();
            s.setCodigo(codigo);
            s.setNombre(nombre);
            s.setDireccion(direccion);
            s.setActiva(true);
            s.setCentral(central);
            s = sucursalService.crear(s);
            Almacen almacen = new Almacen();
            almacen.setCodigo("ALM-" + s.getCodigo());
            almacen.setNombre("Almacén " + s.getNombre());
            almacen.setPrincipal(false);
            almacen.setSucursal(s);
            inventarioService.crearAlmacen(almacen);
            auditoriaService.registrarCreacion("sucursal", nombre,
                    "Código: " + codigo + (direccion != null ? " | Dirección: " + direccion : "") + " | Almacén creado para transferencias.");
        } else {
            // Actualizar sucursal existente
            Sucursal datos = new Sucursal();
            datos.setCodigo(codigo);
            datos.setNombre(nombre);
            datos.setDireccion(direccion);
            datos.setActiva(true);
            datos.setCentral(central);
            sucursalService.actualizar(id, datos);
            auditoriaService.registrarAccion("PUT", "Actualizar sucursal",
                    "Código: " + codigo + " | Nombre: " + nombre + (direccion != null ? " | Dirección: " + direccion : "") + (central ? " | Central" : ""));
        }
        return "redirect:/web/sucursales";
    }

    @PostMapping("/eliminar")
    public String eliminar(@RequestParam("id") Long id) {
        sucursalService.desactivar(id);
        auditoriaService.registrarAccion("DELETE", "Desactivar sucursal", "ID: " + id);
        return "redirect:/web/sucursales";
    }

    @PostMapping("/marcar-central")
    public String marcarComoCentral(@RequestParam("sucursalId") Long sucursalId) {
        sucursalService.marcarComoCentral(sucursalId);
        return "redirect:/web/sucursales";
    }

    @GetMapping("/inventario")
    public String inventarioSucursal(@RequestParam("sucursalId") Long sucursalId,
                                     Model model) {
        Sucursal sucursal = sucursalService.obtenerPorId(sucursalId);
        Map<Producto, Integer> stock = inventarioService.stockPorSucursal(sucursalId);
        model.addAttribute("sucursal", sucursal);
        model.addAttribute("stockPorProducto", stock);
        model.addAttribute("sucursales", sucursalService.listarTodas());
        model.addAttribute("sucursalIdSeleccionada", sucursalId);
        return "sucursales-inventario";
    }

    @GetMapping("/ventas")
    public String ventasPorSucursal(@RequestParam("sucursalId") Long sucursalId,
                                    @RequestParam(value = "desde", required = false) String desdeStr,
                                    @RequestParam(value = "hasta", required = false) String hastaStr,
                                    org.springframework.ui.Model model) {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.LocalDate desde = (desdeStr == null || desdeStr.isBlank()) ? hoy.minusDays(30) : java.time.LocalDate.parse(desdeStr);
        java.time.LocalDate hasta = (hastaStr == null || hastaStr.isBlank()) ? hoy : java.time.LocalDate.parse(hastaStr);

        Sucursal sucursal = sucursalService.obtenerPorId(sucursalId);
        java.util.List<Venta> ventas = ventaService.listarPorSucursalEntreFechas(sucursalId, desde, hasta);
        java.math.BigDecimal total = ventaService.totalVentasPorSucursalEntreFechas(sucursalId, desde, hasta);

        model.addAttribute("sucursales", sucursalService.listarTodas());
        model.addAttribute("sucursal", sucursal);
        model.addAttribute("sucursalIdSeleccionada", sucursalId);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("ventas", ventas);
        model.addAttribute("totalVentas", total);
        return "sucursales-ventas";
    }
}

