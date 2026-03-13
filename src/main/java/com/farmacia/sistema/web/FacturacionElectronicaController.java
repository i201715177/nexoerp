package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.empresa.Empresa;
import com.farmacia.sistema.domain.empresa.EmpresaService;
import com.farmacia.sistema.domain.facturacion.SunatIntegrationService;
import com.farmacia.sistema.domain.guiaremision.GuiaRemision;
import com.farmacia.sistema.domain.guiaremision.GuiaRemisionService;
import com.farmacia.sistema.domain.venta.*;
import com.farmacia.sistema.tenant.TenantContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/web/facturacion-electronica")
public class FacturacionElectronicaController {

    private final VentaService ventaService;
    private final NotaDebitoRepository notaDebitoRepository;
    private final GuiaRemisionService guiaRemisionService;
    private final EmpresaService empresaService;
    private final SunatIntegrationService sunatService;
    private final SequenceComprobanteService sequenceService;

    public FacturacionElectronicaController(VentaService ventaService,
                                            NotaDebitoRepository notaDebitoRepository,
                                            GuiaRemisionService guiaRemisionService,
                                            EmpresaService empresaService,
                                            SunatIntegrationService sunatService,
                                            SequenceComprobanteService sequenceService) {
        this.ventaService = ventaService;
        this.notaDebitoRepository = notaDebitoRepository;
        this.guiaRemisionService = guiaRemisionService;
        this.empresaService = empresaService;
        this.sunatService = sunatService;
        this.sequenceService = sequenceService;
    }

    @GetMapping
    public String dashboard(Model model) {
        List<Venta> ventas = ventaService.listarTodas();

        List<Venta> facturas = ventas.stream()
                .filter(v -> "FAC".equals(v.getTipoComprobante()))
                .sorted((a, b) -> b.getFechaHora().compareTo(a.getFechaHora()))
                .toList();
        List<Venta> boletas = ventas.stream()
                .filter(v -> !"FAC".equals(v.getTipoComprobante()))
                .sorted((a, b) -> b.getFechaHora().compareTo(a.getFechaHora()))
                .toList();

        Long tenantId = TenantContext.getTenantId();
        List<NotaDebito> notasDebito = tenantId != null
                ? notaDebitoRepository.findByTenantIdOrderByFechaDesc(tenantId)
                : notaDebitoRepository.findAll();

        List<GuiaRemision> guias = guiaRemisionService.listarTodas();

        long totalFacturas = facturas.size();
        long totalBoletas = boletas.size();
        long totalNC = 0;
        long totalND = notasDebito.size();
        long totalGR = guias.size();
        long pendientesSunat = ventas.stream()
                .filter(v -> "PENDIENTE".equals(v.getEstadoSunat()))
                .count();

        model.addAttribute("facturas", facturas);
        model.addAttribute("boletas", boletas);
        model.addAttribute("notasDebito", notasDebito);
        model.addAttribute("guias", guias);
        model.addAttribute("totalFacturas", totalFacturas);
        model.addAttribute("totalBoletas", totalBoletas);
        model.addAttribute("totalNC", totalNC);
        model.addAttribute("totalND", totalND);
        model.addAttribute("totalGR", totalGR);
        model.addAttribute("pendientesSunat", pendientesSunat);
        model.addAttribute("sunatHabilitado", sunatService.isHabilitado());
        model.addAttribute("sunatModo", sunatService.getModo());

        return "facturacion-electronica";
    }

    @PostMapping("/nota-debito")
    public String crearNotaDebito(@RequestParam("ventaId") Long ventaId,
                                  @RequestParam("monto") BigDecimal monto,
                                  @RequestParam("motivo") String motivo,
                                  RedirectAttributes ra) {
        try {
            Venta venta = ventaService.obtenerPorId(ventaId);
            NotaDebito nd = new NotaDebito();
            nd.setVenta(venta);
            nd.setSerie("FD01");
            nd.setNumero(String.format("%08d", sequenceService.getNextNumeroPorTipo("ND")));
            nd.setFecha(LocalDateTime.now());
            nd.setMonto(monto);
            nd.setMotivo(motivo);
            nd.setEstado("EMITIDA");
            nd.setEstadoSunat("PENDIENTE");
            notaDebitoRepository.save(nd);
            ra.addFlashAttribute("mensaje", "Nota de débito " + nd.getSerieNumero() + " emitida correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/web/facturacion-electronica";
    }
}
