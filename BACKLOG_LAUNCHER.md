# Backlog tecnico del launcher minimalista

Este documento recoge el backlog base para construir un launcher Android completo en Kotlin + Jetpack Compose, tomando como referencia visual las capturas de la carpeta `images`.

## Objetivo del MVP

Entregar un launcher funcional que:

- pueda establecerse como launcher por defecto
- muestre una home minimalista con reloj y fecha
- permita buscar y abrir apps instaladas rapidamente
- incluya ajustes basicos persistentes
- tenga un onboarding simple inspirado en las capturas

## Stack recomendado

- Kotlin
- Jetpack Compose
- MVVM
- StateFlow
- Hilt
- Navigation Compose
- DataStore
- Room opcional para futuras estadisticas
- `LauncherApps` y `PackageManager` para integracion Android

## Backlog MVP

### 1. Bootstrap del proyecto

- Crear proyecto Android con Kotlin, Compose, Hilt y Navigation Compose.
- Configurar `minSdk`, `targetSdk`, variantes debug/release y reglas basicas de lint.
- Crear la estructura base por features.

### 2. Tema visual minimalista

- Definir paleta: negro, gris oscuro, blanco roto.
- Crear sistema de tipografias, espaciados, radios y bordes.
- Implementar componentes base reutilizables:
  - `MinimalButton`
  - `MinimalCard`
  - `SettingsRow`
  - `SearchField`
  - `AppListItem`

### 3. Arquitectura base

- Crear `MainActivity`.
- Configurar navegacion Compose.
- Montar `ViewModel` base por feature.
- Preparar `UiState`, `UiEvent` y `UiAction`.

### 4. Registro como launcher

- Configurar en `AndroidManifest.xml` la actividad principal con:
  - `ACTION_MAIN`
  - `CATEGORY_HOME`
  - `CATEGORY_DEFAULT`
- Verificar que Android permita elegir la app como launcher predeterminado.
- Probar el comportamiento al pulsar Home.

### 5. Acceso a aplicaciones instaladas

- Crear `AppsRepository`.
- Obtener apps lanzables con `LauncherApps` o `PackageManager`.
- Mapear a un modelo `LaunchableApp` con:
  - nombre
  - package name
  - actividad de lanzamiento
  - icono
- Ordenar alfabeticamente y filtrar duplicados.

### 6. Apertura de aplicaciones

- Implementar la apertura de apps desde package/activity.
- Manejar errores si una app ya no esta disponible.
- Verificar que el flujo sea rapido y estable.

### 7. Pantalla Home

- Crear una home minimalista con:
  - reloj grande
  - fecha
  - accesos rapidos opcionales
  - gesto o toque para abrir el listado
- Actualizar la hora en vivo con bajo coste.
- Ajustar layout para distintas alturas de pantalla.

### 8. Pantalla de lista de apps

- Añadir buscador en la parte superior.
- Mostrar lista vertical simple.
- Añadir indice alfabetico lateral.
- Soportar scroll rapido.
- Abrir app al hacer tap.

### 9. Busqueda en tiempo real

- Filtrar por nombre mientras se escribe.
- Normalizar acentos y mayusculas.
- Mantener buen rendimiento con muchas apps.
- Decidir si el teclado se abre automaticamente al entrar.

### 10. Persistencia de ajustes

- Configurar DataStore.
- Guardar:
  - formato horario
  - mostrar/ocultar reloj
  - onboarding visto
  - favoritas
  - apps ocultas

### 11. Pantalla de ajustes

- Implementar opciones del MVP:
  - reloj visible
  - formato 12/24 horas
  - reiniciar onboarding
  - gestionar favoritas
  - ocultar apps
- Mantener consistencia visual con el resto del launcher.

### 12. Onboarding

- Crear flujo de tarjetas informativas similar al de las capturas.
- Guardar progreso y estado completado.
- Permitir omitir y reabrir desde ajustes.

### 13. Favoritas y accesos rapidos

- Seleccionar 2-4 apps favoritas.
- Mostrarlas en Home.
- Dejar preparado el terreno para reordenacion futura.

### 14. Apps ocultas

- Ocultar apps de la lista principal.
- Crear vista sencilla para restaurarlas.
- Evitar perdidas de configuracion.

### 15. Rendimiento y pulido

- Cachear iconos.
- Minimizar recomposiciones en Compose.
- Revisar fluidez con listas grandes.
- Ajustar contraste, accesibilidad y tamanos tactiles.

## Backlog Fase 2

- Bloqueo temporal de apps.
- Mensajes de foco o digital detox.
- Estadisticas basicas de uso.
- Modulos configurables en Home.
- Backup o exportacion de configuracion.
- Gestos y accesos directos avanzados.

## Historias clave

- Como usuario, quiero que al pulsar Home aparezca este launcher.
- Como usuario, quiero buscar una app escribiendo su nombre y abrirla al instante.
- Como usuario, quiero ver solo lo esencial en pantalla.
- Como usuario, quiero ocultar apps que me distraen.
- Como usuario, quiero configurar pocas cosas, pero las importantes.

## Definicion de MVP terminado

El MVP se considera terminado cuando:

- la app puede elegirse como launcher por defecto
- la home es funcional y estable
- la lista de apps carga correctamente
- la busqueda funciona en tiempo real
- las apps se abren sin errores relevantes
- los ajustes basicos quedan persistidos
- el onboarding puede completarse y reiniciarse

## Orden recomendado de implementacion

1. Bootstrap del proyecto
2. Tema y componentes base
3. Registro como launcher
4. Repositorio de apps instaladas
5. Lista de apps y apertura
6. Pantalla Home
7. Ajustes con DataStore
8. Onboarding
9. Favoritas y apps ocultas
10. Pulido

## Notas tecnicas

- Flutter puede servir para prototipos visuales, pero para un launcher completo se recomienda Kotlin + Compose por su integracion directa con Android.
- Algunas funciones avanzadas de bloqueo o control de uso pueden variar segun fabricante y version de Android.
- Conviene empezar con soporte para Android 12+ para reducir complejidad inicial.
