package com.energiai.energiaiapi.controller;

import com.energiai.energiaiapi.dto.AnalisisRequest;
import com.energiai.energiaiapi.dto.OnnxPruebaResponse;
import com.energiai.energiaiapi.service.OnnxPruebaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de pruebas para version2.0.onnx.
 * Acepta el mismo body que {@code POST /api/analisis} ({@code factura} + {@code guardar} opcional).
 * Ignora {@code guardar} y {@code resultado}: solo corre inferencia ONNX.
 */
@RestController
@RequestMapping("/api/pruebas/onnx")
@ConditionalOnBean(OnnxPruebaService.class)
@Tag(name = "Pruebas ONNX", description = "Inferencia aislada con version2.0.onnx (mismo body que /api/analisis)")
public class OnnxPruebaController {

    private final OnnxPruebaService onnxPruebaService;

    public OnnxPruebaController(OnnxPruebaService onnxPruebaService) {
        this.onnxPruebaService = onnxPruebaService;
    }

    @PostMapping
    @Operation(summary = "Prueba de clasificacion ONNX v2",
            description = "Mismo JSON que /api/analisis: { \"factura\": { ... }, \"guardar\": false }. "
                    + "Codifica 6 features y consulta version2.0.onnx. "
                    + "No persiste ni calcula costo/recomendaciones.")
    public ResponseEntity<OnnxPruebaResponse> predecir(@Valid @RequestBody AnalisisRequest request) {
        return ResponseEntity.ok(onnxPruebaService.predecir(request.factura()));
    }
}
