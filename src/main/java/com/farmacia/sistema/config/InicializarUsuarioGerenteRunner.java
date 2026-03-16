package com.farmacia.sistema.config;

import com.farmacia.sistema.domain.empresa.EmpresaRepository;
import com.farmacia.sistema.domain.usuario.Usuario;
import com.farmacia.sistema.domain.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Deja un único usuario gerente (jimmy / 46881219). Si jimmy no existe, borra todos los usuarios
 * y crea a jimmy con rol GERENTE. Los clientes que se agreguen después tendrán rol ADMIN.
 */
@Component
@Order(200)
public class InicializarUsuarioGerenteRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializarUsuarioGerenteRunner.class);
    private static final String USERNAME_GERENTE = "jimmy";
    private static final String PASSWORD_GERENTE = "46881219";
    private static final String ROL_GERENTE = "GERENTE";

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;

    public InicializarUsuarioGerenteRunner(UsuarioRepository usuarioRepository,
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
            log.warn("Usuario gerente '{}' no creado: no hay empresas activas.", USERNAME_GERENTE);
            return;
        }
        var toDelete = usuarioRepository.findAll().stream()
                .filter(u -> !USERNAME_GERENTE.equals(u.getUsername()))
                .toList();
        toDelete.forEach(usuarioRepository::delete);
        if (!toDelete.isEmpty()) {
            log.info("Usuarios eliminados (solo queda gerente): {}", toDelete.size());
        }
        Long tenantId = primeraEmpresa.get().getId();
        Usuario u = new Usuario();
        u.setUsername(USERNAME_GERENTE);
        u.setPassword(passwordEncoder.encode(PASSWORD_GERENTE));
        u.setNombreCompleto("Gerente");
        u.setRol(ROL_GERENTE);
        u.setTenantId(tenantId);
        u.setActivo(true);
        usuarioRepository.save(u);
        log.info("Usuario gerente '{}' creado (rol {}). Contraseña: la configurada.", USERNAME_GERENTE, ROL_GERENTE);
    }
}
