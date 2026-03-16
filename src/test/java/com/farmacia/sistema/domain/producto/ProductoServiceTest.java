package com.farmacia.sistema.domain.producto;

import com.farmacia.sistema.domain.inventario.InventarioMovimientoRepository;
import com.farmacia.sistema.domain.inventario.LoteProductoRepository;
import com.farmacia.sistema.domain.inventario.StockAlmacenRepository;
import com.farmacia.sistema.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock private ProductoRepository repository;
    @Mock private StockAlmacenRepository stockAlmacenRepository;
    @Mock private LoteProductoRepository loteProductoRepository;
    @Mock private InventarioMovimientoRepository movimientoRepository;

    private ProductoService service;

    private Producto productoBase;

    @BeforeEach
    void setUp() {
        service = new ProductoService(repository, stockAlmacenRepository, loteProductoRepository, movimientoRepository);
        TenantContext.setTenantId(1L);
        productoBase = new Producto();
        productoBase.setId(1L);
        productoBase.setNombre("Paracetamol 500mg");
        productoBase.setLaboratorio("GeneriLab");
        productoBase.setPresentacion("Tableta");
        productoBase.setPrecioVenta(new BigDecimal("2.50"));
        productoBase.setStockActual(100);
        productoBase.setStockMinimo(10);
        productoBase.setCodigo("PRD-00001");
        productoBase.setActivo(true);
    }

    @Test
    void listarTodos_conTenant_retornaProductos() {
        when(repository.findByTenantId(1L)).thenReturn(List.of(productoBase));

        List<Producto> result = service.listarTodos();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Paracetamol 500mg", result.get(0).getNombre());
    }

    @Test
    void obtenerPorId_existente_retornaProducto() {
        when(repository.findById(1L)).thenReturn(Optional.of(productoBase));

        Producto result = service.obtenerPorId(1L);

        assertNotNull(result);
        assertEquals("Paracetamol 500mg", result.getNombre());
    }

    @Test
    void obtenerPorId_noExistente_lanzaExcepcion() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.obtenerPorId(999L));
    }

    @Test
    void crear_productoValido_generaCodigo() {
        Producto nuevo = new Producto();
        nuevo.setNombre("Ibuprofeno 400mg");
        nuevo.setLaboratorio("FarmaLab");
        nuevo.setPresentacion("Tableta");
        nuevo.setPrecioVenta(new BigDecimal("3.00"));
        nuevo.setStockActual(50);
        nuevo.setStockMinimo(5);

        when(repository.findByTenantId(1L)).thenReturn(List.of());
        when(repository.save(any(Producto.class))).thenAnswer(i -> {
            Producto p = i.getArgument(0);
            p.setId(2L);
            return p;
        });

        Producto result = service.crear(nuevo);

        assertNotNull(result.getCodigo());
        assertTrue(result.getCodigo().startsWith("PRD-"));
        verify(repository).save(any(Producto.class));
    }

    @Test
    void crear_codigoDuplicado_lanzaExcepcion() {
        Producto nuevo = new Producto();
        nuevo.setCodigo("PRD-00001");
        nuevo.setNombre("Duplicado");
        nuevo.setLaboratorio("Lab");
        nuevo.setPresentacion("Tab");
        nuevo.setPrecioVenta(BigDecimal.ONE);
        nuevo.setStockActual(10);
        nuevo.setStockMinimo(1);

        when(repository.existsByTenantIdAndCodigo(1L, "PRD-00001")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.crear(nuevo));
    }

    @Test
    void actualizarStock_valido_actualizaCorrectamente() {
        when(repository.findById(1L)).thenReturn(Optional.of(productoBase));
        when(repository.save(any(Producto.class))).thenReturn(productoBase);

        service.actualizarStock(1L, 80);

        assertEquals(80, productoBase.getStockActual());
        verify(repository).save(productoBase);
    }

    @Test
    void actualizarStock_negativo_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> service.actualizarStock(1L, -5));
    }

    @Test
    void crear_stockActualMenorQueMinimo_lanzaExcepcion() {
        Producto nuevo = new Producto();
        nuevo.setNombre("Test");
        nuevo.setLaboratorio("Lab");
        nuevo.setPresentacion("Tab");
        nuevo.setPrecioVenta(BigDecimal.ONE);
        nuevo.setStockActual(3);
        nuevo.setStockMinimo(10);

        assertThrows(IllegalArgumentException.class, () -> service.crear(nuevo));
    }

    @Test
    void crear_stockMaximoMenorQueMinimo_lanzaExcepcion() {
        Producto nuevo = new Producto();
        nuevo.setNombre("Test");
        nuevo.setLaboratorio("Lab");
        nuevo.setPresentacion("Tab");
        nuevo.setPrecioVenta(BigDecimal.ONE);
        nuevo.setStockActual(10);
        nuevo.setStockMinimo(5);
        nuevo.setStockMaximo(3);

        assertThrows(IllegalArgumentException.class, () -> service.crear(nuevo));
    }
}
