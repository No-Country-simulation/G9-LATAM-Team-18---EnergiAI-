package com.energiai.energiaiapi.domain.enums;

import com.energiai.energiaiapi.util.TextoNormalizado;

import java.util.Arrays;
import java.util.Optional;

/**
 * Estaciones (hemisferio sur, alineado al producto Latam / AR).
 * Preferir inferencia desde {@link Mes}; el campo JSON {@code estacion_anio} es opcional legado.
 */
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
        String n = TextoNormalizado.de(texto);
        // Alias ASCII sin ñ
        if (n.equals("otono")) {
            return Optional.of(OTONO);
        }
        return Arrays.stream(values())
                .filter(e -> TextoNormalizado.de(e.valor).equals(n))
                .findFirst();
    }

    /**
     * Hemisferio sur: Dic–Feb verano, Mar–May otoño, Jun–Ago invierno, Sep–Nov primavera.
     */
    public static Optional<EstacionAnio> desdeMes(Integer month) {
        if (month == null || month < 1 || month > 12) {
            return Optional.empty();
        }
        return Optional.of(switch (month) {
            case 12, 1, 2 -> VERANO;
            case 3, 4, 5 -> OTONO;
            case 6, 7, 8 -> INVIERNO;
            default -> PRIMAVERA; // 9,10,11
        });
    }

    public static String[] valores() {
        return Arrays.stream(values()).map(EstacionAnio::getValor).toArray(String[]::new);
    }
}
