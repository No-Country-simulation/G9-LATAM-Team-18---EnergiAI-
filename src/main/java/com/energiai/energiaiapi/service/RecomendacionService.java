package com.energiai.energiaiapi.service;

import com.energiai.energiaiapi.domain.enums.AntiguedadElectrodomesticos;
import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.domain.enums.EstacionAnio;
import com.energiai.energiaiapi.dto.FacturaDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Genera recomendaciones a partir de reglas simples sobre la factura y la categoria.
 */
@Service
public class RecomendacionService {

    public List<String> generar(FacturaDTO f, CategoriaEficiencia categoria) {
        List<String> recs = new ArrayList<>();

        if (Boolean.FALSE.equals(f.tieneIluminacionLed())) {
            recs.add("Cambia la iluminacion a tecnologia LED para reducir el consumo de iluminacion.");
        }
        if (Boolean.TRUE.equals(f.usoHorarioPico())) {
            recs.add("Evita el uso de equipos de alto consumo en horario pico para reducir el costo.");
        }
        if (Boolean.TRUE.equals(f.tieneAireAcondicionado())) {
            EstacionAnio estacion = EstacionAnio.desde(f.estacionAnio()).orElse(null);
            if (estacion == EstacionAnio.VERANO) {
                recs.add("En verano, configura el aire acondicionado en 24 grados y mantene los filtros limpios.");
            } else {
                recs.add("Configura el aire acondicionado en 24 grados y realiza mantenimiento de los filtros.");
            }
        }
        if (Boolean.TRUE.equals(f.tieneCalentador())) {
            recs.add("Reduce la temperatura del calentador electrico y aisla el tanque para ahorrar energia.");
        }
        AntiguedadElectrodomesticos.desde(f.antiguedadElectrodomesticos()).ifPresent(a -> {
            if (a.esAntigua()) {
                recs.add("Considera reemplazar electrodomesticos antiguos por modelos de mayor eficiencia energetica.");
            }
        });
        if (categoria == CategoriaEficiencia.INEFICIENTE) {
            recs.add("Tu consumo es alto respecto a inmuebles similares: revisa los equipos de mayor consumo.");
        }

        if (recs.isEmpty()) {
            recs.add("Buen trabajo: tu consumo se encuentra dentro de un rango eficiente. Manten estos habitos.");
        }
        return recs;
    }
}
