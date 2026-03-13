package com.farmacia.sistema.domain.facturacion;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MontoEnLetras {

    private MontoEnLetras() {}

    private static final String[] UNIDADES = {"", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE"};
    private static final String[] DECENAS = {"DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE"};
    private static final String[] DECENAS2 = {"", "", "VEINTE", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"};
    private static final String[] CENTENAS = {"", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS", "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

    public static String convertir(BigDecimal monto, String moneda) {
        if (monto == null) monto = BigDecimal.ZERO;
        monto = monto.setScale(2, RoundingMode.HALF_UP);
        long entero = monto.longValue();
        int decimales = monto.remainder(BigDecimal.ONE).movePointRight(2).intValue();

        String letras = convertirEntero(entero);
        String monedaTexto = "SOLES";
        if ("USD".equalsIgnoreCase(moneda)) monedaTexto = "DOLARES AMERICANOS";

        return "SON: " + letras + " CON " + String.format("%02d", decimales) + "/100 " + monedaTexto;
    }

    public static String convertir(BigDecimal monto) {
        return convertir(monto, "PEN");
    }

    private static String convertirEntero(long n) {
        if (n == 0) return "CERO";
        if (n == 100) return "CIEN";

        StringBuilder sb = new StringBuilder();
        if (n >= 1_000_000) {
            long millones = n / 1_000_000;
            if (millones == 1) sb.append("UN MILLON ");
            else sb.append(convertirEntero(millones)).append(" MILLONES ");
            n %= 1_000_000;
        }
        if (n >= 1000) {
            long miles = n / 1000;
            if (miles == 1) sb.append("MIL ");
            else sb.append(convertirEntero(miles)).append(" MIL ");
            n %= 1000;
        }
        if (n >= 100) {
            if (n == 100) { sb.append("CIEN"); return sb.toString().trim(); }
            sb.append(CENTENAS[(int) (n / 100)]).append(" ");
            n %= 100;
        }
        if (n >= 20) {
            sb.append(DECENAS2[(int) (n / 10)]);
            if (n % 10 != 0) sb.append(" Y ").append(UNIDADES[(int) (n % 10)]);
            sb.append(" ");
        } else if (n >= 10) {
            sb.append(DECENAS[(int) (n - 10)]).append(" ");
        } else if (n > 0) {
            sb.append(UNIDADES[(int) n]).append(" ");
        }

        return sb.toString().trim();
    }
}
