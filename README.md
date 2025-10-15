# Block-blast-helper# Block-blast-helper

Android assistant app (overlay + screen capture + decision engine) for an 8×8 block puzzle helper.

This repo will include:
- Android app (package: `com.abd.blockassistant`)
- Overlay + MediaProjection (screen capture)
- Vision (adaptive 8×8 board + piece extraction via k-means on features)
- Engine (beam search, no rotation)
- CI: GitHub Actions to build debug APK and upload as artifact.