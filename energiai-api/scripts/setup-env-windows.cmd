@echo off
REM Aplica variables EnergiAI en Windows.
REM Preferido: primero genera en Linux ./scripts/generate-windows-env-from-profile.sh
REM y copia setup-env-windows.generated.ps1 junto a este .cmd

cd /d "%~dp0"
echo.
echo EnergiAI - variables de entorno (Windows)
echo.

if exist "%~dp0setup-env-windows.generated.ps1" (
  echo Usando setup-env-windows.generated.ps1 ^(automatico desde .profile^)
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-env-windows.generated.ps1"
) else (
  echo No hay .generated.ps1 — abriendo wrapper...
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-env-windows.ps1" %*
)

if errorlevel 1 (
  echo.
  echo Error. Si falta el generated, en Linux corre:
  echo   ./scripts/generate-windows-env-from-profile.sh
  echo y copia el archivo .generated.ps1 a esta carpeta.
  pause
  exit /b 1
)

echo.
pause
