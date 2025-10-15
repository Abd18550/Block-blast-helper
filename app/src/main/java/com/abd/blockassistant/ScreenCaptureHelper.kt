package com.abd.blockassistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import java.nio.ByteBuffer

object ScreenCaptureHelper {
    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionManager: MediaProjectionManager? = null
    
    fun hasMediaProjection(): Boolean = mediaProjection != null
    
    fun requestMediaProjection(activity: Activity, requestCode: Int) {
        if (mediaProjectionManager == null) {
            mediaProjectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) 
                as MediaProjectionManager
        }
        
        val intent = mediaProjectionManager?.createScreenCaptureIntent()
        activity.startActivityForResult(intent, requestCode)
    }
    
    fun onMediaProjectionResult(resultCode: Int, data: Intent) {
        if (resultCode == Activity.RESULT_OK) {
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
        }
    }
    
    fun captureScreen(context: Context, callback: (Bitmap?) -> Unit) {
        val projection = mediaProjection ?: run {
            callback(null)
            return
        }
        
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        
        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        val virtualDisplay = projection.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            null
        )
        
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val image = imageReader.acquireLatestImage()
                val bitmap = image?.let { convertImageToBitmap(it) }
                image?.close()
                callback(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            } finally {
                virtualDisplay.release()
                imageReader.close()
            }
        }, 100)
    }
    
    private fun convertImageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        
        return if (rowPadding == 0) {
            bitmap
        } else {
            Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
        }
    }
    
    fun release() {
        mediaProjection?.stop()
        mediaProjection = null
    }
}
