package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.empresa.Empresa;
import com.farmacia.sistema.domain.empresa.EmpresaService;
import com.farmacia.sistema.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/web/configuracion")
@PreAuthorize("hasRole('GERENTE')")
public class ConfiguracionWebController {

    private final EmpresaService empresaService;

    public ConfiguracionWebController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    public String mostrar(Model model) {
        Empresa empresa = empresaService.obtenerPorId(TenantContext.getTenantId());
        model.addAttribute("empresa", empresa);
        return "configuracion";
    }

    @PostMapping
    public String guardar(@RequestParam String nombre, @RequestParam(required = false) String ruc,
                          @RequestParam(required = false) String direccion,
                          @RequestParam(required = false) String telefono,
                          @RequestParam(required = false) String emailContacto,
                          @RequestParam(required = false) String tipoDocumento,
                          @RequestParam(required = false) String descripcion,
                          RedirectAttributes ra) {
        Empresa e = empresaService.obtenerPorId(TenantContext.getTenantId());
        e.setNombre(nombre);
        e.setRuc(ruc);
        e.setDireccion(direccion);
        e.setTelefono(telefono);
        e.setEmailContacto(emailContacto);
        e.setTipoDocumento(tipoDocumento);
        e.setDescripcion(descripcion);
        empresaService.guardarDirecto(e);
        ra.addFlashAttribute("ok", "Configuración guardada exitosamente");
        return "redirect:/web/configuracion";
    }
}
