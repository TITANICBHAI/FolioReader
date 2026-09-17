# Folio Reader Android Release Keystore Guide

## What is the keystore?

The release keystore is the file that contains Folio Reader's Android signing
identity. Android app stores use the signing identity to verify that future
updates come from the same publisher.

If the release keystore is lost, you may not be able to publish updates to the
same app on Google Play. Keep secure backups outside the project workspace,
such as an encrypted password manager attachment, secure cloud storage, or an
offline backup.

Never commit the keystore, its password, or a Base64 copy of it to GitHub.

## Folio Reader app details

| Field | Value |
|---|---|
| App name | Folio Reader |
| Application ID | `com.tbtechs.folioreader` |
| Android namespace | `com.tbtechs.folioreader` |
| Keystore file | `my-upload-key.jks` |
| Keystore format | JKS |
| Key alias | `folioreader` |
| Validity | 10,000 days |
| Organization | TBTechs |

The keystore file is already generated in the Replit workspace and is ignored
by Git. The password is intentionally not written in this guide or in the
Gradle files.

## Configure Replit Secrets

The release signing configuration in `app/build.gradle.kts` reads these
values from the environment:

| Secret name | Value |
|---|---|
| `STORE_PASSWORD` | The keystore password |
| `KEY_PASSWORD` | The password for the `folioreader` key |

The current release configuration uses:

```text
my-upload-key.jks
folioreader
```

`KEYSTORE_PATH` is optional. Set it only when the keystore is stored at a
different path.

Do not put any of these values in `.env`, Markdown files, Gradle files, or
shell scripts that will be committed.

## Back up the keystore

Create at least two secure backups of:

```text
my-upload-key.jks
```

Verify the alias and fingerprint before backing it up:

```bash
keytool -list -v \
  -keystore my-upload-key.jks \
  -alias folioreader
```

The command prompts for the keystore password. Keep the SHA-256 fingerprint
with your release records so you can identify this signing identity later.

## Build a release APK

From the project root:

```bash
./gradlew :app:assembleRelease
```

The signed APK is written to:

```text
app/build/outputs/apk/release/app-release.apk
```

Use an APK for direct installation, beta distribution, or services such as
Firebase App Distribution.

## Build a release Android App Bundle

```bash
./gradlew :app:bundleRelease
```

The signed AAB is written to:

```text
app/build/outputs/bundle/release/app-release.aab
```

Use an AAB for Google Play Store uploads. Google Play generates optimized APKs
for individual devices from the bundle.

## APK versus AAB

| Format | Use |
|---|---|
| APK | Direct installation, device testing, beta sharing |
| AAB | Google Play Store distribution |

## Add another alias to the same keystore

You normally do not need a new keystore for another app owned by the same
publisher. You can add another key alias to the existing keystore:

```bash
keytool -genkeypair -v \
  -keystore my-upload-key.jks \
  -alias anotherapp \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=TBTechs, OU=Dev, O=TBTechs, L=Unknown, ST=Unknown, C=US"
```

The command prompts for the keystore and key passwords. Do not add those
passwords directly to the command history.

After adding an alias, verify it:

```bash
keytool -list -keystore my-upload-key.jks
```

Each Android app must continue using the alias it was originally signed with.
For Folio Reader, that alias is:

```text
folioreader
```

## Generate a Base64 copy when an external CI system requires it

Some CI systems accept a Base64-encoded keystore instead of a file. Generate
the value locally:

```bash
base64 -w0 my-upload-key.jks > my-upload-key.jks.base64
```

On macOS, use:

```bash
base64 my-upload-key.jks | tr -d '\n' > my-upload-key.jks.base64
```

Treat the `.base64` file as sensitive as the original keystore. Delete it
after securely transferring the value, and never commit it.

## GitHub Actions note

This repository currently contains a manual Replit workflow for pushing source
changes to GitHub. It does not currently contain a GitHub Actions release
workflow.

If a GitHub Actions release workflow is added later, store the keystore and
passwords as GitHub Actions secrets rather than putting them in workflow YAML.
Typical secret names are:

```text
RELEASE_KEYSTORE_BASE64
RELEASE_STORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

For Folio Reader, `RELEASE_KEY_ALIAS` must be:

```text
folioreader
```

## Upload to Google Play

1. Open the Google Play Console.
2. Create or open the Folio Reader app with application ID
   `com.tbtechs.folioreader`.
3. Open the desired testing or production track.
4. Create a new release.
5. Upload `app-release.aab`.
6. Add release notes and submit the release.

Use the Internal testing track before production when validating the first
release.

## Emergency fingerprint check

If you need to confirm that a build uses the Folio Reader signing identity:

```bash
keytool -list -v \
  -keystore my-upload-key.jks \
  -alias folioreader
```

Compare the SHA-256 fingerprint with your backed-up release records.

## Windows APK signing tools

If an unsigned APK must be aligned and signed manually on Windows, use the
Android SDK Build Tools:

```bat
zipalign.exe -v -p 4 app-release-unsigned.apk app-release-aligned.apk
```

Then sign it with the Folio Reader keystore:

```bat
apksigner.bat sign ^
  --ks "my-upload-key.jks" ^
  --ks-key-alias folioreader ^
  --out app-release-signed.apk ^
  app-release-aligned.apk
```

Verify the result:

```bat
apksigner.bat verify --verbose app-release-signed.apk
```

The verification output should report a valid v2 or v3 signature.