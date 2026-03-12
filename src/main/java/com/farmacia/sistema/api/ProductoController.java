package com.farmacia.sistema.api;

import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Producto> listar() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public Producto obtener(@PathVariable Long id) {
        return service.obtenerPorId(id);
    }

    @GetMapping("/codigo/{codigo}")
    public Producto obtenerPorCodigo(@PathVariable String codigo) {
        return service.obtenerPorCodigo(codigo);
    }

    @PostMapping
    public ResponseEntity<Producto> crear(@RequestBody @Valid Producto producto) {
        Producto creado = service.crear(producto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public Producto actualizar(@PathVariable Long id, @RequestBody @Valid Producto producto) {
        return service.actualizar(id, producto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}

