package com.energiai.energiaiapi.validation;

import com.energiai.energiaiapi.dto.AnalisisRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CamposJsonTest {

    @Test
    void traduceRutaAnidadaANombresDelContratoJson() {
        assertEquals("factura.tipo_inmueble",
                CamposJson.rutaJson(AnalisisRequest.class, "factura.tipoInmueble"));
        assertEquals("factura.horas_alto_consumo",
                CamposJson.rutaJson(AnalisisRequest.class, "factura.horasPromedioUso"));
        // El componente se llama "mes" en Java y "month" en el JSON.
        assertEquals("factura.month",
                CamposJson.rutaJson(AnalisisRequest.class, "factura.mes"));
    }

    @Test
    void conservaCamposSinJsonProperty() {
        assertEquals("factura", CamposJson.rutaJson(AnalisisRequest.class, "factura"));
        assertEquals("guardar", CamposJson.rutaJson(AnalisisRequest.class, "guardar"));
    }

    @Test
    void toleraRaizDesconocidaYRutasVacias() {
        assertEquals("factura.tipoInmueble", CamposJson.rutaJson(null, "factura.tipoInmueble"));
        assertEquals("campoInexistente", CamposJson.rutaJson(AnalisisRequest.class, "campoInexistente"));
        assertEquals("", CamposJson.rutaJson(AnalisisRequest.class, ""));
    }
}
