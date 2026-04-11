# F-Droid notes

Estado actual para envio a F-Droid:

- Repositorio publico: https://github.com/sasogu/minilauncher
- Branch publicado: main
- Tag publicado: v0.1.0
- Commit de referencia: 3b084e1
- Licencia incluida: Apache-2.0
- Metadata fastlane base incluida

## Checklist inmediato

1. Capturas definitivas preparadas en fastlane/metadata/android/en-US/images/phoneScreenshots/.
2. Release en GitHub creada: https://github.com/sasogu/minilauncher/releases/tag/v0.1.0.
3. Pendiente: abrir solicitud en https://gitlab.com/fdroid/fdroiddata/-/issues con la plantilla de abajo.

## Datos del proyecto

- App name: Minilauncher
- Application ID: com.minilauncher
- Repo: https://github.com/sasogu/minilauncher
- License: Apache-2.0
- Version name: 0.1.0
- Version code: 1
- Build command: ./gradlew assembleRelease
- Min SDK: 26
- Target SDK: 35

## Texto sugerido para GitHub Release (v0.1.0)

Title:

v0.1.0 - Initial public release

Body:

Initial public release of Minilauncher.

Highlights:

- Minimal home with time, date and battery ring
- Favorite apps on the home screen
- Full app list on a separate page
- Fixed phone and camera shortcuts
- Intentional-use prompt before launching apps
- Optional local reminder after selected time

Technical info:

- Package name: com.minilauncher
- Version name: 0.1.0
- Version code: 1
- License: Apache-2.0

## Texto sugerido para issue en fdroiddata

Title:

RFP: Minilauncher

Body:

### Please add my app to F-Droid

- Name: Minilauncher
- Application ID: com.minilauncher
- Source code: https://github.com/sasogu/minilauncher
- License: Apache-2.0
- Current version: 0.1.0 (versionCode 1)
- Release tag: v0.1.0

App description:

Minilauncher is a minimalist Android launcher focused on reducing friction, distractions and impulsive phone usage.

Main features:

- Minimal home with time, date and battery ring
- Favorites pinned on home
- Separate page for full app list
- Fixed phone and camera shortcuts
- Intentional-use prompt before launching apps
- Optional local reminder after selected duration

Build information:

- Build system: Gradle (Android)
- Build command: ./gradlew assembleRelease
- Min SDK: 26
- Target SDK: 35

Additional notes:

- No ads
- No trackers
- No Google Play Services dependency

## Version corta lista para pegar en fdroiddata

Title:

RFP: Minilauncher

Body:

Please add my app to F-Droid.

- Name: Minilauncher
- Application ID: com.minilauncher
- Source code: https://github.com/sasogu/minilauncher
- License: Apache-2.0
- Current version: 0.1.0 (versionCode 1)
- Release: https://github.com/sasogu/minilauncher/releases/tag/v0.1.0
- Build command: ./gradlew assembleRelease
- Min SDK: 26
- Target SDK: 35

Short description:

Minilauncher is a minimalist Android launcher focused on reducing distractions and impulsive app usage.

Notes:

- No ads
- No trackers
- No Google Play Services dependency

## Borrador de metadata para fdroiddata

Archivo esperado en fdroiddata:

metadata/com.minilauncher.yml

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

	Features include a minimal home, favorites, full app list,
	fixed phone/camera shortcuts and an intentional-use prompt before
	opening apps.

RepoType: git
Repo: https://github.com/sasogu/minilauncher.git

Builds:
	- versionName: 0.1.0
		versionCode: 1
		commit: v0.1.0
		subdir: app
		gradle:
			- yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 0.1.0
CurrentVersionCode: 1
```

Nota: el equipo de F-Droid puede ajustar esta metadata durante la revision.
