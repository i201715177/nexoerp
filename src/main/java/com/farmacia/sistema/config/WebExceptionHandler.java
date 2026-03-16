package com.farmacia.sistema.config;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(basePackages = "com.farmacia.sistema.web")
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleEntityNotFound(EntityNotFoundException ex, RedirectAttributes ra) {
        log.warn("Recurso no encontrado en web: {}", ex.getMessage());
        ra.addFlashAttribute("errorGlobal", "Recurso no encontrado: " + ex.getMessage());
        return "redirect:/web/dashboard";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, RedirectAttributes ra) {
        log.warn("Argumento inválido en web: {}", ex.getMessage());
        ra.addFlashAttribute("errorGlobal", ex.getMessage());
        return "redirect:/web/dashboard";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneric(Exception ex, RedirectAttributes ra) {
        log.error("Error inesperado en web: {} - {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        ra.addFlashAttribute("errorGlobal", "Ocurrió un error inesperado. Intente de nuevo.");
        return "redirect:/web/dashboard";
    }
}
