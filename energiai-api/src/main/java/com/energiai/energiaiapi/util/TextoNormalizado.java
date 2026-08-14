package com.energiai.energiaiapi.util;

import java.util.Locale;

/**
 * Normalizacion conservadora para enums / allowlists:
 * trim + minusculas + pliega tildes vocales, pero <strong>conserva {@code ñ}</strong>
 * para no aceptar typos del tipo {@code meñor} → {@code menor}.
 */
public final class TextoNormalizado {

    private TextoNormalizado() {
    }

    public static String de(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase(Locale.ROOT)
                .replace('á', 'a').replace('é', 'e').replace('í', 'i')
                .replace('ó', 'o').replace('ú', 'u')
                .replace('Á', 'a').replace('É', 'e').replace('Í', 'i')
                .replace('Ó', 'o').replace('Ú', 'u');
    }
}
