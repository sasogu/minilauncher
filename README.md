# Minilauncher

Launcher minimalista para Android, pensado para reducir fricción, ruido visual y uso impulsivo del teléfono.

## Estado

Version publicada: `0.1.0`

Proxima version en desarrollo: `0.2.0`

Incluye:

- home minimalista con reloj, fecha y aro de batería
- apertura de alarmas al tocar el reloj
- pantalla lateral con todas las aplicaciones
- favoritas persistentes en la home
- accesos fijos a teléfono y cámara
- filtro en home y en listado de apps
- pausa consciente antes de abrir apps
- recordatorio local opcional tras el tiempo elegido
- idioma configurable por usuario (espanol, valenciano e ingles)

## Idiomas

- Espanol (`es`)
- Valenciano (`ca`)
- Ingles (`en`)

El idioma se puede cambiar desde la pantalla de aplicaciones.

## Stack

- Kotlin
- Jetpack Compose
- Android nativo

## Build local

```bash
./gradlew assembleDebug
```

APK de debug:

- `app/build/outputs/apk/debug/app-debug.apk`

## Licencia

Este proyecto se distribuye bajo `Apache-2.0`.

## F-Droid

Release publicada para inicio de alta en F-Droid:

- tag: `v0.1.0`
- release: `https://github.com/sasogu/minilauncher/releases/tag/v0.1.0`


## Roadmap

La hoja de ruta tecnica vive en:

- `BACKLOG_LAUNCHER.md`
