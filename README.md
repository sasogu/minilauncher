# Minilauncher

Launcher minimalista para Android inspirado en una interfaz sobria, enfocada en reducir friccion, distracciones y ruido visual.

## Direccion del proyecto

Aunque el repositorio empezo con idea de Flutter, la recomendacion actual es construir el launcher en:

- Kotlin
- Jetpack Compose

Esto permite una integracion mucho mas solida con Android para funciones clave como:

- registro como launcher por defecto
- listado de apps instaladas
- apertura rapida de aplicaciones
- futuras funciones avanzadas de personalizacion y control de uso

## Objetivo del MVP

El MVP debe incluir:

- Home minimalista con reloj y fecha
- Lista de apps con busqueda instantanea
- Apertura de aplicaciones instaladas
- Ajustes basicos persistentes
- Onboarding simple inspirado en las capturas de `images`

## Referencias visuales

Las capturas base del proyecto estan en:

- `images/`

## Backlog

La hoja de ruta tecnica actual esta en:

- `BACKLOG_LAUNCHER.md`

## Siguiente direccion tecnica

El siguiente paso recomendado es crear la base del proyecto Android nativo con Jetpack Compose y empezar por:

1. Bootstrap del proyecto
2. Tema visual minimalista
3. Registro como launcher
4. Lista de apps y busqueda
