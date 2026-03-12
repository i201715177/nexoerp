package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.auditoria.AuditoriaService;
import com.farmacia.sistema.domain.cliente.Cliente;
import com.farmacia.sistema.domain.cliente.ClienteService;
import com.farmacia.sistema.domain.venta.VentaService;
import com.farmacia.sistema.export.ExcelExportUtil;
import com.farmacia.sistema.export.PdfExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.farmacia.sistema.domain.auditoria.AuditoriaService.cmp;
import static com.farmacia.sistema.domain.auditoria.AuditoriaService.nuevoCambios;

@Controller
@RequestMapping("/web/clientes")
public class ClienteWebController {

    private final ClienteService clienteService;
    private final VentaService ventaService;
    private final AuditoriaService auditoriaService;

    public ClienteWebController(ClienteService clienteService,
                                VentaService ventaService,
                                AuditoriaService auditoriaService) {
        this.clienteService = clienteService;
        this.ventaService = ventaService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public String listar(Model model) {
        var clientes = clienteService.listarTodos();
        model.addAttribute("clientes", clientes);
        model.addAttribute("cliente", new Cliente());
        java.util.Map<Long, String> ultimaCompraPorCliente = new java.util.HashMap<>();
        java.util.Map<Long, java.math.BigDecimal> totalPorCliente = new java.util.HashMap<>();
        java.util.Map<Long, String> segmentoPorCliente = new java.util.HashMap<>();
        for (Cliente c : clientes) {
            ultimaCompraPorCliente.put(c.getId(), ventaService.getDescripcionUltimaVentaPorCliente(c.getId()));
            java.math.BigDecimal total = ventaService.getTotalCompradoPorCliente(c.getId());
            totalPorCliente.put(c.getId(), total);
            segmentoPorCliente.put(c.getId(), calcularSegmento(total));
        }
        long oroCount = segmentoPorCliente.values().stream().filter("ORO"::equals).count();
        model.addAttribute("ultimaCompraPorCliente", ultimaCompraPorCliente);
        model.addAttribute("totalPorCliente", totalPorCliente);
        model.addAttribute("segmentoPorCliente", segmentoPorCliente);
        model.addAttribute("oroCount", oroCount);
        return "clientes";
    }

    private String calcularSegmento(java.math.BigDecimal total) {
        if (total == null) return "BRONCE";
        if (total.compareTo(new java.math.BigDecimal("1000")) >= 0) return "ORO";
        if (total.compareTo(new java.math.BigDecimal("300")) >= 0) return "PLATA";
        return "BRONCE";
    }

    @PostMapping
    public String crear(@ModelAttribute("cliente") @Valid Cliente cliente,
                        BindingResult bindingResult,
                        Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("clientes", clienteService.listarTodos());
            return "clientes";
        }
        try {
            if (cliente.getId() != null && cliente.getId() > 0) {
                Cliente anterior = clienteService.obtenerPorId(cliente.getId());
                Map<String, String[]> cambios = nuevoCambios();
                cmp(cambios, "Nombres", anterior.getNombres(), cliente.getNombres());
                cmp(cambios, "Apellidos", anterior.getApellidos(), cliente.getApellidos());
                cmp(cambios, "Tipo doc", anterior.getTipoDocumento(), cliente.getTipoDocumento());
                cmp(cambios, "N° doc", anterior.getNumeroDocumento(), cliente.getNumeroDocumento());
                cmp(cambios, "Teléfono", anterior.getTelefono(), cliente.getTelefono());
                cmp(cambios, "Email", anterior.getEmail(), cliente.getEmail());
                cmp(cambios, "Dirección", anterior.getDireccion(), cliente.getDireccion());
                cmp(cambios, "Activo", anterior.isActivo(), cliente.isActivo());

                clienteService.actualizar(cliente.getId(), cliente);
                auditoriaService.registrarActualizacion("cliente",
                        anterior.getNombres() + " " + (anterior.getApellidos() != null ? anterior.getApellidos() : ""),
                        cambios);
            } else {
                clienteService.crear(cliente);
                auditoriaService.registrarCreacion("cliente",
                        cliente.getNombres() + " " + (cliente.getApellidos() != null ? cliente.getApellidos() : ""),
                        "Doc: " + cliente.getTipoDocumento() + " " + cliente.getNumeroDocumento());
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("clientes", clienteService.listarTodos());
            model.addAttribute("errorCliente", e.getMessage());
            return "clientes";
        }
        return "redirect:/web/clientes";
    }

    @PostMapping("/eliminar")
    public String eliminar(@RequestParam("id") Long id) {
        Cliente c = clienteService.obtenerPorId(id);
        auditoriaService.registrarEliminacion("cliente",
                c.getNombres() + " " + (c.getApellidos() != null ? c.getApellidos() : ""),
                "Doc: " + c.getTipoDocumento() + " " + c.getNumeroDocumento());
        clienteService.eliminar(id);
        return "redirect:/web/clientes";
    }

    @GetMapping("/excel")
    public void exportarExcel(HttpServletResponse response) throws Exception {
        var clientes = clienteService.listarTodos();
        String[] headers = {"Tipo Doc", "N° Documento", "Nombres", "Apellidos",
                "Teléfono", "Email", "Dirección", "Puntos", "Activo"};
        List<Object[]> filas = new ArrayList<>();
        for (Cliente c : clientes) {
            filas.add(new Object[]{
                    c.getTipoDocumento(), c.getNumeroDocumento(), c.getNombres(),
                    c.getApellidos(), c.getTelefono(), c.getEmail(),
                    c.getDireccion(), c.getPuntos(), c.isActivo()
            });
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=clientes_" + LocalDate.now() + ".xlsx");
        try (Workbook wb = ExcelExportUtil.crearReporte("Listado de Clientes",
                "Exportado el " + LocalDate.now(), headers, filas)) {
            wb.write(response.getOutputStream());
        }
    }

    @GetMapping("/pdf")
    public void exportarPdf(HttpServletResponse response) throws Exception {
        var clientes = clienteService.listarTodos();
        String[] headers = {"Tipo Doc", "N° Documento", "Nombres", "Apellidos",
                "Teléfono", "Email", "Puntos", "Activo"};
        List<Object[]> filas = new ArrayList<>();
        for (Cliente c : clientes) {
            filas.add(new Object[]{
                    c.getTipoDocumento(), c.getNumeroDocumento(), c.getNombres(),
                    c.getApellidos(), c.getTelefono(), c.getEmail(),
                    c.getPuntos(), c.isActivo()
            });
        }
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=clientes_" + LocalDate.now() + ".pdf");
        PdfExportUtil.crearReporte(response.getOutputStream(), "Listado de Clientes",
                "Exportado el " + LocalDate.now(), headers, filas, null);
    }

    @GetMapping("/historial")
    public String historial(@RequestParam("clienteId") Long clienteId, Model model) {
        Cliente cliente = clienteService.obtenerPorId(clienteId);
        var ventas = ventaService.listarPorCliente(clienteId);
        java.math.BigDecimal total = ventaService.getTotalCompradoPorCliente(clienteId);
        model.addAttribute("cliente", cliente);
        model.addAttribute("ventas", ventas);
        model.addAttribute("totalComprado", total);
        return "clientes-historial";
    }
}
