package com.farmacia.sistema.util;

/**
 * Valida tipo de documento (DNI o RUC) y número según reglas peruanas:
 * DNI = exactamente 8 dígitos.
 * RUC = exactamente 11 dígitos.
 */
public final class DocumentoValidator {

    private static final int DNI_LENGTH = 8;
    private static final int RUC_LENGTH = 11;

    private DocumentoValidator() {}

    /**
     * Valida que el tipo sea DNI o RUC y que el número tenga exactamente
     * 8 dígitos (DNI) o 11 dígitos (RUC). Solo se permiten dígitos en el número.
     *
     * @param tipoDocumento "DNI" o "RUC"
     * @param numeroDocumento número sin espacios (solo dígitos)
     * @throws IllegalArgumentException si el tipo no es DNI/RUC o el número no cumple la longitud
     */
    public static void validar(String tipoDocumento, String numeroDocumento) {
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            throw new IllegalArgumentException("El tipo de documento debe ser DNI o RUC.");
        }
        String tipo = tipoDocumento.trim().toUpperCase();
        if (!"DNI".equals(tipo) && !"RUC".equals(tipo)) {
            throw new IllegalArgumentException("El tipo de documento debe ser DNI o RUC.");
        }
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            throw new IllegalArgumentException("El número de documento es obligatorio.");
        }
        String num = numeroDocumento.trim().replaceAll("\\s", "");
        if (!num.matches("\\d+")) {
            throw new IllegalArgumentException("El número de documento solo debe contener dígitos.");
        }
        if ("DNI".equals(tipo)) {
            if (num.length() != DNI_LENGTH) {
                throw new IllegalArgumentException("El DNI debe tener exactamente 8 dígitos.");
            }
        } else {
            if (num.length() != RUC_LENGTH) {
                throw new IllegalArgumentException("El RUC debe tener exactamente 11 dígitos.");
            }
        }
    }
}
