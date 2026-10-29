# Aura · asistente personal de organización para Android

Aura es una app Android nativa (Kotlin + Jetpack Compose + Material 3) para organizar el día:
tareas, agenda, rutinas, compras, pagos, clima con sugerencias de vestimenta y, con prioridad,
las actividades y compromisos de tu hijo. Funciona sin cuenta, sin servidor y sin internet
(salvo el clima). **No usa inteligencia artificial**: todo se basa en reglas, filtros y cálculos.

---

## 1. Obtener el APK sin instalar nada (recomendado)

Cada vez que se sube código a GitHub, el flujo **«Compilar APK de Aura»** (GitHub Actions)
ejecuta las pruebas y genera el APK de depuración en los servidores de GitHub.

1. En GitHub abre la pestaña **Actions** del repositorio.
2. Entra en la ejecución más reciente con ✓ verde.
3. Al final de la página, en **Artifacts**, descarga **aura-debug-apk** (es un `.zip`).
4. Descomprímelo: dentro está `app-debug.apk`.

También puedes lanzar una compilación a mano: **Actions → Compilar APK de Aura → Run workflow**.
Los APK se conservan 14 días para no ocupar espacio.

## 2. Instalar el APK en tu teléfono

1. Pasa `app-debug.apk` al teléfono (descárgalo directamente desde GitHub en el navegador del
   teléfono, o envíatelo por correo / Drive / cable).
2. Ábrelo desde la app **Archivos** o la notificación de descarga.
3. Android pedirá permitir «Instalar apps desconocidas» para esa app (navegador o Archivos).
   Actívalo solo para esa instalación.
4. Pulsa **Instalar**. Si Play Protect avisa de que la app es desconocida, elige
   «Instalar de todas formas»: es normal en apps de prueba que no vienen de Play Store.

**Actualizar:** instala el nuevo APK encima del anterior. Tus datos se conservan porque todas
las compilaciones de depuración se firman con la misma clave de prueba (`app/debug.keystore`).
Si alguna vez Android dice que el paquete «entra en conflicto», desinstala la versión anterior
(esto **borra** los datos guardados en el teléfono).

Requisitos: Android 8.0 (API 26) o superior.

## 3. Compilar desde GitHub Codespaces (sin Android Studio)

1. En GitHub: **Code → Codespaces → Create codespace on main** (o en tu rama).
2. El codespace usa `.devcontainer/devcontainer.json`: Java 17 y, al crearse, ejecuta
   `scripts/setup-android-sdk.sh`, que instala **solo** lo necesario del Android SDK
   (cmdline-tools, `platforms;android-35`, `build-tools;34.0.0`, `platform-tools`).
   No se descargan emuladores. Si ya están instalados, el script no descarga nada.
3. En la terminal del codespace:

   ```bash
   ./gradlew testDebugUnitTest   # pruebas de la lógica (conflictos, rutinas, clima, importes)
   ./gradlew assembleDebug       # genera el APK
   ```

4. El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. En el explorador de archivos de
   VS Code haz clic derecho → **Download** para bajarlo, o simplemente sube tus cambios y usa el
   APK de GitHub Actions (sección 1).

Si el codespace se creó sin el devcontainer, ejecuta tú mismo `bash scripts/setup-android-sdk.sh`.
La compilación usa el Gradle Wrapper del proyecto (`./gradlew`, Gradle 8.11.1).

## 4. Hacer cambios en el futuro sin Android Studio

- Edita los archivos en el editor web de Codespaces (o incluso en github.com con la tecla `.`).
- Ejecuta `./gradlew testDebugUnitTest assembleDebug` en el codespace para comprobarlos, o sube
  los cambios y deja que GitHub Actions compile.
- Dónde está cada cosa:

| Carpeta (`app/src/main/java/com/leilapp/aura/`) | Contenido |
|---|---|
| `data/` | Base de datos Room (entidades, DAO), preferencias (DataStore), clima (Open-Meteo) |
| `domain/` | Lógica pura y probada: conflictos de horario, rutinas, recordatorios, vestimenta, formatos |
| `reminders/` | Notificaciones y alarmas (AlarmManager), reprogramación al reiniciar |
| `ui/theme/` | Colores y tipografía. Para un acento nuevo, agrégalo a `AuraAccent` |
| `ui/<sección>/` | Pantallas: `today` (Mi día), `child` (Mi hijo), `agenda`, `tasks`, `routines`, `shopping`, `payments`, `weather`, `settings` |
| `ui/navigation/` | Barra inferior y rutas |

- Si cambias la estructura de la base de datos, sube `version` en `AuraDatabase` y añade una
  migración; los esquemas se exportan a `app/schemas/`.

## 5. Funciones implementadas

- **Mi día:** saludo según la hora, fecha, resumen, próximo compromiso, actividades de tu hijo
  (primero), agenda de hoy, tareas de hoy y vencidas, rutinas con progreso, pagos próximos,
  resumen del clima, accesos rápidos (tarea, evento, mi hijo, recordatorio) y avisos de conflicto.
- **Mi hijo:** actividades, citas, eventos escolares, materiales por preparar, artículos por
  comprar, fechas importantes y recordatorios, con título, fecha, hora de inicio y fin, ubicación,
  descripción, categoría, prioridad, completado, recordatorio y notas. Resaltado en color
  melocotón en toda la app. Filtros por tipo y estado. Advertencia de conflictos.
- **Tareas:** crear, editar, eliminar con confirmación, completar, fecha, hora opcional,
  prioridad, categoría (8 iniciales + categorías propias), notas, recordatorio; filtros Hoy,
  Próximas, Vencidas, Todas, Completadas y por categoría.
- **Agenda:** vista por día con tira de fechas, lista de próximos, eventos y tareas del día,
  ubicación, notas, recordatorios, distinción visual de «Mi hijo» y advertencias de superposición
  (al ver y al editar). Nunca se mueven ni eliminan eventos automáticamente.
- **Rutinas:** creación libre, días de la semana, hora opcional, activar/desactivar, marcar por
  día (cada día empieza sin marcar), progreso diario y recordatorio opcional.
- **Compras:** varias listas, artículos con cantidad, marcar comprados, editar, eliminar, quitar
  comprados.
- **Pagos:** nombre, importe, vencimiento, categoría, estado, recordatorio y notas. Sin conexión
  con bancos.
- **Clima y vestimenta:** pronóstico real de Open-Meteo (5 días), ciudad escrita a mano o
  ubicación aproximada una sola vez; sugerencias por reglas (frío, calor, lluvia, UV, nieve,
  amplitud térmica) y advertencias. Si no hay conexión muestra el motivo y permite reintentar;
  la última consulta se guarda para verla sin conexión indicando su hora.
- **Notificaciones:** tareas, eventos, actividades de tu hijo (canal propio de prioridad alta),
  rutinas, pagos y fechas importantes, con anticipación configurable. Se reprograman tras
  reiniciar o actualizar. Antes de avisar se comprueba que el elemento siga pendiente, y cada
  elemento tiene una sola alarma (no hay duplicados).
- **Ajustes:** nombre para el saludo, 5 colores de acento, estado de permisos, categorías,
  cargar/borrar datos de ejemplo (marcados «Ejemplo:»), información de privacidad.

## 6. Funciones pendientes (siguientes fases)

- Tema claro y más personalización (fuentes, fondos).
- Eventos recurrentes en la agenda (hoy solo las rutinas se repiten).
- Armario digital: registrar prendas y sugerir combinaciones según clima y actividades.
- Widget de pantalla de inicio para «Mi día».
- Exportar / importar copia de seguridad local y sincronización opcional con cuenta.
- Vista mensual del calendario.
- Pruebas instrumentadas de interfaz (requieren dispositivo o emulador).

## 7. Permisos y configuraciones que debes conocer

| Permiso | Para qué | Cuándo se pide |
|---|---|---|
| Notificaciones (Android 13+) | Mostrar los recordatorios que tú programes | Tarjeta explicativa en «Mi día»; puedes decir «Ahora no» |
| Alarmas exactas (opcional) | Avisar a la hora exacta | Nunca se fuerza: se activa en **Más → Ajustes**. Sin él, Android puede retrasar los avisos unos minutos |
| Ubicación aproximada (opcional) | Consultar el clima de donde estás, una vez | Solo al pulsar «Usar mi ubicación aproximada» |
| Internet | Solo el clima | Automático, no requiere aprobación |
| Inicio del teléfono | Reprogramar recordatorios tras reiniciar | Automático |

- **Clima:** Open-Meteo no necesita clave API ni cuenta. Solo se envía la ciudad o las
  coordenadas redondeadas. Es gratuito para uso personal no comercial.
- **Batería:** algunos fabricantes (Xiaomi, Samsung, Huawei…) detienen apps en segundo plano.
  Si los avisos no llegan, permite a Aura funcionar sin restricciones de batería.
- **Copia de seguridad:** está desactivada para que tus datos no salgan del teléfono. Si
  desinstalas la app se borran.

## 8. Errores encontrados y soluciones aplicadas

- **Repositorio vacío:** se creó el proyecto desde cero con Gradle Kotlin DSL y catálogo de
  versiones (`gradle/libs.versions.toml`).
- **El entorno de desarrollo inicial no podía descargar el Android SDK** (la red bloqueaba
  `dl.google.com` y `maven.google.com`). Solución: compilar y probar en GitHub Actions, que
  incluye el SDK, y dejar `scripts/setup-android-sdk.sh` para Codespaces. La lógica pura
  (`domain/`) se verificó con el compilador de Kotlin y JUnit (17 pruebas).
- **APK de prueba que no se podía actualizar sin perder datos:** cada máquina genera su propia
  clave de depuración. Se incluyó una clave de **depuración** fija (`app/debug.keystore`,
  contraseña pública `android`). No es una clave de producción.

## 9. Versión de lanzamiento

`./gradlew assembleRelease` genera un APK **sin firmar** con R8 y reducción de recursos
activados. Para publicarlo hace falta crear una clave de producción propia; no se ha creado
ninguna. Para uso personal, el APK de depuración es suficiente.

## Tecnología

Kotlin 2.0.21 · AGP 8.7.3 · Gradle 8.11.1 · Compose BOM 2024.12.01 · Material 3 · Room 2.6.1 ·
DataStore 1.1.1 · Navigation Compose 2.8.5 · minSdk 26 · targetSdk/compileSdk 35 · Java 17.
Sin Retrofit ni librerías de red: el clima usa `HttpURLConnection` y `org.json` de Android.
