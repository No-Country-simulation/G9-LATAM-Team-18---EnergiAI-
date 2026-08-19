package com.energiai.energiaiapi.service.recomendacion;

/**
 * Tema cerrado seleccionado por reglas. {@code textoBase} es el fallback determinista;
 * Gemini solo puede reformular este contenido, no inventar temas nuevos.
 */
public record TemaRecomendacion(String codigo, String textoBase) {
}
