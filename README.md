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

### 🏸 Official Sports Engines & Rule-Sets
- 🏸 **Badminton Scoring Engine (BWF Official)**: Complete support for rally point scoring, dynamic serving court rotation (Right on even, Left on odd), win-by-two deuce extension, and hard cap sudden death at 30 points.
- 🏓 **Pickleball Scoring Engine (USAPA Official)**: Complete side-out scoring engine with official three-part callouts (`0-0-2` / `Score-Receiver-Server`), first-server exception for doubles, and side-out transitions.
- 👥 **Singles & Doubles Formats**: Effortless switching between 1v1 Singles and 2v2 Doubles with automatic player rotation, server handoff, and diagonal receiving assignments.
- 🏆 **Best-of-N Games Progression**: Support for Best of 1, 3, or 5 games with automatic set victory detection, set score archives, and court side swap triggers.

### ⌚ Smartwatch & Wear OS Ecosystem
- ⌚ **Standalone & Companion Modes**: Wear OS app functions 100% standalone on the watch or synchronized in real time with the paired Android phone.
- 📐 **On-Wrist 2D Mini Court**: Real-time rendering of court geometry, active player positions, serving quadrants, and net lines directly on round watch displays.
- ⚡ **Dual-Channel Live Sync**: Seamless two-way state syncing powered by Google Play Services `DataClient` (persistent state distribution) and `MessageClient` (low-latency direct RPC).
- 🧩 **Wear OS Tiles & Complications**:
  - **Tile Provider (`MainTileService`)**: Glanceable tile for quick score previews and one-tap match launches.
  - **Watch Face Complication (`MainComplicationService`)**: Dynamic short-text complication displaying live scores right on your favorite watch face.
- 🔒 **Always-On Screen Keep-Awake**: Automatic `FLAG_KEEP_SCREEN_ON` prevents display timeouts during fast-paced rallies.

### 🎮 Effortless Controls & Rapid Mid-Game Input
- 👆 **Split-Screen Giant Tap Zones**: High-contrast, responsive touch targets with animated ripple feedback for no-look score entry on court.
- 🖐️ **Directional Swipe Gestures (Mobile & Wear OS)**:
  - ⬆️ **Swipe Up**: Award +1 point to Team A
  - ⬇️ **Swipe Down**: Award +1 point to Team B
  - ➡️ **Swipe Right**: Instant Undo of last action
  - ⬅️ **Swipe Left**: Redo undone action
- 🎛️ **Physical Hardware Volume Control**: Log points mid-rally without glancing at the screen using device volume buttons (Volume Up = Serving Point, Volume Down = Receiving Point / Side-out).
- ↩️ **Unlimited Undo / Redo History**: Complete immutable state snapshots allowing instantaneous correction of scoring mistakes.

### 🔊 Audio, Sensory & Spectator Experience
- 🗣️ **Text-to-Speech (TTS) Voice Announcements**: Automatic vocal callouts of scorelines, server assignments, game points, match points, and final victories on both Phone and Watch.
- 📳 **Multi-Tiered Haptic Feedback Patterns**: Distinct vibration signatures differentiating between standard points, deuces, game points, and match victories.
- 👁️ **High-Visibility Spectator Mode**: Full-screen dialog with oversized typography designed for referees, coaches, and spectators viewing from courtside.
- ⏱️ **Rally & Match Timer Tracking**: Tracks rally timestamps and match durations.

---

## 🏗️ Architecture & Multi-Module Stack

HaxTracker is engineered with **Clean Architecture**, separation of concerns, and unidirectional data flow (UDF):

```
HaxTracker/
├── core-game/       # Pure Kotlin state machine engine, rule-sets, models & serializers
├── mobile/          # Android Phone UI (Jetpack Compose, ViewModel, Material 3, TTS)
└── wear/            # Wear OS UI (Compose for Wear OS, Mini Court, Tiles, Complications)
```

### Tech Stack
- **Language**: 100% Kotlin
- **UI Toolkit**: Jetpack Compose (Phone) & Compose for Wear OS (Smartwatches)
- **Design System**: Material Design 3 (`androidx.compose.material3` and Wear Material 3)
- **State Management**: Kotlin StateFlow, Coroutines & Android ViewModel
- **Synchronization**: Google Play Services Wearable Data Layer (`DataClient` & `MessageClient`)
- **Audio & Sensory**: Android `TextToSpeech`, Android `Vibrator` / `VibratorManager` APIs
- **Build System**: Gradle Kotlin DSL (`.gradle.kts`), Version Catalogs (`gradle/libs.versions.toml`)
- **Testing**: JUnit 4 unit tests with comprehensive coverage of sports engines, court algorithms, and sync pipelines

---

## 🛠️ GitHub Actions CI/CD Pipeline

To ensure maximum stability and reliability across every pull request and commit, our automated CI pipeline verifies both mobile and smartwatch platforms:

- 🧪 **Unit Test Suite**: Runs automated unit tests across all modules (`:core-game`, `:mobile`, and `:wear`) covering:
  - BWF Badminton scoring rules, deuce, and 30-point sudden death cap
  - USAPA Pickleball scoring, side-out logic, and first-server exception
  - Dynamic 2D Court position calculations and slot assignments
  - State serialization, round-trip JSON parsing, and action dispatching
  - Phone and Wear sync repositories, state transitions, and undo/redo handling
- 🔍 **Android Lint Analysis**: Strict code quality, manifest integrity, and resource checks via `./gradlew lintDebug`.
- 📦 **Multi-Artifact Build**: Compiles debug APKs for both Mobile (`mobile-debug.apk`) and Wear OS (`wear-debug.apk`).
- 📱 **Mobile Multi-SDK Emulator Matrix**: Boots headless Android Virtual Devices (API 26, 30, 34) on `google_apis` x86_64, installs the mobile APK, launches `MainActivity`, verifies process stability, and archives launch screenshots.
- ⌚ **Wear OS Smartwatch Emulator Matrix**: Boots Wear OS AVDs (API 30, 34) using `android-wear` x86_64 system images and the `wearos_small_round` profile, installs the wearable APK, launches Wear OS `MainActivity`, verifies smartwatch process stability, and captures round watch screenshots.

Workflows configuration: [`.github/workflows/ci.yml`](.github/workflows/ci.yml)

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

# Build all modules
./gradlew build

# Run comprehensive unit tests across core-game, mobile, and wear
./gradlew test

# Run Android lint checks
./gradlew lintDebug

# Assemble Debug APKs for Phone and Smartwatch
./gradlew assembleDebug
```

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
