package com.farmacia.sistema.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Extiende UserDetails de Spring Security para incluir tenantId y datos extra del usuario.
 */
public class TenantUserDetails extends User {

    private final Long tenantId;
    private final Long sucursalId;
    private final String nombreCompleto;
    private final String rol;

    public TenantUserDetails(String username, String password, Long tenantId,
                             Long sucursalId, String nombreCompleto, String rol,
                             Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.tenantId = tenantId;
        this.sucursalId = sucursalId;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
    }

    public Long getTenantId() { return tenantId; }
    /** Sucursal asignada al usuario (vendedor ve solo stock de esta sucursal). */
    public Long getSucursalId() { return sucursalId; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getRol() { return rol; }
}
