package com.maditahraoui.screenbridge

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    companion object {
        private const val REQUEST_CAPTURE = 1001
        private const val REQUEST_NOTIFICATIONS = 1002
    }

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS
            )
        }

        val title = TextView(this).apply {
            text = "ScreenBridge"
            textSize = 26f
            setPadding(32, 48, 32, 24)
        }

        status = TextView(this).apply {
            text = "Screen capture is stopped."
            textSize = 16f
            setPadding(32, 16, 32, 16)
        }

        val start = Button(this).apply {
            text = "Start screen capture"
            setOnClickListener {
                val manager = getSystemService(MediaProjectionManager::class.java)
                startActivityForResult(
                    manager.createScreenCaptureIntent(),
                    REQUEST_CAPTURE
                )
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(status)
            addView(start)
        }

        setContentView(layout)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != REQUEST_CAPTURE || resultCode != RESULT_OK || data == null) {
            status.text = "Screen capture permission was not granted."
            return
        }

        val intent = Intent(this, ScreenCaptureService::class.java).apply {
            putExtra("resultCode", resultCode)
            putExtra("data", data)
        }

        startForegroundService(intent)
        status.text = "Capture active. Saving a screenshot every 5 seconds."
    }
}
