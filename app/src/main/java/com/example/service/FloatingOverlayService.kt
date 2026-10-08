package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.R
import com.example.data.pref.PreferenceManager
import com.example.ui.dashboard.MaxoActivity
import com.example.ui.theme.MaxoTheme
import com.example.util.DeviceInfoUtils
import com.example.util.FpsTracker

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefManager: PreferenceManager
    private lateinit var overlayLifecycleOwner: OverlayLifecycleOwner
    private val fpsTracker = FpsTracker()

    private var composeView: ComposeView? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private var isExpandedState by mutableStateOf(false)
    private var currentTempState by mutableStateOf("32.0 °C")

    private val handler = Handler(Looper.getMainLooper())
    private val tempRunnable = object : Runnable {
        override fun run() {
            try {
                val snapshot = DeviceInfoUtils.getDeviceSnapshot(applicationContext)
                currentTempState = snapshot.temperatureText
            } catch (_: Exception) {}
            handler.postDelayed(this, 3000)
        }
    }

    companion object {
        const val CHANNEL_ID = "maxo_floating_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.example.service.ACTION_STOP"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefManager = PreferenceManager.getInstance(this)
        overlayLifecycleOwner = OverlayLifecycleOwner()

        createNotificationChannel()
        startForegroundServiceNotification()

        if (Settings.canDrawOverlays(this)) {
            fpsTracker.start()
            handler.post(tempRunnable)
            initComposeOverlay()
            prefManager.isOverlayActive = true
        } else {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MAXO Floating Panel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "MAXO Floating control panel service"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification() {
        val launchIntent = Intent(this, MaxoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MAXO Overlay Active")
            .setContentText("Tap to open MAXO Dashboard")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification, 0)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun getScreenBounds(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            val size = android.graphics.Point()
            @Suppress("DEPRECATION")
            display.getSize(size)
            Pair(size.x, size.y)
        }
    }

    private fun initComposeOverlay() {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 260
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(overlayLifecycleOwner)
            setViewTreeViewModelStoreOwner(overlayLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(overlayLifecycleOwner)

            setContent {
                MaxoTheme {
                    FloatingOverlayUi(
                        userKey = prefManager.userKey,
                        fps = fpsTracker.currentFps,
                        temperature = currentTempState,
                        isPanelExpanded = isExpandedState,
                        onToggleExpand = {
                            isExpandedState = !isExpandedState
                        },
                        onDragDelta = { dx, dy ->
                            windowParams?.let { params ->
                                val (screenWidth, screenHeight) = getScreenBounds()
                                val viewW = composeView?.width?.coerceAtLeast(100) ?: 100
                                val viewH = composeView?.height?.coerceAtLeast(100) ?: 100

                                val maxX = (screenWidth - viewW).coerceAtLeast(0)
                                val maxY = (screenHeight - viewH).coerceAtLeast(0)

                                params.x = (params.x + dx.toInt()).coerceIn(0, maxX)
                                params.y = (params.y + dy.toInt()).coerceIn(0, maxY)

                                composeView?.let { cv ->
                                    try {
                                        windowManager.updateViewLayout(cv, params)
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                    )
                }
            }
        }

        composeView = view
        try {
            windowManager.addView(composeView, windowParams)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        prefManager.isOverlayActive = false
        fpsTracker.stop()
        handler.removeCallbacks(tempRunnable)

        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            composeView = null
        }

        overlayLifecycleOwner.destroy()
    }
}
