package com.abd.blockassistant

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var btnCalibrate: Button
    private lateinit var btnStartOverlay: Button
    private lateinit var btnAnalyze: Button

    private val REQUEST_OVERLAY_PERMISSION = 1001
    private val REQUEST_SCREEN_CAPTURE = 1002
    private val REQUEST_CALIBRATE = 1003

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        btnCalibrate = findViewById(R.id.btnCalibrate)
        btnStartOverlay = findViewById(R.id.btnStartOverlay)
        btnAnalyze = findViewById(R.id.btnAnalyze)

        btnCalibrate.setOnClickListener {
            startActivityForResult(Intent(this, CalibratorActivity::class.java), REQUEST_CALIBRATE)
        }

        btnStartOverlay.setOnClickListener {
            startOverlay()
        }

        btnAnalyze.setOnClickListener {
            analyzeScreen()
        }
    }

    private fun startOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
                Toast.makeText(this, R.string.permission_overlay_required, Toast.LENGTH_LONG).show()
                return
            }
        }
        
        val serviceIntent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        statusText.text = getString(R.string.status_overlay_running)
    }

    private fun analyzeScreen() {
        if (!ScreenCaptureHelper.hasMediaProjection()) {
            ScreenCaptureHelper.requestMediaProjection(this, REQUEST_SCREEN_CAPTURE)
            return
        }
        
        statusText.text = getString(R.string.status_analyzing)
        
        // Capture screen and analyze
        ScreenCaptureHelper.captureScreen(this) { bitmap ->
            if (bitmap != null) {
                // TODO: Extract board and analyze
                // For now, just show a placeholder result
                runOnUiThread {
                    statusText.text = "Captured: ${bitmap.width}x${bitmap.height}"
                }
            } else {
                runOnUiThread {
                    statusText.text = "Capture failed"
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_OVERLAY_PERMISSION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(this)) {
                        startOverlay()
                    }
                }
            }
            REQUEST_SCREEN_CAPTURE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    ScreenCaptureHelper.onMediaProjectionResult(resultCode, data)
                    analyzeScreen()
                }
            }
            REQUEST_CALIBRATE -> {
                if (resultCode == Activity.RESULT_OK) {
                    statusText.text = "Calibration saved"
                }
            }
        }
    }
}
