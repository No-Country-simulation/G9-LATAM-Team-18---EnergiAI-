package com.energiai.energiaiapi.dto.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Solo booleanos JSON {@code true}/{@code false}. Rechaza strings ("true", "si") y numeros
 * con mensaje en espanol.
 */
public class StrictJsonBooleanDeserializer extends JsonDeserializer<Boolean> {

    @Override
    public Boolean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_NULL) {
            return null;
        }
        if (t == JsonToken.VALUE_TRUE) {
            return Boolean.TRUE;
        }
        if (t == JsonToken.VALUE_FALSE) {
            return Boolean.FALSE;
        }
        if (t == JsonToken.VALUE_STRING) {
            return (Boolean) ctxt.handleWeirdStringValue(Boolean.class, p.getText(),
                    "se espera booleano JSON true o false (no string)");
        }
        if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) {
            return (Boolean) ctxt.handleWeirdNumberValue(Boolean.class, p.getNumberValue(),
                    "se espera booleano JSON true o false (no numero)");
        }
        return (Boolean) ctxt.handleUnexpectedToken(Boolean.class, p);
    }
}
