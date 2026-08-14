package com.energiai.energiaiapi.service.inference;

import com.energiai.energiaiapi.dto.FacturaDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test unitario del fallback en Java. No levanta el contexto de Spring:
 * construye ModeloParametros directamente desde el JSON del classpath.
 */
class ClasificacionServiceLocalTest {

    private ClasificacionServiceLocal nuevoServicio() throws Exception {
        ModeloParametros modelo = new ModeloParametros(
                new ObjectMapper(),
                new ClassPathResource("model/modelo_energiai.json"));
        return new ClasificacionServiceLocal(modelo);
    }

    @Test
    void clasificaConSoloCamposObligatorios() throws Exception {
        ClasificacionServiceLocal servicio = nuevoServicio();

        FacturaDTO factura = new FacturaDTO(
                320, true, 8, "casa", 4.5,
                null, null, null, null, null, null, null, null);

        ClasificadorPort.Clasificacion resultado = servicio.clasificar(factura);

        assertNotNull(resultado.categoria());
        assertEquals(3, resultado.probabilidades().size());

        double suma = resultado.probabilidades().values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, suma, 1e-9, "Las probabilidades del softmax deben sumar 1");
    }

    @Test
    void manejaTipoInmuebleYEstacionValidos() throws Exception {
        ClasificacionServiceLocal servicio = nuevoServicio();

        FacturaDTO factura = new FacturaDTO(
                500, false, 12, "departamento", 6.0,
                "verano", "enero", 4, true, false, true, "mayor a 10 años", 0.80);

        ClasificadorPort.Clasificacion resultado = servicio.clasificar(factura);

        assertNotNull(resultado.categoria());
        assertTrue(resultado.probabilidades().containsKey("Eficiente"));
    }
}
