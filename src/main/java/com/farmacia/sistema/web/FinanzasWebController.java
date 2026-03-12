package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.compra.CompraService;
import com.farmacia.sistema.domain.compra.CuentaPagar;
import com.farmacia.sistema.domain.finanzas.FinanzasService;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.venta.Venta;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/web/finanzas")
@PreAuthorize("hasAnyRole('ADMIN', 'SAAS_ADMIN')")
public class FinanzasWebController {

    private final FinanzasService finanzasService;
    private final CompraService compraService;

    public FinanzasWebController(FinanzasService finanzasService,
                                 CompraService compraService) {
        this.finanzasService = finanzasService;
        this.compraService = compraService;
    }

    @GetMapping
    public String dashboard(Model model) {
        LocalDate hoy = LocalDate.now();
        BigDecimal ventasHoy = finanzasService.totalVentasEntre(hoy, hoy);
        BigDecimal cxpPendiente = finanzasService.totalCuentasPorPagarPendientes();
        BigDecimal cxcPendiente = finanzasService.totalCuentasPorCobrar();
        BigDecimal utilidadHoy = finanzasService.calcularUtilidadEntre(hoy, hoy);

        List<FinanzasService.MargenProductoResumen> dummyList = java.util.List.of();

        var topMargen = finanzasService.topMargenPorProducto(5);

        model.addAttribute("ventasHoy", ventasHoy);
        model.addAttribute("cxpPendiente", cxpPendiente);
        model.addAttribute("cxcPendiente", cxcPendiente);
        model.addAttribute("utilidadHoy", utilidadHoy);
        model.addAttribute("topMargen", topMargen);
        model.addAttribute("cuentasCobrarDetalle", finanzasService.listarCuentasPorCobrarDetalle());
        model.addAttribute("hoy", hoy);
        return "finanzas";
    }

    @GetMapping("/cuentas-cobrar")
    public String cuentasPorCobrar(Model model) {
        model.addAttribute("cuentasCobrarDetalle", finanzasService.listarCuentasPorCobrarDetalle());
        return "finanzas-cuentas-cobrar";
    }

    @GetMapping("/cuentas-pagar")
    public String cuentasPorPagar(Model model) {
        List<CuentaPagar> pendientes = compraService.listarCuentasPendientes();
        List<CuentaPagar> todas = compraService.listarTodasCuentas();
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("todas", todas);
        return "cuentas-pagar";
    }

    @GetMapping("/utilidad")
    public String utilidad(@RequestParam(value = "desde", required = false) String desdeStr,
                           @RequestParam(value = "hasta", required = false) String hastaStr,
                           Model model) {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = (desdeStr == null || desdeStr.isBlank()) ? hoy.minusDays(7) : LocalDate.parse(desdeStr);
        LocalDate hasta = (hastaStr == null || hastaStr.isBlank()) ? hoy : LocalDate.parse(hastaStr);

        BigDecimal ventas = finanzasService.totalVentasEntre(desde, hasta);
        BigDecimal utilidad = finanzasService.calcularUtilidadEntre(desde, hasta);

        List<Map.Entry<Producto, FinanzasService.MargenProductoResumen>> topMargen = finanzasService.topMargenPorProducto(10);

        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("ventas", ventas);
        model.addAttribute("utilidad", utilidad);
        model.addAttribute("topMargen", topMargen);
        return "finanzas-utilidad";
    }
}

