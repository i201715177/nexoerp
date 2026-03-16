package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.inventario.InventarioService;
import com.farmacia.sistema.domain.merma.Merma;
import com.farmacia.sistema.domain.merma.MermaService;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/web/mermas")
public class MermaWebController {

    private final MermaService mermaService;
    private final ProductoService productoService;
    private final InventarioService inventarioService;

    public MermaWebController(MermaService mermaService,
                              ProductoService productoService,
                              InventarioService inventarioService) {
        this.mermaService = mermaService;
        this.productoService = productoService;
        this.inventarioService = inventarioService;
    }

    @GetMapping
    public String listar(Model model,
                         @RequestParam(value = "tipo", required = false) String tipoFiltro,
                         @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                         @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        List<Merma> mermas;
        if (desde != null && hasta != null) {
            mermas = mermaService.listarEntreFechas(desde.atStartOfDay(), hasta.atTime(23, 59, 59));
        } else if (tipoFiltro != null && !tipoFiltro.isBlank()) {
            mermas = mermaService.listarPorTipo(tipoFiltro);
        } else {
            mermas = mermaService.listarTodas();
        }
        if (mermas.size() > 200) mermas = mermas.subList(0, 200);

        List<Producto> productos = productoService.listarTodos();
        Map<String, Object> resumen = mermaService.resumenMermas();
        List<Producto> proximosVencer = mermaService.alertasProximoVencer(30);
        List<Map<String, Object>> excesoLotes = mermaService.alertasExcesoMermasPorLote(10);

        model.addAttribute("mermas", mermas);
        model.addAttribute("productos", productos);
        model.addAttribute("almacenes", inventarioService.listarAlmacenes());
        model.addAttribute("tipoFiltro", tipoFiltro);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("resumen", resumen);
        model.addAttribute("proximosVencer", proximosVencer);
        model.addAttribute("excesoLotes", excesoLotes);
        model.addAttribute("tiposMerma", List.of("PRODUCTO_VENCIDO", "PRODUCTO_DANADO", "ROTURA", "ERROR_INVENTARIO", "DESTRUCCION_AUTORIZADA", "DETERIORO", "EXTRAVIO", "OTRO"));
        return "mermas";
    }

    @PostMapping("/registrar")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR','QUIMICO_FARMACEUTICO')")
    public String registrar(
            @RequestParam Long productoId,
            @RequestParam(required = false) Long almacenId,
            @RequestParam int cantidad,
            @RequestParam(required = false, defaultValue = "OTRO") String tipoMerma,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) String observaciones,
            @RequestParam(required = false) String lote,
            @RequestParam(required = false) String responsableAutorizado,
            @RequestParam(required = false) String aprobacionQF,
            @RequestParam(required = false) String actaDestruccion,
            @RequestParam(required = false) String numeroReporte,
            RedirectAttributes ra) {
        try {
            String usuario = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null
                    ? org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName()
                    : "Sistema";
            Merma m = mermaService.registrarMerma(productoId, almacenId, cantidad, tipoMerma, motivo, usuario,
                    observaciones, lote, responsableAutorizado, aprobacionQF, actaDestruccion, numeroReporte);
            ra.addFlashAttribute("mensajeMerma", "Merma " + m.getNumeroMerma() + " registrada. Stock actualizado de " + m.getStockAntes() + " a " + m.getStockDespues() + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMerma", e.getMessage());
        }
        return "redirect:/web/mermas";
    }

    @GetMapping("/producto/{productoId}")
    public String porProducto(@PathVariable Long productoId, Model model) {
        List<Merma> mermas = mermaService.listarPorProducto(productoId);
        Producto p = productoService.obtenerPorId(productoId);
        model.addAttribute("mermas", mermas);
        model.addAttribute("producto", p);
        return "mermas-producto";
    }

    @GetMapping("/acta-destruccion/{id}")
    public String actaDestruccion(@PathVariable Long id, Model model) {
        Merma merma = mermaService.obtenerPorId(id);
        model.addAttribute("merma", merma);
        return "mermas-acta-destruccion";
    }

    @GetMapping("/reportes/por-producto")
    public String reportePorProducto(Model model) {
        model.addAttribute("reporte", mermaService.reportePorProducto());
        return "mermas-reporte-producto";
    }

    @GetMapping("/reportes/por-periodo")
    public String reportePorPeriodo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        LocalDate d = desde != null ? desde : LocalDate.now().withDayOfMonth(1);
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        List<Merma> mermas = mermaService.listarEntreFechas(d.atStartOfDay(), h.atTime(23, 59, 59));
        model.addAttribute("mermas", mermas);
        model.addAttribute("desde", d);
        model.addAttribute("hasta", h);
        return "mermas-reporte-periodo";
    }
}
