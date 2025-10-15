package com.abd.blockassistant

import android.content.Context
import android.graphics.*
import android.view.View

data class Placement(
    val row: Int,
    val col: Int,
    val cells: List<Pair<Int, Int>>,
    val order: Int
)

class OverlayView(context: Context) : View(context) {
    private var boardRect: Rect? = null
    private val placements = mutableListOf<Placement>()
    private val clearedLines = mutableSetOf<Int>() // rows or cols to shade
    
    private val gridPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(100, 255, 255, 255)
    }
    
    private val placementPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    
    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint().apply {
        textSize = 40f
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    init {
        loadCalibration()
    }

    private fun loadCalibration() {
        val prefs = context.getSharedPreferences("calibration", Context.MODE_PRIVATE)
        val left = prefs.getInt("board_left", -1)
        if (left != -1) {
            boardRect = Rect(
                left,
                prefs.getInt("board_top", 0),
                prefs.getInt("board_right", 0),
                prefs.getInt("board_bottom", 0)
            )
        }
    }

    fun updatePlacements(newPlacements: List<Placement>, cleared: Set<Int> = emptySet()) {
        placements.clear()
        placements.addAll(newPlacements)
        clearedLines.clear()
        clearedLines.addAll(cleared)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val board = boardRect ?: return
        
        val cellWidth = board.width() / 8f
        val cellHeight = board.height() / 8f
        
        // Draw grid
        for (i in 0..8) {
            val x = board.left + i * cellWidth
            val y = board.top + i * cellHeight
            canvas.drawLine(x, board.top.toFloat(), x, board.bottom.toFloat(), gridPaint)
            canvas.drawLine(board.left.toFloat(), y, board.right.toFloat(), y, gridPaint)
        }
        
        // Draw cleared lines shading
        fillPaint.color = Color.argb(80, 255, 0, 0)
        for (line in clearedLines) {
            if (line < 8) { // row
                val y = board.top + line * cellHeight
                canvas.drawRect(
                    board.left.toFloat(),
                    y,
                    board.right.toFloat(),
                    y + cellHeight,
                    fillPaint
                )
            } else { // col
                val col = line - 8
                val x = board.left + col * cellWidth
                canvas.drawRect(
                    x,
                    board.top.toFloat(),
                    x + cellWidth,
                    board.bottom.toFloat(),
                    fillPaint
                )
            }
        }
        
        // Draw placements
        for (placement in placements) {
            placementPaint.color = when (placement.order) {
                1 -> Color.CYAN
                2 -> Color.YELLOW
                3 -> Color.MAGENTA
                else -> Color.WHITE
            }
            
            for ((dr, dc) in placement.cells) {
                val row = placement.row + dr
                val col = placement.col + dc
                
                val left = board.left + col * cellWidth
                val top = board.top + row * cellHeight
                val right = left + cellWidth
                val bottom = top + cellHeight
                
                canvas.drawRect(left, top, right, bottom, placementPaint)
            }
            
            // Draw order number at first cell
            val firstCell = placement.cells.firstOrNull() ?: continue
            val row = placement.row + firstCell.first
            val col = placement.col + firstCell.second
            val x = board.left + (col + 0.5f) * cellWidth
            val y = board.top + (row + 0.5f) * cellHeight + textPaint.textSize / 3
            
            canvas.drawText(placement.order.toString(), x, y, textPaint)
        }
    }
}
