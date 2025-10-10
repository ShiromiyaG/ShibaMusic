# ShibaMusic

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="ShibaMusic Icon" width="160" />
</p>

ShibaMusic is an Android music player focused on streaming and offline playback using the Subsonic infrastructure. Built with Kotlin and Jetpack Compose, the app combines a modern UI with advanced features such as smart synchronization, background downloads, and Media3 integration.

## ✨ Highlights
- **100% Compose UI** with organized navigation for library, full player, and a persistent mini player.
- **Subsonic integration** for browsing, syncing, and playing your remote music library.
- **Offline mode** with support for scheduled downloads, progress monitoring, and local playback.
- **Media3 + coroutines** for smooth playback, gapless audio, and efficient resource usage.
- **Hilt DI + Room** for consistent dependency injection and robust data persistence.

## 🏗️ Architecture & Technologies
- Kotlin 1.9+, coroutines and Flow.
- Jetpack Compose (Material 3, Animation, Foundation).
- Navigation Compose, ViewModels and Lifecycle KTX.
- Media3 (Session, ExoPlayer, UI, Cast).
- Room (DAO, Migrations), Retrofit/OkHttp.
- Hilt for DI, WorkManager for background workers and scheduling.

## 🚀 Getting Started

### Prerequisites
- Android Studio Giraffe (or newer).
- JDK 17 (configured via Gradle Toolchain).
- Device/emulator running Android 7.0 (API 24) or higher.

### Setup
```bash
./gradlew --refresh-dependencies
```

### Common build & test commands
```bash
./gradlew assembleDebug          # Debug build
./gradlew testDebugUnitTest      # Unit tests
./gradlew lint                   # Static analysis
```

## 🤝 Contributing
- Avoid duplicating existing utilities (`Preferences`, `MusicUtil`, `OfflineRepository`).
- When altering Room entities, add/update migrations.
- Add tests where practical for critical logic changes.

## 📄 License
ShibaMusic is released under the GNU General Public License v3.0, 
based on the `Tempo` music client by `CappielloAntonio`.

---

This app is based on [Tempo](https://github.com/eddyizm/tempo) 
by eddyizm, licensed under GPL-3.0.
