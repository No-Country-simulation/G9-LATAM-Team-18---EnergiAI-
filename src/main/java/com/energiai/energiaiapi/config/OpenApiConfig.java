package com.energiai.energiaiapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI 3 via springdoc-openapi (Swagger UI en {@code /swagger-ui.html}).
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI energiaiOpenAPI() {
        Map<String, Object> errorValidacion = errorCasoQa5();

        return new OpenAPI()
                .info(new Info()
                        .title("EnergiAI API")
                        .version("v1 (xgboost-v2)")
                        .description("""
                                ![EnergiAI](/branding/banner-energiai.png)

                                Backend **EnergiAI** (Hackathon ONE G9 · Alura + Oracle): clasifica eficiencia \
                                energetica (**Eficiente / Moderado / Ineficiente**), estima costo e IIE, y genera \
                                recomendaciones (reglas + Gemini hibrido).

                                ## Como interactuar con la API
                                | Modalidad | Cuando usarla | Como |
                                |---|---|---|
                                | **Swagger UI** | Explorar contratos, Try it out, Authorize JWT | `/swagger-ui.html` |
                                | **Bruno** | Coleccion versionada (invitado, JWT, OAuth, ONNX, QA) | carpeta `bruno/` |
                                | **curl / HTTP** | Scripts, CI, smoke rapido | `POST /api/analisis` |
                                | **Invitado** | Demo sin cuenta; no persiste | sin `Authorization`, `guardar=false` |
                                | **JWT (registro/login)** | Historial + bloque `costos` | `Authorization: Bearer <jwt>` |
                                | **OAuth Google/Facebook** | Canje de token social → JWT | `POST /api/auth/oauth/{google\\|facebook}` |
                                | **Frontend** | Formulario del prototipo contra esta API | mismo JSON snake_case |

                                En Swagger: **Authorize** → pegar el JWT (`Bearer` ya esta implicito). \
                                Spec OpenAPI JSON: `/v3/api-docs`.

                                | Recurso | URL |
                                |---|---|
                                | Swagger UI | `/swagger-ui.html` |
                                | OpenAPI JSON | `/v3/api-docs` |
                                | Local | `http://localhost:8080/swagger-ui.html` |
                                | OCI | `http://146.181.33.44:8080/swagger-ui.html` |

                                ## Endpoints
                                | Metodo | Ruta | Auth | Uso |
                                |---|---|---|---|
                                | POST | `/api/analisis` | JWT opcional | Clasificacion + negocio; JWT agrega `costos` |
                                | GET | `/api/historial` | JWT | Lista analisis guardados |
                                | GET | `/api/historial/{id}` | JWT | Detalle propio |
                                | POST | `/api/auth/registro` | No | Alta + JWT |
                                | POST | `/api/auth/login` | No | Login + JWT |
                                | POST | `/api/auth/oauth/google` | No | Canje `id_token` → JWT |
                                | POST | `/api/auth/oauth/facebook` | No | Canje `access_token` → JWT |
                                | POST | `/api/pruebas/onnx` | No | Smoke ONNX xgboost (sin negocio) |
                                | POST | `/api/pruebas/onnx-rf` | No | Legacy RF (pruebas) |

                                ## Casos de prueba QA (Nivel 2 — API)
                                Mismo set de 5 perfiles usado en notebook, API y plataforma. En **Try it out** \
                                elegir el ejemplo correspondiente. Consumo de negocio de referencia: 320 kWh.

                                | # | Nombre | Payload | Esperado |
                                |---|---|---|---|
                                | 1 | Eficiente claro | Depto, month 4, pico `no`, 1.0 h, 3 equipos | 200, categoria Eficiente |
                                | 2 | Ineficiente claro | Casa, month 7, pico `si`, 10.0 h, 22 equipos | 200, categoria Ineficiente |
                                | 3 | Frontera | Depto, month 3, pico `si`, 6.5 h, 8 equipos | 200, Moderado o Ineficiente |
                                | 4 | Limite valido | Depto, month 1, pico `no`, 0.0 h, 0 equipos | 200, Eficiente; sin NaN |
                                | 5 | Invalido | `Oficina`, month 13, 30.0 h | **400** con `fieldErrors` por campo |

                                El caso 5 no clasifica: valida y responde un desglose en espanol \
                                (`message` resume; `fieldErrors` detalla cada campo y el valor recibido).

                                ## Modelo
                                Inferencia con `modelo_xgboost_v2.onnx` (`xgboost-v2`, tensor `float_input` [1×22]). \
                                Contrato de factura en **snake_case** alineado a DS. \
                                Rollback: `APP_MODELO_ONNX_RUTA=classpath:model/modelo_xgboost.onnx`.

                                ## Recomendaciones (reglas / hibrido / Gemini)
                                `APP_RECOMENDACIONES_MODO`:
                                - `reglas` — set cerrado, deterministico
                                - `hibrido` (default) — las reglas eligen temas; Gemini reformula la frase patron
                                - `gemini` — reformula el mismo set; si falta `GEMINI_API_KEY` o hay timeout, fallback a reglas

                                Con JWT, Gemini recibe ademas las cifras de `costos` e `historial_resumen`.

                                ## Costos: invitado vs modo historial
                                `costo_estimado_mensual` es **siempre** `consumo_mensual × tarifa`. Con **JWT valido** \
                                la respuesta agrega:

                                | Bloque | Contenido |
                                |---|---|
                                | `costos` | Recargo por estacion + recargos accionables, costo ajustado, ahorro, proyeccion y `benchmark` |
                                | `historial_resumen` | Promedios previos, variacion consumo/costo, comparativa de estacion |

                                Parametros en `parametros_costos.json` (`APP_COSTOS_RUTA`). Default de umbrales: \
                                `metricas_final`. Rollback: `APP_COSTOS_UMBRALES=parametros`.

                                ## Validacion estricta (factura)
                                **Obligatorios:** `consumo_mensual`, `uso_horario_pico`, `cantidad_equipos`, \
                                `tipo_inmueble`, `horas_alto_consumo`, `month`.

                                - `consumo_mensual`: **entero** JSON, rango **80–1200** kWh (sin decimales ni `1E3`).
                                - `uso_horario_pico`: exactamente `"si"` \\| `"no"` (string; no boolean).
                                - `month`: entero 1–12 o nombre del mes (sin float/cientifica).
                                - `tipo_inmueble`: Casa \\| Departamento \\| Monoambiente (case-insensitive, sin padding).
                                - `tiene_*`: solo booleanos JSON `true`/`false`.
                                - `estacion_anio`: opcional/legado (la estacion se infiere desde `month`).

                                ### Forma de los errores 400
                                `fieldErrors` trae **un item por campo invalido**, clave en snake_case JSON y valor \
                                recibido entre parentesis; `message` resume cuantos y cuales fallaron. Todo en espanol.

                                Errores de **tipo/formato** (string donde va entero, `1E3`, boolean como `"true"`) \
                                cortan la lectura del JSON y se reportan de a uno. Errores de **dominio** (rangos, \
                                allowlists) se acumulan y salen todos juntos — como el caso QA #5.
                                """)
                        .contact(new Contact()
                                .name("Equipo Backend — ONE G9 / EnergiAI"))
                        .license(new License().name("MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local"),
                        new Server().url("http://146.181.33.44:8080").description("OCI (VM)")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT de POST /api/auth/login | /registro | /oauth/google | /oauth/facebook"))
                        .addResponses("BadRequest", new ApiResponse()
                                .description("JSON invalido o validacion de campos (400). Caso QA #5: varios fieldErrors.")
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().example(errorValidacion))))
                        .addResponses("Unauthorized", new ApiResponse()
                                .description("JWT ausente o invalido (401)"))
                        .addExamples("AnalisisMinimo", ejemplo(
                                "Minimo (invitado)",
                                "Solo campos obligatorios; guardar=false sin JWT.",
                                analisis(factura(320, "si", 8, "Departamento", 6.5, 3), false)))
                        .addExamples("AnalisisCompleto", ejemplo(
                                "Con opcionales (sin estacion_anio)",
                                "Payload preferido: estacion inferida desde month. Sin JWT = invitado.",
                                analisis(facturaCompleta(), false)))
                        .addExamples("AnalisisAutenticado", ejemplo(
                                "Registrado (JWT + guardar)",
                                "guardar=true. Requiere Authorize. Respuesta incluye costos e historial_resumen.",
                                analisis(facturaCompleta(), true)))
                        .addExamples("CasoQA1Eficiente", ejemplo(
                                "QA #1 — Eficiente claro",
                                "Departamento, abril, sin pico, 1 h, 3 equipos. Esperado: 200, categoria Eficiente.",
                                analisis(factura(320, "no", 3, "Departamento", 1.0, 4), false)))
                        .addExamples("CasoQA2Ineficiente", ejemplo(
                                "QA #2 — Ineficiente claro",
                                "Casa, julio, con pico, 10 h, 22 equipos. Esperado: 200, categoria Ineficiente.",
                                analisis(factura(320, "si", 22, "Casa", 10.0, 7), false)))
                        .addExamples("CasoQA3Frontera", ejemplo(
                                "QA #3 — Frontera",
                                "Departamento, marzo, con pico, 6.5 h, 8 equipos. Esperado: 200, Moderado o Ineficiente.",
                                analisis(factura(320, "si", 8, "Departamento", 6.5, 3), false)))
                        .addExamples("CasoQA4Limite", ejemplo(
                                "QA #4 — Limite valido (ceros)",
                                "Departamento, enero, sin pico, 0 h, 0 equipos. Esperado: 200, Eficiente, sin NaN.",
                                analisis(factura(320, "no", 0, "Departamento", 0.0, 1), false)))
                        .addExamples("CasoQA5Invalido", ejemplo(
                                "QA #5 — Invalido (400)",
                                "tipo_inmueble=Oficina, month=13, horas=30. Esperado: 400 con fieldErrors por campo.",
                                analisis(factura(320, "si", 8, "Oficina", 30.0, 13), false)))
                        .addExamples("CostosAutenticado", ejemplo(
                                "Bloque costos (JWT)",
                                "Fragmento de POST /api/analisis con JWT. fuente_umbrales=metricas_final.",
                                costosEjemplo()))
                        .addExamples("ErrorValidacionQA5", ejemplo(
                                "400 — desglose caso QA #5",
                                "Tres campos invalidos acumulados (dominio/rango), nombres snake_case y valor recibido.",
                                errorValidacion)));
    }

    private static Map<String, Object> factura(
            int consumo, String pico, int equipos, String tipo, double horas, int month) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("consumo_mensual", consumo);
        f.put("uso_horario_pico", pico);
        f.put("cantidad_equipos", equipos);
        f.put("tipo_inmueble", tipo);
        f.put("horas_alto_consumo", horas);
        f.put("month", month);
        return f;
    }

    private static Map<String, Object> facturaCompleta() {
        Map<String, Object> f = factura(320, "si", 8, "Departamento", 6.5, 3);
        f.put("numero_personas", 4);
        f.put("tiene_aire_acondicionado", true);
        f.put("tiene_calentador", false);
        f.put("tiene_iluminacion_led", true);
        f.put("antiguedad_electrodomesticos", "menor a 5 años");
        f.put("tarifa_electrica", 0.75);
        return f;
    }

    private static Map<String, Object> analisis(Map<String, Object> factura, boolean guardar) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("factura", factura);
        body.put("guardar", guardar);
        return body;
    }

    private static Map<String, Object> costosEjemplo() {
        Map<String, Object> benchmark = new LinkedHashMap<>();
        benchmark.put("tipo_inmueble", "Departamento");
        benchmark.put("estacion", "otoño");
        benchmark.put("umbral_eficiente_kwh", 217.0);
        benchmark.put("umbral_moderado_kwh", 355.7);
        benchmark.put("consumo_kwh", 320);
        benchmark.put("brecha_vs_eficiente_kwh", 103.0);
        benchmark.put("posicion_rango", "moderado");

        Map<String, Object> costos = new LinkedHashMap<>();
        costos.put("estacion", "otoño");
        costos.put("tarifa_aplicada", 0.75);
        costos.put("costo_bruto_mensual", 240.0);
        costos.put("pct_estacional", 0.0848);
        costos.put("costo_ajustado_mensual", 280.752);
        costos.put("ahorro_potencial_mensual", 36.0);
        costos.put("ahorro_potencial_anual", 432.0);
        costos.put("benchmark", benchmark);
        costos.put("parametros_version", "datasetup-final-final-v1");
        costos.put("fuente_umbrales", "metricas_final");
        return costos;
    }

    private static Map<String, Object> errorCasoQa5() {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("factura.horas_alto_consumo",
                "horas_alto_consumo no puede superar 24 (recibido: 30.0)");
        campos.put("factura.month",
                "month debe ser 1-12 o el nombre del mes (recibido: \"13\")");
        campos.put("factura.tipo_inmueble",
                "tipo_inmueble debe ser Casa, Departamento o Monoambiente (sin espacios) "
                        + "(recibido: \"Oficina\")");

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("timestamp", "2026-08-14T16:12:00Z");
        error.put("status", 400);
        error.put("error", "Bad Request");
        error.put("message", "Error de validacion en 3 campos: "
                + "factura.horas_alto_consumo, factura.month, factura.tipo_inmueble");
        error.put("path", "/api/analisis");
        error.put("fieldErrors", campos);
        return error;
    }

    private static Example ejemplo(String summary, String description, Object value) {
        return new Example().summary(summary).description(description).value(value);
    }
}
