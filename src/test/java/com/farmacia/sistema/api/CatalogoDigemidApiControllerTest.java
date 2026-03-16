package com.farmacia.sistema.api;

import com.farmacia.sistema.domain.digemid.CatalogoDigemid;
import com.farmacia.sistema.domain.digemid.CatalogoDigemidRepository;
import com.farmacia.sistema.domain.digemid.CatalogoDigemidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoDigemidApiControllerTest {

    @Mock private CatalogoDigemidRepository repository;

    private CatalogoDigemidService service;
    private CatalogoDigemidApiController controller;

    @BeforeEach
    void setUp() {
        service = new CatalogoDigemidService(repository);
        controller = new CatalogoDigemidApiController(service);
    }

    @Test
    void buscar_retornaMapsCorrecto() {
        CatalogoDigemid c = new CatalogoDigemid();
        c.setId(1L);
        c.setPrincipioActivo("Diazepam");
        c.setNombreComercial("Valium");
        c.setTipoProductoControlado("PSICOTROPICO");
        c.setListaControl("LISTA_IV");
        c.setRequiereReceta(true);
        c.setTipoReceta("RECETA_RETENIDA");
        c.setControlStockEspecial(true);
        c.setObservacion("Psicotrópico Lista IV");

        when(repository.buscarPorTermino("Diazepam")).thenReturn(List.of(c));

        List<Map<String, Object>> result = controller.buscar("Diazepam");

        assertFalse(result.isEmpty());
        Map<String, Object> item = result.get(0);
        assertEquals("Diazepam", item.get("principioActivo"));
        assertEquals("Valium", item.get("nombreComercial"));
        assertEquals("PSICOTROPICO", item.get("tipoProductoControlado"));
        assertEquals("LISTA_IV", item.get("listaControl"));
        assertEquals(true, item.get("requiereReceta"));
        assertEquals("RECETA_RETENIDA", item.get("tipoReceta"));
    }

    @Test
    void buscar_sinResultados_retornaListaVacia() {
        when(repository.buscarPorTermino("xyz")).thenReturn(List.of());

        List<Map<String, Object>> result = controller.buscar("xyz");

        assertTrue(result.isEmpty());
    }
}
