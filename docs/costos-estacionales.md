# Costos con estacionalidad (modo historial)

Origen: `Datasetup FINAL FINAL.xlsx`, hoja **Parametros** (columnas Q..AJ) y hoja
**metricas_final** (umbrales confirmados del v2). `Parametros!B10:E28` queda como rollback
(`APP_COSTOS_UMBRALES=parametros`). Este documento es la trazabilidad celda → campo JSON.

## Alcance

| Modo | `costo_estimado_mensual` | Bloque `costos` | Bloque `historial_resumen` |
|---|---|---|---|
| Invitado (sin JWT) | `consumo_mensual × tarifa` | ausente | ausente |
| Registrado (con JWT) | `consumo_mensual × tarifa` (igual) | presente | presente |

El eje interno de la aplicación no cambia: la categoría de eficiencia sigue saliendo
exclusivamente del pipeline `JSON → modelo_xgboost_v2.onnx → JSON`. Todo lo de este documento
es post-procesamiento de negocio sobre la factura ya canonicalizada.

## Parámetros

Externalizados en `src/main/resources/model/parametros_costos.json`
(ruta configurable con `APP_COSTOS_RUTA`). Versión actual: `datasetup-final-final-v1`.

Umbrales del `benchmark`:

| `APP_COSTOS_UMBRALES` | Origen | Uso |
|---|---|---|
| `metricas_final` (default) | `metricas_final` (coincide con `metricas!I4:L24`) | Etiquetado confirmado de entrenamiento del v2 |
| `parametros` | `Parametros!B10:E28` | Rollback a los umbrales originales |

El switch no requiere recompilar ni tocar recargos. El campo JSON `fuente_umbrales` indica cuál
juego está activo. Ambos juegos viajan en el mismo archivo para que el rollback no pueda
desincronizar los porcentajes Q..AJ.

### Recargo estacional

Deriva de los consumos de referencia de la planilla (`Parametros!B30:E34`):

`pct_estacional = (consumo_estacion / consumo_invierno) / base_aumento`, con `base_aumento = 10`.

| Estación | Consumo referencia (kWh) | Recargo |
|---|---|---|
| Primavera | 358.8 | 8.56% |
| Verano | 298.3 | 7.11% |
| Otoño | 355.7 | 8.48% |
| Invierno | 419.3 | 10.00% |

La suma de los cuatro (`0.3415`, celda `D36`) se usa para el anual estacionalizado.

### Recargos accionables ("llave 15%", celda `K2`)

| Condición | Campo del request | Recargo |
|---|---|---|
| Usa energía en horario pico | `uso_horario_pico = "si"` | 15% |
| No tiene iluminación LED | `tiene_iluminacion_led = false` | 15% |
| Electrodomésticos de más de 5 años | `antiguedad_electrodomesticos` ∈ {`menor a 10 años`, `mayor a 10 años`} | 15% |

La planilla usa tres buckets de antigüedad y penaliza `Mayor a >5 años`. El contrato de la API
tiene cuatro buckets, así que el recargo aplica a los dos que superan los 5 años.

`tiene_aire_acondicionado` y `tiene_calentador` **no** generan recargo: entran al modelo y a las
recomendaciones, pero la planilla no los usa en el costo.

## Trazabilidad celda → campo

| Celda | Fórmula | Campo JSON |
|---|---|---|
| Q | `tarifa × consumo` | `costo_bruto_mensual` |
| R | lookup estacional | `pct_estacional` |
| S | `R × Q` | `monto_estacional` |
| T | `si uso_horario_pico` | `pct_horario_pico` |
| U | `T × Q` | `monto_horario_pico` |
| V | `si no hay LED` | `pct_sin_led` |
| W | `V × Q` | `monto_sin_led` |
| X | `si antigüedad > 5 años` | `pct_antiguedad` |
| Y | `X × Q` | `monto_antiguedad` |
| AA | `R + T + V + X` | `pct_ajuste_total` |
| AE | `Q × (1 + AA)` | `costo_ajustado_mensual` |
| Z | `Q × 12` | `costo_anual_bruto` |
| AF | `Z × (1 + AA)` | `costo_anual_estimado` |
| AG | `T + V + X` | `pct_ahorro_potencial` |
| AH | `Q × AG` | `ahorro_potencial_mensual` |
| AJ | `Z × AG` | `ahorro_potencial_anual` |

Las celdas `AB`, `AC`, `AD` e `AI` de la planilla son intermedias: `AC` se reduce a `R` y `AJ`
ya expresa el resultado final, así que no se replican.

### Campos que no vienen de una celda

- `costo_anual_estacionalizado`: suma de la proyección por estación tomando 3 meses cada una,
  es decir `Q × (12 + 3 × 0.3415 + 12 × pct_accionables)`. Es más realista que `AF`, que aplica
  el recargo de una sola estación a los doce meses. Se exponen los dos.
- `proyeccion_estacional`: costo mensual estimado en las cuatro estaciones manteniendo consumo
  y tarifa. Responde "cuánto voy a pagar cuando cambie la estación".
- `benchmark`: rango de consumo esperado para el tipo de inmueble en esa estación. **Default:
  hoja `metricas_final`**. Rollback a Parametros con `APP_COSTOS_UMBRALES=parametros`.
  **Es informativo**: no clasifica; la categoría la define el ONNX. En `metricas_final` Casa
  queda más holgada que Departamento; en Parametros ambos tipos compartían umbrales.

## Verificación

`CalculadoraCostosEstacionalesTest` replica las filas 4, 5 y 6 de la planilla:

| Fila | Entrada | `costo_ajustado_mensual` esperado |
|---|---|---|
| 4 | 250 kWh, verano, sin LED | 228.95625 |
| 5 | 150 kWh, otoño, sin recargos accionables | 122.04 |
| 6 | 3.2 kWh, invierno, pico + equipos antiguos | 3.36 |

## Persistencia

La migración `V5__costos_estacionales.sql` agrega el desglose a la tabla `analisis`, incluyendo
`parametros_costos_version`. El historial devuelve la foto guardada y no recalcula: si mañana
cambian los recargos, los análisis viejos siguen siendo auditables con la versión que los produjo.

## Recomendaciones matizadas

Cuando hay JWT, `ReglasRecomendacion` agrega temas con cifras concretas (variación contra el
historial, ahorro potencial en USD, estación más cara) y el prompt de Gemini recibe un bloque de
contexto con esas cifras pre-formateadas. El primer tema contextual encabeza la lista para que
sobreviva al recorte por `app.recomendaciones.max-items`. Gemini solo puede citar las cifras
provistas: la regla de "no inventar datos" sigue vigente.

## Discrepancias detectadas en la planilla

- `Parametros!C2:D2` sigue declarando el rango de consumo 80–1500 kWh; el contrato vigente es
  80–1200. La API valida 80–1200.
- Los umbrales de clasificación aparecen tres veces: `Parametros!B10:E28` (originales),
  `metricas!I4:L24` (recalibrados) y `metricas_final` (copia confirmada de los recalibrados).
  El default es `metricas_final`. El recargo Q..AJ no depende de esta elección.
