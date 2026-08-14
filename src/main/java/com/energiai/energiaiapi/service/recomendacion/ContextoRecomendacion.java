package com.energiai.energiaiapi.service.recomendacion;

import com.energiai.energiaiapi.dto.CostosEstacionalesDTO;
import com.energiai.energiaiapi.dto.ResumenHistorialDTO;

/**
 * Contexto extra para matizar las recomendaciones del usuario registrado: estacionalidad,
 * variaciones de costo y comparativa contra su historial. En modo invitado se usa
 * {@link #vacio()} y las recomendaciones quedan exactamente como antes.
 */
public record ContextoRecomendacion(CostosEstacionalesDTO costos, ResumenHistorialDTO historial) {

    private static final ContextoRecomendacion VACIO = new ContextoRecomendacion(null, null);

    public static ContextoRecomendacion vacio() {
        return VACIO;
    }

    public boolean tieneCostos() {
        return costos != null;
    }

    public boolean tieneHistorial() {
        return historial != null && historial.tienePrevios();
    }

    public boolean vacia() {
        return !tieneCostos() && !tieneHistorial();
    }
}
