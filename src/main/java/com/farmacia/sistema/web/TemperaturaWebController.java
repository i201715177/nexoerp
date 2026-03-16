package com.farmacia.sistema.web;

import com.farmacia.sistema.domain.inventario.InventarioService;
import com.farmacia.sistema.domain.temperatura.TemperaturaService;
import com.farmacia.sistema.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/web/temperatura")
public class TemperaturaWebController {

    private final TemperaturaService service;
    private final InventarioService inventarioService;

    public TemperaturaWebController(TemperaturaService service, InventarioService inventarioService) {
        this.service = service;
        this.inventarioService = inventarioService;
    }

    @GetMapping
    public String listar(Model model, HttpServletRequest request) {
        model.addAttribute("zonas", service.listarZonas());
        model.addAttribute("registros", service.listarRegistros());
        model.addAttribute("alertas", service.alertasFueraRango());
        model.addAttribute("almacenes", inventarioService.listarAlmacenes());
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            model.addAttribute("apiKey", service.getOrCreateApiKey(tenantId));
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort())
                    + (request.getContextPath() != null && !request.getContextPath().isEmpty() ? request.getContextPath() : "");
            model.addAttribute("apiUrl", baseUrl + "/api/temperatura/lectura");
        }
        return "temperatura";
    }

    @PostMapping("/regenerar-clave")
    public String regenerarClave(RedirectAttributes ra) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            service.regenerateApiKey(tenantId);
            ra.addFlashAttribute("ok", "Clave de API regenerada. Pásale la nueva clave al responsable del sensor o gateway.");
        }
        // Volvemos explícitamente a la pestaña de temperatura (tarjeta de integración)
        return "redirect:/web/temperatura#integracion-sensores";
    }

    // Evita error 405 si alguien entra por GET directo a /web/temperatura/regenerar-clave
    @GetMapping("/regenerar-clave")
    public String regenerarClaveGet() {
        return "redirect:/web/temperatura#integracion-sensores";
    }

    @PostMapping("/zonas")
    public String crearZona(@RequestParam String nombre, @RequestParam(required = false) Long almacenId,
                            @RequestParam(required = false) BigDecimal tempMin,
                            @RequestParam(required = false) BigDecimal tempMax,
                            @RequestParam(required = false) BigDecimal humMin,
                            @RequestParam(required = false) BigDecimal humMax,
                            @RequestParam(required = false, defaultValue = "false") boolean refrigeracion,
                            RedirectAttributes ra) {
        service.crearZona(nombre, almacenId, tempMin, tempMax, humMin, humMax, refrigeracion);
        ra.addFlashAttribute("ok", "Zona creada");
        return "redirect:/web/temperatura";
    }

    @PostMapping("/registrar")
    public String registrar(@RequestParam Long zonaId, @RequestParam BigDecimal temperatura,
                            @RequestParam(required = false) BigDecimal humedad,
                            @RequestParam(required = false) String observacion,
                            Authentication auth, RedirectAttributes ra) {
        service.registrar(zonaId, temperatura, humedad, observacion, auth.getName());
        ra.addFlashAttribute("ok", "Registro guardado");
        return "redirect:/web/temperatura";
    }
}
