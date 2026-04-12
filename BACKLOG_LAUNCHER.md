# Backlog tecnico del launcher minimalista

Este documento refleja el estado real del proyecto y las siguientes prioridades de desarrollo para `Minilauncher`.

## Estado actual

La base funcional ya esta construida.

Actualmente la app incluye:

- launcher minimalista con reloj, fecha y aro de bateria
- lista completa de aplicaciones con busqueda en tiempo real
- apertura de apps y dialogo de pausa consciente
- recordatorios locales de uso
- favoritas persistentes en Home
- pulsacion larga en favoritas para moverlas al inicio
- accesos fijos a telefono y camara
- selector de idioma configurable por usuario
- soporte para espanol, valenciano e ingles
- persistencia con `DataStore`
- toggle en ajustes para activar/desactivar dialogo de tiempo por app
- indicador de fase lunar en la home
- busqueda web rapida desde Home con gesto horizontal izquierda -> derecha
- cache de iconos y carga incremental de apps
- tests unitarios basicos para utilidades y persistencia
- metadata base y flujo inicial preparados para F-Droid

## Stack actual

- Kotlin
- Jetpack Compose
- StateFlow
- DataStore Preferences
- JUnit
- `PackageManager` para apps instaladas

## Completado

### Base de producto

- Registro como launcher por defecto.
- Home minimalista funcional.
- Pantalla de aplicaciones con busqueda.
- Apertura rapida de apps.
- Favoritas persistentes.
- Recordatorio local tras tiempo seleccionado.
- Busqueda web rapida por gesto desde Home.

### Ajustes y persistencia

- Persistencia de idioma.
- Persistencia de favoritas y orden de favoritas.
- Migracion desde `SharedPreferences` a `DataStore`.
- Persistencia de preferencia para activar/desactivar el dialogo de tiempo.
- Persistencia de apps ocultas y restauracion desde ajustes.

### Rendimiento y arquitectura

- Carga incremental de apps.
- Cache de iconos en memoria.
- Refactor parcial de logica fuera de la UI a `LauncherState.kt`.
- Tests unitarios para `normalize`, `filterApps`, `FavoritesStore` y `LanguageStore`.

### Release y F-Droid

- Metadata base de `fastlane`.
- Capturas iniciales.
- Changelog por version en `fastlane/metadata`.
- Scripts internos de release y sincronizacion de changelog.
- Nota corta de privacidad en `README.md`.

## Prioridad inmediata v0.2.x

### 1. Pantalla de ajustes real

- [x] Crear una pantalla de ajustes visible desde la app.
- [x] Mover ahi el selector de idioma.
- [x] Preparar ajustes futuros sin seguir cargando la pantalla de apps.

### 2. Apps ocultas

- [x] Permitir ocultar apps desde la lista principal.
- [x] Crear una vista para restaurarlas.
- [x] Persistirlas en `DataStore`.

### 3. Pulido de favoritas

- [x] Revisar la UX de marcado de favoritas para que sea mas evidente.
- [x] Evaluar feedback visual o pequeno hint para descubrir la pulsacion larga en Home.
- [x] Anadir feedback haptico al marcar favoritas y al ocultar/restaurar apps.
- [x] Ajustar tamano de texto/iconos en lista de apps para mostrar nombres mas completos.


### 4. Accesibilidad y pulido visual

- [x] Revisar tamanos tactiles minimos.
- [x] Revisar contraste de iconos y textos secundarios.
- [x] Validar el comportamiento de la barra inferior en distintos dispositivos.

## Siguientes mejoras tecnicas

### 1. Arquitectura

- [x] Seguir extrayendo logica de `MainActivity`.
- [x] Introducir una capa mas clara de acciones/eventos de UI.
- [x] Evaluar si compensa introducir `ViewModel` para separar ciclo de vida y estado.

### 2. Testing

- [x] Anadir tests para `LauncherStateStore`.
- [x] Anadir tests para migracion de datos desde preferencias antiguas.
- [x] Anadir tests instrumentados minimos para flujos criticos.

### 3. Rendimiento

- [x] Medir tiempos de primera carga vs recarga.
- [x] Revisar si compensa precachear una cantidad limitada de iconos visibles.
- [x] Reducir recomposiciones innecesarias en la lista.

## Backlog Fase 2

- Onboarding breve y reiniciable.
- Apps bloqueadas temporalmente.
- Mensajes de foco o digital detox.
- Estadisticas basicas de uso local.
- Modulos configurables en Home.
- Backup o exportacion de configuracion.
- Gestos y accesos directos avanzados.
- Capturas por idioma para la ficha de F-Droid.

## Historias clave

- Como usuario, quiero que al pulsar Home aparezca este launcher.
- Como usuario, quiero buscar una app escribiendo su nombre y abrirla al instante.
- Como usuario, quiero fijar mis apps mas usadas en Home y reordenarlas rapidamente.
- Como usuario, quiero reducir friccion y distracciones al usar el telefono.
- Como usuario, quiero configurar solo lo importante.

## Criterios para v0.2.0

La version `0.2.0` deberia quedar cerrada cuando se cumpla al menos esto:

- pantalla de ajustes funcional
- selector de idioma movido a ajustes```
- tests ampliados sobre la nueva logica de estado
- changelog y flujo de release repetibles sin pasos manuales innecesarios

## Notas tecnicas

- El proyecto sigue priorizando simplicidad antes que arquitectura compleja.
- Antes de introducir nuevas dependencias grandes, conviene agotar mejoras con la base actual.
- Las funciones avanzadas de control de uso pueden variar segun fabricante y version de Android.
