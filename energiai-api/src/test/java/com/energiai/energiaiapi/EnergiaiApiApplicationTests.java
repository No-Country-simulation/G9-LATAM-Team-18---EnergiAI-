package com.energiai.energiaiapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        // Evita que SPRING_DATASOURCE_URL del entorno pise el H2 de test.
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.jwt.secret=secreto-de-test-de-al-menos-32-bytes-1234567890"
})
@ActiveProfiles("test")
class EnergiaiApiApplicationTests {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring arranca (beans, JPA con H2, seguridad, carga del modelo JSON).
    }
}
