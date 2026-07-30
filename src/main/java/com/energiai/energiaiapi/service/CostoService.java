package com.energiai.energiaiapi.service;

import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.service.inference.ModeloParametros;
import org.springframework.stereotype.Service;

/**
 * Calculos de negocio derivados (no dependen del modelo ML):
 * costo mensual estimado e indice de intensidad energetica (IIE).
 */
@Service
public class CostoService {

    private final ModeloParametros modelo;

    public CostoService(ModeloParametros modelo) {
        this.modelo = modelo;
    }

    /** costo = consumo_kwh * tarifa (individual si vino, si no la de referencia del contrato). */
    public double calcularCostoMensual(FacturaDTO f) {
        double tarifa = (f.tarifaElectrica() != null)
                ? f.tarifaElectrica()
                : modelo.get().tarifaReferenciaKwh();
        return f.consumoKwh() * tarifa;
    }

    /** IIE = consumo_kwh / (personas * area). Null si faltan los datos opcionales. */
    public Double calcularIndiceEficiencia(FacturaDTO f) {
        if (f.numeroPersonas() == null || f.areaInmueble() == null) {
            return null;
        }
        double denominador = f.numeroPersonas() * f.areaInmueble();
        if (denominador <= 0) {
            return null;
        }
        return f.consumoKwh() / denominador;
    }
}
