# Cartoon AI

AI-powered cartoon movie generator for Android built with Kotlin, Jetpack Compose (Material 3), Firebase AI Logic, and Cloud Firestore.

## Application Details

- **App Name:** Cartoon AI
- **Application ID:** `com.aistudio.cartoonai.k7m9xp`
- **Minimum SDK:** 24
- **Target / Compile SDK:** 36

## Project Structure

- `app/src/main/java/com/example/`
  - `MainActivity.kt` — Application entry point and navigation shell
  - `ai/CartoonAiService.kt` — Script, scene, and character generation via Firebase AI Logic
  - `data/` — Data models and Firestore/project repositories
  - `firebase/` — App Check and Firestore error handling utilities
  - `media/` — `CartoonVideoRenderer.kt`, `VoiceAndAudioEngine.kt`, `VideoExportHelper.kt` for scene animation rendering, voice/audio playback, and MP4 export
  - `ui/` — Theme, ViewModels, reusable components, and screens (`HomeDashboardScreen`, `GeneratorStudioScreen`, `VideoPlayerScreen`, `CharactersScreen`, `ProjectsAndAccountScreen`, `AuthScreen`)
- `firestore.rules`, `firestore.test.js`, `firebase-blueprint.json`, `firebase.json` — Firestore security rules and schema configuration
- `apk-distribution/` — Prebuilt verified debug APK (`app-debug.apk`) and SHA-256 checksum (`app-debug.apk.sha256`)
- `.github/workflows/publish-apk.yml` — GitHub Actions workflow that validates `apk-distribution/app-debug.apk` and publishes it to GitHub Releases

## Building & APK Distribution

### Debug APK

```bash
gradle :app:assembleDebug
```

The verified debug APK is stored at `apk-distribution/app-debug.apk` with checksum `apk-distribution/app-debug.apk.sha256`.

### Release APK

`app/build.gradle.kts` configures release signing via environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) and upload key alias `upload`. Supply your release keystore and credentials before running:

```bash
gradle :app:assembleRelease
```

### Automated GitHub Release Publishing

Pushing `apk-distribution/app-debug.apk` to `main` or `master` (or manually running **Publish Prebuilt Debug APK** from the GitHub **Actions** tab) validates the APK checksum and archive integrity and publishes `app-debug.apk` to the repository's latest GitHub Release.
