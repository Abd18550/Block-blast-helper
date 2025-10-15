package com.abd.blockassistant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CalibratorActivity : AppCompatActivity() {
    private lateinit var calibrationView: CalibrationView
    private lateinit var instructionsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibrator)

        val container = findViewById<FrameLayout>(R.id.root) ?: run {
            // If root doesn't exist, wrap the whole view
            val rootView = window.decorView.findViewById<View>(android.R.id.content)
            rootView as FrameLayout
        }

        instructionsText = findViewById(R.id.instructionsText)
        
        calibrationView = CalibrationView(this)
        container.addView(calibrationView, 0)

        findViewById<Button>(R.id.btnDone).setOnClickListener {
            saveCalibration()
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun saveCalibration() {
        val prefs = getSharedPreferences("calibration", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        calibrationView.boardRect?.let { rect ->
            editor.putInt("board_left", rect.left)
            editor.putInt("board_top", rect.top)
            editor.putInt("board_right", rect.right)
            editor.putInt("board_bottom", rect.bottom)
        }
        
        calibrationView.pieceRects.forEachIndexed { index, rect ->
            editor.putInt("piece${index}_left", rect.left)
            editor.putInt("piece${index}_top", rect.top)
            editor.putInt("piece${index}_right", rect.right)
            editor.putInt("piece${index}_bottom", rect.bottom)
        }
        
        editor.apply()
    }

    private inner class CalibrationView(context: Context) : View(context) {
        var boardRect: Rect? = null
        val pieceRects = mutableListOf<Rect>()
        
        private var startX = 0f
        private var startY = 0f
        private var currentX = 0f
        private var currentY = 0f
        private var isDrawing = false
        
        private val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    currentX = event.x
                    currentY = event.y
                    isDrawing = true
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    currentX = event.x
                    currentY = event.y
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDrawing) {
                        val rect = Rect(
                            minOf(startX.toInt(), currentX.toInt()),
                            minOf(startY.toInt(), currentY.toInt()),
                            maxOf(startX.toInt(), currentX.toInt()),
                            maxOf(startY.toInt(), currentY.toInt())
                        )
                        
                        if (boardRect == null) {
                            boardRect = rect
                            instructionsText.text = "Select piece 1"
                        } else if (pieceRects.size < 3) {
                            pieceRects.add(rect)
                            when (pieceRects.size) {
                                1 -> instructionsText.text = "Select piece 2"
                                2 -> instructionsText.text = "Select piece 3"
                                3 -> instructionsText.text = "Tap Done to save"
                            }
                        }
                        
                        isDrawing = false
                        invalidate()
                    }
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            
            // Draw board rect
            boardRect?.let { rect ->
                paint.color = Color.GREEN
                canvas.drawRect(rect, paint)
            }
            
            // Draw piece rects
            pieceRects.forEachIndexed { index, rect ->
                paint.color = when (index) {
                    0 -> Color.CYAN
                    1 -> Color.YELLOW
                    2 -> Color.MAGENTA
                    else -> Color.WHITE
                }
                canvas.drawRect(rect, paint)
            }
            
            // Draw current selection
            if (isDrawing) {
                paint.color = Color.RED
                canvas.drawRect(
                    minOf(startX, currentX),
                    minOf(startY, currentY),
                    maxOf(startX, currentX),
                    maxOf(startY, currentY),
                    paint
                )
            }
        }
    }
}
