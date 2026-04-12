# Minilauncher

Launcher minimalista para Android, pensado para reducir fricción, ruido visual y uso impulsivo del teléfono.

## Estado

Version publicada: `0.2.0`

Incluye:

- home minimalista con reloj, fecha y aro de batería
- apertura de alarmas al tocar el reloj
- pantalla lateral con todas las aplicaciones
- favoritas persistentes en la home
- pulsación larga en favoritas para moverlas al inicio
- accesos fijos a teléfono y cámara
- filtro en home y en listado de apps
- pausa consciente antes de abrir apps
- toggle en ajustes para activar/desactivar la pausa consciente por app
- recordatorio local opcional tras el tiempo elegido
- fase lunar en la home junto a reloj y fecha
- busqueda web rapida en Home con gesto horizontal izquierda -> derecha
- idioma configurable por usuario (espanol, valenciano e ingles)
- cache de iconos y carga incremental de aplicaciones
- selector de tema claro/oscuro desde ajustes
- apps ocultas con restauracion desde ajustes
- hint de reordenacion de favoritas descartable
- mejoras de accesibilidad (targets tactiles minimos y descripciones para TalkBack)
- pulido de contraste en textos/iconos secundarios
- barra inferior ajustada para distintos tamanos de pantalla
- permiso de superposicion guiado para mejorar la vuelta al launcher al terminar el tiempo

## Idiomas

- Espanol (`es`)
- Valenciano (`ca`)
- Ingles (`en`)

El idioma se puede cambiar desde ajustes.

## Stack

- Kotlin
- Jetpack Compose
- Android nativo
- DataStore Preferences
- JUnit para tests unitarios
- AndroidX Test + Compose UI Test para tests instrumentados minimos

## Arquitectura

La logica de estado se separo de la UI para reducir acoplamiento.

Actualmente:

- `MainActivity` orquesta efectos del sistema (intents, permisos, ciclo de vida)
- `LauncherViewModel` centraliza estado y acciones/eventos de UI
- `LauncherState.kt` concentra logica de dominio del launcher
- preferencias persistentes usan `DataStore`

Esto facilita pruebas, mantenimiento y futuras features.

## Build local

```bash
./gradlew assembleDebug
```

Tests unitarios:

```bash
./gradlew testDebugUnitTest
```

Tests instrumentados (dispositivo/emulador):

```bash
./gradlew connectedDebugAndroidTest
```

Nota: en algunos dispositivos puede aparecer confirmacion de instalacion del APK de pruebas.

## Rendimiento

- Carga de apps incremental para mejorar respuesta percibida.
- Precache limitado de iconos visibles para acelerar primer render del listado.
- Metricas de primera carga vs recarga en logs (`LauncherPerformance`).

## Release interna

Scripts disponibles:

- `./scripts/update_fastlane_changelog.sh`
- `./scripts/release.sh`

Ejemplos:

```bash
./scripts/update_fastlane_changelog.sh
./scripts/release.sh
./scripts/release.sh --tag
./scripts/release.sh --tag --github-release
```

`update_fastlane_changelog.sh` genera o actualiza el archivo:

- `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`

tomando la seccion correspondiente de `CHANGELOG.md`.

`release.sh` hace checks basicos, sincroniza el changelog de fastlane,
ejecuta tests unitarios, genera el APK release y opcionalmente crea tag
y release en GitHub.

APK de debug:

- `app/build/outputs/apk/debug/app-debug.apk`

## Licencia

Este proyecto se distribuye bajo `Apache-2.0`.

## Privacidad

Minilauncher no incluye anuncios, trackers ni analitica de terceros.

La app:

- no requiere cuenta de usuario
- no envia datos personales a servidores externos
- no comparte informacion con terceros
- guarda solo preferencias locales del launcher en el dispositivo

Ejemplos de datos locales guardados:

- idioma seleccionado
- orden y listado de favoritas
- ajustes necesarios para el funcionamiento del launcher

Los recordatorios de uso se generan localmente en el dispositivo.

## F-Droid

Release publicada para inicio de alta en F-Droid:

- tag: `v0.2.0`
- release: `https://github.com/sasogu/minilauncher/releases/tag/v0.2.0`


## Roadmap

La hoja de ruta tecnica vive en:

- `BACKLOG_LAUNCHER.md`
