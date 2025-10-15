# Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         ANDROID DEVICE                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌────────────────────┐                                        │
│  │   MainActivity     │                                        │
│  │  ┌──────────────┐  │                                        │
│  │  │ [Calibrate]  │──┼──► Opens CalibratorActivity           │
│  │  └──────────────┘  │         │                              │
│  │  ┌──────────────┐  │         ▼                              │
│  │  │Start Overlay │──┼──► ┌──────────────────────┐           │
│  │  └──────────────┘  │    │ CalibratorActivity   │           │
│  │  ┌──────────────┐  │    │                      │           │
│  │  │   Analyze    │──┼──┐ │ ┌──────────────────┐ │           │
│  │  └──────────────┘  │  │ │ │ CalibrationView  │ │           │
│  │  ┌──────────────┐  │  │ │ │ (Touch Drawing)  │ │           │
│  │  │Status: Ready │  │  │ │ └──────────────────┘ │           │
│  │  └──────────────┘  │  │ │         │            │           │
│  └────────────────────┘  │ │         ▼            │           │
│           │               │ │  SharedPreferences   │           │
│           │               │ │  (board/piece rects) │           │
│           │               │ └──────────────────────┘           │
│           │               │                                     │
│           ▼               │                                     │
│  ┌──────────────────────┐ │                                    │
│  │  ScreenCaptureHelper │ │                                    │
│  │  ┌────────────────┐  │ │                                    │
│  │  │MediaProjection │  │ │                                    │
│  │  │ VirtualDisplay │  │ │                                    │
│  │  │  ImageReader   │  │ │                                    │
│  │  └────────────────┘  │ │                                    │
│  │         │             │ │                                    │
│  │         ▼             │ │                                    │
│  │  ┌────────────────┐  │ │                                    │
│  │  │ Bitmap Capture │  │ │                                    │
│  │  └────────────────┘  │ │                                    │
│  └──────────┬────────────┘ │                                   │
│             │               │                                    │
│             ▼               │                                    │
│  ┌──────────────────────┐  │                                   │
│  │   BoardExtractor     │  │                                   │
│  │  ┌────────────────┐  │  │                                   │
│  │  │ extractBoard   │  │  │                                   │
│  │  │  (adaptive)    │  │  │                                   │
│  │  └────────────────┘  │  │                                   │
│  │         │             │  │                                   │
│  │         ▼             │  │                                   │
│  │  Array<IntArray>      │  │                                   │
│  │  (8x8 board state)    │  │                                   │
│  └──────────┬────────────┘  │                                   │
│             │                │                                    │
│             ▼                │                                    │
│  ┌──────────────────────┐   │     ┌──────────────────────┐     │
│  │  [Future: Engine]    │   │     │   OverlayService      │     │
│  │  Beam Search         │───┼────►│   (Foreground)        │     │
│  │  Placements 1,2,3    │   │     │  ┌──────────────────┐ │     │
│  └──────────────────────┘   │     │  │  Notification    │ │     │
│                              │     │  └──────────────────┘ │     │
│                              │     │           │           │     │
│                              │     │           ▼           │     │
│                              │     │  ┌──────────────────┐ │     │
│                              │     │  │  OverlayView     │ │     │
│                              │     │  │                  │ │     │
│                              │     │  │ • 8x8 Grid      │ │     │
│  ┌────────────────────────┐ │     │  │ • Placements    │ │     │
│  │  GAME APP              │ │     │  │   (1, 2, 3)     │ │     │
│  │  (Block Puzzle)        │ │     │  │ • Line Shading  │ │     │
│  │                        │◄┼─────┼──┤ • Touch-through │ │     │
│  │  8x8 Grid              │ │     │  │                  │ │     │
│  │  Piece Previews (x3)   │ │     │  └──────────────────┘ │     │
│  │                        │ │     │  TYPE_APPLICATION_     │     │
│  └────────────────────────┘ │     │  OVERLAY (Floating)   │     │
│                              │     └──────────────────────┘     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

DATA FLOW:
==========
1. User calibrates: CalibratorActivity → SharedPreferences
2. User starts overlay: MainActivity → OverlayService → OverlayView
3. User analyzes:
   a. MainActivity → ScreenCaptureHelper → Bitmap
   b. Bitmap → BoardExtractor → Array<IntArray>
   c. [Future] Array → DecisionEngine → List<Placement>
   d. List<Placement> → OverlayService → OverlayView.updatePlacements()
4. OverlayView renders suggestions on top of game

PERMISSIONS:
============
SYSTEM_ALERT_WINDOW     → Overlay display
FOREGROUND_SERVICE      → Keep service alive
MEDIA_PROJECTION        → Screen capture
POST_NOTIFICATIONS      → Service notification

THREADING:
==========
UI Thread:    MainActivity, CalibratorActivity, OverlayView
Capture:      Handler.postDelayed (100ms wait)
Future:       WorkManager/Coroutines for heavy computation
```
