package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.digemid.*;
import com.farmacia.sistema.domain.inventario.InventarioService;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/web/digemid")
public class DigemidWebController {

    private final DigemidService digemidService;
    private final InventarioService inventarioService;
    private final ProductoService productoService;

    public DigemidWebController(DigemidService digemidService, InventarioService inventarioService, ProductoService productoService) {
        this.digemidService = digemidService;
        this.inventarioService = inventarioService;
        this.productoService = productoService;
    }

    @GetMapping
    public String dashboard(Model model) {
        List<Producto> controlados = digemidService.listarProductosControlados();
        List<StockControlado> stockControlado = digemidService.listarStockControlado();
        List<RegistroReceta> ultimasRecetas = digemidService.listarRegistrosReceta();
        if (ultimasRecetas.size() > 50) ultimasRecetas = ultimasRecetas.subList(0, 50);
        List<DistribucionControlada> distribuciones = digemidService.listarDistribuciones();
        if (distribuciones.size() > 30) distribuciones = distribuciones.subList(0, 30);
        Map<String, Object> alertas = digemidService.resumenAlertasCompleto();
        List<StockControlado> porVencer = digemidService.alertasVencimientoProximo(30);
        List<StockControlado> vencidos = digemidService.alertasVencidos();
        List<Map<String, Object>> excesoVenta = digemidService.alertasExcesoVenta(7, 50);

        model.addAttribute("productosControlados", controlados);
        model.addAttribute("stockControlado", stockControlado);
        model.addAttribute("ultimasRecetas", ultimasRecetas);
        model.addAttribute("distribuciones", distribuciones);
        model.addAttribute("alertas", alertas);
        model.addAttribute("porVencer", porVencer);
        model.addAttribute("vencidos", vencidos);
        model.addAttribute("excesoVenta", excesoVenta);
        model.addAttribute("almacenes", inventarioService.listarAlmacenes());
        model.addAttribute("productosParaForm", productoService.listarTodos().stream().filter(Producto::isRequiereReceta).toList());
        return "digemid";
    }

    @GetMapping("/kardex/{productoId}")
    public String kardex(@PathVariable Long productoId, Model model) {
        List<MovimientoControlado> kardex = digemidService.kardexControladoPorProducto(productoId);
        Producto p;
        try {
            p = productoService.obtenerPorId(productoId);
        } catch (Exception e) {
            p = null;
        }
        model.addAttribute("kardex", kardex);
        model.addAttribute("producto", p);
        return "digemid-kardex";
    }

    @PostMapping("/recetas")
    public String guardarReceta(@ModelAttribute RegistroReceta reg, RedirectAttributes ra) {
        try {
            digemidService.guardarRegistroReceta(reg);
            ra.addFlashAttribute("mensajeReceta", "Receta registrada correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorReceta", e.getMessage());
        }
        return "redirect:/web/digemid";
    }

    @GetMapping("/reportes/movimientos")
    public String reporteMovimientos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        LocalDate d = desde != null ? desde : LocalDate.now().minusMonths(1);
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        List<MovimientoControlado> movimientos = digemidService.reporteRegulatorioMovimientos(d, h);
        model.addAttribute("movimientos", movimientos);
        model.addAttribute("desde", d);
        model.addAttribute("hasta", h);
        return "digemid-reporte-movimientos";
    }

    @GetMapping("/reportes/recetas")
    public String reporteRecetas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        LocalDate d = desde != null ? desde : LocalDate.now().minusMonths(1);
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        List<RegistroReceta> recetas = digemidService.reporteRegulatorioRecetas(d, h);
        model.addAttribute("recetas", recetas);
        model.addAttribute("desde", d);
        model.addAttribute("hasta", h);
        return "digemid-reporte-recetas";
    }

    @PostMapping("/distribucion/recibir/{id}")
    public String confirmarRecepcionDistribucion(@PathVariable Long id,
                                                 @RequestParam(required = false) String usuarioRecepcion,
                                                 RedirectAttributes ra) {
        try {
            digemidService.confirmarRecepcionDistribucion(id, usuarioRecepcion);
            ra.addFlashAttribute("mensajeDigemid", "Recepción de distribución registrada.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorDigemid", e.getMessage());
        }
        return "redirect:/web/digemid";
    }

    @PostMapping("/entrada")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR','QUIMICO_FARMACEUTICO')")
    public String registrarEntrada(@RequestParam Long productoId,
                                   @RequestParam(required = false) Long almacenId,
                                   @RequestParam(required = false) String lote,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaVencimiento,
                                   @RequestParam int cantidad,
                                   @RequestParam(required = false) String referencia,
                                   @RequestParam(required = false) String ubicacion,
                                   RedirectAttributes ra) {
        try {
            digemidService.registrarEntradaControlada(productoId, almacenId, lote, fechaVencimiento, cantidad, referencia, ubicacion);
            ra.addFlashAttribute("mensajeDigemid", "Entrada de producto controlado registrada.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorDigemid", e.getMessage());
        }
        return "redirect:/web/digemid";
    }

    @PostMapping("/destruccion")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR','QUIMICO_FARMACEUTICO')")
    public String registrarDestruccion(@RequestParam Long productoId,
                                       @RequestParam(required = false) Long almacenId,
                                       @RequestParam int cantidad,
                                       @RequestParam(required = false) String motivo,
                                       @RequestParam(required = false) String documento,
                                       RedirectAttributes ra) {
        try {
            String usuario = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null
                    ? org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName() : "Sistema";
            digemidService.registrarDestruccion(productoId, almacenId, cantidad, motivo, documento, usuario);
            ra.addFlashAttribute("mensajeDigemid", "Destrucción autorizada registrada.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorDigemid", e.getMessage());
        }
        return "redirect:/web/digemid";
    }

    @PostMapping("/distribucion")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR','QUIMICO_FARMACEUTICO')")
    public String crearDistribucion(@RequestParam Long productoId,
                                     @RequestParam Long almacenOrigenId,
                                     @RequestParam Long almacenDestinoId,
                                     @RequestParam int cantidad,
                                     @RequestParam(required = false) String lote,
                                     @RequestParam(required = false) String referencia,
                                     @RequestParam(required = false) String clienteRuc,
                                     @RequestParam(required = false) String clienteRazonSocial,
                                     @RequestParam(required = false) String tipoEstablecimiento,
                                     @RequestParam(required = false) String numeroFactura,
                                     @RequestParam(required = false) String numeroGuiaRemision,
                                     RedirectAttributes ra) {
        try {
            String usuario = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null
                    ? org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName() : "Sistema";
            digemidService.crearDistribucion(productoId, almacenOrigenId, almacenDestinoId, cantidad, lote, referencia, usuario,
                    clienteRuc, clienteRazonSocial, tipoEstablecimiento, numeroFactura, numeroGuiaRemision);
            ra.addFlashAttribute("mensajeDigemid", "Distribución de controlado creada.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorDigemid", e.getMessage());
        }
        return "redirect:/web/digemid";
    }

    @GetMapping("/reportes/psicotropicos")
    public String reportePsicotropicos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        LocalDate d = desde != null ? desde : LocalDate.now().withDayOfMonth(1);
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        model.addAttribute("movimientos", digemidService.reportePorTipoProducto("PSICOTROPICO", d, h));
        model.addAttribute("desde", d);
        model.addAttribute("hasta", h);
        model.addAttribute("tituloReporte", "Reporte mensual de Psicotrópicos");
        return "digemid-reporte-movimientos";
    }

    @GetMapping("/reportes/estupefacientes")
    public String reporteEstupefacientes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        LocalDate d = desde != null ? desde : LocalDate.now().withDayOfMonth(1);
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        model.addAttribute("movimientos", digemidService.reportePorTipoProducto("ESTUPEFACIENTE", d, h));
        model.addAttribute("desde", d);
        model.addAttribute("hasta", h);
        model.addAttribute("tituloReporte", "Reporte de Estupefacientes");
        return "digemid-reporte-movimientos";
    }

    @GetMapping("/reportes/lotes")
    public String reporteControlLotes(Model model) {
        model.addAttribute("stockControlado", digemidService.reporteControlLotes());
        model.addAttribute("tituloReporte", "Control de lotes - Productos controlados");
        return "digemid-reporte-lotes";
    }

    @GetMapping("/reportes/distribucion")
    public String reporteDistribucion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        LocalDate d = desde != null ? desde : LocalDate.now().minusMonths(3);
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        model.addAttribute("distribuciones", digemidService.reporteHistorialDistribucion(d, h));
        model.addAttribute("desde", d);
        model.addAttribute("hasta", h);
        model.addAttribute("tituloReporte", "Historial de distribución controlada");
        return "digemid-reporte-distribucion";
    }
}
