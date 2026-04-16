# F-Droid notes

Estado actual para envio a F-Droid:

- Estado: en pausa hasta decidir la estrategia de paquete tras el cambio a `es.sasogu.minilauncher`

- Repositorio publico: https://github.com/sasogu/minilauncher
- Branch publicado: main
- Proximo tag: v0.5.0
- Licencia incluida: Apache-2.0
- Metadata fastlane base incluida

## Checklist inmediato

1. Pendiente decidir si F-Droid seguira con el paquete antiguo o con `es.sasogu.minilauncher`
2. Preparar release y metadata definitivas para `0.5.0`
3. Changelog de fastlane a regenerar para `versionCode 5`
4. Validacion local a repetir con el nuevo paquete antes de abrir solicitud en fdroiddata

## Datos del proyecto

- App name: Minilauncher
- Application ID: es.sasogu.minilauncher
- Repo: https://github.com/sasogu/minilauncher
- License: Apache-2.0
- Version name: 0.5.0
- Version code: 5
- Build command: ./gradlew assembleRelease
- Min SDK: 26
- Target SDK: 35

## Verificaciones tecnicas ya hechas

- `fastlane/metadata/android/en-US/short_description.txt` queda por debajo de 80 caracteres
- `fastlane/metadata/android/en-US` esta consistente en ingles
- `gradle-wrapper.properties` incluye `distributionSha256Sum`
- `gradle-wrapper.jar` fue regenerado y ya no arrastra la version antigua del wrapper
- `./gradlew testDebugUnitTest` termina correctamente
- `./gradlew assembleRelease` termina correctamente

## Observaciones para la revision

- Permiso declarado: `POST_NOTIFICATIONS`, usado para recordatorios locales opcionales
- No se detectan dependencias de Google Play Services, Firebase ni SDKs de tracking en el proyecto
- La build release muestra warnings por APIs de color de barras del sistema deprecadas en `MainActivity`, pero no bloquean compilacion ni publicacion

## Texto sugerido para issue en fdroiddata

Title:

RFP: Minilauncher

Body:

### Please add my app to F-Droid

- Name: Minilauncher
- Application ID: es.sasogu.minilauncher
- Source code: https://github.com/sasogu/minilauncher
- License: Apache-2.0
- Current version: 0.5.0 (versionCode 5)
- Release: pendiente de crear para v0.5.0
- Build command: ./gradlew assembleRelease
- Min SDK: 26
- Target SDK: 35

App description:

Minilauncher is a minimalist Android launcher focused on reducing friction, distractions and impulsive phone usage.

Main features:

- Minimal home with time, date and battery ring
- Favorites pinned on home with quick reordering
- Separate page for the full app list
- Search on home and app list
- Settings screen for language and theme
- Intentional-use prompt before launching apps
- Optional toggle to disable the intentional-use prompt
- Optional local reminder after selected duration
- Moon phase indicator on the home screen
- Quick web search from Home with a left-to-right swipe gesture

Notes:

- No ads
- No trackers
- No Google Play Services dependency

## Version corta lista para pegar en fdroiddata

Please add my app to F-Droid.

- Name: Minilauncher
- Application ID: es.sasogu.minilauncher
- Source code: https://github.com/sasogu/minilauncher
- License: Apache-2.0
- Current version: 0.5.0 (versionCode 5)
- Release: pendiente de crear para v0.5.0
- Build command: ./gradlew assembleRelease
- Min SDK: 26
- Target SDK: 35

## Borrador de metadata para fdroiddata

Archivo esperado en fdroiddata:

metadata/es.sasogu.minilauncher.yml

Plantilla inicial:

```yml
Categories:
  - System
License: Apache-2.0
AuthorName: sasogu
AuthorWebSite: https://github.com/sasogu
SourceCode: https://github.com/sasogu/minilauncher
IssueTracker: https://github.com/sasogu/minilauncher/issues
Changelog: https://github.com/sasogu/minilauncher/releases

AutoName: Minilauncher
Summary: Minimal Android launcher with intentional app opening
Description: |
  Minilauncher is a minimalist Android launcher focused on reducing
  friction, distractions and impulsive phone usage.

  Features include a minimal home, favorites, app search,
  theme and language settings, optional intentional-use prompt before
  opening apps, moon phase indicator, and quick web search gesture.

RepoType: git
Repo: https://github.com/sasogu/minilauncher.git

Builds:
  - versionName: 0.5.0
    versionCode: 5
    commit: v0.5.0
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 0.5.0
CurrentVersionCode: 5
```

Nota: el equipo de F-Droid puede ajustar esta metadata durante la revision.
