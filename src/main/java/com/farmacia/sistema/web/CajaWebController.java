package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.caja.CajaTurno;
import com.farmacia.sistema.domain.caja.CajaTurnoService;
import com.farmacia.sistema.domain.inventario.InventarioService;
import com.farmacia.sistema.domain.inventario.Transferencia;
import com.farmacia.sistema.domain.sucursal.Sucursal;
import com.farmacia.sistema.domain.sucursal.SucursalService;
import com.farmacia.sistema.security.TenantUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import com.farmacia.sistema.export.ExcelExportUtil;
import com.farmacia.sistema.export.PdfExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/web/caja")
public class CajaWebController {

    private final CajaTurnoService cajaTurnoService;
    private final SucursalService sucursalService;
    private final InventarioService inventarioService;
    private final com.farmacia.sistema.domain.auditoria.AuditoriaService auditoriaService;

    public CajaWebController(CajaTurnoService cajaTurnoService,
                             SucursalService sucursalService,
                             InventarioService inventarioService,
                             com.farmacia.sistema.domain.auditoria.AuditoriaService auditoriaService) {
        this.cajaTurnoService = cajaTurnoService;
        this.sucursalService = sucursalService;
        this.inventarioService = inventarioService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public String index(@RequestParam(value = "sucursalId", required = false) Long sucursalId, Model model) {
        Long sucursalIdVendedor = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof TenantUserDetails tud) {
            sucursalIdVendedor = tud.getSucursalId();
        }
        if (sucursalIdVendedor != null && sucursalId == null) {
            sucursalId = sucursalIdVendedor;
        }
        Optional<CajaTurno> cajaAbierta = cajaTurnoService.obtenerCajaAbierta(sucursalId);
        CajaTurno caja = cajaAbierta.orElse(null);
        model.addAttribute("cajaAbierta", caja);
        var turnos = sucursalId != null ? cajaTurnoService.listarPorSucursal(sucursalId) : cajaTurnoService.listarTodos();
        model.addAttribute("turnos", turnos);
        model.addAttribute("sucursalFiltroId", sucursalId);
        Map<Long, BigDecimal> ventasPorTurno = new HashMap<>();
        for (CajaTurno t : turnos) {
            ventasPorTurno.put(t.getId(), cajaTurnoService.getTotalVentasEnTurno(t.getId()));
        }
        model.addAttribute("ventasPorTurno", ventasPorTurno);
        if (caja != null) {
            java.math.BigDecimal totalVentas = cajaTurnoService.getTotalVentasEnTurno(caja.getId());
            model.addAttribute("totalVentasTurno", totalVentas);
            model.addAttribute("montoEsperadoCaja", caja.getMontoInicial().add(totalVentas));
            model.addAttribute("resumenPorMedioPago", cajaTurnoService.getResumenPagosPorMedio(caja.getId()));
        } else {
            model.addAttribute("totalVentasTurno", java.math.BigDecimal.ZERO);
            model.addAttribute("montoEsperadoCaja", null);
            model.addAttribute("resumenPorMedioPago", List.<Map<String,Object>>of());
        }
        List<Sucursal> sucursales = sucursalIdVendedor != null
                ? List.of(sucursalService.obtenerPorId(sucursalIdVendedor))
                : sucursalService.listarTodas();
        model.addAttribute("sucursales", sucursales);
        // Sucursal en contexto: la de la caja abierta o la elegida en "Ver sucursal"
        Long sucursalCaja = caja != null && caja.getSucursal() != null ? caja.getSucursal().getId() : sucursalId;
        // Solo mostrar transferencias pendientes cuando hay una sucursal definida y solo las destinadas a ESA sucursal (válido para muchas sucursales)
        List<Transferencia> pendientesRecepcion = sucursalCaja != null
                ? inventarioService.listarPendientesPorSucursal(sucursalCaja)
                : List.of();
        model.addAttribute("pendientesRecepcion", pendientesRecepcion);
        return "caja";
    }

    @PostMapping("/confirmar-recepcion/{id}")
    public String confirmarRecepcionDesdeCaja(@PathVariable("id") Long id,
                                              @RequestParam(value = "sucursalId", required = false) Long sucursalId,
                                              RedirectAttributes ra) {
        try {
            String usuario = SecurityContextHolder.getContext().getAuthentication() != null
                    ? SecurityContextHolder.getContext().getAuthentication().getName() : "Sistema";
            inventarioService.confirmarRecepcion(id, usuario);
            ra.addFlashAttribute("mensaje", "Recepción confirmada. El producto ya está disponible en su almacén.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return sucursalId != null ? "redirect:/web/caja?sucursalId=" + sucursalId : "redirect:/web/caja";
    }

    @PostMapping("/abrir")
    public String abrir(@RequestParam(value = "montoInicial", required = false) String montoInicialStr,
                        @RequestParam(value = "sucursalId", required = false) Long sucursalId,
                        @RequestParam(value = "nombreVendedor", required = false) String nombreVendedor,
                        RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof TenantUserDetails tud && tud.getSucursalId() != null) {
            sucursalId = tud.getSucursalId();
        }
        try {
            BigDecimal montoInicial = BigDecimal.ZERO;
            if (montoInicialStr != null && !montoInicialStr.isBlank()) {
                try {
                    montoInicial = new BigDecimal(montoInicialStr.trim().replace(',', '.'));
                } catch (NumberFormatException e) {
                    montoInicial = BigDecimal.ZERO;
                }
            }
            CajaTurno turno = cajaTurnoService.abrirCaja(montoInicial, sucursalId, nombreVendedor);
            String vendedor = turno.getNombreVendedor() != null && !turno.getNombreVendedor().isBlank()
                    ? turno.getNombreVendedor() : turno.getUsuario();
            auditoriaService.registrarCreacion("apertura de caja", "Caja abierta",
                    "Monto inicial: S/ " + montoInicial + " | Cajero/Vendedor: " + vendedor);
            ra.addFlashAttribute("mensaje", "Caja abierta correctamente. Monto asignado: S/ " + montoInicial);
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return sucursalId != null ? "redirect:/web/caja?sucursalId=" + sucursalId : "redirect:/web/caja";
    }

    @GetMapping("/excel")
    public void exportarExcel(@RequestParam(value = "sucursalId", required = false) Long sucursalId,
                              HttpServletResponse response) throws Exception {
        var turnos = sucursalId != null ? cajaTurnoService.listarPorSucursal(sucursalId) : cajaTurnoService.listarTodos();
        String[] headers = {"ID", "Vendedor", "Apertura", "Cierre", "Monto Inicial",
                "Monto Cierre", "Ventas Turno", "Estado", "Observaciones"};
        List<Object[]> filas = new ArrayList<>();
        for (CajaTurno t : turnos) {
            String vendedor = t.getNombreVendedor() != null && !t.getNombreVendedor().isBlank() ? t.getNombreVendedor() : t.getUsuario();
            filas.add(new Object[]{
                    t.getId(), vendedor, t.getFechaApertura(), t.getFechaCierre(),
                    t.getMontoInicial(), t.getMontoCierre(),
                    cajaTurnoService.getTotalVentasEnTurno(t.getId()),
                    t.getEstado(), t.getObservaciones()
            });
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=caja_turnos_" + LocalDate.now() + ".xlsx");
        try (Workbook wb = ExcelExportUtil.crearReporte("Turnos de Caja",
                "Exportado el " + LocalDate.now(), headers, filas)) {
            wb.write(response.getOutputStream());
        }
    }

    @GetMapping("/pdf")
    public void exportarPdf(@RequestParam(value = "sucursalId", required = false) Long sucursalId,
                           HttpServletResponse response) throws Exception {
        var turnos = sucursalId != null ? cajaTurnoService.listarPorSucursal(sucursalId) : cajaTurnoService.listarTodos();
        String[] headers = {"ID", "Vendedor", "Apertura", "Cierre",
                "M. Inicial", "M. Cierre", "Ventas", "Estado"};
        List<Object[]> filas = new ArrayList<>();
        for (CajaTurno t : turnos) {
            String vendedor = t.getNombreVendedor() != null && !t.getNombreVendedor().isBlank() ? t.getNombreVendedor() : t.getUsuario();
            filas.add(new Object[]{
                    t.getId(), vendedor, t.getFechaApertura(), t.getFechaCierre(),
                    t.getMontoInicial(), t.getMontoCierre(),
                    cajaTurnoService.getTotalVentasEnTurno(t.getId()),
                    t.getEstado()
            });
        }
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=caja_turnos_" + LocalDate.now() + ".pdf");
        PdfExportUtil.crearReporte(response.getOutputStream(), "Turnos de Caja",
                "Exportado el " + LocalDate.now(), headers, filas, null);
    }

    @GetMapping(value = "/resumen-medio-pago", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> resumenMedioPago(@RequestParam("turnoId") Long turnoId) {
        CajaTurno turno = cajaTurnoService.obtenerPorId(turnoId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("turnoId", turno.getId());
        out.put("vendedor", turno.getNombreVendedor() != null && !turno.getNombreVendedor().isBlank()
                ? turno.getNombreVendedor() : turno.getUsuario());
        out.put("apertura", turno.getFechaApertura() != null ? turno.getFechaApertura().format(fmt) : "-");
        out.put("cierre", turno.getFechaCierre() != null ? turno.getFechaCierre().format(fmt) : "-");
        out.put("sucursal", turno.getSucursal() != null ? turno.getSucursal().getNombre() : "Central");
        out.put("estado", turno.getEstado());
        out.put("montoInicial", turno.getMontoInicial());
        out.put("totalVentas", cajaTurnoService.getTotalVentasEnTurno(turnoId));
        out.put("resumen", cajaTurnoService.getResumenPagosPorMedio(turnoId));
        return out;
    }

    @PostMapping("/cerrar")
    public String cerrar(@RequestParam("turnoId") Long turnoId,
                         @RequestParam(value = "montoCierre", required = false) BigDecimal montoCierre,
                         @RequestParam(value = "observaciones", required = false) String observaciones,
                         @RequestParam(value = "sucursalId", required = false) Long sucursalId,
                         RedirectAttributes ra) {
        try {
            CajaTurno turno = cajaTurnoService.obtenerPorId(turnoId);
            List<Map<String, Object>> resumen = cajaTurnoService.getResumenPagosPorMedio(turnoId);
            cajaTurnoService.cerrarCaja(turnoId, montoCierre, observaciones);
            String vendedor = turno.getNombreVendedor() != null && !turno.getNombreVendedor().isBlank()
                    ? turno.getNombreVendedor() : turno.getUsuario();
            StringBuilder detalle = new StringBuilder();
            detalle.append("Turno #").append(turnoId).append(" | Cajero: ").append(vendedor)
                    .append(" | Monto cierre: S/ ").append(montoCierre != null ? montoCierre : "-");
            if (observaciones != null && !observaciones.isBlank())
                detalle.append(" | Obs: ").append(observaciones);
            if (resumen != null && !resumen.isEmpty()) {
                detalle.append(" | Por medio de pago: ");
                for (int i = 0; i < resumen.size(); i++) {
                    Map<String, Object> r = resumen.get(i);
                    if (i > 0) detalle.append("; ");
                    detalle.append(r.get("medio")).append(": ").append(r.get("cantidad"))
                            .append(" pagos S/ ").append(r.get("total"));
                }
            }
            auditoriaService.registrarAccion("PUT", "Cerrar caja", detalle.toString());
            ra.addFlashAttribute("mensaje", "Caja cerrada correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return sucursalId != null ? "redirect:/web/caja?sucursalId=" + sucursalId : "redirect:/web/caja";
    }

    /** Elimina el turno del historial (solo deja de mostrarse). Auditoría y ventas se mantienen. Solo turnos cerrados. */
    @PostMapping("/eliminar-del-historial")
    public String eliminarDelHistorial(@RequestParam("turnoId") Long turnoId,
                                      @RequestParam(value = "sucursalId", required = false) Long sucursalId,
                                      RedirectAttributes ra) {
        try {
            CajaTurno turno = cajaTurnoService.obtenerPorId(turnoId);
            cajaTurnoService.ocultarDelHistorial(turnoId);
            String vendedor = turno.getNombreVendedor() != null && !turno.getNombreVendedor().isBlank()
                    ? turno.getNombreVendedor() : turno.getUsuario();
            auditoriaService.registrarEliminacion("turno del historial",
                    "Turno #" + turnoId + " (" + vendedor + ")",
                    "Quitado del historial de caja. Datos y ventas se mantienen.");
            ra.addFlashAttribute("mensaje", "Turno quitado del historial. Los datos siguen en auditoría.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo quitar del historial: " + (e.getMessage() != null ? e.getMessage() : "error desconocido"));
        }
        return sucursalId != null ? "redirect:/web/caja?sucursalId=" + sucursalId : "redirect:/web/caja";
    }
}
