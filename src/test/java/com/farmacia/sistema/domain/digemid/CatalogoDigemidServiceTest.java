package com.farmacia.sistema.domain.digemid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoDigemidServiceTest {

    @Mock private CatalogoDigemidRepository repository;

    private CatalogoDigemidService service;

    @BeforeEach
    void setUp() {
        service = new CatalogoDigemidService(repository);
    }

    @Test
    void buscar_terminoCorto_retornaVacio() {
        List<CatalogoDigemid> result = service.buscar("ab");
        assertTrue(result.isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void buscar_terminoNull_retornaVacio() {
        List<CatalogoDigemid> result = service.buscar(null);
        assertTrue(result.isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void buscar_terminoValido_buscaEnRepositorio() {
        CatalogoDigemid tramadol = new CatalogoDigemid();
        tramadol.setId(1L);
        tramadol.setPrincipioActivo("Tramadol");
        tramadol.setTipoProductoControlado("ESTUPEFACIENTE");
        tramadol.setListaControl("LISTA_II");
        tramadol.setRequiereReceta(true);
        tramadol.setTipoReceta("RECETA_ESPECIAL");
        tramadol.setControlStockEspecial(true);

        when(repository.buscarPorTermino("Tramadol")).thenReturn(List.of(tramadol));

        List<CatalogoDigemid> result = service.buscar("Tramadol");

        assertFalse(result.isEmpty());
        assertEquals("Tramadol", result.get(0).getPrincipioActivo());
        assertEquals("ESTUPEFACIENTE", result.get(0).getTipoProductoControlado());
        assertTrue(result.get(0).isRequiereReceta());
        verify(repository).buscarPorTermino("Tramadol");
    }

    @Test
    void buscar_sinResultados_retornaVacio() {
        when(repository.buscarPorTermino("NoExiste")).thenReturn(List.of());

        List<CatalogoDigemid> result = service.buscar("NoExiste");

        assertTrue(result.isEmpty());
    }
}
