#!/usr/bin/env python3
"""Escribe setup-env-windows.generated.ps1 a partir del entorno actual (ya cargado)."""
from __future__ import annotations

import os
from pathlib import Path


def esc(s: str) -> str:
    return s.replace("`", "``").replace('"', '`"').replace("$", "`$")


def emit(name: str, value: str | None) -> str:
    if not value:
        return ""
    return f'Set-UserEnv "{name}" "{esc(value)}"\n'


def main() -> None:
    out = Path(os.environ["OUT"])
    names = [
        "ENERGIAI_DB_HOST", "ENERGIAI_DB_PORT", "ENERGIAI_DB_NAME",
        "POSTGRES_DB", "POSTGRES_USER", "POSTGRES_PASSWORD",
        "SPRING_PROFILES_ACTIVE", "SPRING_DATASOURCE_URL",
        "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD",
        "JWT_SECRET", "JWT_EXPIRATION_MS", "APP_CORS_ALLOWED_ORIGINS",
        "APP_OAUTH2_ENABLED", "APP_OAUTH2_SUCCESS_REDIRECT",
        "GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET",
        "FACEBOOK_CLIENT_ID", "FACEBOOK_CLIENT_SECRET",
        "APP_RECOMENDACIONES_MODO", "APP_RECOMENDACIONES_MAX_ITEMS",
        "APP_RECOMENDACIONES_GEMINI_MODELO", "APP_RECOMENDACIONES_GEMINI_TIMEOUT_MS",
        "GEMINI_API_KEY", "GOOGLE_GENERATIVE_AI_API_KEY",
        "APP_MODELO_ESTRATEGIA",
    ]

    lines = [
        "#Requires -Version 5.1",
        "# AUTOGENERADO desde ~/.profile — NO versionar (contiene secretos).",
        '$ErrorActionPreference = "Stop"',
        "function Set-UserEnv([string]$Name, [string]$Value) {",
        '  [Environment]::SetEnvironmentVariable($Name, $Value, "User")',
        '  Set-Item -Path "Env:$Name" -Value $Value',
        "}",
        'Write-Host "Aplicando variables EnergiAI (modo automatico)..." -ForegroundColor Cyan',
    ]
    included: list[str] = []
    for n in names:
        chunk = emit(n, os.environ.get(n))
        if chunk:
            lines.append(chunk.rstrip("\n"))
            included.append(n)

    lines += [
        'Write-Host "OK. Cierra y reabre la terminal / Cursor / IDE." -ForegroundColor Green',
        'Write-Host "Prueba:  echo `$env:SPRING_DATASOURCE_URL"',
    ]

    out.write_text("\n".join(lines) + "\n", encoding="utf-8-sig")
    os.chmod(out, 0o600)
    print(f"Generado: {out}")
    print("Variables incluidas:")
    for n in included:
        print(f"  - {n}")
    print()
    print("Siguiente:")
    print("  1) Copia de forma privada al PC Windows (NO al git):")
    print("       scripts/setup-env-windows.generated.ps1")
    print("  2) En Windows: doble clic en setup-env-windows.cmd")


if __name__ == "__main__":
    main()
