package com.farmacia.sistema.api;

import com.farmacia.sistema.domain.merma.Merma;
import com.farmacia.sistema.domain.merma.MermaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mermas")
@CrossOrigin(origins = "*")
public class MermaController {

    private final MermaService mermaService;

    public MermaController(MermaService mermaService) {
        this.mermaService = mermaService;
    }

    @GetMapping
    public List<Merma> listar() {
        return mermaService.listarTodas();
    }

    @GetMapping("/{id}")
    public Merma obtener(@PathVariable Long id) {
        return mermaService.obtenerPorId(id);
    }

    @PostMapping("/registrar")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR','QUIMICO_FARMACEUTICO')")
    public ResponseEntity<Merma> registrar(
            @RequestParam Long productoId,
            @RequestParam(required = false) Long almacenId,
            @RequestParam int cantidad,
            @RequestParam(required = false, defaultValue = "OTRO") String tipoMerma,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) String usuarioRegistro,
            @RequestParam(required = false) String observaciones,
            @RequestParam(required = false) String lote,
            @RequestParam(required = false) String responsableAutorizado,
            @RequestParam(required = false) String aprobacionQF,
            @RequestParam(required = false) String actaDestruccion,
            @RequestParam(required = false) String numeroReporte) {
        Merma m = mermaService.registrarMerma(productoId, almacenId, cantidad, tipoMerma, motivo, usuarioRegistro,
                observaciones, lote, responsableAutorizado, aprobacionQF, actaDestruccion, numeroReporte);
        return ResponseEntity.status(HttpStatus.CREATED).body(m);
    }

    @GetMapping("/producto/{productoId}")
    public List<Merma> listarPorProducto(@PathVariable Long productoId) {
        return mermaService.listarPorProducto(productoId);
    }

    @GetMapping("/entre-fechas")
    public List<Merma> listarEntreFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return mermaService.listarEntreFechas(desde, hasta);
    }

    @GetMapping("/tipo/{tipoMerma}")
    public List<Merma> listarPorTipo(@PathVariable String tipoMerma) {
        return mermaService.listarPorTipo(tipoMerma);
    }

    @GetMapping("/reportes/por-producto")
    public List<Map<String, Object>> reportePorProducto() {
        return mermaService.reportePorProducto();
    }

    @GetMapping("/resumen")
    public Map<String, Object> resumen() {
        return mermaService.resumenMermas();
    }
}
