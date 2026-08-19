package com.energiai.energiaiapi.dto.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.Locale;

/**
 * Solo {@code "si"} / {@code "no"} (case-insensitive, sin padding ni alias {@code n}/{@code s}).
 */
public class SiNoBooleanDeserializer extends JsonDeserializer<Boolean> {

    @Override
    public Boolean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_NULL) {
            return null;
        }
        if (t != JsonToken.VALUE_STRING) {
            return (Boolean) ctxt.handleUnexpectedToken(Boolean.class, p);
        }
        String raw = p.getText();
        if (raw == null) {
            return null;
        }
        if (!raw.equals(raw.strip())) {
            return (Boolean) ctxt.handleWeirdStringValue(Boolean.class, raw,
                    "uso_horario_pico no admite espacios; use exactamente \"si\" o \"no\"");
        }
        String s = raw.toLowerCase(Locale.ROOT);
        // Normaliza tilde de "sí" → se rechaza; solo ASCII "si"
        if (s.equals("si")) {
            return true;
        }
        if (s.equals("no")) {
            return false;
        }
        return (Boolean) ctxt.handleWeirdStringValue(Boolean.class, raw,
                "uso_horario_pico debe ser exactamente \"si\" o \"no\"");
    }
}
