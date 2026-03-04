# GCW App

This is an Android project that loads `https://www.qsgl.net/html/gcw/index.html#/` in a WebView.

## Prerequisites

- JDK 17 or higher
- Android SDK
- Gradle (or use the provided wrapper if generated, otherwise install Gradle)

## Project Structure

- `app/src/main/java/com/example/gcw/MainActivity.java`: The main activity containing the WebView.
- `app/src/main/AndroidManifest.xml`: Application manifest with internet permission.
- `app/build.gradle`: App-level build configuration with signing config.
- `release-key.keystore`: The keystore used for signing the APK.

## Signing Details

The project is configured to sign the release build automatically using the generated keystore.

- **Keystore**: `app/release-key.keystore`
- **Alias**: `key0`
- **Store Password**: `password`
- **Key Password**: `password`

## How to Build

1. Open the project in Android Studio.
2. Allow it to sync and download dependencies.
3. Build the project: `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
4. Locate the APK in `app/build/outputs/apk/release/app-release.apk`.

Alternatively, via command line (if Gradle and Android SDK are configured):

```bash
gradle assembleRelease
```

The signed APK will be generated at `app/build/outputs/apk/release/app-release.apk`.
