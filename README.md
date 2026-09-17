# Folio Reader

Folio Reader is an Android PDF reader for English learners. It identifies
challenging vocabulary while you read, provides definitions and translations,
and helps you build a personal study list from the words you encounter.

## Features

- Open English PDFs from the device, file managers, browsers, and sharing apps.
- Read text-based PDFs and scanned PDFs with on-device OCR.
- Highlight difficult words and tap them for definitions, examples, synonyms,
  and translations.
- Use the built-in offline dictionary without requiring a network connection.
- Save words to a personal vocabulary list and track learning progress.
- Review saved words with flashcards and quizzes.
- Export vocabulary as Anki-compatible CSV or readable plain text.
- Download the on-device English-to-Hindi ML Kit model for offline translation.
- Optionally request AI explanations using your own Gemini API key.
- Use Classroom mode with Nearby Connections for shared word activities.
- Choose light, dark, or system appearance and configure reading preferences.

## Technology

- Kotlin
- Jetpack Compose and Material 3
- Android Gradle Plugin 9.1.1
- Gradle 9.3.1
- Android API 35
- Minimum Android API 26
- Room for local data
- Hilt for dependency injection
- ML Kit for OCR and translation
- PDFBox for PDF text extraction
- Nearby Connections for Classroom mode

The Android application module is `app`, with application ID
`com.tbtechs.folioreader`.

## Requirements

- [Android Studio](https://developer.android.com/studio)
- JDK 17
- Android SDK Platform 35
- Android SDK Build-Tools 35.0.0
- An Android emulator or a physical Android device

Folio Reader is an Android application. It is not a Kotlin desktop app and
does not have a desktop `gradle :run` target or a direct Replit VNC preview.

## Run locally

1. Clone the repository and open its root directory in Android Studio.
2. Allow Gradle to sync and install the Android SDK components requested by the
   project.
3. Create a local debug keystore if `debug.keystore` does not already exist:

   ```bash
   keytool -genkeypair \
     -keystore debug.keystore \
     -storepass android \
     -keypass android \
     -alias androiddebugkey \
     -keyalg RSA \
     -keysize 2048 \
     -validity 10000 \
     -dname "CN=Android Debug,O=Android,C=US"
   ```

4. Optional: create a `.env` file in the project root for Gemini explanations:

   ```dotenv
   GEMINI_API_KEY=your_gemini_api_key
   ```

   The key is optional. The app's offline reading, dictionary, vocabulary,
   OCR, and ML Kit translation features do not require it.

5. Start an emulator or connect a physical Android device.
6. Run the `app` configuration from Android Studio, or use:

   ```bash
   ./gradlew :app:installDebug
   ```

## Gradle commands

Build a debug APK:

```bash
./gradlew :app:assembleDebug
```

Run local unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

Build the release APK and Play Store bundle:

```bash
./gradlew :app:assembleRelease :app:bundleRelease
```

The release build expects a signing keystore. The app reads these environment
variables:

- `KEYSTORE_PATH` (defaults to `my-upload-key.jks` in the project root)
- `STORE_PASSWORD`
- `KEY_ALIAS` (defaults to `folioreader`)
- `KEY_PASSWORD`

Do not commit a keystore, passwords, or `.env` files.

## GitHub Actions

Two workflows are available under `.github/workflows`:

### Debug APK

`build-debug.yml` runs on pushes to `main` and through manual dispatch. It
builds and uploads a debug APK as a workflow artifact.

### Release APK and AAB

`build-release.yml` is manually triggered. It builds and uploads both the
release APK and the Play Store AAB.

The release workflow requires these GitHub Actions repository secrets:

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

The repository's current release alias is `folioreader`. Keep the keystore
and its passwords backed up securely; losing the upload keystore can complicate
future Play Store releases.

## Project structure

```text
app/src/main/java/com/tbtechs/folioreader/
├── data/          PDF, OCR, dictionary, translation, database, and AI access
├── domain/        Core models and vocabulary use cases
├── navigation/    Compose navigation destinations
├── ui/            Reader, vocabulary, classroom, settings, and themes
└── util/          Shared Android utilities
```

Tests live under `app/src/test` and `app/src/androidTest`.

## Privacy and local data

Reading state, vocabulary, cached definitions, and preferences are stored
locally on the device. Gemini explanations are optional and use the API key
configured by the user. The app also requests the Android permissions needed
for OCR, audio/classroom features, and Nearby Connections.
