package com.energiai.energiaiapi.domain.enums;

import com.energiai.energiaiapi.util.TextoNormalizado;

import java.util.Arrays;
import java.util.Optional;

public enum AntiguedadElectrodomesticos {

    MENOR_A_3("menor a 3 años"),
    MENOR_A_5("menor a 5 años"),
    MENOR_A_10("menor a 10 años"),
    MAYOR_A_10("mayor a 10 años");

    private final String valor;

    AntiguedadElectrodomesticos(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static Optional<AntiguedadElectrodomesticos> desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String n = TextoNormalizado.de(texto);
        return Arrays.stream(values())
                .filter(a -> TextoNormalizado.de(a.valor).equals(n))
                .findFirst();
    }

    public static String[] valores() {
        return Arrays.stream(values()).map(AntiguedadElectrodomesticos::getValor).toArray(String[]::new);
    }

    /** True si conviene recomendar reemplazo (10 años o mas). */
    public boolean esAntigua() {
        return this == MAYOR_A_10 || this == MENOR_A_10;
    }
}
