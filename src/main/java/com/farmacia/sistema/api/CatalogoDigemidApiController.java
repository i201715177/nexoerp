package com.farmacia.sistema.api;

import com.farmacia.sistema.domain.digemid.CatalogoDigemid;
import com.farmacia.sistema.domain.digemid.CatalogoDigemidService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/catalogo-digemid")
@CrossOrigin(origins = "*")
public class CatalogoDigemidApiController {

    private final CatalogoDigemidService service;

    public CatalogoDigemidApiController(CatalogoDigemidService service) {
        this.service = service;
    }

    /**
     * Busca en el catálogo DIGEMID por nombre de producto o principio activo.
     * Usado por el formulario de productos para auto-detección.
     * Ejemplo: GET /api/catalogo-digemid/buscar?q=tramadol
     */
    @GetMapping("/buscar")
    public List<Map<String, Object>> buscar(@RequestParam String q) {
        return service.buscar(q).stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("principioActivo", c.getPrincipioActivo());
            m.put("nombreComercial", c.getNombreComercial());
            m.put("tipoProductoControlado", c.getTipoProductoControlado());
            m.put("listaControl", c.getListaControl());
            m.put("requiereReceta", c.isRequiereReceta());
            m.put("tipoReceta", c.getTipoReceta());
            m.put("controlStockEspecial", c.isControlStockEspecial());
            m.put("observacion", c.getObservacion());
            return m;
        }).collect(Collectors.toList());
    }
}
