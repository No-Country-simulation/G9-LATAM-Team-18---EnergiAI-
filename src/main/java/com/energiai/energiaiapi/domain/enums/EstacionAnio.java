package com.energiai.energiaiapi.domain.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum EstacionAnio {

    PRIMAVERA("primavera"),
    VERANO("verano"),
    OTONO("otoño"),
    INVIERNO("invierno");

    private final String valor;

    EstacionAnio(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static Optional<EstacionAnio> desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String n = normalizar(texto);
        return Arrays.stream(values())
                .filter(e -> normalizar(e.valor).equals(n))
                .findFirst();
    }

    public static String[] valores() {
        return Arrays.stream(values()).map(EstacionAnio::getValor).toArray(String[]::new);
    }

    private static String normalizar(String s) {
        return s.trim().toLowerCase(Locale.ROOT)
                .replace('á', 'a').replace('é', 'e').replace('í', 'i')
                .replace('ó', 'o').replace('ú', 'u').replace('ñ', 'n');
    }
}
