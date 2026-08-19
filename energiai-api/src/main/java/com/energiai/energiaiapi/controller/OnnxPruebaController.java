package com.energiai.energiaiapi.controller;

import com.energiai.energiaiapi.dto.AnalisisRequest;
import com.energiai.energiaiapi.dto.ErrorResponse;
import com.energiai.energiaiapi.dto.OnnxPruebaResponse;
import com.energiai.energiaiapi.service.OnnxPruebaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de pruebas para {@code modelo_xgboost_v2.onnx}.
 * Acepta el mismo body que {@code POST /api/analisis}.
 * Ignora {@code guardar} y {@code resultado}: solo corre inferencia ONNX.
 */
@RestController
@RequestMapping("/api/pruebas/onnx")
@ConditionalOnBean(OnnxPruebaService.class)
@Tag(name = "Pruebas ONNX", description = "Inferencia aislada xgboost-v2 (mismo body que /api/analisis)")
public class OnnxPruebaController {

    private final OnnxPruebaService onnxPruebaService;

    public OnnxPruebaController(OnnxPruebaService onnxPruebaService) {
        this.onnxPruebaService = onnxPruebaService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Prueba de clasificacion ONNX (xgboost-v2)",
            description = """
                    Mismo JSON que `/api/analisis`: `{ "factura": { ... }, "guardar": false }`.
                    Codifica el vector float[22] y consulta `modelo_xgboost_v2.onnx`.
                    No persiste ni calcula costo/recomendaciones.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inferencia OK",
                    content = @Content(schema = @Schema(implementation = OnnxPruebaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validacion / JSON estricto",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OnnxPruebaResponse> predecir(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AnalisisRequest.class),
                            examples = {
                                    @ExampleObject(name = "Minimo", ref = "#/components/examples/AnalisisMinimo")
                            }))
            @Valid @RequestBody AnalisisRequest request) {
        return ResponseEntity.ok(onnxPruebaService.predecir(request.factura()));
    }
}
