package com.energiai.energiaiapi.service.recomendacion;

import java.util.Locale;

/**
 * Formato de cifras para los textos base y el contexto que se envia a Gemini.
 * Se usa punto decimal (Locale.US) para que el prompt no mezcle separadores.
 */
final class Cifras {

    private Cifras() {
    }

    static String usd(double valor) {
        return "USD " + String.format(Locale.US, "%.2f", valor);
    }

    /** Recibe una fraccion (0.15) y devuelve el porcentaje legible ("15%"). */
    static String pct(double fraccion) {
        double porcentaje = fraccion * 100;
        String formato = Math.abs(porcentaje - Math.rint(porcentaje)) < 0.05 ? "%.0f%%" : "%.1f%%";
        return String.format(Locale.US, formato, porcentaje);
    }

    /** Porcentaje con signo explicito, para variaciones ("+8.3%" / "-4%"). */
    static String pctVariacion(double fraccion) {
        String base = pct(Math.abs(fraccion));
        return (fraccion >= 0 ? "+" : "-") + base;
    }

    static String kwh(double valor) {
        return String.format(Locale.US, "%.0f kWh", valor);
    }
}
