package com.energiai.energiaiapi.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstacionAnioTest {

    @Test
    void desdeMesHemisferioSur() {
        assertEquals(EstacionAnio.VERANO, EstacionAnio.desdeMes(1).orElseThrow());
        assertEquals(EstacionAnio.VERANO, EstacionAnio.desdeMes(12).orElseThrow());
        assertEquals(EstacionAnio.OTONO, EstacionAnio.desdeMes(3).orElseThrow());
        assertEquals(EstacionAnio.INVIERNO, EstacionAnio.desdeMes(7).orElseThrow());
        assertEquals(EstacionAnio.PRIMAVERA, EstacionAnio.desdeMes(10).orElseThrow());
    }

    @Test
    void antiguedadRechazaTypoConEnyeIncorrecta() {
        assertTrue(AntiguedadElectrodomesticos.desde("meñor a 5 años").isEmpty());
        assertEquals(AntiguedadElectrodomesticos.MENOR_A_5,
                AntiguedadElectrodomesticos.desde("menor a 5 años").orElseThrow());
    }
}
