package com.farmacia.sistema.util;

/**
 * Valida número celular peruano: debe empezar por 9 y tener exactamente 9 dígitos.
 */
public final class CelularValidator {

    private CelularValidator() {}

    /**
     * Si el teléfono no está vacío, valida que tenga exactamente 9 dígitos y que empiece por 9.
     *
     * @param telefono número de celular (puede ser null o vacío; en ese caso no se valida)
     * @throws IllegalArgumentException si tiene valor pero no cumple el formato
     */
    public static void validar(String telefono) {
        if (telefono == null || telefono.isBlank()) return;
        String soloDigitos = telefono.trim().replaceAll("\\D", "");
        if (soloDigitos.isEmpty()) return;
        if (soloDigitos.length() < 9) {
            throw new IllegalArgumentException("El número celular debe tener exactamente 9 dígitos.");
        }
        if (soloDigitos.length() > 9) {
            throw new IllegalArgumentException("El número celular no debe tener más de 9 dígitos.");
        }
        if (soloDigitos.charAt(0) != '9') {
            throw new IllegalArgumentException("El número celular debe comenzar con 9.");
        }
    }
}
