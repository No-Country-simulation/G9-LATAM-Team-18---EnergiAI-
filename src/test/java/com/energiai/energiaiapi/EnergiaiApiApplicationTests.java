package com.energiai.energiaiapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EnergiaiApiApplicationTests {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring arranca (beans, JPA con H2, seguridad, carga del modelo JSON).
    }
}
