# Block-blast-helper

Android assistant app (overlay + screen capture + decision engine) for an 8×8 block puzzle helper.

This repo will include:
- Android app (package: `com.abd.blockassistant`)
- Overlay + MediaProjection (screen capture)
- Vision (adaptive 8×8 board + piece extraction via k-means on features)
- Engine (beam search, no rotation)
- CI: GitHub Actions to build debug APK and upload as artifact.

## Project Structure

```
Block-blast-helper/
├── app/
│   ├── build.gradle.kts          # App module build configuration
│   ├── proguard-rules.pro        # ProGuard rules
│   └── src/main/
│       ├── AndroidManifest.xml   # App manifest with permissions
│       ├── java/com/abd/blockassistant/
│       │   ├── MainActivity.kt           # Main activity with 3 buttons
│       │   ├── CalibratorActivity.kt     # Calibration screen
│       │   ├── OverlayService.kt         # Foreground service for overlay
│       │   ├── OverlayView.kt            # Custom view for overlay display
│       │   ├── ScreenCaptureHelper.kt    # MediaProjection helper
│       │   └── BoardExtractor.kt         # Vision/extraction utilities
│       └── res/                  # Resources (layouts, strings, etc.)
├── build.gradle.kts              # Root build configuration
├── settings.gradle.kts           # Gradle settings
└── gradlew                       # Gradle wrapper

```

## Features (MVP)

### 1. MainActivity
Three main buttons:
- **Calibrate**: Opens CalibratorActivity to select the 8×8 board rectangle and three piece rectangles
- **Start Overlay**: Requests overlay permission and starts the OverlayService
- **Analyze / Update**: Captures screen, analyzes it, and updates overlay with suggestions
- Status TextView for feedback

### 2. OverlayService + OverlayView
- Foreground service with persistent notification
- Touch-through overlay (TYPE_APPLICATION_OVERLAY)
- Displays:
  - Semi-transparent 8×8 grid aligned to calibrated board
  - Colored frames for suggested piece placements (labeled 1, 2, 3)
  - Optional shading for rows/columns to be cleared

### 3. ScreenCaptureHelper
- MediaProjection-based screen capture
- Single-frame bitmap capture using ImageReader + VirtualDisplay

### 4. BoardExtractor
Vision/extraction utilities:
- `extractBoard8x8Adaptive()`: Extracts 8×8 board state from bitmap using adaptive thresholding
- `extractPieces()`: Detects piece shapes from calibrated regions
- `detectClearedLines()`: Identifies rows/columns that would be cleared

## Configuration

- **Package**: com.abd.blockassistant
- **compileSdk**: 35
- **targetSdk**: 35
- **minSdk**: 24
- **Android Gradle Plugin**: 8.3.2
- **Kotlin**: 2.0.0
- **Gradle**: 8.10.2

## Permissions

- `SYSTEM_ALERT_WINDOW`: For overlay display
- `FOREGROUND_SERVICE`: For persistent overlay service
- `FOREGROUND_SERVICE_MEDIA_PROJECTION`: For screen capture
- `POST_NOTIFICATIONS`: For service notification

## Building

```bash
./gradlew assembleDebug
```

The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

## CI/CD

GitHub Actions workflow automatically builds the debug APK on pushes and pull requests to main branch. The artifact is uploaded and can be downloaded from the Actions tab.
