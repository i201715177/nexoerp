package com.farmacia.sistema.security;

import com.farmacia.sistema.domain.usuario.Usuario;
import com.farmacia.sistema.domain.usuario.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Carga usuarios desde la BD (tabla usuarios) en vez de memoria.
 * Devuelve un TenantUserDetails que incluye el tenantId del usuario.
 */
@Service
public class TenantUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public TenantUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario u = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        if (!u.isActivo()) {
            throw new UsernameNotFoundException("Usuario desactivado: " + username);
        }

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + u.getRol())
        );

        return new TenantUserDetails(
                u.getUsername(),
                u.getPassword(),
                u.getTenantId(),
                u.getSucursalId(),
                u.getNombreCompleto(),
                u.getRol(),
                authorities
        );
    }
}
