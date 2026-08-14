package com.energiai.energiaiapi.validation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Traduce rutas de Bean Validation (nombres Java del bean) a los nombres del contrato JSON,
 * resolviendo {@code @JsonProperty} en cada tramo.
 *
 * <p>Ej.: {@code factura.tipoInmueble} → {@code factura.tipo_inmueble}. Asi el desglose de
 * errores 400 nombra los campos tal como los envio el cliente.
 */
public final class CamposJson {

    private CamposJson() {
    }

    /**
     * @param raiz     clase del objeto validado ({@code null} → se devuelve la ruta sin traducir)
     * @param rutaBean ruta separada por puntos, con indices opcionales ({@code items[0].campo})
     */
    public static String rutaJson(Class<?> raiz, String rutaBean) {
        if (rutaBean == null || rutaBean.isBlank()) {
            return rutaBean;
        }
        StringBuilder salida = new StringBuilder(rutaBean.length() + 8);
        Class<?> actual = raiz;
        for (String tramo : rutaBean.split("\\.")) {
            int corchete = tramo.indexOf('[');
            String nombre = corchete < 0 ? tramo : tramo.substring(0, corchete);
            String indice = corchete < 0 ? "" : tramo.substring(corchete);

            Resuelto resuelto = resolver(actual, nombre);
            if (!salida.isEmpty()) {
                salida.append('.');
            }
            salida.append(resuelto.nombreJson()).append(indice);
            actual = resuelto.tipo();
        }
        return salida.toString();
    }

    private static Resuelto resolver(Class<?> tipo, String nombre) {
        if (tipo == null) {
            return new Resuelto(nombre, null);
        }
        String nombreJson = nombre;
        Class<?> siguiente = null;

        Field campo = buscarCampo(tipo, nombre);
        if (campo != null) {
            siguiente = campo.getType();
            nombreJson = valorAnotacion(campo.getAnnotation(JsonProperty.class), nombreJson);
        }
        // Fallback por si la anotacion quedo solo en el accesor del record.
        if (nombreJson.equals(nombre)) {
            try {
                Method accesor = tipo.getMethod(nombre);
                nombreJson = valorAnotacion(accesor.getAnnotation(JsonProperty.class), nombreJson);
                if (siguiente == null) {
                    siguiente = accesor.getReturnType();
                }
            } catch (NoSuchMethodException ignorado) {
                // sin accesor: se conserva el nombre Java
            }
        }
        return new Resuelto(nombreJson, siguiente);
    }

    private static String valorAnotacion(JsonProperty anotacion, String porDefecto) {
        if (anotacion == null || anotacion.value().isBlank()) {
            return porDefecto;
        }
        return anotacion.value();
    }

    private static Field buscarCampo(Class<?> tipo, String nombre) {
        for (Class<?> c = tipo; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(nombre);
            } catch (NoSuchFieldException ignorado) {
                // sigue por la superclase
            }
        }
        return null;
    }

    private record Resuelto(String nombreJson, Class<?> tipo) {
    }
}
