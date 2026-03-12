package com.farmacia.sistema.config;

import com.farmacia.sistema.domain.empresa.EmpresaRepository;
import com.farmacia.sistema.domain.usuario.Usuario;
import com.farmacia.sistema.domain.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el usuario "darwin" con rol GERENTE si no existe.
 * El gerente solo ve Admin SaaS, Facturación y Solicitudes (no productos, ventas, auditoría, etc.).
 * Contraseña por defecto: app.gerente.default-password (por defecto "Gerente2025!").
 */
@Component
@Order(100)
public class InicializarGerenteRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializarGerenteRunner.class);
    private static final String USERNAME_GERENTE = "darwin";
    private static final String ROL_GERENTE = "GERENTE";

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.gerente.default-password:Gerente2025!}")
    private String defaultPassword;

    public InicializarGerenteRunner(UsuarioRepository usuarioRepository,
                                    EmpresaRepository empresaRepository,
                                    PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByUsername(USERNAME_GERENTE)) {
            return;
        }
        var primeraEmpresa = empresaRepository.findFirstByActivaTrueOrderByIdAsc();
        if (primeraEmpresa.isEmpty()) {
            log.info("Usuario gerente '{}' no creado: no hay empresas. Cree una empresa y reinicie o inserte el usuario manualmente.", USERNAME_GERENTE);
            return;
        }
        Long tenantId = primeraEmpresa.get().getId();
        Usuario u = new Usuario();
        u.setUsername(USERNAME_GERENTE);
        u.setPassword(passwordEncoder.encode(defaultPassword));
        u.setNombreCompleto("Gerente");
        u.setRol(ROL_GERENTE);
        u.setTenantId(tenantId);
        u.setActivo(true);
        usuarioRepository.save(u);
        log.info("Usuario gerente '{}' creado (rol {}). Contraseña por defecto: configurable con app.gerente.default-password. Cambie la contraseña tras el primer acceso.", USERNAME_GERENTE, ROL_GERENTE);
    }
}
