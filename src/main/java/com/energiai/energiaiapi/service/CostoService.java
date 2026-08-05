package com.energiai.energiaiapi.service;

import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.service.inference.ModeloParametros;
import org.springframework.stereotype.Service;

/**
 * Calculos de negocio derivados (no dependen del modelo ML):
 * costo mensual estimado e indice de intensidad energetica (IIE).
 * Tras el cambio area -> estacion, el IIE se calcula como consumo / personas.
 */
@Service
public class CostoService {

    private final ModeloParametros modelo;

    public CostoService(ModeloParametros modelo) {
        this.modelo = modelo;
    }

    /** costo = consumo_mensual * tarifa (individual si vino, si no la de referencia). */
    public double calcularCostoMensual(FacturaDTO f) {
        double tarifa = (f.tarifaElectrica() != null)
                ? f.tarifaElectrica()
                : modelo.get().tarifaReferenciaKwh();
        return f.consumoMensual() * tarifa;
    }

    /** IIE = consumo_mensual / numero_personas. Null si no se informo el numero de personas. */
    public Double calcularIndiceEficiencia(FacturaDTO f) {
        if (f.numeroPersonas() == null || f.numeroPersonas() <= 0) {
            return null;
        }
        return f.consumoMensual() / f.numeroPersonas();
    }
}
