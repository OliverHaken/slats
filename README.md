# Slats Android

This is a native Android/Kotlin port of `slats_ver_7.swift`. It keeps the same core loop:

- tap to start
- expanding eight-direction slats
- tap a white line segment to spawn a new slat and score
- game over when all slats leave the screen

## Install Dependencies

The easiest path is Android Studio:

1. Install the latest stable Android Studio.
2. Open this `android/` directory as the project.
3. Let Android Studio install the recommended SDK and Gradle components.
4. Run the `app` configuration on an emulator or Android device.

For command-line testing, install:

- JDK 17
- Gradle 9.5.0 or newer
- Android SDK Platform 37
- Android SDK Build Tools 36.0.0 or newer
- Android SDK Platform Tools

On macOS with Homebrew, a typical setup is:

```sh
brew install openjdk@17 gradle
brew install --cask android-studio
```

Then install Android SDK packages from Android Studio's SDK Manager, or with `sdkmanager`:

```sh
sdkmanager "platform-tools" "platforms;android-37" "build-tools;36.0.0"
```

## Build And Run

From this directory:

```sh
gradle :app:assembleDebug
gradle :app:installDebug
```

If you prefer the Gradle wrapper, open the project in Android Studio and let it create/sync one, or run:

```sh
gradle wrapper --gradle-version 9.5.0
./gradlew :app:assembleDebug
```

## Dependency Versions Used

- Android Gradle Plugin: `9.3.0`
- Kotlin support: built into Android Gradle Plugin 9.x
- Compile SDK / Target SDK: `37`
- Minimum SDK: `23`

## GitHub Pages Web Version

The browser edition is in `docs/` and uses plain HTML, CSS, and JavaScript. It
does not require a build step.

To publish it from a GitHub repository:

1. Push this project to GitHub.
2. Open the repository's **Settings → Pages**.
3. Under **Build and deployment**, select **Deploy from a branch**.
4. Select your main branch and the **/docs** folder, then click **Save**.

GitHub will display the public site URL after deployment finishes.
