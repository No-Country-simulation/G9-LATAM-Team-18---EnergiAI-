package com.energiai.energiaiapi.service.costos;

import com.energiai.energiaiapi.dto.BenchmarkConsumoDTO;
import com.energiai.energiaiapi.dto.CostosEstacionalesDTO;
import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.dto.ProyeccionEstacionalDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paridad con {@code Datasetup FINAL FINAL.xlsx}, hoja Parametros filas 4 a 6
 * (recargos Q..AJ) y hoja metricas_final (benchmark).
 */
class CalculadoraCostosEstacionalesTest {

    private static final double DELTA = 1e-6;

    private final CalculadoraCostosEstacionales calculadora = new CalculadoraCostosEstacionales(parametros());

    private static ParametrosCosto parametros() {
        try {
            return new ParametrosCosto(new ObjectMapper(),
                    new ClassPathResource("model/parametros_costos.json"));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar parametros_costos.json", e);
        }
    }

    @Test
    void fila4DePlanilla_veranoSinLed() {
        // 250 kWh, tarifa 0.75, verano (month=1), sin horario pico, sin LED, equipos <= 3 años.
        FacturaDTO f = factura(250, false, "1", false, "menor a 3 años");

        CostosEstacionalesDTO c = calculadora.calcular(f, 187.5, 0.75);

        assertEquals("verano", c.estacion());
        assertEquals(187.5, c.costoBrutoMensual(), DELTA);          // Q4
        assertEquals(0.0711, c.pctEstacional(), DELTA);             // R4
        assertEquals(13.33125, c.montoEstacional(), DELTA);         // S4
        assertEquals(0.0, c.pctHorarioPico(), DELTA);               // T4
        assertEquals(0.15, c.pctSinLed(), DELTA);                   // V4
        assertEquals(28.125, c.montoSinLed(), DELTA);               // W4
        assertEquals(0.0, c.pctAntiguedad(), DELTA);                // X4
        assertEquals(0.2211, c.pctAjusteTotal(), DELTA);            // AA4
        assertEquals(228.95625, c.costoAjustadoMensual(), DELTA);   // AE4
        assertEquals(2250.0, c.costoAnualBruto(), DELTA);           // Z4
        assertEquals(2747.475, c.costoAnualEstimado(), DELTA);      // AF4
        assertEquals(0.15, c.pctAhorroPotencial(), DELTA);          // AG4
        assertEquals(28.125, c.ahorroPotencialMensual(), DELTA);    // AH4
        assertEquals(337.5, c.ahorroPotencialAnual(), DELTA);       // AJ4
    }

    @Test
    void fila5DePlanilla_otonoSinRecargosAccionables() {
        // 150 kWh, tarifa 0.75, otoño (month=4), con LED, equipos <= 5 años.
        FacturaDTO f = factura(150, false, "4", true, "menor a 5 años");

        CostosEstacionalesDTO c = calculadora.calcular(f, 112.5, 0.75);

        assertEquals("otoño", c.estacion());
        assertEquals(0.0848, c.pctEstacional(), DELTA);             // R5
        assertEquals(9.54, c.montoEstacional(), DELTA);             // S5
        assertEquals(122.04, c.costoAjustadoMensual(), DELTA);      // AE5
        assertEquals(1464.48, c.costoAnualEstimado(), DELTA);       // AF5
        assertEquals(0.0, c.pctAhorroPotencial(), DELTA);           // AG5
        assertEquals(0.0, c.ahorroPotencialAnual(), DELTA);         // AJ5
    }

    @Test
    void fila6DePlanilla_inviernoConPicoYEquiposAntiguos() {
        // Fila 6 de la planilla. El consumo (3.2 kWh) queda fuera del rango que valida la API:
        // se usa aqui solo para verificar la aritmetica contra la celda original.
        FacturaDTO f = factura(3, true, "7", true, "mayor a 10 años");

        CostosEstacionalesDTO c = calculadora.calcular(f, 2.4, 0.75);

        assertEquals("invierno", c.estacion());
        assertEquals(0.1, c.pctEstacional(), DELTA);                // R6
        assertEquals(0.15, c.pctHorarioPico(), DELTA);              // T6
        assertEquals(0.15, c.pctAntiguedad(), DELTA);               // X6
        assertEquals(0.0, c.pctSinLed(), DELTA);                    // V6
        assertEquals(0.4, c.pctAjusteTotal(), DELTA);               // AA6
        assertEquals(3.36, c.costoAjustadoMensual(), DELTA);        // AE6
        assertEquals(0.3, c.pctAhorroPotencial(), DELTA);           // AG6
        assertEquals(0.72, c.ahorroPotencialMensual(), DELTA);      // AH6
        assertEquals(8.64, c.ahorroPotencialAnual(), DELTA);        // AJ6
    }

    @Test
    void antiguedadIntermediaTambienRecibeRecargo() {
        // La planilla penaliza "mayor a 5 años": en el contrato de la API eso cubre
        // "menor a 10 años" y "mayor a 10 años".
        CostosEstacionalesDTO conRecargo = calculadora.calcular(
                factura(320, false, "7", true, "menor a 10 años"), 240.0, 0.75);
        CostosEstacionalesDTO sinRecargo = calculadora.calcular(
                factura(320, false, "7", true, "menor a 5 años"), 240.0, 0.75);

        assertEquals(0.15, conRecargo.pctAntiguedad(), DELTA);
        assertEquals(0.0, sinRecargo.pctAntiguedad(), DELTA);
    }

    @Test
    void proyeccionCubreLasCuatroEstacionesYMarcaLaActual() {
        CostosEstacionalesDTO c = calculadora.calcular(
                factura(320, false, "4", true, "menor a 5 años"), 240.0, 0.75);

        assertEquals(4, c.proyeccionEstacional().size());
        assertEquals(1, c.proyeccionEstacional().stream().filter(ProyeccionEstacionalDTO::esEstacionActual).count());
        assertTrue(c.proyeccionEstacional().stream()
                .filter(ProyeccionEstacionalDTO::esEstacionActual)
                .allMatch(p -> p.estacion().equals("otoño")));
        assertEquals("invierno", c.estacionMasCara().estacion());
    }

    @Test
    void anualEstacionalizadoSumaTresMesesPorEstacion() {
        CostosEstacionalesDTO c = calculadora.calcular(
                factura(320, true, "4", true, "menor a 5 años"), 240.0, 0.75);

        // 240 * (12 + 3*0.3415 + 12*0.15)
        double esperado = 240.0 * (12 + 3 * 0.3415 + 12 * 0.15);
        assertEquals(esperado, c.costoAnualEstacionalizado(), 1e-9);
    }

    @Test
    void benchmarkUbicaElConsumoEnElRangoDelTipoDeInmueble() {
        CostosEstacionalesDTO c = calculadora.calcular(
                factura(320, false, "7", true, "menor a 5 años"), 240.0, 0.75);

        BenchmarkConsumoDTO b = c.benchmark();
        assertNotNull(b);
        assertEquals(249.3, b.umbralEficienteKwh(), DELTA);
        assertEquals(365.7, b.umbralModeradoKwh(), DELTA);
        assertEquals("moderado", b.posicionRango());
        assertEquals(70.7, b.brechaVsEficienteKwh(), 1e-9);
        assertEquals("metricas_final", c.fuenteUmbrales());
    }

    @Test
    void rollbackAParametrosCambiaSoloElBenchmark() throws IOException {
        CalculadoraCostosEstacionales conParametros = new CalculadoraCostosEstacionales(
                new ParametrosCosto(new ObjectMapper(),
                        new ClassPathResource("model/parametros_costos.json"),
                        "parametros"));

        FacturaDTO f = factura(320, false, "7", true, "menor a 5 años");
        CostosEstacionalesDTO defaultM = calculadora.calcular(f, 240.0, 0.75);
        CostosEstacionalesDTO rollback = conParametros.calcular(f, 240.0, 0.75);

        // Los costos (Q..AJ) no dependen de los umbrales.
        assertEquals(defaultM.costoAjustadoMensual(), rollback.costoAjustadoMensual(), DELTA);
        assertEquals(defaultM.pctEstacional(), rollback.pctEstacional(), DELTA);

        assertEquals("parametros", rollback.fuenteUmbrales());
        assertEquals(249.3, rollback.benchmark().umbralEficienteKwh(), DELTA);
        assertEquals(419.3, rollback.benchmark().umbralModeradoKwh(), DELTA);
    }

    @Test
    void aliasMetricasEquivaleAMetricasFinal() throws IOException {
        CalculadoraCostosEstacionales alias = new CalculadoraCostosEstacionales(
                new ParametrosCosto(new ObjectMapper(),
                        new ClassPathResource("model/parametros_costos.json"),
                        "metricas"));
        CostosEstacionalesDTO c = alias.calcular(
                factura(320, false, "7", true, "menor a 5 años"), 240.0, 0.75);
        assertEquals("metricas_final", c.fuenteUmbrales());
        assertEquals(365.7, c.benchmark().umbralModeradoKwh(), DELTA);
    }

    @Test
    void versionDeParametrosViajaEnElDesglose() {
        CostosEstacionalesDTO c = calculadora.calcular(
                factura(320, false, "7", true, "menor a 5 años"), 240.0, 0.75);

        assertEquals("datasetup-final-final-v1", c.parametrosVersion());
    }

    private static FacturaDTO factura(int consumo,
                                      boolean picoConsumo,
                                      String month,
                                      boolean led,
                                      String antiguedad) {
        return new FacturaDTO(
                consumo, picoConsumo, 8, "Departamento", 6.5,
                null, month, null, null, null, led, antiguedad, 0.75);
    }
}
