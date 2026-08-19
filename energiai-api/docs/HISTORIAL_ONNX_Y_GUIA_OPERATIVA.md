# EnergiAI API — Historial ONNX y guía operativa

Documento generado a partir de la adhesión de la dependencia **ONNX Runtime (Microsoft)** en adelante.  
Incluye historial de cambios, variables de entorno, endpoints con ejemplos JSON, y comandos de build/ejecución.

> Base URL local: `http://localhost:8080`  
> Swagger UI: `http://localhost:8080/swagger-ui.html`  
> Header común en POST con body: `Content-Type: application/json`

---

## 1. Historial de modificaciones (desde ONNX Runtime)

### 1.1. Dependencia ONNX Runtime (Microsoft)

- Se agregó al `pom.xml`:
  - `com.microsoft.onnxruntime:onnxruntime`
  - Versión usada: **1.28.0** (última estable en Maven Central al momento de la integración; se pidió 1.20.0 y se actualizó).
- Propiedad Maven: `onnxruntime.version`.

**Objetivo:** permitir inferencia en Java con modelos `.onnx` sin runtime de Python.

### 1.2. Primer modelo de prueba (`modelo_rf.onnx`)

- Artefacto colocado junto al `pom.xml` y copiado a `src/main/resources/model/modelo_rf.onnx`.
- Código original de prueba (`IA.java` / `EnergiA` + `UnGranPerfil`) se reorganizó en el paquete:
  - `com.energiai.energiaiapi.onnx.*`
- Endpoint de prueba inicial: `POST /api/pruebas/onnx` con `param1`…`param10` (vector de 10 floats).
- Contrato del modelo RF legacy:
  - Input: `float_input` `[1, 10]`
  - Outputs: `label` (int64 0/1) + `probabilities` `[1, 2]`

### 1.3. Logging de requests HTTP (perfil `dev`)

- Bean `CommonsRequestLoggingFilter` (`RequestLoggingConfig`).
- En `application-dev.yml`: nivel `DEBUG` para el filtro.
- Muestra en consola método, URI, cliente y payload JSON.

### 1.4. Modelo de producción de clasificación (`version2.0.onnx`)

- Nuevo artefacto: `version2.0.onnx` (junto al `pom` y en `src/main/resources/model/`).
- Inspección del modelo:
  - Input: `float_input` **`[batch, 6]`** (float32)
  - Outputs: `output_label` (string: `eficiente` / `moderado` / `ineficiente`) + `output_probability` (ZipMap)
- Arquitectura nueva:
  - `FacturaFeatureEncoder` — Factura JSON → `float[6]`
  - `PerfilFacturaV2` — pre/postprocesamiento (incluye parseo de `OnnxMap`)
  - `ClasificadorOnnxAdapter` — implementación default de `ClasificadorPort`
  - `ClasificacionServiceLocal` — fallback softmax JSON solo si `app.modelo.estrategia=local`
- `POST /api/analisis` clasifica con ONNX v2 cuando no viene `resultado` del cliente.
- `fuenteClasificacion`: `BACKEND_ONNX` | `FRONTEND_ONNX` | `BACKEND_FALLBACK`
- `modeloVersion`: `2.0` (configurable)

**Encoding interno (6 features):**

| Índice | Feature | Origen | ¿Imputable? |
|---:|---|---|---|
| 0 | consumoMensual | obligatorio API + modelo | No |
| 1 | usoHorarioPico (0/1) | obligatorio API + modelo | No |
| 2 | cantidadEquipos | obligatorio API + modelo | No |
| 3 | tipoInmueble enc | **obligatorio API + modelo** (`monoambiente=0`, `departamento=1`, `casa=2`) | **No** |
| 4 | horasPromedioUso | obligatorio API + modelo (1–24) | No |
| 5 | estacionAnio enc | opcional API (`primavera=0`…`invierno=3`) | Sí |

### 1.5. Endpoints de prueba reorganizados

- `POST /api/pruebas/onnx` — ahora usa el **mismo body** que `/api/analisis` (`{ "factura": {...}, "guardar": false }`). Solo inferencia; no persiste ni calcula costo/recomendaciones.
- `POST /api/pruebas/onnx-rf` — legacy con `modelo_rf.onnx` y `param1`…`param10`.

### 1.6. Validación de `horasPromedioUso`

- Rango obligatorio: **mínimo 1, máximo 24** (`@DecimalMin` / `@DecimalMax` en `FacturaDTO`).
- Misma regla en la consola CLI.

### 1.7. `tipoInmueble` obligatorio para el modelo ONNX (refactor)

`tipoInmueble` ya era obligatorio en la API (`@NotBlank` + valores permitidos). Se reforzó
el contrato de punta a punta para que también lo sea **en el pipeline del modelo**:

- **`FacturaDTO`**: documentado y marcado en OpenAPI como `requiredMode = REQUIRED`
  (junto con los otros 4 obligatorios). Mensaje de validación: `"tipoInmueble es obligatorio"`.
- **`FacturaFeatureEncoder`**: deja de aplicar un fallback silencioso a `casa` si el valor
  falta o es inválido. Ahora usa `codificarObligatorio(...)`:
  - si `tipoInmueble` es null/blank → `IllegalArgumentException`
  - si no es `monoambiente|departamento|casa` → `IllegalArgumentException`
  - si no hay encoding en el contrato JSON → `IllegalArgumentException`
- **`ClasificacionServiceLocal`** (estrategia `local`): también rechaza `tipoInmueble`
  ausente/inválido en lugar de imputar con la media del scaler.
- **Test**: `FacturaFeatureEncoderTest.rechazaTipoInmuebleAusentePorqueEsObligatorioParaElModelo`.

Valores válidos (sin imputación): `monoambiente` (0), `departamento` (1), `casa` (2).

### 1.8. Campo `mes` en Factura (API + persistencia)

Se agregó `mes` como variable opcional del contrato HTTP y de la tabla `factura`:

- **Flyway** `V3__agregar_mes_factura.sql`: columna `mes VARCHAR(255)`.
- **`FacturaDTO` / entidad `Factura` / historial / consola CLI**: mismo patrón que `estacionAnio`.
- **Validación** (`@ValorPermitido` + enum `Mes`):
  `enero` | `febrero` | `marzo` | `abril` | `mayo` | `junio` |
  `julio` | `agosto` | `septiembre` | `octubre` | `noviembre` | `diciembre`.
- No forma parte del vector ONNX v2 (sigue siendo 6 features); se usa para contexto/persistencia.

Contrato HTTP actualizado: **Factura 5+8**.

### 1.9. Variables de conexión a BD externalizadas

- En `~/.profile` (bloque ENERGIAI):
  - `ENERGIAI_DB_HOST`, `ENERGIAI_DB_PORT`, `ENERGIAI_DB_NAME`
  - `SPRING_DATASOURCE_URL` compuesto a partir de esas partes
- Se eliminó la IP hardcodeada de los YAML `dev`/`cli` como fuente de verdad (la URL viene del entorno).

### 1.10. Clases / rutas clave del flujo ONNX v2

| Pieza | Paquete / ruta |
|---|---|
| Adapter (carga + `clasificar`) | `com.energiai.energiaiapi.service.inference.ClasificadorOnnxAdapter` |
| Runtime ONNX (`createSession` / `run`) | `com.energiai.energiaiapi.onnx.EnergiA` |
| Perfil v2 | `com.energiai.energiaiapi.onnx.PerfilFacturaV2` |
| Encoder | `com.energiai.energiaiapi.onnx.FacturaFeatureEncoder` |
| Modelo | `classpath:model/version2.0.onnx` → propiedad `app.modelo.onnx-ruta` |
| Atributo de ruta en config | `app.modelo.onnx-ruta` / env `APP_MODELO_ONNX_RUTA` |
| Resource inyectado | parámetro `recursoOnnx` en el constructor del adapter |
| Path/bytes en runtime | `EnergiA.direccionIA` (file) o `EnergiA.modeloBytes` (classpath) |

---

## 2. Instrucciones — variables de entorno

### 2.1. Obligatorias / recomendadas (bloque `~/.profile`)

Tras editar: `source ~/.profile`

| Variable | Descripción | Ejemplo |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil Spring | `dev` |
| `ENERGIAI_DB_HOST` | Host PostgreSQL | `146.181.33.44` |
| `ENERGIAI_DB_PORT` | Puerto PostgreSQL | `5432` |
| `ENERGIAI_DB_NAME` | Nombre de la base | `miapp` |
| `SPRING_DATASOURCE_URL` | JDBC completo | `jdbc:postgresql://${ENERGIAI_DB_HOST}:${ENERGIAI_DB_PORT}/${ENERGIAI_DB_NAME}` |
| `SPRING_DATASOURCE_USERNAME` | Usuario BD | `oracleone18` |
| `SPRING_DATASOURCE_PASSWORD` | Password BD | *(secreto)* |
| `JWT_SECRET` | Secreto JWT (≥ 32 bytes) | *(secreto largo)* |
| `JAVA_HOME` | JDK 21 | `/usr/lib64/jvm/java-21-openjdk-21` |

### 2.2. Adicionales relacionadas con el modelo ONNX (opcionales)

| Variable | Descripción | Default |
|---|---|---|
| `APP_MODELO_ESTRATEGIA` | `onnx` (default) o `local` (softmax JSON) | `onnx` |
| `APP_MODELO_ONNX_RUTA` | Ruta del modelo v2 | `classpath:model/version2.0.onnx` |
| `APP_MODELO_ONNX_VERSION` | Versión reportada en responses | `2.0` |
| `APP_MODELO_ONNX_RF_RUTA` | Modelo RF legacy (pruebas) | `classpath:model/modelo_rf.onnx` |
| `APP_MODELO_RUTA` | JSON de encodings / tarifa / softmax local | `classpath:model/modelo_energiai.json` |
| `JWT_EXPIRATION_MS` | Expiración del token | `86400000` (24 h) |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes CORS | `http://localhost:5173,http://localhost:3000` |
| `SERVER_PORT` | Puerto HTTP | `8080` |

### 2.3. Fragmento sugerido para `~/.profile`

```bash
# #########################################################################
# ##   ENERGIAI - VARIABLES SENSIBLES                                    ##
# #########################################################################
export SPRING_PROFILES_ACTIVE=dev

export ENERGIAI_DB_HOST="146.181.33.44"
export ENERGIAI_DB_PORT="5432"
export ENERGIAI_DB_NAME="miapp"
export SPRING_DATASOURCE_URL="jdbc:postgresql://${ENERGIAI_DB_HOST}:${ENERGIAI_DB_PORT}/${ENERGIAI_DB_NAME}"
export SPRING_DATASOURCE_USERNAME="oracleone18"
export SPRING_DATASOURCE_PASSWORD="***"

export JWT_SECRET="***secreto-de-al-menos-32-bytes***"

export JAVA_HOME="/usr/lib64/jvm/java-21-openjdk-21"
export PATH="$JAVA_HOME/bin:$PATH"

# Opcionales ONNX
# export APP_MODELO_ESTRATEGIA=onnx
# export APP_MODELO_ONNX_RUTA=classpath:model/version2.0.onnx
# ##################### FIN ENERGIAI ######################################
```

---

## 3. Endpoints — detalle completo

### Resumen

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/api/auth/registro` | No | Alta de usuario + JWT |
| POST | `/api/auth/login` | No | Login + JWT |
| POST | `/api/analisis` | Opcional | Análisis de eficiencia (ONNX + negocio + persistencia opcional) |
| GET | `/api/historial` | JWT recomendado | Historial del usuario autenticado |
| POST | `/api/pruebas/onnx` | No | Solo inferencia ONNX v2 (mismo body que análisis) |
| POST | `/api/pruebas/onnx-rf` | No | Inferencia legacy RF 10 features |
| GET | `/swagger-ui.html` | No | Documentación interactiva |

---

### 3.1. `POST /api/auth/registro`

**Request**
```json
{
  "email": "daniel@energiai.com",
  "password": "claveSegura123",
  "nombre": "Daniel"
}
```

**Response `201 Created`**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tipo": "Bearer",
  "email": "daniel@energiai.com",
  "nombre": "Daniel"
}
```

**Errores frecuentes**
- `400` — body ausente / JSON mal formado / validación
- `409` — email ya registrado

---

### 3.2. `POST /api/auth/login`

**Request**
```json
{
  "email": "daniel@energiai.com",
  "password": "claveSegura123"
}
```

**Response `200 OK`**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tipo": "Bearer",
  "email": "daniel@energiai.com",
  "nombre": "Daniel"
}
```

Usar el token así en requests protegidas:
```http
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

---

### 3.3. `POST /api/analisis`

Clasifica con `version2.0.onnx` (si no viene `resultado`), calcula costo / IIE / recomendaciones, y puede persistir.

#### Campos de `factura`

**Obligatorios (5)** — también bloqueantes para `version2.0.onnx`
- `consumoMensual` (float, > 0)
- `usoHorarioPico` (boolean)
- `cantidadEquipos` (int, > 0)
- `tipoInmueble` (string, **obligatorio del modelo**): `monoambiente` | `departamento` | `casa` (no se imputa)
- `horasPromedioUso` (float): **1 a 24**

**Opcionales (8)**
- `estacionAnio`: `primavera` | `verano` | `otoño` | `invierno`
- `mes`: `enero` | `febrero` | `marzo` | `abril` | `mayo` | `junio` | `julio` | `agosto` | `septiembre` | `octubre` | `noviembre` | `diciembre`
- `numeroPersonas` (int, > 0)
- `tieneAireAcondicionado` (boolean)
- `tieneCalentador` (boolean)
- `tieneIluminacionLed` (boolean)
- `antiguedadElectrodomesticos`: `menor a 3 años` | `menor a 5 años` | `menor a 10 años` | `mayor a 10 años`
- `tarifaElectrica` (float, ≥ 0)

#### Request mínimo
```json
{
  "factura": {
    "consumoMensual": 350,
    "usoHorarioPico": true,
    "cantidadEquipos": 8,
    "tipoInmueble": "departamento",
    "horasPromedioUso": 6
  },
  "guardar": false
}
```

#### Request completo
```json
{
  "factura": {
    "consumoMensual": 350,
    "usoHorarioPico": true,
    "cantidadEquipos": 8,
    "tipoInmueble": "departamento",
    "horasPromedioUso": 6,
    "estacionAnio": "verano",
    "mes": "enero",
    "numeroPersonas": 4,
    "tieneAireAcondicionado": true,
    "tieneCalentador": false,
    "tieneIluminacionLed": true,
    "antiguedadElectrodomesticos": "menor a 5 años",
    "tarifaElectrica": 0.75
  },
  "resultado": {
    "categoria": "Moderado",
    "probabilidades": {
      "Eficiente": 0.12,
      "Ineficiente": 0.36,
      "Moderado": 0.52
    }
  },
  "guardar": true
}
```

> Si mandás `guardar: true`, incluí header `Authorization: Bearer <token>`.  
> Si omitís `resultado`, clasifica el backend con ONNX (`fuenteClasificacion: BACKEND_ONNX`).

#### Response `200 OK` (ejemplo)
```json
{
  "categoria": "MODERADO",
  "probabilidades": {
    "Eficiente": 0.12,
    "Ineficiente": 0.36,
    "Moderado": 0.52
  },
  "costoEstimadoMensual": 262.5,
  "indiceEficiencia": 87.5,
  "recomendaciones": [
    "Evita el uso de equipos de alto consumo en horario pico para reducir el costo.",
    "En verano, configura el aire acondicionado en 24 grados y mantene los filtros limpios."
  ],
  "modeloVersion": "2.0",
  "fuenteClasificacion": "BACKEND_ONNX",
  "guardado": false,
  "analisisId": null,
  "creadoEn": "2026-07-31T21:00:00.000Z"
}
```

---

### 3.4. `GET /api/historial`

**Headers**
```http
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response `200 OK`**
```json
[
  {
    "id": 1,
    "creadoEn": "2026-07-31T21:00:00.000Z",
    "categoria": "MODERADO",
    "probabilidad": 0.52,
    "costoEstimadoMensual": 262.5,
    "indiceEficiencia": 87.5,
    "consumoMensual": 350.0,
    "tipoInmueble": "departamento",
    "recomendaciones": [
      "Evita el uso de equipos de alto consumo en horario pico para reducir el costo."
    ]
  }
]
```

Sin token válido → `401`.

---

### 3.5. `POST /api/pruebas/onnx`

Mismo body que `/api/analisis`. Ignora `guardar` y `resultado`. Solo inferencia ONNX v2.

**Request**
```json
{
  "factura": {
    "consumoMensual": 350,
    "usoHorarioPico": true,
    "cantidadEquipos": 8,
    "tipoInmueble": "departamento",
    "horasPromedioUso": 6,
    "estacionAnio": "verano",
    "mes": "enero",
    "numeroPersonas": 4,
    "tieneIluminacionLed": true,
    "tarifaElectrica": 0.75
  },
  "guardar": false
}
```

**Response `200 OK`**
```json
{
  "categoria": "MODERADO",
  "probabilidades": {
    "Eficiente": 0.12,
    "Ineficiente": 0.36,
    "Moderado": 0.52
  },
  "modeloVersion": "2.0",
  "fuenteClasificacion": "BACKEND_ONNX",
  "vectorEnviado": [350.0, 1.0, 8.0, 1.0, 6.0, 1.0]
}
```

`vectorEnviado` es el `float[6]` que realmente entra a `version2.0.onnx`.

---

### 3.6. `POST /api/pruebas/onnx-rf` (legacy)

**Request**
```json
{
  "param1": 0,
  "param2": 0,
  "param3": 0,
  "param4": 0,
  "param5": 0,
  "param6": 2.33,
  "param7": 70.90,
  "param8": 1,
  "param9": 1,
  "param10": 1
}
```

**Response `200 OK`**
```json
{
  "prediccion": 1,
  "probabilidades": [0.303, 0.697],
  "modelo": "class path resource [model/modelo_rf.onnx]"
}
```

---

### 3.7. Errores comunes

**Body ausente / mal formado → `400`**
```json
{
  "timestamp": "2026-07-31T17:57:17.800Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Cuerpo de la solicitud ausente o JSON mal formado. Verifica que envies un body JSON y el header 'Content-Type: application/json'.",
  "path": "/api/analisis",
  "fieldErrors": null
}
```

**Validación de campos → `400`**

`fieldErrors` trae un item por campo inválido, con la clave en snake_case (igual que el JSON
enviado) y el valor recibido entre paréntesis. `message` resume cuántos y cuáles fallaron.

```json
{
  "timestamp": "2026-07-31T17:57:17.800Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Error de validacion en 3 campos: factura.horas_alto_consumo, factura.month, factura.tipo_inmueble",
  "path": "/api/analisis",
  "fieldErrors": {
    "factura.horas_alto_consumo": "horas_alto_consumo no puede superar 24 (recibido: 30.0)",
    "factura.month": "month debe ser 1-12 o el nombre del mes (recibido: \"13\")",
    "factura.tipo_inmueble": "tipo_inmueble debe ser Casa, Departamento o Monoambiente (sin espacios) (recibido: \"Oficina\")"
  }
}
```

**Error de tipo / formato → `400` (un campo por vez)**

Cuando el valor no se puede ni leer con el tipo esperado (un string donde va un entero, `1E3`,
`"true"` en lugar de `true`), Jackson corta la lectura del JSON en ese campo y los demás quedan
sin evaluar. El `message` lo aclara para que no parezca que el resto del payload está bien.

```json
{
  "timestamp": "2026-07-31T17:57:17.800Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Error de formato en factura.consumo_mensual: la lectura del JSON se detuvo en ese campo. Corrige el formato y reenvia para validar los campos restantes.",
  "path": "/api/analisis",
  "fieldErrors": {
    "factura.consumo_mensual": "factura.consumo_mensual: se espera entero JSON (sin parte decimal ni notacion cientifica) (recibido: 320.5)"
  }
}
```

---

## 4. Build y ejecución

Requiere **JDK 21** y variables de entorno cargadas (`source ~/.profile`).

### 4.1. Generar el ejecutable (JAR)

Desde el directorio del proyecto `energiai-api/`:

```bash
export JAVA_HOME="/usr/lib64/jvm/java-21-openjdk-21"
export PATH="$JAVA_HOME/bin:$PATH"

cd "/home/dani/Documentos/Eficiencia Energética/energiai-api"
mvn -DskipTests package
```

Artefacto generado:

```text
target/energiai-api-0.0.1-SNAPSHOT.jar
```

Con tests:

```bash
mvn package
```

### 4.2. Ejecutar la API (perfil web / `dev`)

```bash
source ~/.profile
cd "/home/dani/Documentos/Eficiencia Energética/energiai-api"
java -jar target/energiai-api-0.0.1-SNAPSHOT.jar
```

O forzando perfil:

```bash
java -jar target/energiai-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

API en: `http://localhost:8080`  
Swagger: `http://localhost:8080/swagger-ui.html`

### 4.3. Ejecutar la consola CLI (menú interactivo)

```bash
source ~/.profile
java -jar target/energiai-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=cli
```

### 4.4. Verificación rápida post-arranque

```bash
curl -s -X POST http://localhost:8080/api/pruebas/onnx \
  -H "Content-Type: application/json" \
  -d '{
    "factura": {
      "consumoMensual": 350,
      "usoHorarioPico": true,
      "cantidadEquipos": 8,
      "tipoInmueble": "departamento",
      "horasPromedioUso": 6,
      "estacionAnio": "verano"
    },
    "guardar": false
  }'
```

---

## 5. Notas finales

- El contrato HTTP de eficiencia es **Factura 5+8**; el modelo ONNX v2 consume internamente **6 features** codificadas.
- De esos 5 obligatorios, **`tipoInmueble` es requisito del modelo**: sin él (o con valor inválido) la API responde `400` y el encoder ONNX no clasifica.
- `modelo_rf.onnx` (10 features, binario) queda solo en `/api/pruebas/onnx-rf`.
- Para producción en la VM OCI, usar perfil `prod` y `SPRING_DATASOURCE_URL` apuntando a `localhost` si la BD corre en la misma máquina.
- No versionar secretos (password BD, JWT) en el repositorio; mantenerlos en `~/.profile` o un gestor de secretos.
