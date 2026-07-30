# EnergiAI API

Backend del proyecto **EnergiAI** (Hackathon ONE G9 - Alura + Oracle): analiza el
consumo eléctrico de un inmueble, clasifica el perfil de eficiencia
(**Eficiente / Moderado / Ineficiente**), estima el costo mensual y genera
recomendaciones, exponiendo todo vía API REST.

> Estado: **esqueleto**. Las clases y contratos están definidos; la lógica de
> negocio fina (recomendaciones avanzadas, OAuth2, endurecimiento de seguridad)
> se completa en las siguientes iteraciones.

## Stack

- Java 21, Spring Boot 3.3
- Spring Web, Validation, Data JPA, Security (+ OAuth2 client listo)
- PostgreSQL 16 (driver `org.postgresql`)
- JWT (jjwt 0.12.x)
- springdoc-openapi (Swagger UI)
- Maven · Docker (imágenes multiarch amd64/arm64 para la VM A1.Flex de OCI)

## Decisión de arquitectura clave: ¿dónde corre el modelo?

El modelo se compila a **ONNX** y corre en el **frontend** (ONNX Runtime Web).
Por eso el backend es un **sistema de registro**, no un motor de inferencia:

1. El frontend clasifica con ONNX y envía la factura + el resultado.
2. El backend **valida**, calcula **negocio** (costo, índice, recomendaciones) y
   **persiste** de forma opcional.
3. Si el resultado del frontend no viene, el backend usa un **fallback en Java**
   (softmax leyendo `modelo_energiai.json`) detrás del puerto `ClasificadorPort`.

```mermaid
flowchart LR
    subgraph FE[Frontend SPA]
      ONNX[ONNX Runtime Web<br/>clasifica en el navegador]
    end
    subgraph BE[energiai-api - Spring Boot]
      Ctrl[Controllers REST]
      Port[[ClasificadorPort]]
      Local[ClasificacionServiceLocal<br/>softmax fallback]
      Neg[Costo / Indice / Recomendaciones]
      Sec[JWT filter - hoy permitAll]
      JPA[(Spring Data JPA)]
    end
    DB[(PostgreSQL 16<br/>Docker en VM OCI Compute)]
    Model[/modelo_energiai.json/]

    ONNX -->|POST /api/analisis| Ctrl
    Ctrl --> Neg
    Ctrl -.sin resultado.-> Port --> Local --> Model
    Ctrl --> Sec
    Ctrl --> JPA --> DB
```

## Árbol de clases (qué programar / extender)

```
com.energiai.energiaiapi
├── EnergiaiApiApplication
├── config
│   ├── SecurityConfig         # permitAll ahora; endurecer con JWT/OAuth2 luego
│   ├── CorsConfig
│   └── OpenApiConfig
├── controller
│   ├── AnalisisController     # POST /api/analisis
│   ├── HistorialController    # GET  /api/historial   (requiere JWT)
│   └── AuthController         # POST /api/auth/registro | /api/auth/login
├── dto
│   ├── FacturaDTO             # 5 obligatorios + 7 opcionales (Bean Validation)
│   ├── ResultadoModeloDTO     # clasificación que envía el frontend (ONNX)
│   ├── AnalisisRequest / AnalisisResponse
│   ├── HistorialItemResponse
│   ├── RegistroRequest / LoginRequest / AuthResponse
│   └── ErrorResponse
├── domain
│   ├── Usuario / Factura / Analisis         # entidades JPA
│   └── enums (CategoriaEficiencia, AuthProvider)
├── repository
│   ├── UsuarioRepository
│   └── AnalisisRepository
├── service
│   ├── AnalisisService        # orquesta clasificación + negocio + persistencia
│   ├── CostoService           # costo mensual + índice IIE
│   ├── RecomendacionService   # reglas de recomendación
│   ├── HistorialService
│   ├── UsuarioService         # registro/login + UserDetailsService
│   └── inference
│       ├── ClasificadorPort           # puerto (estrategia de inferencia)
│       ├── ClasificacionServiceLocal  # fallback softmax en Java
│       └── ModeloParametros           # carga modelo_energiai.json
├── security
│   ├── JwtService
│   └── JwtAuthenticationFilter         # no bloqueante: puebla contexto si hay token
└── exception
    ├── GlobalExceptionHandler
    ├── RecursoNoEncontradoException
    └── ReglaNegocioException
```

## Endpoints

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/api/analisis` | Opcional | Analiza una factura. Guarda si `guardar=true` y hay sesión |
| GET | `/api/historial` | JWT | Historial del usuario autenticado |
| POST | `/api/auth/registro` | No | Registro (email + password) → JWT |
| POST | `/api/auth/login` | No | Login → JWT |
| GET | `/swagger-ui.html` | No | Documentación interactiva |

### Ejemplo `POST /api/analisis`

```json
{
  "factura": {
    "consumoKwh": 320,
    "usoHorarioPico": true,
    "cantidadEquipos": 8,
    "tipoInmueble": "Casa",
    "horasAltoConsumo": 4.5,
    "areaInmueble": 85.0,
    "numeroPersonas": 4,
    "tieneIluminacionLed": false,
    "tarifaElectrica": 0.75
  },
  "resultado": {
    "categoria": "Moderado",
    "probabilidades": {"Eficiente": 0.3, "Moderado": 0.5, "Ineficiente": 0.2}
  },
  "guardar": false
}
```

## Cómo correr

### Local con Docker (app + PostgreSQL)

```bash
cp .env.example .env   # completar credenciales
docker compose up --build
# API:     http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
```

### Local con Maven (contra la BD dev remota en OCI)

```bash
export SPRING_DATASOURCE_PASSWORD='...'   # ver BD_DATOS.txt (no versionar)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

> Requiere **JDK 21**. La VM de OCI ya lo instala (`setup_vm.sh`).

### Tests (usa H2, no requiere PostgreSQL)

```bash
mvn test
```

## Perfiles

| Perfil | BD | `ddl-auto` | Uso |
|---|---|---|---|
| `dev` | PostgreSQL remoto (OCI `146.181.33.44`) | `update` | Desarrollo |
| `prod` | PostgreSQL local (VM) | `validate` | Producción en la VM |
| `test` | H2 en memoria | `create-drop` | Tests |

## Migraciones (Flyway)

Flyway es el **dueño del esquema** (Hibernate corre con `ddl-auto: none`), lo que da
trazabilidad completa desde el arranque: cada cambio de BD es un script versionado en
`src/main/resources/db/migration/` y queda registrado en la tabla `flyway_schema_history`.

- `V1__init_schema.sql`: crea `usuario`, `factura`, `analisis`, `analisis_recomendacion`.
- Config: `baseline-on-migrate=true`, `baseline-version=0` (permite aplicar V1 sobre la
  BD de OCI que ya tiene objetos previos).
- Para un cambio nuevo: agregar `V2__descripcion.sql` (nunca editar un script ya aplicado).

## Pendiente (próximas iteraciones)

- Endurecer `SecurityConfig` (exigir JWT en `/api/historial`).
- Activar OAuth2 (Google/Facebook) — dependencia ya incluida.
- Cargar los coeficientes reales del modelo cuando Ciencia de Datos los entregue.
