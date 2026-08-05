package com.energiai.energiaiapi.controller;

import com.energiai.energiaiapi.dto.OnnxRfLegacyRequest;
import com.energiai.energiaiapi.dto.OnnxRfLegacyResponse;
import com.energiai.energiaiapi.service.OnnxRfLegacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pruebas/onnx-rf")
@Tag(name = "Pruebas ONNX RF legacy", description = "modelo_rf.onnx — 10 features, salida binaria 0/1")
public class OnnxRfLegacyController {

    private final OnnxRfLegacyService service;

    public OnnxRfLegacyController(OnnxRfLegacyService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Inferencia legacy con modelo_rf.onnx (param1..param10)")
    public ResponseEntity<OnnxRfLegacyResponse> predecir(@Valid @RequestBody OnnxRfLegacyRequest request) {
        return ResponseEntity.ok(service.predecir(request));
    }
}
