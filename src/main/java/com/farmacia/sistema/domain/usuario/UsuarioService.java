package com.farmacia.sistema.domain.usuario;

import com.farmacia.sistema.domain.empresa.EmpresaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EmpresaService empresaService;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder,
                          EmpresaService empresaService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.empresaService = empresaService;
    }

    public List<Usuario> listarPorTenant(Long tenantId) {
        return repository.findByTenantIdOrderByUsernameAsc(tenantId);
    }

    public Usuario obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
    }

    public Usuario crear(String username, String passwordPlano, String nombreCompleto,
                         String rol, Long tenantId) {
        return crear(username, passwordPlano, nombreCompleto, rol, tenantId, null);
    }

    public Usuario crear(String username, String passwordPlano, String nombreCompleto,
                         String rol, Long tenantId, Long sucursalId) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        if (passwordPlano == null || passwordPlano.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }
        if (rol == null || rol.isBlank()) {
            throw new IllegalArgumentException("El rol es obligatorio.");
        }

        if (repository.existsByUsername(username)) {
            throw new IllegalArgumentException("Ya existe un usuario con el nombre '" + username + "'.");
        }
        if (tenantId != null) {
            Integer maxUsuarios = empresaService.obtenerPorId(tenantId).getMaxUsuariosEfectivo();
            if (maxUsuarios != null) {
                long actual = repository.countByTenantId(tenantId);
                if (actual >= maxUsuarios) {
                    throw new IllegalArgumentException("Se ha alcanzado el límite de usuarios del plan (" + maxUsuarios + "). Actualice de plan para añadir más usuarios.");
                }
            }
        }
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(passwordPlano));
        u.setNombreCompleto(nombreCompleto);
        u.setRol(rol);
        u.setTenantId(tenantId);
        u.setSucursalId(sucursalId);
        u.setActivo(true);
        return repository.save(u);
    }

    public Usuario crearSiNoExiste(String username, String passwordPlano, String nombreCompleto,
                                   String rol, Long tenantId) {
        return repository.findByUsername(username)
                .orElseGet(() -> crear(username, passwordPlano, nombreCompleto, rol, tenantId));
    }

    public void cambiarEstado(Long id, boolean activo) {
        Usuario u = obtenerPorId(id);
        u.setActivo(activo);
        repository.save(u);
    }

    public void resetPassword(Long id, String nuevoPasswordPlano) {
        Usuario u = obtenerPorId(id);
        u.setPassword(passwordEncoder.encode(nuevoPasswordPlano));
        repository.save(u);
    }

    public void actualizarRolSiDifiere(String username, String nuevoRol) {
        repository.findByUsername(username).ifPresent(u -> {
            if (!nuevoRol.equals(u.getRol())) {
                u.setRol(nuevoRol);
                repository.save(u);
            }
        });
    }

    public void actualizarSucursal(Long usuarioId, Long sucursalId) {
        Usuario u = obtenerPorId(usuarioId);
        u.setSucursalId(sucursalId);
        repository.save(u);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
