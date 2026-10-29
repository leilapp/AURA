#!/usr/bin/env bash
# Instala SOLO los componentes de línea de comandos del Android SDK necesarios para compilar Aura.
# Pensado para GitHub Codespaces (Linux). No instala emuladores ni Android Studio (~1 GB en total).
# Uso:  bash scripts/setup-android-sdk.sh
set -euo pipefail

SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-11076708_latest.zip"   # cmdline-tools 12.0
PACKAGES=("platforms;android-35" "build-tools;34.0.0" "platform-tools")

echo "== Comprobando Java =="
if ! command -v java >/dev/null 2>&1; then
  echo "No se encontró Java. En Codespaces usa el devcontainer del repositorio (incluye Java 17)." >&2
  exit 1
fi
java -version 2>&1 | head -1

SDKMANAGER="$SDK_DIR/cmdline-tools/latest/bin/sdkmanager"
if [ -x "$SDKMANAGER" ]; then
  echo "== Herramientas de línea de comandos ya instaladas en $SDK_DIR =="
else
  echo "== Descargando cmdline-tools en $SDK_DIR =="
  mkdir -p "$SDK_DIR/cmdline-tools"
  tmp="$(mktemp -d)"
  curl -fsSL -o "$tmp/tools.zip" "https://dl.google.com/android/repository/$CMDLINE_TOOLS_ZIP"
  unzip -q "$tmp/tools.zip" -d "$tmp"
  rm -rf "$SDK_DIR/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
  rm -rf "$tmp"
fi

missing=()
for p in "${PACKAGES[@]}"; do
  dir="$SDK_DIR/${p//;//}"
  [ -d "$dir" ] || missing+=("$p")
done

if [ ${#missing[@]} -gt 0 ]; then
  echo "== Instalando: ${missing[*]} =="
  yes | "$SDKMANAGER" --sdk_root="$SDK_DIR" --licenses >/dev/null || true
  "$SDKMANAGER" --sdk_root="$SDK_DIR" "${missing[@]}"
else
  echo "== Paquetes del SDK ya presentes =="
fi

# Gradle lee la ruta del SDK desde local.properties (archivo ignorado por git).
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
echo "sdk.dir=$SDK_DIR" > "$ROOT/local.properties"
echo "== Listo. SDK en $SDK_DIR =="
echo "Compila con:  ./gradlew assembleDebug"
