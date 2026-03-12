package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.empresa.Empresa;
import com.farmacia.sistema.domain.empresa.EmpresaService;
import com.farmacia.sistema.domain.empresa.PlanSuscripcion;
import com.farmacia.sistema.domain.empresa.PlanSuscripcionService;
import com.farmacia.sistema.domain.empresa.SaasAdminService;
import com.farmacia.sistema.domain.empresa.TipoSuscripcion;
import com.farmacia.sistema.domain.empresa.FacturaSaaSService;
import com.farmacia.sistema.domain.sucursal.Sucursal;
import com.farmacia.sistema.domain.sucursal.SucursalRepository;
import com.farmacia.sistema.domain.usuario.Usuario;
import com.farmacia.sistema.domain.usuario.UsuarioService;
import com.farmacia.sistema.dto.EmpresaUsoDto;
import com.farmacia.sistema.tenant.TenantContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/web/admin/empresas")
public class SaasAdminController {

    private final EmpresaService empresaService;
    private final SaasAdminService saasAdminService;
    private final UsuarioService usuarioService;
    private final PlanSuscripcionService planSuscripcionService;
    private final FacturaSaaSService facturaSaaSService;
    private final SucursalRepository sucursalRepository;
    private final com.farmacia.sistema.domain.auditoria.AuditoriaService auditoriaService;

    public SaasAdminController(EmpresaService empresaService,
                               SaasAdminService saasAdminService,
                               UsuarioService usuarioService,
                               PlanSuscripcionService planSuscripcionService,
                               SucursalRepository sucursalRepository,
                               com.farmacia.sistema.domain.auditoria.AuditoriaService auditoriaService,
                               FacturaSaaSService facturaSaaSService) {
        this.empresaService = empresaService;
        this.saasAdminService = saasAdminService;
        this.usuarioService = usuarioService;
        this.planSuscripcionService = planSuscripcionService;
        this.sucursalRepository = sucursalRepository;
        this.auditoriaService = auditoriaService;
        this.facturaSaaSService = facturaSaaSService;
    }

    @GetMapping
    public String dashboard(Model model) {
        List<EmpresaUsoDto> resumen = saasAdminService.resumenUsoEmpresas();
        model.addAttribute("empresasUso", resumen);
        model.addAttribute("empresas", empresaService.listarTodas());
        model.addAttribute("nuevaEmpresa", new Empresa());
        model.addAttribute("planes", planSuscripcionService.listarActivos());
        return "saas-empresas";
    }

    @PostMapping
    public String crearEmpresa(@RequestParam("codigo") String codigo,
                               @RequestParam("nombre") String nombre,
                               @RequestParam(value = "descripcion", required = false) String descripcion,
                               @RequestParam(value = "emailContacto", required = false) String emailContacto,
                               @RequestParam(value = "tipoDocumento", required = false) String tipoDocumento,
                               @RequestParam(value = "ruc", required = false) String ruc,
                               @RequestParam(value = "planId", required = false) Long planId,
                               @RequestParam(value = "tipoSuscripcion", required = false) String tipoSuscripcionStr,
                               RedirectAttributes ra) {
        try {
            Empresa e = new Empresa();
            e.setCodigo(codigo);
            e.setNombre(nombre);
            e.setDescripcion(descripcion);
            if (emailContacto != null && !emailContacto.isBlank()) {
                e.setEmailContacto(emailContacto.trim());
            }
            if (tipoDocumento != null && !tipoDocumento.isBlank()) {
                e.setTipoDocumento(tipoDocumento.trim());
            }
            if (ruc != null && !ruc.isBlank()) {
                e.setRuc(ruc.trim());
            }
            e.setActiva(true);
            if (planId != null) {
                e.setPlan(planSuscripcionService.obtenerPorId(planId));
                if (tipoSuscripcionStr != null && !tipoSuscripcionStr.isBlank()) {
                    try {
                        e.setTipoSuscripcion(TipoSuscripcion.valueOf(tipoSuscripcionStr.trim()));
                    } catch (IllegalArgumentException ignored) { }
                }
            }
            empresaService.crear(e);
            auditoriaService.registrarCreacion("empresa", nombre,
                    "Código: " + codigo + (descripcion != null ? " | Descripción: " + descripcion : ""));
            ra.addFlashAttribute("mensaje", "Empresa registrada correctamente.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/web/admin/empresas";
    }

    @PostMapping("/correo")
    public String actualizarCorreoDueño(@RequestParam("empresaId") Long empresaId,
                                       @RequestParam(value = "emailContacto", required = false) String emailContacto,
                                       RedirectAttributes ra) {
        Empresa e = empresaService.obtenerPorId(empresaId);
        e.setEmailContacto(emailContacto != null && !emailContacto.isBlank() ? emailContacto.trim() : null);
        empresaService.actualizar(empresaId, e);
        ra.addFlashAttribute("mensaje", "Correo del dueño actualizado. Los recordatorios de vencimiento se enviarán a ese email.");
        return "redirect:/web/admin/empresas";
    }

    @PostMapping("/estado")
    public String cambiarEstado(@RequestParam("empresaId") Long empresaId,
                                @RequestParam("activa") boolean activa,
                                RedirectAttributes ra) {
        Empresa existente = empresaService.obtenerPorId(empresaId);
        boolean estadoAnterior = existente.isActiva();
        existente.setActiva(activa);
        empresaService.actualizar(empresaId, existente);
        auditoriaService.registrarAccion("PUT",
                activa ? "Activar empresa" : "Suspender empresa",
                existente.getNombre() + " (" + existente.getCodigo() + ")"
                        + " | Estado: " + (estadoAnterior ? "Activa" : "Inactiva")
                        + " → " + (activa ? "Activa" : "Suspendida"));
        ra.addFlashAttribute("mensaje", activa ? "Empresa activada." : "Empresa suspendida.");
        return "redirect:/web/admin/empresas";
    }

    @PostMapping("/eliminar")
    public String eliminarEmpresa(@RequestParam("empresaId") Long empresaId,
                                  RedirectAttributes ra) {
        try {
            Empresa e = empresaService.obtenerPorId(empresaId);
            // Primero eliminamos las facturas SaaS asociadas a esta empresa
            facturaSaaSService.eliminarPorEmpresa(empresaId);
            auditoriaService.registrarEliminacion("empresa",
                    e.getNombre(), "Código: " + e.getCodigo());
            empresaService.eliminar(empresaId);
            ra.addFlashAttribute("mensaje", "Empresa '" + e.getNombre() + "' eliminada.");
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("error",
                    "No se puede eliminar la empresa porque tiene datos relacionados (usuarios, sucursales, ventas, etc.). " +
                            "Puede suspenderla cambiando su estado a inactiva.");
        }
        return "redirect:/web/admin/empresas";
    }

    // ---- Gestión de usuarios por empresa ----

    @GetMapping("/usuarios")
    public String listarUsuarios(@RequestParam("empresaId") Long empresaId, Model model) {
        Empresa empresa = empresaService.obtenerPorIdConPlan(empresaId);
        List<Usuario> usuarios = usuarioService.listarPorTenant(empresaId);
        List<Sucursal> sucursales = sucursalRepository.findByTenantId(empresaId);
        Map<Long, String> sucursalIdANombre = sucursales != null
                ? sucursales.stream().collect(Collectors.toMap(Sucursal::getId, s -> s.getNombre() != null ? s.getNombre() : ""))
                : Map.of();
        List<EmpresaUsoDto> resumen = saasAdminService.resumenUsoEmpresas();

        model.addAttribute("empresaSeleccionada", empresa);
        model.addAttribute("usuariosEmpresa", usuarios);
        model.addAttribute("maxUsuariosEmpresa", empresa.getMaxUsuariosEfectivo());
        model.addAttribute("sucursales", sucursales != null ? sucursales : List.of());
        model.addAttribute("sucursalIdANombre", sucursalIdANombre);
        model.addAttribute("empresasUso", resumen);
        model.addAttribute("empresas", empresaService.listarTodas());
        model.addAttribute("nuevaEmpresa", new Empresa());
        model.addAttribute("planes", planSuscripcionService.listarActivos());
        return "saas-empresas";
    }

    @PostMapping("/usuarios")
    public String crearUsuario(@RequestParam("empresaId") Long empresaId,
                               @RequestParam("username") String username,
                               @RequestParam("password") String password,
                               @RequestParam("nombreCompleto") String nombreCompleto,
                               @RequestParam("rol") String rol,
                               @RequestParam(value = "sucursalId", required = false) String sucursalIdStr,
                               RedirectAttributes ra) {
        Long sucursalId = null;
        if (sucursalIdStr != null && !sucursalIdStr.isBlank()) {
            try {
                sucursalId = Long.parseLong(sucursalIdStr.trim());
            } catch (NumberFormatException ignored) { }
        }
        Long tenantAnterior = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(empresaId);
            usuarioService.crear(username, password, nombreCompleto, rol, empresaId, sucursalId);
            Empresa emp = empresaService.obtenerPorId(empresaId);
            auditoriaService.registrarCreacion("usuario", username,
                    "Empresa: " + emp.getNombre() + " | Rol: " + rol + " | Nombre: " + nombreCompleto);
            ra.addFlashAttribute("mensaje", "Usuario '" + username + "' creado correctamente.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        } finally {
            if (tenantAnterior != null) {
                TenantContext.setTenantId(tenantAnterior);
            } else {
                TenantContext.clear();
            }
        }
        return "redirect:/web/admin/empresas/usuarios?empresaId=" + empresaId;
    }

    @PostMapping("/usuarios/sucursal")
    public String actualizarSucursalUsuario(@RequestParam("usuarioId") Long usuarioId,
                                            @RequestParam("empresaId") Long empresaId,
                                            @RequestParam(value = "sucursalId", required = false) String sucursalIdStr,
                                            RedirectAttributes ra) {
        Long sucursalId = null;
        if (sucursalIdStr != null && !sucursalIdStr.isBlank()) {
            try {
                sucursalId = Long.parseLong(sucursalIdStr.trim());
            } catch (NumberFormatException ignored) { }
        }
        usuarioService.actualizarSucursal(usuarioId, sucursalId);
        ra.addFlashAttribute("mensaje", "Sucursal del usuario actualizada. Debe volver a iniciar sesión para que se aplique.");
        return "redirect:/web/admin/empresas/usuarios?empresaId=" + empresaId;
    }

    @PostMapping("/usuarios/estado")
    public String cambiarEstadoUsuario(@RequestParam("usuarioId") Long usuarioId,
                                       @RequestParam("empresaId") Long empresaId,
                                       @RequestParam("activo") boolean activo,
                                       RedirectAttributes ra) {
        usuarioService.cambiarEstado(usuarioId, activo);
        Usuario u = usuarioService.obtenerPorId(usuarioId);
        auditoriaService.registrarAccion("PUT",
                activo ? "Activar usuario" : "Desactivar usuario",
                u.getUsername() + " | " + (activo ? "Activado" : "Desactivado"));
        ra.addFlashAttribute("mensaje", activo ? "Usuario activado." : "Usuario desactivado.");
        return "redirect:/web/admin/empresas/usuarios?empresaId=" + empresaId;
    }

    @PostMapping("/usuarios/eliminar")
    public String eliminarUsuario(@RequestParam("usuarioId") Long usuarioId,
                                  @RequestParam("empresaId") Long empresaId,
                                  RedirectAttributes ra) {
        Usuario u = usuarioService.obtenerPorId(usuarioId);
        auditoriaService.registrarEliminacion("usuario",
                u.getUsername(), "Empresa ID: " + empresaId + " | Rol: " + u.getRol());
        usuarioService.eliminar(usuarioId);
        ra.addFlashAttribute("mensaje", "Usuario '" + u.getUsername() + "' eliminado.");
        return "redirect:/web/admin/empresas/usuarios?empresaId=" + empresaId;
    }

    @PostMapping("/usuarios/reset-password")
    public String resetPassword(@RequestParam("usuarioId") Long usuarioId,
                                @RequestParam("empresaId") Long empresaId,
                                @RequestParam("nuevoPassword") String nuevoPassword,
                                RedirectAttributes ra) {
        usuarioService.resetPassword(usuarioId, nuevoPassword);
        Usuario u = usuarioService.obtenerPorId(usuarioId);
        auditoriaService.registrarAccion("PUT", "Reset password", u.getUsername());
        ra.addFlashAttribute("mensaje", "Contraseña de '" + u.getUsername() + "' actualizada.");
        return "redirect:/web/admin/empresas/usuarios?empresaId=" + empresaId;
    }
}

