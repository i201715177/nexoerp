package com.farmacia.sistema.config;

import com.farmacia.sistema.domain.empresa.EmpresaService;
import com.farmacia.sistema.domain.inventario.Almacen;
import com.farmacia.sistema.domain.inventario.AlmacenRepository;
import com.farmacia.sistema.domain.inventario.StockAlmacen;
import com.farmacia.sistema.domain.inventario.StockAlmacenRepository;
import com.farmacia.sistema.domain.producto.Producto;
import com.farmacia.sistema.domain.producto.ProductoRepository;
import com.farmacia.sistema.domain.sucursal.Sucursal;
import com.farmacia.sistema.domain.sucursal.SucursalRepository;
import com.farmacia.sistema.domain.usuario.UsuarioService;
import com.farmacia.sistema.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class InicializarAlmacenRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializarAlmacenRunner.class);

    private final AlmacenRepository almacenRepository;
    private final StockAlmacenRepository stockAlmacenRepository;
    private final ProductoRepository productoRepository;
    private final SucursalRepository sucursalRepository;
    private final EmpresaService empresaService;
    private final UsuarioService usuarioService;
    private final JdbcTemplate jdbcTemplate;

    public InicializarAlmacenRunner(AlmacenRepository almacenRepository,
                                    StockAlmacenRepository stockAlmacenRepository,
                                    @Lazy ProductoRepository productoRepository,
                                    SucursalRepository sucursalRepository,
                                    EmpresaService empresaService,
                                    UsuarioService usuarioService,
                                    JdbcTemplate jdbcTemplate) {
        this.almacenRepository = almacenRepository;
        this.stockAlmacenRepository = stockAlmacenRepository;
        this.productoRepository = productoRepository;
        this.sucursalRepository = sucursalRepository;
        this.empresaService = empresaService;
        this.usuarioService = usuarioService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        migrarConstraintsMultiTenant();
        Long tenantId = empresaService.empresaPorDefecto().getId();
        TenantContext.setTenantId(tenantId);
        try {
            usuarioService.crearSiNoExiste("admin", "admin123",
                    "Administrador del Sistema", "SAAS_ADMIN", tenantId);
            usuarioService.actualizarRolSiDifiere("admin", "SAAS_ADMIN");
            log.info("Usuario SAAS_ADMIN verificado para tenant {}", tenantId);
            runInicializacion();
        } finally {
            TenantContext.clear();
        }
    }

    private void migrarConstraintsMultiTenant() {
        String[][] oldConstraints = {
                {"productos", "UK_H04WPYQWDDOBLTUQQ56CP6S05"},
                {"clientes", "UK_2VICCGF178BD74VFBQ8CTSV8T"},
                {"proveedores", "UK_98GWXR7AS3YYD2KCK23HQN68G"},
                {"sucursales", "UK_KLSQV8UCMF2WP1707DHDWKT6L"},
                {"almacenes", "UK_PTLTGMKR6B3FT2449F629I4G2"},
        };
        for (String[] entry : oldConstraints) {
            String table = entry[0];
            String constraint = entry[1];
            try {
                jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraint);
                log.info("Constraint viejo eliminado: {}.{}", table, constraint);
            } catch (Exception e) {
                log.debug("Constraint {}.{} ya no existe o no se pudo eliminar: {}", table, constraint, e.getMessage());
            }
        }
    }

    private void runInicializacion() {
        Long tenantId = TenantContext.getTenantId();
        Sucursal sucursalCentral = sucursalRepository.findByTenantIdAndCodigo(tenantId, "S01")
                .orElseGet(() -> {
                    Sucursal s = new Sucursal();
                    s.setCodigo("S01");
                    s.setNombre("Sucursal Central");
                    s.setActiva(true);
                    return sucursalRepository.save(s);
                });
        if (almacenRepository.findFirstByTenantIdAndPrincipalTrue(tenantId).isEmpty()) {
            Almacen principal = new Almacen();
            principal.setCodigo("P01");
            principal.setNombre("Principal");
            principal.setPrincipal(true);
            principal.setSucursal(sucursalCentral);
            almacenRepository.save(principal);
        }
        Almacen principal = almacenRepository.findFirstByTenantIdAndPrincipalTrue(tenantId).orElse(null);
        if (principal != null) {
            List<Producto> productos = tenantId != null
                    ? productoRepository.findByTenantId(tenantId)
                    : productoRepository.findAll();
            for (Producto p : productos) {
                if (stockAlmacenRepository.findByAlmacenIdAndProductoId(principal.getId(), p.getId()).isEmpty()) {
                    StockAlmacen sa = new StockAlmacen();
                    sa.setAlmacen(principal);
                    sa.setProducto(p);
                    sa.setCantidad(p.getStockActual() != null ? p.getStockActual() : 0);
                    stockAlmacenRepository.save(sa);
                }
            }
        }
    }
}
