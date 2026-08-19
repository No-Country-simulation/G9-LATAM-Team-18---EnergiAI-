package com.energiai.energiaiapi.exception;

import com.energiai.energiaiapi.dto.ErrorResponse;
import com.energiai.energiaiapi.validation.CamposJson;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Tope de campos citados en {@code message}; el detalle completo va en {@code fieldErrors}. */
    private static final int MAX_CAMPOS_EN_RESUMEN = 6;

    /** Prefijo en ingles que Jackson antepone al texto de nuestros deserializadores. */
    private static final Pattern PREFIJO_JVM =
            Pattern.compile("^Cannot (?:deserialize|coerce)[^:]*:\\s*");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        BindingResult resultado = ex.getBindingResult();
        Class<?> raiz = resultado.getTarget() == null ? null : resultado.getTarget().getClass();

        // TreeMap: el desglose sale ordenado y estable aunque las violaciones lleguen sin orden.
        Map<String, String> fieldErrors = new TreeMap<>();
        for (FieldError error : resultado.getFieldErrors()) {
            fieldErrors.merge(CamposJson.rutaJson(raiz, error.getField()),
                    detalleCampo(error), GlobalExceptionHandler::unirMensajes);
        }
        for (ObjectError error : resultado.getGlobalErrors()) {
            fieldErrors.merge(error.getObjectName(),
                    mensajeODefault(error.getDefaultMessage()), GlobalExceptionHandler::unirMensajes);
        }

        ErrorResponse body = ErrorResponse.validation(
                HttpStatus.BAD_REQUEST.value(),
                resumenValidacion(fieldErrors.keySet()),
                request.getRequestURI(),
                fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /** Mensaje de la constraint + el valor que llego, para no obligar a adivinar que se rechazo. */
    private static String detalleCampo(FieldError error) {
        String mensaje = mensajeODefault(error.getDefaultMessage());
        return mensaje + " (recibido: " + valorLegible(error.getRejectedValue()) + ")";
    }

    private static String mensajeODefault(String mensaje) {
        return mensaje == null || mensaje.isBlank() ? "valor invalido" : mensaje;
    }

    private static String unirMensajes(String previo, String nuevo) {
        return previo.contains(nuevo) ? previo : previo + "; " + nuevo;
    }

    private static String resumenValidacion(Set<String> campos) {
        String lista = campos.stream().limit(MAX_CAMPOS_EN_RESUMEN).collect(Collectors.joining(", "));
        if (campos.size() > MAX_CAMPOS_EN_RESUMEN) {
            lista += ", …";
        }
        return campos.size() == 1
                ? "Error de validacion en 1 campo: " + lista
                : "Error de validacion en " + campos.size() + " campos: " + lista;
    }

    /** Strings entre comillas y recortados; el resto tal cual. Evita respuestas gigantes. */
    private static String valorLegible(Object valor) {
        if (valor == null) {
            return "null";
        }
        if (valor instanceof CharSequence texto) {
            String s = texto.toString();
            if (s.length() > 60) {
                s = s.substring(0, 60) + "…";
            }
            return "\"" + s + "\"";
        }
        return String.valueOf(valor);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex,
                                                           HttpServletRequest request) {
        String descripcion = mensajeLegible(ex);
        String campo = jacksonFieldPath(rootCause(ex));
        if (campo == null || campo.isBlank()) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(
                    HttpStatus.BAD_REQUEST.value(), "Bad Request", descripcion, request.getRequestURI()));
        }
        // Jackson corta la lectura en el primer campo ilegible: el resto del payload todavia
        // no paso por Bean Validation, y conviene aclararlo para no dar por bueno lo demas.
        String message = "Error de formato en " + campo + ": la lectura del JSON se detuvo en ese campo. "
                + "Corrige el formato y reenvia para validar los campos restantes.";
        return ResponseEntity.badRequest().body(ErrorResponse.validation(
                HttpStatus.BAD_REQUEST.value(), message, request.getRequestURI(),
                Map.of(campo, descripcion)));
    }

    /** Descripcion del problema sin el nombre del campo (lo aporta la clave de {@code fieldErrors}). */
    private static String mensajeLegible(HttpMessageNotReadableException ex) {
        Throwable root = rootCause(ex);
        if (root instanceof InvalidFormatException ife) {
            Class<?> target = ife.getTargetType();
            String recibido = " (recibido: " + valorLegible(ife.getValue()) + ")";
            // Preferir el mensaje del deserializador estricto: es el mas especifico.
            String propio = mensajePropio(ife.getOriginalMessage());
            if (propio != null) {
                return propio + recibido;
            }
            if (target == Boolean.class || target == boolean.class) {
                return "debe ser booleano JSON true o false (no string ni numero)" + recibido;
            }
            if (target == Integer.class || target == int.class || target == Long.class || target == long.class) {
                return "debe ser un entero JSON (sin decimales ni notacion cientifica)" + recibido;
            }
            if (target == Double.class || target == double.class || target == Float.class || target == float.class) {
                return "debe ser un numero decimal JSON (sin notacion cientifica)" + recibido;
            }
            return "valor con formato invalido" + recibido;
        }
        if (root instanceof MismatchedInputException mie) {
            String propio = mensajePropio(mie.getOriginalMessage());
            return propio != null ? propio : "tipo de dato incorrecto para el campo";
        }
        String m = root.getMessage();
        if (m == null || m.isBlank() || pareceMensajeJvmIngles(m)) {
            return "JSON invalido o valor de campo no permitido. "
                    + "Verifica tipos (booleanos true/false, enteros sin decimales, "
                    + "uso_horario_pico \"si\"|\"no\", sin notacion cientifica).";
        }
        return m.length() > 400 ? m.substring(0, 400) + "…" : m;
    }

    /**
     * Jackson envuelve el texto del deserializador en un prefijo propio en ingles
     * ({@code Cannot deserialize value of type `java.lang.String` from number 13: <nuestro texto>}).
     * Sin quitarlo, el mensaje en español se descartaba y la respuesta quedaba en un
     * generico "valor con formato invalido".
     */
    private static String mensajePropio(String detalle) {
        if (detalle == null || detalle.isBlank()) {
            return null;
        }
        String limpio = PREFIJO_JVM.matcher(detalle).replaceFirst("").trim();
        if (limpio.isBlank() || pareceMensajeJvmIngles(limpio)) {
            return null;
        }
        return limpio;
    }

    private static boolean pareceMensajeJvmIngles(String m) {
        String s = m.toLowerCase();
        return s.contains("cannot deserialize")
                || s.contains("not a valid")
                || s.contains("unexpected token")
                || s.contains("from string")
                || s.contains("as java.lang");
    }

    private static String jacksonFieldPath(Throwable t) {
        if (!(t instanceof JsonMappingException jme)) {
            return null;
        }
        List<JsonMappingException.Reference> path = jme.getPath();
        if (path == null || path.isEmpty()) {
            return null;
        }
        return path.stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : ("[" + ref.getIndex() + "]"))
                .collect(Collectors.joining("."));
    }

    private static Throwable rootCause(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> handleReglaNegocio(ReglaNegocioException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNoEncontrado(RecursoNoEncontradoException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex,
                                                              HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return build(status, ex.getReason(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno: " + ex.getMessage(), request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
