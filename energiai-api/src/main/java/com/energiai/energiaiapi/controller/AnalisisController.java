package com.energiai.energiaiapi.controller;

import com.energiai.energiaiapi.dto.AnalisisRequest;
import com.energiai.energiaiapi.dto.AnalisisResponse;
import com.energiai.energiaiapi.dto.ErrorResponse;
import com.energiai.energiaiapi.security.SecurityUtils;
import com.energiai.energiaiapi.service.AnalisisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analisis")
@Tag(name = "Analisis", description = "Clasificacion + negocio (+ eco ONNX)")
public class AnalisisController {

    private final AnalisisService analisisService;

    public AnalisisController(AnalisisService analisisService) {
        this.analisisService = analisisService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Analiza consumo electrico (invitado o autenticado)",
            description = """
                    Clasifica con `modelo_xgboost_v2.onnx` (o usa `resultado` del FE si viene), \
                    calcula costo/IIE/recomendaciones y devuelve eco DS (`consulta_modelo`, \
                    `features_sinteticas`, `vector_onnx`).

                    - **Invitado:** sin JWT y `guardar=false`. `costo_estimado_mensual` = consumo × tarifa.
                    - **Con JWT:** agrega `costos` (estacionalidad, recargos, ahorro potencial, \
                      proyeccion por estacion y `benchmark` informativo de `metricas_final`) y \
                      `historial_resumen`; las recomendaciones se matizan con esas cifras. \
                      Rollback de umbrales: `APP_COSTOS_UMBRALES=parametros`.
                    - **Persistir:** `guardar=true` + `Authorization: Bearer <JWT>`.
                    - **Factura:** `consumo_mensual` entero 80–1200; `tiene_*` solo booleanos JSON; \
                      `estacion_anio` opcional (estacion de negocio desde `month`).
                    - **QA:** ejemplos CasoQA1…CasoQA5 (el #5 responde 400 con `fieldErrors` por campo).
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analisis OK. Invitado: sin bloques costos/historial_resumen. "
                    + "Con JWT: incluye costos (benchmark metricas_final, fuente_umbrales=metricas_final) e historial_resumen. "
                    + "Fragmento de costos: example CostosAutenticado. QA #1–#4: 200.",
                    content = @Content(schema = @Schema(implementation = AnalisisResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validacion / JSON estricto. QA #5: varios fieldErrors.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "CasoQA5", ref = "#/components/examples/ErrorValidacionQA5"))),
            @ApiResponse(responseCode = "401", description = "guardar=true sin JWT valido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AnalisisResponse> analizar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Factura DS + flags",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AnalisisRequest.class),
                            examples = {
                                    @ExampleObject(name = "MinimoInvitado", ref = "#/components/examples/AnalisisMinimo"),
                                    @ExampleObject(name = "Completo", ref = "#/components/examples/AnalisisCompleto"),
                                    @ExampleObject(name = "Autenticado", ref = "#/components/examples/AnalisisAutenticado"),
                                    @ExampleObject(name = "QA1Eficiente", ref = "#/components/examples/CasoQA1Eficiente"),
                                    @ExampleObject(name = "QA2Ineficiente", ref = "#/components/examples/CasoQA2Ineficiente"),
                                    @ExampleObject(name = "QA3Frontera", ref = "#/components/examples/CasoQA3Frontera"),
                                    @ExampleObject(name = "QA4Limite", ref = "#/components/examples/CasoQA4Limite"),
                                    @ExampleObject(name = "QA5Invalido", ref = "#/components/examples/CasoQA5Invalido")
                            }))
            @Valid @RequestBody AnalisisRequest request,
            Authentication authentication) {
        String email = SecurityUtils.emailDe(authentication).orElse(null);
        return ResponseEntity.ok(analisisService.analizar(request, email));
    }
}
