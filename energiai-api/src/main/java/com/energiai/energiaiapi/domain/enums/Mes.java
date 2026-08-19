package com.energiai.energiaiapi.domain.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum Mes {

    ENERO("enero", 1),
    FEBRERO("febrero", 2),
    MARZO("marzo", 3),
    ABRIL("abril", 4),
    MAYO("mayo", 5),
    JUNIO("junio", 6),
    JULIO("julio", 7),
    AGOSTO("agosto", 8),
    SEPTIEMBRE("septiembre", 9),
    OCTUBRE("octubre", 10),
    NOVIEMBRE("noviembre", 11),
    DICIEMBRE("diciembre", 12);

    private final String valor;
    private final int numero;

    Mes(String valor, int numero) {
        this.valor = valor;
        this.numero = numero;
    }

    public String getValor() {
        return valor;
    }

    public int getNumero() {
        return numero;
    }

    public static Optional<Mes> desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String t = texto.trim();
        try {
            int n = Integer.parseInt(t);
            return desdeNumero(n);
        } catch (NumberFormatException ignored) {
            // nombre del mes
        }
        String n = normalizar(t);
        return Arrays.stream(values())
                .filter(e -> normalizar(e.valor).equals(n))
                .findFirst();
    }

    public static Optional<Mes> desdeNumero(int numero) {
        return Arrays.stream(values()).filter(e -> e.numero == numero).findFirst();
    }

    /** Acepta "3", "03", "marzo", etc. */
    public static Optional<Integer> numeroDesde(String texto) {
        return desde(texto).map(Mes::getNumero);
    }

    public static String[] valores() {
        return Arrays.stream(values()).map(Mes::getValor).toArray(String[]::new);
    }

    private static String normalizar(String s) {
        return s.trim().toLowerCase(Locale.ROOT)
                .replace('á', 'a').replace('é', 'e').replace('í', 'i')
                .replace('ó', 'o').replace('ú', 'u').replace('ñ', 'n');
    }
}
