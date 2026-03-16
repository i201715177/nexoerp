package com.farmacia.sistema.api;

import com.farmacia.sistema.domain.digemid.*;
import com.farmacia.sistema.domain.producto.Producto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/digemid")
@CrossOrigin(origins = "*")
public class DigemidController {

    private final DigemidService digemidService;

    public DigemidController(DigemidService digemidService) {
        this.digemidService = digemidService;
    }

    @GetMapping("/productos-controlados")
    public List<Producto> listarProductosControlados() {
        return digemidService.listarProductosControlados();
    }

    @GetMapping("/productos-controlados/tipo/{tipo}")
    public List<Producto> listarPorTipo(@PathVariable String tipo) {
        return digemidService.listarPorTipoProductoControlado(tipo);
    }

    @GetMapping("/productos-controlados/stock-bajo")
    public List<Producto> listarStockBajo() {
        return digemidService.listarProductosControlados().stream()
                .filter(p -> p.getStockActual() != null && p.getStockMinimo() != null && p.getStockActual() <= p.getStockMinimo())
                .toList();
    }

    @GetMapping("/productos-controlados/por-vencer")
    public List<StockControlado> listarPorVencer() {
        return digemidService.alertasVencimientoProximo(30);
    }

    @GetMapping("/productos-controlados/vencidos")
    public List<StockControlado> listarVencidos() {
        return digemidService.alertasVencidos();
    }

    @GetMapping("/stock-controlado")
    public List<StockControlado> listarStockControlado() {
        return digemidService.listarStockControlado();
    }

    @GetMapping("/stock-controlado/producto/{productoId}")
    public List<StockControlado> stockPorProducto(@PathVariable Long productoId) {
        return digemidService.stockControladoPorProducto(productoId);
    }

    @GetMapping("/recetas")
    public List<RegistroReceta> listarRecetas() {
        return digemidService.listarRegistrosReceta();
    }

    @PostMapping("/recetas")
    public ResponseEntity<RegistroReceta> guardarReceta(@RequestBody RegistroReceta reg) {
        return ResponseEntity.ok(digemidService.guardarRegistroReceta(reg));
    }

    @GetMapping("/recetas/validar-duplicado")
    public List<String> alertasRecetaDuplicada(@RequestParam String numeroReceta,
                                                @RequestParam(required = false) Long excluirId) {
        return digemidService.alertasRecetaDuplicada(numeroReceta, excluirId);
    }

    @GetMapping("/distribuciones")
    public List<DistribucionControlada> listarDistribuciones() {
        return digemidService.listarDistribuciones();
    }

    @GetMapping("/kardex/producto/{productoId}")
    public List<MovimientoControlado> kardexControlado(@PathVariable Long productoId) {
        return digemidService.kardexControladoPorProducto(productoId);
    }

    @GetMapping("/alertas/resumen")
    public Map<String, Object> resumenAlertas() {
        return digemidService.resumenAlertas();
    }

    @GetMapping("/alertas/stock-bajo")
    public List<StockControlado> alertasStockBajo(@RequestParam(defaultValue = "5") int umbral) {
        return digemidService.alertasStockControladoBajo(umbral);
    }

    @GetMapping("/alertas/vencimiento")
    public List<StockControlado> alertasVencimiento(@RequestParam(defaultValue = "30") int dias) {
        return digemidService.alertasVencimientoProximo(dias);
    }

    @GetMapping("/reportes/movimientos")
    public List<MovimientoControlado> reporteMovimientos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return digemidService.reporteRegulatorioMovimientos(desde, hasta);
    }

    @GetMapping("/reportes/recetas")
    public List<RegistroReceta> reporteRecetas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return digemidService.reporteRegulatorioRecetas(desde, hasta);
    }

    @PostMapping("/entrada")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR','QUIMICO_FARMACEUTICO')")
    public ResponseEntity<StockControlado> registrarEntrada(@RequestParam Long productoId,
                                                            @RequestParam(required = false) Long almacenId,
                                                            @RequestParam(required = false) String lote,
                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaVencimiento,
                                                            @RequestParam int cantidad,
                                                            @RequestParam(required = false) String referencia) {
        return ResponseEntity.ok(digemidService.registrarEntradaControlada(productoId, almacenId, lote, fechaVencimiento, cantidad, referencia, null));
    }

    @PostMapping("/destruccion")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR','QUIMICO_FARMACEUTICO')")
    public ResponseEntity<Void> registrarDestruccion(@RequestParam Long productoId,
                                                      @RequestParam(required = false) Long almacenId,
                                                      @RequestParam int cantidad,
                                                      @RequestParam(required = false) String motivo,
                                                      @RequestParam(required = false) String documento) {
        digemidService.registrarDestruccion(productoId, almacenId, cantidad, motivo, documento, "API");
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/stock/{stockId}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR')")
    public ResponseEntity<Void> eliminarRegistroStock(@PathVariable Long stockId) {
        digemidService.listarStockControlado();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/alertas/exceso-venta")
    public List<Map<String, Object>> alertasExcesoVenta(@RequestParam(defaultValue = "7") int dias,
                                                         @RequestParam(defaultValue = "50") int umbral) {
        return digemidService.alertasExcesoVenta(dias, umbral);
    }

    @GetMapping("/alertas/resumen-completo")
    public Map<String, Object> resumenAlertasCompleto() {
        return digemidService.resumenAlertasCompleto();
    }

    @GetMapping("/reportes/psicotropicos")
    public List<MovimientoControlado> reportePsicotropicos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return digemidService.reportePorTipoProducto("PSICOTROPICO", desde, hasta);
    }

    @GetMapping("/reportes/estupefacientes")
    public List<MovimientoControlado> reporteEstupefacientes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return digemidService.reportePorTipoProducto("ESTUPEFACIENTE", desde, hasta);
    }

    @GetMapping("/reportes/lotes")
    public List<StockControlado> reporteControlLotes() {
        return digemidService.reporteControlLotes();
    }

    @GetMapping("/reportes/distribucion")
    public List<DistribucionControlada> reporteDistribucion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return digemidService.reporteHistorialDistribucion(desde, hasta);
    }
}
