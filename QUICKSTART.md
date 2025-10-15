# Quick Start Guide

## Prerequisites
- JDK 17 or higher
- Android SDK (will be downloaded automatically by Gradle)
- Android Studio (recommended) or command-line tools

## Building the Project

### Using Gradle (Command Line)

1. Clone the repository:
```bash
git clone https://github.com/Abd18550/Block-blast-helper.git
cd Block-blast-helper
```

2. Build the debug APK:
```bash
./gradlew assembleDebug
```

3. Find the APK at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Using Android Studio

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the project directory
4. Wait for Gradle sync to complete
5. Click "Build" → "Build Bundle(s) / APK(s)" → "Build APK(s)"

## Installing on Device

### Via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Via Android Studio
1. Connect your device or start an emulator
2. Click the "Run" button (green play icon)
3. Select your device

## Using the App

### Step 1: Grant Permissions
On first launch, the app will request:
- Overlay permission (for displaying suggestions)
- Media projection permission (for screen capture)
- Notification permission (Android 13+)

### Step 2: Calibration
1. Open your block puzzle game
2. Launch Block Assistant
3. Tap "Calibrate"
4. Draw a rectangle around the 8×8 game board
5. Draw rectangles around the three piece preview areas
6. Tap "Done"

### Step 3: Start Overlay
1. Tap "Start Overlay"
2. If prompted, grant overlay permission in system settings
3. Return to the app
4. You should see a semi-transparent grid overlay

### Step 4: Get Suggestions
1. Switch to your game
2. Return to Block Assistant (overlay stays visible)
3. Tap "Analyze / Update"
4. The overlay will show numbered suggestions (1, 2, 3)
5. Place pieces in the suggested order

## Troubleshooting

### Overlay Not Visible
- Ensure overlay permission is granted
- Check if the service is running (notification should be visible)
- Try stopping and restarting the overlay

### Screen Capture Fails
- Grant media projection permission when prompted
- Some devices may not support screen capture
- Check Android version (requires API 21+)

### Calibration Issues
- Ensure the game board is fully visible
- Draw rectangles precisely
- Re-calibrate if game resolution changes

### Build Fails
- Ensure internet connection for Gradle dependencies
- Check JDK version (requires 17+)
- Clear Gradle cache: `./gradlew clean`
- Invalidate caches in Android Studio

## Development

### Project Structure
```
app/src/main/java/com/abd/blockassistant/
├── MainActivity.kt           # Main entry point
├── CalibratorActivity.kt     # Calibration UI
├── OverlayService.kt         # Background service
├── OverlayView.kt            # Overlay rendering
├── ScreenCaptureHelper.kt    # Screen capture
└── BoardExtractor.kt         # Vision/extraction
```

### Key Classes
- **MainActivity**: Coordinates user actions
- **CalibratorActivity**: Handles calibration
- **OverlayService**: Manages overlay lifecycle
- **OverlayView**: Draws suggestions on screen
- **ScreenCaptureHelper**: Captures screen frames
- **BoardExtractor**: Extracts game state from image

### Adding Features

#### To Add Decision Engine:
1. Create `DecisionEngine.kt`
2. Implement search algorithm (beam search recommended)
3. Call from `MainActivity.analyzeScreen()` after board extraction
4. Pass results to `OverlayView.updatePlacements()`

#### To Improve Vision:
1. Enhance `BoardExtractor.extractBoard8x8Adaptive()`
2. Add color-based detection
3. Implement ML-based piece recognition
4. Add calibration validation

#### To Add Settings:
1. Create `SettingsActivity.kt`
2. Add preferences for sensitivity, colors, etc.
3. Link from MainActivity
4. Apply settings in OverlayView

## Testing

### Manual Testing Checklist
- [ ] Calibration saves correctly
- [ ] Overlay displays on other apps
- [ ] Screen capture works
- [ ] Board extraction returns valid data
- [ ] Placements display correctly
- [ ] Service survives app backgrounding
- [ ] Permissions are requested properly

### Device Requirements
- Android 7.0 (API 24) or higher
- Screen: 1080p or higher recommended
- RAM: 2GB minimum

## Performance Tips
- Screen capture is expensive - limit frequency
- Cache calibration data
- Use Handler.postDelayed() for timing control
- Consider using WorkManager for background analysis

## Support
For issues, feature requests, or contributions:
- GitHub Issues: https://github.com/Abd18550/Block-blast-helper/issues
- Pull Requests welcome!

## License
See LICENSE file for details.
