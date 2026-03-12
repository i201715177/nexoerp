package com.farmacia.sistema.api;

import com.farmacia.sistema.api.venta.CrearVentaRequest;
import com.farmacia.sistema.domain.venta.Venta;
import com.farmacia.sistema.domain.venta.VentaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@CrossOrigin(origins = "*")
public class VentaController {

    private final VentaService service;

    public VentaController(VentaService service) {
        this.service = service;
    }

    @GetMapping
    public List<Venta> listar() {
        return service.listarTodas();
    }

    @GetMapping("/{id}")
    public Venta obtener(@PathVariable Long id) {
        return service.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody @Valid CrearVentaRequest request) {
        try {
            Venta creada = service.crearVenta(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(creada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("error", e.getMessage()));
        }
    }
}

