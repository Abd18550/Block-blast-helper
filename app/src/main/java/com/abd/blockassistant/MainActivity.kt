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
    private lateinit var btnStart: Button

    private val REQUEST_OVERLAY_PERMISSION = 1001
    private val REQUEST_SCREEN_CAPTURE = 1002
    private val REQUEST_CALIBRATE = 1003
    private val REQUEST_POST_NOTIFICATIONS = 1004

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
    btnCalibrate = findViewById(R.id.btnCalibrate)
    btnStartOverlay = findViewById(R.id.btnStartOverlay)
    btnAnalyze = findViewById(R.id.btnAnalyze)
    btnStart = findViewById(R.id.btnStart)

        btnCalibrate.setOnClickListener {
            startActivityForResult(Intent(this, CalibratorActivity::class.java), REQUEST_CALIBRATE)
        }

        btnStartOverlay.setOnClickListener {
            startOverlay()
        }

        btnAnalyze.setOnClickListener { analyzeScreen() }
    btnStart.setOnClickListener { startEverythingAndLaunchGame() }
    }

    private fun startOverlay() {
        // Android 13+ requires runtime notifications permission to post foreground service notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_POST_NOTIFICATIONS)
                // we'll resume onActivityResult/permission callback
                return
            }
        }
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
                val prefs = getSharedPreferences("calibration", MODE_PRIVATE)
                val boardLeft = prefs.getInt("board_left", -1)
                if (boardLeft == -1) {
                    runOnUiThread { statusText.text = "Please calibrate first" }
                    return@captureScreen
                }
                val boardTop = prefs.getInt("board_top", 0)
                val boardRight = prefs.getInt("board_right", 0)
                val boardBottom = prefs.getInt("board_bottom", 0)
                val boardRect = android.graphics.Rect(boardLeft, boardTop, boardRight, boardBottom)

                val board = BoardExtractor.extractBoard8x8Adaptive(bitmap, boardRect)
                val (placements, cleared) = Solver.computeGreedyBestCells(board, 3)

                // Update overlay if running
                OverlayServiceController.updateOverlay(this, placements, cleared)
                runOnUiThread { statusText.text = "Updated suggestions (${placements.size})" }
            } else {
                runOnUiThread {
                    statusText.text = "Capture failed"
                }
            }
        }
    }

    private fun startEverythingAndLaunchGame() {
        // Request overlay if needed, then request screen capture, then start overlay and begin a periodic analyze loop.
        val needsOverlayPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)
        if (needsOverlayPerm) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            Toast.makeText(this, R.string.permission_overlay_required, Toast.LENGTH_LONG).show()
            return
        }

        // Start overlay immediately
        startOverlay()

        // Request screen capture if not granted yet
        if (!ScreenCaptureHelper.hasMediaProjection()) {
            ScreenCaptureHelper.requestMediaProjection(this, REQUEST_SCREEN_CAPTURE)
        } else {
            // Start periodic analysis right away
            OverlayServiceController.startAnalyzeLoop(applicationContext)
        }

        // Try to launch the Block Blast game
        launchBlockBlastGame()
    }

    private fun launchBlockBlastGame() {
        // Heuristic known package names; user can adjust package in settings later
        val candidates = listOf(
            "com.blockblast.puzzle", // example placeholder
            "com.blockblastgame.puzzle",
            "com.block.blaster" // fallback guesses
        )
        for (pkg in candidates) {
            val launch = packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                startActivity(launch)
                return
            }
        }
        Toast.makeText(this, "Could not find Block Blast app. Open it manually.", Toast.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_OVERLAY_PERMISSION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(this)) {
                        startOverlay()
                        // If startEverything was pressed, continue with capture loop
                        if (ScreenCaptureHelper.hasMediaProjection()) {
                            OverlayServiceController.startAnalyzeLoop(this)
                        } else {
                            ScreenCaptureHelper.requestMediaProjection(this, REQUEST_SCREEN_CAPTURE)
                        }
                    }
                }
            }
            REQUEST_SCREEN_CAPTURE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    ScreenCaptureHelper.onMediaProjectionResult(resultCode, data)
                    // If overlay is running, begin loop; else just one-shot analyze
                    OverlayServiceController.startAnalyzeLoop(applicationContext)
                }
            }
            REQUEST_CALIBRATE -> {
                if (resultCode == Activity.RESULT_OK) {
                    statusText.text = "Calibration saved"
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            // Retry starting overlay regardless; if denied, user can still see limited behavior
            startOverlay()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        OverlayServiceController.stopAnalyzeLoop()
        // Keep projection for background overlay usage; comment out to fully stop
        // ScreenCaptureHelper.release()
    }
}
