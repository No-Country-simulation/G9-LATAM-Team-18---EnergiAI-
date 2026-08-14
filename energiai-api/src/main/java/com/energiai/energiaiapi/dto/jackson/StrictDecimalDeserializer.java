package com.energiai.energiaiapi.dto.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Decimal estricto: rechaza notacion cientifica ({@code 1E3}, {@code 1.0e1}).
 * Acepta enteros JSON y floats literales sin exponente (p. ej. {@code 6.5}).
 */
public class StrictDecimalDeserializer extends JsonDeserializer<Double> {

    private static final Pattern CIENTIFICA = Pattern.compile(".*[eE].*");
    private static final Pattern DECIMAL_OK = Pattern.compile("^[+-]?\\d+(\\.\\d+)?$");

    @Override
    public Double deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_NULL) {
            return null;
        }
        String raw = p.getText();
        if (raw == null || raw.isBlank()) {
            return (Double) ctxt.handleWeirdStringValue(Double.class, raw, "valor numerico vacio");
        }
        if (CIENTIFICA.matcher(raw).matches()) {
            return (Double) ctxt.handleWeirdNumberValue(Double.class, p.getNumberValue(),
                    "notacion cientifica no permitida: use decimal/entero explicito (ej. 1000, no 1E3)");
        }
        if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) {
            return p.getDoubleValue();
        }
        if (t == JsonToken.VALUE_STRING) {
            String s = raw.trim();
            if (!s.equals(raw)) {
                return (Double) ctxt.handleWeirdStringValue(Double.class, raw,
                        "no se permiten espacios alrededor del numero");
            }
            if (CIENTIFICA.matcher(s).matches() || !DECIMAL_OK.matcher(s).matches()) {
                return (Double) ctxt.handleWeirdStringValue(Double.class, raw,
                        "formato numerico invalido (sin notacion cientifica)");
            }
            return Double.valueOf(s);
        }
        return (Double) ctxt.handleUnexpectedToken(Double.class, p);
    }
}
