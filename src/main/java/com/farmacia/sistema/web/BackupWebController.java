package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.cliente.ClienteService;
import com.farmacia.sistema.domain.producto.ProductoService;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.cliente.Cliente;
import com.farmacia.sistema.domain.venta.VentaService;
import com.farmacia.sistema.domain.venta.Venta;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/web/backup")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR')")
public class BackupWebController {

    private final ProductoService productoService;
    private final ClienteService clienteService;
    private final VentaService ventaService;

    public BackupWebController(ProductoService productoService, ClienteService clienteService,
                               VentaService ventaService) {
        this.productoService = productoService;
        this.clienteService = clienteService;
        this.ventaService = ventaService;
    }

    @GetMapping
    public String mostrar(Model model) {
        model.addAttribute("totalProductos", productoService.listarTodos().size());
        model.addAttribute("totalClientes", clienteService.listarTodos().size());
        model.addAttribute("totalVentas", ventaService.listarTodas().size());
        return "backup";
    }

    @GetMapping("/exportar/productos")
    public void exportarProductos(HttpServletResponse response) throws Exception {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=productos_" + timestamp() + ".csv");
        PrintWriter w = response.getWriter();
        w.println("ID,Codigo,Nombre,Laboratorio,Categoria,Presentacion,PrecioVenta,CostoUnitario,StockActual,StockMinimo,Activo");
        for (Producto p : productoService.listarTodos()) {
            w.printf("%d,%s,%s,%s,%s,%s,%s,%s,%d,%d,%s%n",
                    p.getId(), csv(p.getCodigo()), csv(p.getNombre()), csv(p.getLaboratorio()),
                    csv(p.getCategoria()), csv(p.getPresentacion()),
                    p.getPrecioVenta(), p.getCostoUnitario(), p.getStockActual(), p.getStockMinimo(),
                    p.isActivo());
        }
    }

    @GetMapping("/exportar/clientes")
    public void exportarClientes(HttpServletResponse response) throws Exception {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=clientes_" + timestamp() + ".csv");
        PrintWriter w = response.getWriter();
        w.println("ID,TipoDoc,NumeroDoc,Nombres,Apellidos,Telefono,Email,Direccion,Puntos,Activo");
        for (Cliente c : clienteService.listarTodos()) {
            w.printf("%d,%s,%s,%s,%s,%s,%s,%s,%d,%s%n",
                    c.getId(), csv(c.getTipoDocumento()), csv(c.getNumeroDocumento()),
                    csv(c.getNombres()), csv(c.getApellidos()), csv(c.getTelefono()),
                    csv(c.getEmail()), csv(c.getDireccion()), c.getPuntos(), c.isActivo());
        }
    }

    @GetMapping("/exportar/ventas")
    public void exportarVentas(HttpServletResponse response) throws Exception {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=ventas_" + timestamp() + ".csv");
        PrintWriter w = response.getWriter();
        w.println("ID,Fecha,Cliente,Subtotal,Descuento,Total,Estado,Comprobante");
        for (Venta v : ventaService.listarTodas()) {
            w.printf("%d,%s,%s,%s,%s,%s,%s,%s%n",
                    v.getId(), v.getFechaHora(),
                    csv(v.getNombreClienteVenta()),
                    v.getSubtotal(), v.getDescuentoTotal(), v.getTotal(),
                    csv(v.getEstado()),
                    csv((v.getSerieComprobante() != null ? v.getSerieComprobante() + "-" : "") +
                            (v.getNumeroComprobante() != null ? v.getNumeroComprobante() : "")));
        }
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
    }

    private String csv(String v) {
        if (v == null) return "";
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
