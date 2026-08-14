package com.energiai.energiaiapi.dto.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Entero estricto: solo {@code VALUE_NUMBER_INT} o string de digitos.
 * Rechaza floats ({@code 1.0}), cientifica ({@code 1E1}) y padding.
 */
public class StrictIntegerDeserializer extends JsonDeserializer<Integer> {

    private static final Pattern DIGITOS = Pattern.compile("^[+-]?\\d+$");

    @Override
    public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_NULL) {
            return null;
        }
        if (t == JsonToken.VALUE_NUMBER_INT) {
            String raw = p.getText();
            if (raw != null && (raw.indexOf('e') >= 0 || raw.indexOf('E') >= 0)) {
                return (Integer) ctxt.handleWeirdNumberValue(Integer.class, p.getNumberValue(),
                        "notacion cientifica no permitida");
            }
            long v = p.getLongValue();
            if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
                return (Integer) ctxt.handleWeirdNumberValue(Integer.class, v, "fuera de rango int");
            }
            return (int) v;
        }
        if (t == JsonToken.VALUE_NUMBER_FLOAT) {
            return (Integer) ctxt.handleWeirdNumberValue(Integer.class, p.getNumberValue(),
                    "se espera entero JSON (sin parte decimal ni notacion cientifica)");
        }
        if (t == JsonToken.VALUE_TRUE || t == JsonToken.VALUE_FALSE) {
            return (Integer) ctxt.handleWeirdNumberValue(Integer.class, p.getBooleanValue() ? 1 : 0,
                    "se espera entero JSON (no booleano)");
        }
        if (t == JsonToken.VALUE_STRING) {
            String raw = p.getText();
            if (raw == null || !DIGITOS.matcher(raw).matches()) {
                return (Integer) ctxt.handleWeirdStringValue(Integer.class, raw,
                        "se espera entero (digitos, sin espacios ni notacion cientifica)");
            }
            try {
                return Integer.valueOf(raw);
            } catch (NumberFormatException e) {
                return (Integer) ctxt.handleWeirdStringValue(Integer.class, raw, "entero fuera de rango");
            }
        }
        return (Integer) ctxt.handleUnexpectedToken(Integer.class, p);
    }
}
