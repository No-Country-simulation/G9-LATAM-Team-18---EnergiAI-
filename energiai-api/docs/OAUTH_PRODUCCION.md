# OAuth2 con Google — Configuración para Producción

Este documento explica cómo configurar OAuth2 con Google para EnergiAI en dos escenarios:
- **Producción**: Frontend en Netlify + Backend en OCI
- **Desarrollo local**: Frontend y backend en localhost

## Arquitectura Recomendada para Producción

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           FLUJO PRODUCCIÓN (API)                            │
│                                                                             │
│   Frontend (Netlify)              Google                  Backend (OCI)    │
│   https://energi-ai.netlify.app   ─────────              146.181.33.44:8080│
│                                                                             │
│   ┌─────────────┐                                                          │
│   │ 1. Usuario  │                                                          │
│   │    clic en  │                                                          │
│   │   "Google"  │                                                          │
│   └──────┬──────┘                                                          │
│          │                                                                  │
│          ▼                                                                  │
│   ┌─────────────┐     ┌─────────────────┐                                  │
│   │ 2. Google   │────►│ 3. Google OAuth │                                  │
│   │    Sign-In  │     │    consent      │                                  │
│   │    SDK      │◄────│    + id_token   │                                  │
│   └──────┬──────┘     └─────────────────┘                                  │
│          │                                                                  │
│          ▼                                                                  │
│   ┌─────────────┐     ┌─────────────────┐     ┌─────────────┐              │
│   │ 4. Frontend │────►│ 5. POST         │────►│ 6. Backend  │              │
│   │    tiene    │     │ /api/auth/oauth │     │    valida   │              │
│   │    id_token │     │ /google         │     │    id_token │              │
│   └─────────────┘     │ { token: "..." }│     │    → JWT    │              │
│                       └─────────────────┘     └──────┬──────┘              │
│                                                      │                      │
│                       ┌─────────────────┐            │                      │
│                       │ 7. Response     │◄───────────┘                      │
│                       │ { token, email, │                                  │
│                       │   nombre }      │                                  │
│                       └─────────────────┘                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

**¿Por qué API flow en vez de browser redirect?**

| Aspecto | Browser Redirect | API Flow (recomendado) |
|---------|-----------------|------------------------|
| CORS | Requiere configurar redirect URIs en Google Console apuntando al backend | Solo necesita JavaScript origins |
| Mixed content | Problemas si frontend HTTPS y backend HTTP | Funciona bien (la validación es server-side) |
| Complejidad | Sesiones HTTP, cookies, misma página | Stateless, JWT, diferentes dominios OK |
| UX | Redirect completo, página en blanco | Popup o one-tap, más fluido |

---

## Escenario A: Producción (Netlify + OCI)

### Datos del entorno

| Componente | URL |
|------------|-----|
| Frontend | `https://energi-ai.netlify.app` |
| Backend | `http://146.181.33.44:8080` |

### 1. Configurar Google Cloud Console

1. Ir a [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Seleccionar o crear un proyecto
3. Ir a **Credentials** → **Create Credentials** → **OAuth client ID**
4. Tipo: **Web application**
5. Nombre: `EnergiAI Producción`

#### Authorized JavaScript origins (obligatorio)

```
https://energi-ai.netlify.app
```

#### Authorized redirect URIs (solo si usás browser flow, opcional)

```
http://146.181.33.44:8080/login/oauth2/code/google
```

> **Nota**: Para el flujo API (recomendado), no necesitás redirect URIs. Solo JavaScript origins.

6. Copiar **Client ID** y **Client Secret**

### 2. Configurar el backend (OCI)

En el archivo `env.oci` del servidor (o `~/.profile`):

```bash
# CORS - incluir el frontend de Netlify
APP_CORS_ALLOWED_ORIGINS='https://energi-ai.netlify.app,http://localhost:3000,http://localhost:5173'

# OAuth (opcional para browser flow, el API flow funciona solo con GOOGLE_CLIENT_ID)
APP_OAUTH2_ENABLED=false  # true solo si querés el browser flow
GOOGLE_CLIENT_ID=tu-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=tu-client-secret
```

El endpoint `POST /api/auth/oauth/google` **siempre está disponible**, independientemente de `APP_OAUTH2_ENABLED`. Solo necesita `GOOGLE_CLIENT_ID` para validar el token.

### 3. Configurar el frontend (Netlify)

#### Opción A: Google Sign-In con `@react-oauth/google` (React)

```bash
npm install @react-oauth/google
```

```tsx
// src/App.tsx o similar
import { GoogleOAuthProvider, GoogleLogin } from '@react-oauth/google';

const GOOGLE_CLIENT_ID = 'tu-client-id.apps.googleusercontent.com';
const API_URL = 'http://146.181.33.44:8080';

function App() {
  return (
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <LoginButton />
    </GoogleOAuthProvider>
  );
}

function LoginButton() {
  const handleSuccess = async (credentialResponse) => {
    const idToken = credentialResponse.credential;
    
    // Canjear el id_token por JWT de EnergiAI
    const res = await fetch(`${API_URL}/api/auth/oauth/google`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token: idToken })
    });
    
    if (res.ok) {
      const { token, email, nombre } = await res.json();
      localStorage.setItem('energiai.token', token);
      localStorage.setItem('energiai.email', email);
      localStorage.setItem('energiai.nombre', nombre);
      // Redirigir o actualizar UI
    }
  };

  return (
    <GoogleLogin
      onSuccess={handleSuccess}
      onError={() => console.error('Login failed')}
    />
  );
}
```

#### Opción B: Google Identity Services (vanilla JS)

```html
<script src="https://accounts.google.com/gsi/client" async defer></script>

<div id="g_id_onload"
     data-client_id="tu-client-id.apps.googleusercontent.com"
     data-callback="handleGoogleLogin">
</div>
<div class="g_id_signin" data-type="standard"></div>

<script>
const API_URL = 'http://146.181.33.44:8080';

async function handleGoogleLogin(response) {
  const idToken = response.credential;
  
  const res = await fetch(`${API_URL}/api/auth/oauth/google`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token: idToken })
  });
  
  if (res.ok) {
    const { token, email, nombre } = await res.json();
    localStorage.setItem('energiai.token', token);
    localStorage.setItem('energiai.email', email);
    localStorage.setItem('energiai.nombre', nombre);
    location.reload();
  }
}
</script>
```

### 4. Variables de entorno en Netlify

En **Site settings** → **Environment variables**:

```
VITE_GOOGLE_CLIENT_ID=tu-client-id.apps.googleusercontent.com
VITE_API_URL=http://146.181.33.44:8080
```

En el código:

```ts
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;
const API_URL = import.meta.env.VITE_API_URL;
```

---

## Escenario B: Desarrollo Local

### Datos del entorno

| Componente | URL |
|------------|-----|
| Frontend | `http://localhost:3000` (CRA) o `http://localhost:5173` (Vite) |
| Backend | `http://localhost:8080` |

### 1. Google Cloud Console

Agregar a **Authorized JavaScript origins**:

```
http://localhost:3000
http://localhost:5173
```

Si usás browser flow, agregar a **Authorized redirect URIs**:

```
http://localhost:8080/login/oauth2/code/google
```

### 2. Configurar el backend local

Crear `~/.profile` o usar `.env`:

```bash
# CORS ya incluye localhost por defecto
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# OAuth
GOOGLE_CLIENT_ID=tu-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=tu-client-secret

# Si querés probar el browser flow
APP_OAUTH2_ENABLED=true
APP_OAUTH2_SUCCESS_REDIRECT=/oauth-callback.html
```

### 3. Ejecutar

```bash
# Backend
cd energiai-api
source ~/.profile  # o cargar .env
./mvnw spring-boot:run

# Frontend
cd energiai-frontend
npm run dev  # Vite en :5173
```

---

## Resumen: Qué configurar en Google Console

### Para producción (Netlify + OCI)

| Configuración | Valor |
|---------------|-------|
| **JavaScript origins** | `https://energi-ai.netlify.app` |
| **Redirect URIs** | (opcional) `http://146.181.33.44:8080/login/oauth2/code/google` |

### Para desarrollo local

| Configuración | Valor |
|---------------|-------|
| **JavaScript origins** | `http://localhost:3000`, `http://localhost:5173` |
| **Redirect URIs** | (opcional) `http://localhost:8080/login/oauth2/code/google` |

### Credenciales

Podés usar las **mismas credenciales** para ambos entornos agregando todos los origins a la misma OAuth client ID.

---

## Troubleshooting

### "redirect_uri_mismatch"

El redirect URI no coincide exactamente con el configurado en Google Console. Verificar:
- Protocolo (`http` vs `https`)
- Puerto (`:8080` vs sin puerto)
- Path exacto (`/login/oauth2/code/google`)

### "origin_mismatch" o "Not a valid origin"

El JavaScript origin no está autorizado. Agregar la URL exacta del frontend en Google Console.

### CORS error en el POST a `/api/auth/oauth/google`

Verificar que `APP_CORS_ALLOWED_ORIGINS` incluya el origen del frontend.

```bash
# En el servidor
echo $APP_CORS_ALLOWED_ORIGINS
# Debe incluir https://energi-ai.netlify.app
```

### "Token validation failed" o 401

- Verificar que `GOOGLE_CLIENT_ID` esté configurado en el backend
- El `id_token` puede haber expirado (tienen ~1 hora de validez)
- El `aud` (audience) del token debe coincidir con tu Client ID

### El botón de Google no aparece

- Verificar que el script de Google Identity Services esté cargado
- Verificar que el `data-client_id` sea correcto
- Revisar errores en la consola del navegador

---

## Endpoints Relevantes

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| `POST` | `/api/auth/oauth/google` | No | Canjea `id_token` de Google → JWT de EnergiAI |
| `POST` | `/api/auth/oauth/facebook` | No | Canjea `access_token` de Facebook → JWT |
| `GET` | `/api/auth/proveedores` | No | `{ google: true/false }` (indica si browser flow está habilitado) |
| `GET` | `/api/auth/sesion` | JWT | Confirma que el JWT es válido |

### Ejemplo de request

```bash
curl -X POST http://146.181.33.44:8080/api/auth/oauth/google \
  -H "Content-Type: application/json" \
  -d '{"token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."}'
```

### Ejemplo de response

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGdtYWlsLmNvbSIsImlhdCI6...",
  "tipo": "Bearer",
  "email": "user@gmail.com",
  "nombre": "Usuario Ejemplo"
}
```

---

## Checklist de Configuración

### Backend

- [ ] `APP_CORS_ALLOWED_ORIGINS` incluye `https://energi-ai.netlify.app`
- [ ] `GOOGLE_CLIENT_ID` configurado
- [ ] `GOOGLE_CLIENT_SECRET` configurado (solo para browser flow)
- [ ] `JWT_SECRET` configurado (mínimo 32 bytes)
- [ ] Firewall permite conexiones al puerto 8080

### Google Console

- [ ] JavaScript origins incluye URL del frontend
- [ ] (Opcional) Redirect URIs incluye URL del backend
- [ ] OAuth consent screen configurado
- [ ] App en modo "Testing" o publicada

### Frontend

- [ ] Variable de entorno con Client ID
- [ ] Variable de entorno con URL del API
- [ ] Google Sign-In SDK integrado
- [ ] Handler que canjea id_token por JWT
