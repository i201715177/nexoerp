package com.farmacia.sistema.domain.empresa;

import com.farmacia.sistema.domain.cliente.Cliente;
import com.farmacia.sistema.domain.cliente.ClienteRepository;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoRepository;
import com.farmacia.sistema.domain.sucursal.Sucursal;
import com.farmacia.sistema.domain.sucursal.SucursalRepository;
import com.farmacia.sistema.domain.usuario.UsuarioRepository;
import com.farmacia.sistema.domain.venta.Venta;
import com.farmacia.sistema.domain.venta.VentaRepository;
import com.farmacia.sistema.dto.EmpresaUsoDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SaasAdminService {

    private final EmpresaRepository empresaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final SucursalRepository sucursalRepository;
    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;

    public SaasAdminService(EmpresaRepository empresaRepository,
                            ClienteRepository clienteRepository,
                            ProductoRepository productoRepository,
                            SucursalRepository sucursalRepository,
                            VentaRepository ventaRepository,
                            UsuarioRepository usuarioRepository) {
        this.empresaRepository = empresaRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.sucursalRepository = sucursalRepository;
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<EmpresaUsoDto> resumenUsoEmpresas() {
        List<Empresa> empresas = empresaRepository.findAll();
        List<Cliente> clientes = clienteRepository.findAll();
        List<Producto> productos = productoRepository.findAll();
        List<Sucursal> sucursales = sucursalRepository.findAll();
        List<Venta> ventas = ventaRepository.findAll();

        List<EmpresaUsoDto> resultado = new ArrayList<>();
        for (Empresa e : empresas) {
            EmpresaUsoDto dto = new EmpresaUsoDto();
            dto.setId(e.getId());
            dto.setCodigo(e.getCodigo());
            dto.setNombre(e.getNombre());
            dto.setActiva(e.isActiva());

            long cliCount = clientes.stream()
                    .filter(c -> c.getTenantId() != null && c.getTenantId().equals(e.getId()))
                    .count();
            long prodCount = productos.stream()
                    .filter(p -> p.getTenantId() != null && p.getTenantId().equals(e.getId()))
                    .count();
            long sucCount = sucursales.stream()
                    .filter(s -> s.getTenantId() != null && s.getTenantId().equals(e.getId()))
                    .count();
            long venCount = ventas.stream()
                    .filter(v -> v.getTenantId() != null && v.getTenantId().equals(e.getId()))
                    .count();
            BigDecimal montoVentas = ventas.stream()
                    .filter(v -> v.getTenantId() != null && v.getTenantId().equals(e.getId()))
                    .map(Venta::getTotal)
                    .filter(t -> t != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            dto.setClientes(cliCount);
            dto.setProductos(prodCount);
            dto.setSucursales(sucCount);
            dto.setVentas(venCount);
            dto.setMontoVentas(montoVentas);

            long userCount = usuarioRepository.countByTenantId(e.getId());
            dto.setUsuarios(userCount);
            dto.setMaxUsuarios(e.getMaxUsuariosEfectivo());
            if (e.getPlan() != null) {
                dto.setPlanNombre(e.getPlan().getNombre());
                dto.setPlanCodigo(e.getPlan().getCodigo());
            }
            dto.setEmailContacto(e.getEmailContacto());

            resultado.add(dto);
        }
        return resultado;
    }
}

