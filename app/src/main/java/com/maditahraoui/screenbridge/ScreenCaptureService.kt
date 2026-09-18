package com.maditahraoui.screenbridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenCaptureService : Service() {
    companion object {
        private const val CHANNEL_ID = "screenbridge_capture"
        private const val NOTIFICATION_ID = 10
        private const val CAPTURE_INTERVAL_MS = 5_000L
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastSavedAt = 0L

    private val imageListener = ImageReader.OnImageAvailableListener { reader ->
        val now = System.currentTimeMillis()
        if (now - lastSavedAt < CAPTURE_INTERVAL_MS) {
            reader.acquireLatestImage()?.close()
            return@OnImageAvailableListener
        }

        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
        try {
            saveImage(image)
            lastSavedAt = now
        } finally {
            image.close()
        }
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            releaseCaptureResources()
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", -1) ?: return START_NOT_STICKY
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("data", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Intent>("data")
        } ?: return START_NOT_STICKY

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("ScreenBridge")
            .setContentText("Saving a screenshot every 5 seconds")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        if (projection == null) {
            val manager = getSystemService(MediaProjectionManager::class.java)
            projection = manager.getMediaProjection(resultCode, data)
            projection?.registerCallback(projectionCallback, handler)
            startCapture()
        }

        return START_NOT_STICKY
    }

    private fun startCapture() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(
            width,
            height,
            android.graphics.PixelFormat.RGBA_8888,
            2
        )
        imageReader?.setOnImageAvailableListener(imageListener, handler)

        virtualDisplay = projection?.createVirtualDisplay(
            "ScreenBridge",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            handler
        )
    }

    private fun saveImage(image: Image) {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmapWidth = image.width + rowPadding / pixelStride
        val bitmap = Bitmap.createBitmap(
            bitmapWidth,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        val cropped = if (bitmapWidth != image.width) {
            Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
        } else {
            bitmap
        }

        val dir = File(getExternalFilesDir(null), "screenshots")
        if (!dir.exists()) dir.mkdirs()

        val timestamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.US
        ).format(Date())

        val file = File(dir, "screen_$timestamp.jpg")
        FileOutputStream(file).use { output ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 85, output)
        }

        if (cropped !== bitmap) cropped.recycle()
        bitmap.recycle()
    }

    private fun releaseCaptureResources() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        projection?.unregisterCallback(projectionCallback)
        projection?.stop()
        projection = null
    }

    override fun onDestroy() {
        releaseCaptureResources()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
