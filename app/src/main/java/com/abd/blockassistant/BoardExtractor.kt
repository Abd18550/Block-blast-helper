package com.abd.blockassistant

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect

object BoardExtractor {
    /**
     * Extract an 8x8 board state from a bitmap using adaptive thresholding.
     * Returns Array<IntArray> where each cell is 0 (empty) or 1 (filled).
     * 
     * @param bitmap The source bitmap
     * @param boardRect The rectangle defining the 8x8 board area
     * @return 8x8 array representing board state
     */
    fun extractBoard8x8Adaptive(bitmap: Bitmap, boardRect: Rect): Array<IntArray> {
        val board = Array(8) { IntArray(8) }

        // Clamp rect to bitmap bounds to avoid OOB and empty samples
        val clamped = Rect(
            boardRect.left.coerceIn(0, bitmap.width),
            boardRect.top.coerceIn(0, bitmap.height),
            boardRect.right.coerceIn(0, bitmap.width),
            boardRect.bottom.coerceIn(0, bitmap.height)
        )
        if (clamped.width() < 8 || clamped.height() < 8) {
            return board // too small or invalid; return empty board rather than crash
        }
        
        val cellWidth = clamped.width() / 8f
        val cellHeight = clamped.height() / 8f
        
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val centerX = (clamped.left + (col + 0.5f) * cellWidth).toInt()
                val centerY = (clamped.top + (row + 0.5f) * cellHeight).toInt()
                
                // Sample center and a few points around it
                val samples = mutableListOf<Int>()
                for (dy in -2..2 step 2) {
                    for (dx in -2..2 step 2) {
                        val x = centerX + dx
                        val y = centerY + dy
                        if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
                            samples.add(bitmap.getPixel(x, y))
                        }
                    }
                }
                
                if (samples.isEmpty()) {
                    board[row][col] = 0
                } else {
                    // Calculate average brightness
                    var totalBrightness = 0f
                    for (pixel in samples) {
                        val r = Color.red(pixel)
                        val g = Color.green(pixel)
                        val b = Color.blue(pixel)
                        totalBrightness += (r + g + b) / 3f
                    }
                    val avgBrightness = totalBrightness / samples.size
                    
                    // Use adaptive threshold: cells with low brightness are filled
                    board[row][col] = if (avgBrightness < 128) 1 else 0
                }
            }
        }
        
        return board
    }
    
    /**
     * Extract piece shapes from bitmap regions.
     * Returns a list of piece representations as relative cell coordinates.
     * 
     * @param bitmap The source bitmap
     * @param pieceRects List of rectangles defining piece areas
     * @return List of pieces, each as a list of (row, col) relative coordinates
     */
    fun extractPieces(bitmap: Bitmap, pieceRects: List<Rect>): List<List<Pair<Int, Int>>> {
        val pieces = mutableListOf<List<Pair<Int, Int>>>()
        
        for (rect in pieceRects) {
            // For MVP, return a placeholder piece (1x1 block)
            // In a real implementation, this would analyze the bitmap to detect the piece shape
            pieces.add(listOf(Pair(0, 0)))
        }
        
        return pieces
    }
    
    /**
     * Detect which rows and columns would be cleared by a placement.
     * 
     * @param board The current board state
     * @return Set of line indices (0-7 for rows, 8-15 for cols)
     */
    fun detectClearedLines(board: Array<IntArray>): Set<Int> {
        val cleared = mutableSetOf<Int>()
        
        // Check rows
        for (row in 0 until 8) {
            if (board[row].all { it == 1 }) {
                cleared.add(row)
            }
        }
        
        // Check columns
        for (col in 0 until 8) {
            if ((0 until 8).all { row -> board[row][col] == 1 }) {
                cleared.add(8 + col)
            }
        }
        
        return cleared
    }
}
