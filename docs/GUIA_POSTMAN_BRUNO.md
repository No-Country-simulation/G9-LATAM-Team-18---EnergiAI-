# Guía Postman / Bruno — flujos de la API EnergiAI

Cómo ejecutar **todos** los endpoints desde Postman o Bruno: modo invitado,
registro local, JWT, historial y OAuth.

Base URL por defecto: `http://localhost:8080`

Swagger interactivo: `http://localhost:8080/swagger-ui.html`

Collection Bruno versionada: carpeta [`bruno/`](../bruno/) (abrirla con **Open Collection**).
Si se oculta o se borra el panel, los `.bru` siguen en el repo.

---

## Mapa de acceso

```
PUBLICO (sin JWT)                         PROTEGIDO (JWT obligatorio)
-----------------------------             --------------------------------
POST /api/auth/registro                   GET  /api/historial
POST /api/auth/login                      GET  /api/historial/{id}
POST /api/auth/oauth/google
POST /api/auth/oauth/facebook
POST /api/analisis   (guardar=false)      POST /api/analisis  (guardar=true)
POST /api/pruebas/onnx
POST /api/pruebas/onnx-rf
GET  /swagger-ui.html
GET  /oauth2/authorization/{google|facebook}   ← solo si APP_OAUTH2_ENABLED=true
```

---

## Flujo general (ASCII)

```
                    +------------------+
                    |  Cliente Bruno / |
                    |  Postman / SPA   |
                    +--------+---------+
                             |
         +-------------------+-------------------+
         |                                       |
         v                                       v
 +---------------+                     +------------------+
 | MODO INVITADO |                     | MODO AUTENTICADO |
 | sin JWT       |                     | con JWT          |
 +-------+-------+                     +--------+---------+
         |                                       |
         | POST /api/analisis                    | 1) registro | login | oauth
         | guardar=false                         | 2) Authorization: Bearer <jwt>
         |                                       | 3) analisis guardar=true
         v                                       | 4) GET /api/historial[/{id}]
  Resultado en el acto                           v
  (NO se guarda)                          Historial en PostgreSQL
```

---

## 0. Variables de colección (Bruno / Postman)

Crear estas variables en la colección:

| Variable     | Ejemplo                         | Uso                          |
|--------------|---------------------------------|------------------------------|
| `baseUrl`    | `http://localhost:8080`         | Prefijo de todas las URLs    |
| `token`      | *(vacío al inicio)*             | JWT tras login/registro/oauth|
| `email`      | `demo@energiai.test`            | Registro / login             |
| `password`   | `secreto123`                    | Registro / login (≥ 8 chars) |

Header reutilizable cuando haga falta auth:

```
Authorization: Bearer {{token}}
Content-Type: application/json
```

En Bruno: Auth → Bearer Token → `{{token}}`.  
En Postman: Authorization → Bearer Token → `{{token}}`.

---

## 1. Modo invitado — consulta individual por factura

**No requiere login.** Ideal para probar el modelo ONNX sin tocar la BD.

```
[Bruno/Postman]
      |
      |  POST {{baseUrl}}/api/analisis
      |  (sin Authorization)
      |  body: factura + guardar=false
      v
[AnalisisController] --> [ONNX / negocio]
      |
      v
{ categoria, costo, iie, recomendaciones, guardado:false }
```

### Request

`POST {{baseUrl}}/api/analisis`

```json
{
  "factura": {
    "consumo_mensual": 320,
    "uso_horario_pico": "si",
    "cantidad_equipos": 8,
    "tipo_inmueble": "Departamento",
    "horas_alto_consumo": 6.5,
    "month": 3,
    "numero_personas": 4,
    "tarifa_electrica": 0.75
  },
  "guardar": false
}
```

> Contrato DS (snake_case): `tipo_inmueble`, `month`, `uso_horario_pico` (`si`|`no`), `horas_alto_consumo`, `cantidad_equipos`.
> `consumo_mensual` es de negocio (**entero 80–1200 kWh**; no entra al tensor ONNX). La respuesta incluye `consulta_modelo`, `features_sinteticas` y `vector_onnx`. `estacion_anio` es opcional/legado (la estación de negocio se infiere desde `month`).
> Modelo por defecto: **`modelo_xgboost_v2.onnx`** (`modeloVersion`: `xgboost-v2`). Detalle de features: `docs/json-campos-rangos.md`.

### Esperado

- `200 OK`
- `guardado: false`, `analisisId: null`
- Sin JWT → **no** se escribe en `analisis` / historial

Si enviás `"guardar": true` **sin** JWT → `401` JSON:
`Para guardar el analisis debes iniciar sesion (JWT)`.

---

## 2. Registro local (persiste usuario en PostgreSQL)

```
POST /api/auth/registro
        |
        v
  UsuarioService.registrar
  (BCrypt + auth_provider=LOCAL)
        |
        v
  fila en tabla usuario
        |
        v
  AuthResponse { token, tipo:Bearer, email, nombre }
        |
        v
  Guardar token en {{token}}
```

### Request

`POST {{baseUrl}}/api/auth/registro`

```json
{
  "email": "demo@energiai.test",
  "password": "secreto123",
  "nombre": "Demo EnergiAI"
}
```

### Esperado

- `201 Created`
- Copiá `token` → variable `{{token}}`
- Email duplicado → `409 Conflict`

### Script sugerido (Postman Tests / Bruno post-response)

```js
const body = res.getBody(); // Bruno; en Postman: pm.response.json()
if (body.token) {
  bru.setVar("token", body.token); // Bruno
  // pm.collectionVariables.set("token", body.token); // Postman
}
```

---

## 3. Login local

```
POST /api/auth/login  -->  JWT  -->  {{token}}
```

`POST {{baseUrl}}/api/auth/login`

```json
{
  "email": "demo@energiai.test",
  "password": "secreto123"
}
```

- `200 OK` + mismo shape que registro  
- Credenciales inválidas o cuenta solo-OAuth → `409`

---

## 4. Análisis autenticado (guardar en historial)

```
Authorization: Bearer {{token}}
        |
        v
POST /api/analisis  { guardar: true }
        |
        +--> clasifica + calcula negocio
        |
        +--> INSERT analisis (usuario_id del JWT)
        |
        v
{ guardado:true, analisisId: <id>, ... }
```

Mismo body que el modo invitado, cambiando:

```json
"guardar": true
```

Header obligatorio:

```
Authorization: Bearer {{token}}
```

### Extra del modo autenticado: `costos` y `historial_resumen`

Con JWT válido (incluso con `guardar: false`) la respuesta trae dos bloques que en modo invitado
no existen:

```json
"costos": {
  "estacion": "invierno",
  "costo_bruto_mensual": 240.0,
  "pct_estacional": 0.1,
  "pct_horario_pico": 0.15,
  "pct_ajuste_total": 0.25,
  "costo_ajustado_mensual": 300.0,
  "ahorro_potencial_mensual": 36.0,
  "ahorro_potencial_anual": 432.0,
  "proyeccion_estacional": [{ "estacion": "verano", "costo_mensual_estimado": 292.66 }],
  "benchmark": {
    "tipo_inmueble": "Departamento",
    "estacion": "invierno",
    "umbral_eficiente_kwh": 249.3,
    "umbral_moderado_kwh": 365.7,
    "posicion_rango": "moderado"
  },
  "parametros_version": "datasetup-final-final-v1",
  "fuente_umbrales": "metricas_final"
},
"historial_resumen": {
  "analisis_previos": 4,
  "consumo_promedio_kwh": 300.0,
  "variacion_consumo_pct": 0.0667,
  "tendencia": "al_alza"
}
```

`costo_estimado_mensual` no cambia: sigue siendo `consumo_mensual × tarifa` en los dos modos.
`fuente_umbrales` vale `"metricas_final"` (hoja metricas_final). Rollback: `APP_COSTOS_UMBRALES=parametros`.
Detalle de las fórmulas en [`costos-estacionales.md`](costos-estacionales.md).

---

## 5. Historial (solo con JWT)

```
GET /api/historial          --> lista del usuario del token
GET /api/historial/{id}     --> un analisis propio
        |
        X sin JWT / token invalido --> 401 JSON
        X id de otro usuario       --> 404
```

### Listado

`GET {{baseUrl}}/api/historial`  
Auth: Bearer `{{token}}`

### Detalle

`GET {{baseUrl}}/api/historial/1`  
Auth: Bearer `{{token}}`

El `usuarioId` **nunca** se manda en la URL ni en el body: sale del JWT.

---

## 6. OAuth — dos caminos

### 6.A Canje API (recomendado para Postman/Bruno)

Obtenés el token del proveedor en el cliente (Google Sign-In / FB SDK) y lo canjeás:

```
[Google / Facebook SDK]
        |  id_token  o  access_token
        v
POST /api/auth/oauth/google     body: { "token": "<id_token>" }
POST /api/auth/oauth/facebook   body: { "token": "<access_token>" }
        |
        v
Verifica con Google tokeninfo / Graph API
        |
        v
Upsert usuario (auth_provider=GOOGLE|FACEBOOK) en BD
        |
        v
JWT EnergiAI  -->  {{token}}
```

#### Google

`POST {{baseUrl}}/api/auth/oauth/google`

```json
{
  "token": "eyJhbGciOiJSUzI1NiIs...id_token_de_google"
}
```

Si `GOOGLE_CLIENT_ID` está definido, se valida el `aud` del id_token.

#### Facebook

`POST {{baseUrl}}/api/auth/oauth/facebook`

```json
{
  "token": "EAAGm0PX...access_token"
}
```

La app de Facebook debe pedir permiso `email`.

### 6.B Browser redirect (SPA)

Requiere env:

```
APP_OAUTH2_ENABLED=true
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
APP_OAUTH2_SUCCESS_REDIRECT=http://localhost:5173/oauth/callback
```

Redirect URI en la consola del proveedor:

```
http://localhost:8080/login/oauth2/code/google
http://localhost:8080/login/oauth2/code/facebook
```

```
Browser --> GET /oauth2/authorization/google
              |
              v
         Google login
              |
              v
    /login/oauth2/code/google
              |
              v
    SuccessHandler: upsert user + JWT
              |
              v
    Redirect --> {success-redirect}?token=...&email=...
```

En Bruno/Postman el flujo browser es incómodo (cookies + redirects); preferí **6.A**.

---

## 7. Endpoints de prueba ONNX (públicos)

### 7.1 Misma Factura que producción

`POST {{baseUrl}}/api/pruebas/onnx`

```json
{
  "factura": {
    "consumoMensual": 320,
    "usoHorarioPico": true,
    "cantidadEquipos": 8,
    "tipoInmueble": "departamento",
    "horasPromedioUso": 4.5
  },
  "guardar": false
}
```

(Inferencia sola / smoke; no usa historial.)

### 7.2 Legacy RF 10 features

`POST {{baseUrl}}/api/pruebas/onnx-rf`

```json
{
  "param1": 1.0,
  "param2": 0.0,
  "param3": 2.0,
  "param4": 1.0,
  "param5": 3.0,
  "param6": 0.0,
  "param7": 1.0,
  "param8": 0.0,
  "param9": 2.0,
  "param10": 1.0
}
```

---

## 8. Matriz rápida de pruebas

| # | Acción                         | Auth      | Resultado esperado      |
|---|--------------------------------|-----------|-------------------------|
| 1 | Analisis invitado              | No        | 200, `guardado:false`   |
| 2 | Analisis `guardar:true`        | No        | **401**                 |
| 3 | Registro                       | No        | 201 + token             |
| 4 | Login                          | No        | 200 + token             |
| 5 | Analisis `guardar:true`        | Bearer    | 200, `guardado:true`    |
| 6 | GET historial                  | Bearer    | 200 lista               |
| 7 | GET historial/{id}             | Bearer    | 200 item                |
| 8 | GET historial                  | No        | **401** JSON            |
| 9 | OAuth google/facebook          | token IdP | 200 + JWT               |
| 10 | Analisis invitado              | No        | sin `costos` ni `historial_resumen` |
| 11 | Analisis `guardar:false`       | Bearer    | 200 con `costos` + `historial_resumen` |

---

## 9. Errores típicos

```
Sin JWT en /api/historial
  --> 401 { "message": "Debes iniciar sesion (JWT)..." }

guardar=true sin JWT
  --> 401 { "message": "Para guardar el analisis debes iniciar sesion (JWT)" }

JSON mal formado / sin Content-Type
  --> 400

Factura inválida (tipo_inmueble fuera del enum, horas fuera de 0–24, month fuera de 1–12...)
  --> 400 + fieldErrors: un item por campo, en snake_case y con el valor recibido

Valor de tipo incorrecto (string donde va entero, 1E3, "true" en vez de true)
  --> 400 con UN solo campo: la lectura del JSON se corta ahí (el message lo aclara)

Email ya registrado
  --> 409
```

---

## 10. Checklist Bruno (orden sugerido)

```
1. POST auth/registro          → guardar {{token}}
2. POST analisis (invitado)    → sin header Auth, guardar=false
3. POST analisis (auth)        → Bearer {{token}}, guardar=true  → anotar analisisId
4. GET  historial              → Bearer {{token}}
5. GET  historial/{{analisisId}}
6. GET  historial              → sin Auth → confirmar 401
7. (opcional) POST auth/oauth/google con id_token real
```
