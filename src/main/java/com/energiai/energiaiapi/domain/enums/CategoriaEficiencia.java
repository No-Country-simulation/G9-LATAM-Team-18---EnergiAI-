package com.energiai.energiaiapi.domain.enums;

/**
 * Clases objetivo del modelo. Las etiquetas coinciden EXACTAMENTE con el campo
 * "classes" del contrato modelo_energiai.json (mayusculas incluidas).
 */
public enum CategoriaEficiencia {

    EFICIENTE("Eficiente"),
    MODERADO("Moderado"),
    INEFICIENTE("Ineficiente");

    private final String etiqueta;

    CategoriaEficiencia(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * Resuelve la categoria a partir de la etiqueta que expone el modelo
     * (por ejemplo la que envia el frontend tras correr ONNX).
     */
    public static CategoriaEficiencia desdeEtiqueta(String etiqueta) {
        if (etiqueta == null) {
            throw new IllegalArgumentException("La categoria no puede ser nula");
        }
        for (CategoriaEficiencia c : values()) {
            if (c.etiqueta.equalsIgnoreCase(etiqueta.trim()) || c.name().equalsIgnoreCase(etiqueta.trim())) {
                return c;
            }
        }
        throw new IllegalArgumentException("Categoria de eficiencia desconocida: " + etiqueta);
    }
}
