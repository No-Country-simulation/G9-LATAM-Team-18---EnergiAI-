package com.energiai.energiaiapi.dto.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Mes estricto en cuanto al <em>formato</em>: entero JSON o nombre del mes, sin floats,
 * notacion cientifica ni padding.
 *
 * <p>El <em>dominio</em> (1-12 / enero..diciembre) NO se valida aca a proposito: un valor
 * como {@code 13} se deja pasar como texto para que lo rechace {@code @ValorPermitido}.
 * Si se lanzara una excepcion durante la deserializacion, Jackson abortaria la lectura del
 * JSON y el resto de los campos invalidos quedaria sin reportar (la respuesta 400 traeria
 * un solo error en lugar del desglose completo).
 */
public class StrictMonthDeserializer extends JsonDeserializer<String> {

    private static final Pattern SOLO_DIGITOS = Pattern.compile("^\\d+$");
    /** Empieza como numero pero no son solo digitos: {@code 3.5}, {@code 1.003E1}, {@code +3}. */
    private static final Pattern INTENTO_NUMERICO = Pattern.compile("^[+\\-.0-9].*");

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_NULL) {
            return null;
        }
        if (t == JsonToken.VALUE_NUMBER_FLOAT) {
            return (String) ctxt.handleWeirdNumberValue(String.class, p.getNumberValue(),
                    "month debe ser entero 1-12 (notacion cientifica/decimal no permitida)");
        }
        if (t == JsonToken.VALUE_NUMBER_INT) {
            String raw = p.getText();
            if (raw != null && (raw.indexOf('e') >= 0 || raw.indexOf('E') >= 0)) {
                return (String) ctxt.handleWeirdNumberValue(String.class, p.getNumberValue(),
                        "month: notacion cientifica no permitida");
            }
            // Fuera de 1-12 se delega a @ValorPermitido (ver javadoc de la clase).
            return String.valueOf(p.getIntValue());
        }
        if (t == JsonToken.VALUE_STRING) {
            String raw = p.getText();
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            if (!raw.equals(raw.strip())) {
                return (String) ctxt.handleWeirdStringValue(String.class, raw,
                        "month no admite espacios al inicio/final");
            }
            if (SOLO_DIGITOS.matcher(raw).matches()) {
                return raw;
            }
            // Ojo: no se puede filtrar por 'e'/'E' sueltas, los nombres de mes las tienen ("enero").
            if (INTENTO_NUMERICO.matcher(raw).matches()) {
                return (String) ctxt.handleWeirdStringValue(String.class, raw,
                        "month: use un entero 1-12 o el nombre del mes (sin decimales ni notacion cientifica)");
            }
            // Nombre de mes (valido o con typo): pasa como texto y decide @ValorPermitido.
            return raw.toLowerCase(Locale.ROOT);
        }
        return (String) ctxt.handleUnexpectedToken(String.class, p);
    }
}
