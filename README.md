# 🏸 🏓 HaxTracker

[![100% Vibecoded](https://img.shields.io/badge/100%25-Vibecoded-ff69b4?style=for-the-badge&logo=sparkles&logoColor=white)](https://github.com/Haxrox/HaxTracker)
[![Android CI](https://github.com/Haxrox/HaxTracker/actions/workflows/ci.yml/badge.svg?style=for-the-badge)](https://github.com/Haxrox/HaxTracker/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android SDK](https://img.shields.io/badge/Min%20SDK-26%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Wear OS](https://img.shields.io/badge/Wear%20OS-3.0%2B-4285F4?style=for-the-badge&logo=wearos&logoColor=white)](https://developer.android.com/wear)

> **HaxTracker** is a modern, modular, feature-packed score tracking application for **Badminton** and **Pickleball** built for Android Mobile and Wear OS smartwatches using Jetpack Compose.
> 
> ✨ **This application is 100% vibecoded.** ✨ Built from the ground up with AI pair programming, maximum aesthetics, and zero compromises on architectural quality.

---

## 🚀 Key Features

- 🏸 **Badminton Support**: Full support for BWF official scoring rules, serving rotations, deuce handling (win-by-two), and maximum score cap (sudden death at 30 points).
- 🏓 **Pickleball Support**: Complete USAPA official scoring engine including `Score-Receiver-Server` callouts (e.g. `0-0-2`), side-out logic, and first-server exception for doubles.
- 👥 **Singles & Doubles**: Seamless switching between 1v1 Singles and 2v2 Doubles formats.
- 📐 **Dynamic Court Visualization**: Real-time 2D court layout rendering showing active server, active receiver, court positions, side swaps, and net line position.
- ⌚ **Wear OS App & Companion**: Standalone and synchronized Wear OS app with Compose for Wear OS, Tiles, and Watch Face Complications.
- 🔊 **Voice & TTS Announcements**: Built-in Text-to-Speech (TTS) score reader calling out scores and match states automatically after every point.
- 🎛️ **Hardware Volume Control**: Score points effortlessly on court without touching the screen using physical device volume buttons (Volume Up / Down).
- 📳 **Haptic Feedback Patterns**: Distinct haptic vibration signatures for points, game points, and match victories.
- ↩️ **Full Undo / Redo History**: Complete state snapshots allowing instantaneous undo of scoring mistakes.

---

## 🏗️ Architecture & Stack

HaxTracker follows **Clean Architecture** principles and a modern multi-module Kotlin architecture:

```
HaxTracker/
├── core-game/       # Pure Kotlin state machine engine, rule-sets & math logic
├── mobile/          # Android Phone UI (Jetpack Compose, ViewModel, Material 3)
└── wear/            # Wear OS UI (Compose for Wear OS, Tiles, Complications)
```

### Tech Stack
- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose (Mobile) & Compose for Wear OS (Wearables)
- **Design**: Material Design 3 (`androidx.compose.material3`)
- **State Management**: Kotlin StateFlow, Coroutines & Android ViewModel
- **Audio & Hardware**: TextToSpeech API, Android Vibrator API, Key Event Interceptors
- **Build System**: Gradle Kotlin DSL (`.gradle.kts`), Version Catalogs (`libs.versions.toml`)
- **Testing**: JUnit 4 state machine unit tests covering all edge cases & score rule-sets

---

## 🛠️ GitHub Actions CI/CD (1000% Reliability Guarantee)

To guarantee HaxTracker works **1000%** reliably on every single commit and pull request, we maintain automated CI workflows:

- 🧪 **Unit Tests**: Runs complete test suite across `:core-game`, `:mobile`, and `:wear`.
- 🔍 **Android Linting**: Automated code quality and lint checks (`lintDebug`).
- 📦 **Build Verification**: Compiles debug APKs for both Mobile (`:mobile`) and Wear OS (`:wear`).
- 📱 **Multi-SDK Emulator Matrix**: Boots headless AVD emulators across multiple Android SDK versions (API 26, 30, 34), installs the app, launches `MainActivity`, verifies process stability, and captures screenshots.

Workflows location: [`.github/workflows/ci.yml`](.github/workflows/ci.yml)

---

## 💻 Getting Started & Development

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- JDK 17+
- Android SDK 35 (`compileSdk = 35`)

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/Haxrox/HaxTracker.git
cd HaxTracker

# Build the project
./gradlew build

# Run unit tests
./gradlew test

# Assemble Debug APKs
./gradlew assembleDebug
```

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
