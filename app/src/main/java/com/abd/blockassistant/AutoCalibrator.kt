package com.abd.blockassistant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object AutoCalibrator {
    data class Result(val board: Rect, val pieces: List<Rect>)

    /**
     * Try to detect the 8x8 board by scanning for a grid pattern.
     * If found, saves calibration into SharedPreferences and returns true.
     */
    fun autoCalibrateAndSave(context: Context, screen: Bitmap): Boolean {
        val res = detectBoard(screen) ?: return false
        saveCalibration(context, res)
        return true
    }

    private fun saveCalibration(context: Context, result: Result) {
        val prefs = context.getSharedPreferences("calibration", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("board_left", result.board.left)
            putInt("board_top", result.board.top)
            putInt("board_right", result.board.right)
            putInt("board_bottom", result.board.bottom)

            result.pieces.forEachIndexed { i, r ->
                putInt("piece${i}_left", r.left)
                putInt("piece${i}_top", r.top)
                putInt("piece${i}_right", r.right)
                putInt("piece${i}_bottom", r.bottom)
            }
        }.apply()
    }

    private fun detectBoard(screen: Bitmap): Result? {
        // Downscale to reduce computation
        val targetW = 540
        val scale = if (screen.width > targetW) targetW.toFloat() / screen.width else 1f
        val dw = (screen.width * scale).toInt().coerceAtLeast(1)
        val dh = (screen.height * scale).toInt().coerceAtLeast(1)
        val down = if (scale < 1f) Bitmap.createScaledBitmap(screen, dw, dh, true) else screen

        // Build grayscale array
        val gray = IntArray(down.width * down.height)
        var idx = 0
        for (y in 0 until down.height) {
            for (x in 0 until down.width) {
                val p = down.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = (p) and 0xFF
                gray[idx++] = (r + g + b) / 3
            }
        }

        // Scan candidate squares
        val step = max(8, down.width / 60) // coarse step
        val minSide = (min(down.width, down.height) * 0.25f).toInt()
        val maxSide = (min(down.width, down.height) * 0.9f).toInt()
        val sideSteps = listOf(0.35f, 0.5f, 0.65f, 0.8f).map { s -> (min(down.width, down.height) * s).toInt() }
            .filter { it in minSide..maxSide }

        var bestScore = 0.0
        var bestRect: Rect? = null

        for (side in sideSteps) {
            val maxX = down.width - side - 1
            val maxY = down.height - side - 1
            var y = 0
            while (y <= maxY) {
                var x = 0
                while (x <= maxX) {
                    val score = gridScore(gray, down.width, down.height, x, y, side, side)
                    if (score > bestScore) {
                        bestScore = score
                        bestRect = Rect(x, y, x + side, y + side)
                    }
                    x += step
                }
                y += step
            }
        }

        if (bestRect == null || bestScore < 0.2) { // threshold empirical
            if (down !== screen) down.recycle()
            return null
        }

        // Map back to original coordinates
        val invScale = 1f / scale
        val board = Rect(
            (bestRect!!.left * invScale).toInt(),
            (bestRect.top * invScale).toInt(),
            (bestRect.right * invScale).toInt(),
            (bestRect.bottom * invScale).toInt()
        )

        val pieces = guessPieceRects(screen.width, screen.height, board)
        if (down !== screen) down.recycle()
        return Result(board, pieces)
    }

    // Compute how much a region looks like an 8x8 grid by sampling edge strength along 9 lines each way
    private fun gridScore(gray: IntArray, w: Int, h: Int, x0: Int, y0: Int, rw: Int, rh: Int): Double {
        val cols = 8
        val rows = 8
        val linesX = IntArray(cols + 1) { i -> x0 + (i * rw) / cols }
        val linesY = IntArray(rows + 1) { i -> y0 + (i * rh) / rows }

        // Edge strength across vertical lines (horizontal gradient)
        var vScore = 0.0
        for (i in 0..cols) {
            val x = linesX[i]
            val xL = max(x - 1, x0)
            val xR = min(x + 1, x0 + rw - 1)
            var sum = 0
            var cnt = 0
            for (y in y0 until y0 + rh) {
                val a = gray[y * w + xL]
                val b = gray[y * w + xR]
                sum += abs(a - b)
                cnt++
            }
            vScore += sum.toDouble() / max(1, cnt)
        }

        // Edge strength across horizontal lines (vertical gradient)
        var hScore = 0.0
        for (i in 0..rows) {
            val y = linesY[i]
            val yT = max(y - 1, y0)
            val yB = min(y + 1, y0 + rh - 1)
            var sum = 0
            var cnt = 0
            for (x in x0 until x0 + rw) {
                val a = gray[yT * w + x]
                val b = gray[yB * w + x]
                sum += abs(a - b)
                cnt++
            }
            hScore += sum.toDouble() / max(1, cnt)
        }

        // Normalize by window size to make scores comparable
        val norm = (rw + rh).toDouble()
        return (vScore + hScore) / max(1.0, norm) // rough normalization
    }

    private fun guessPieceRects(screenW: Int, screenH: Int, board: Rect): List<Rect> {
        val top = min(screenH - (screenH * 0.18f).toInt(), max(board.bottom + (board.height() * 0.08f).toInt(), board.bottom + 24))
        val height = (screenH * 0.14f).toInt().coerceAtLeast((board.height() * 0.18f).toInt())
        val left = board.left
        val right = board.right
        val totalW = (right - left)
        val gap = (totalW * 0.04f).toInt()
        val cellW = ((totalW - gap * 2) / 3)
        val r1 = Rect(left, top, left + cellW, min(screenH - 4, top + height))
        val r2 = Rect(r1.right + gap, top, r1.right + gap + cellW, min(screenH - 4, top + height))
        val r3 = Rect(r2.right + gap, top, min(right, r2.right + gap + cellW), min(screenH - 4, top + height))
        return listOf(r1, r2, r3)
    }
}
