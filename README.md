# Minilauncher

Launcher minimalista para Android, pensado para reducir friccion, ruido visual y uso impulsivo del telefono.

## Estado

Version actual: `0.1.0`

Incluye:

- home minimalista con reloj, fecha y aro de bateria
- apertura de alarmas al tocar el reloj
- pantalla lateral con todas las aplicaciones
- favoritas persistentes en la home
- accesos fijos a telefono y camara
- filtro en home y en listado de apps
- pausa consciente antes de abrir apps
- recordatorio local opcional tras el tiempo elegido

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


## Roadmap

La hoja de ruta tecnica vive en:

- `BACKLOG_LAUNCHER.md`
