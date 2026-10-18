package com.neonforge.agent.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import com.neonforge.agent.MainActivity
import com.neonforge.agent.net.DeviceStatusProvider

class OverlayMonitorService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var statusProvider: DeviceStatusProvider? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        statusProvider = DeviceStatusProvider(this)
        createChannel()
        startForeground(1, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Settings.canDrawOverlays(this)) {
            addOverlay()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun addOverlay() {
        if (overlayView != null) return

        val layout = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            val params = WindowManager.LayoutParams(
                220, 300,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 24
                y = 120
            }
            layoutParams = params
        }

        val statusText = android.widget.TextView(this).apply {
            text = "NeonForge 监控中"
            setTextColor(android.graphics.Color.WHITE)
            setPadding(16, 16, 16, 16)
        }
        layout.addView(statusText)

        windowManager.addView(layout, params)
        overlayView = layout
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel("neonforge_overlay", "NeonForge 监控", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun buildNotification(): Notification {
        val open = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, "neonforge_overlay")
            .setContentTitle("NeonForge")
            .setContentText("监控服务运行中")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .build()
    }
}
