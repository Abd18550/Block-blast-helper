package com.abd.blockassistant

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper

object OverlayServiceController {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var lastOverlayView: OverlayView? = null

    fun updateOverlay(context: Context, placements: List<Placement>, cleared: Set<Int>) {
        // Try to find the overlay view via the service's instance
        val view = lastOverlayView ?: findOverlayViewFromService(context)?.also { lastOverlayView = it }
        view?.updatePlacements(placements, cleared)
    }

    fun startAnalyzeLoop(context: Context, intervalMs: Long = 800L) {
        if (running) return
        running = true

        val prefs = context.getSharedPreferences("calibration", Context.MODE_PRIVATE)
        val boardLeft = prefs.getInt("board_left", -1)
        if (boardLeft == -1) {
            running = false
            return
        }
        val boardTop = prefs.getInt("board_top", 0)
        val boardRight = prefs.getInt("board_right", 0)
        val boardBottom = prefs.getInt("board_bottom", 0)
        val boardRect = Rect(boardLeft, boardTop, boardRight, boardBottom)

        val tick = object : Runnable {
            override fun run() {
                if (!running) return
                ScreenCaptureHelper.captureScreen(context) { bmp ->
                    try {
                        if (bmp != null) {
                            val board = BoardExtractor.extractBoard8x8Adaptive(bmp, boardRect)
                            val (placements, cleared) = Solver.computeGreedyBestCells(board, 3)
                            updateOverlay(context, placements, cleared)
                        }
                    } catch (t: Throwable) {
                        android.util.Log.e("OverlayLoop", "analyze tick error", t)
                    }
                }
                handler.postDelayed(this, intervalMs)
            }
        }
        handler.post(tick)
    }

    fun stopAnalyzeLoop() {
        running = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun findOverlayViewFromService(context: Context): OverlayView? {
        // We don't have a direct reference; rely on a static hook set by the Service
        return OverlayServiceAccessor.overlayView
    }
}

internal object OverlayServiceAccessor {
    // The Service sets this when it creates the view
    @Volatile
    var overlayView: OverlayView? = null
}
