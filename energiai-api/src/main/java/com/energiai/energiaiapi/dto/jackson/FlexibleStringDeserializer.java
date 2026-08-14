package com.energiai.energiaiapi.dto.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/** Acepta string o numero JSON y lo normaliza a String (p. ej. mes: 3 → "3"). */
public class FlexibleStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() != null && p.currentToken().isNumeric()) {
            return String.valueOf(p.getNumberValue().intValue());
        }
        String text = p.getValueAsString();
        return text == null || text.isBlank() ? null : text.trim();
    }
}
