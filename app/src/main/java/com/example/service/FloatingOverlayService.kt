package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.pref.PreferenceManager
import com.example.ui.dashboard.MaxoActivity
import kotlin.math.abs

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefManager: PreferenceManager

    private var dollarView: View? = null
    private var dollarParams: WindowManager.LayoutParams? = null

    private var panelView: View? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private var isPanelOpen: Boolean = false

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

        createNotificationChannel()
        startForegroundServiceNotification()

        if (Settings.canDrawOverlays(this)) {
            initDollarView()
            initPanelView()
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
            .setContentTitle("MAXO Floating Panel Active")
            .setContentText("Touch dollar icon to open panel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close Overlay", stopPendingIntent)
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

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    private fun initDollarView() {
        val inflater = LayoutInflater.from(this)
        dollarView = inflater.inflate(R.layout.layout_floating_dollar, null)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        dollarParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 350
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoving = false

        dollarView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = dollarParams?.x ?: 0
                    initialY = dollarParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoving = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isMoving = true
                        dollarParams?.x = initialX + dx
                        dollarParams?.y = initialY + dy
                        dollarView?.let { windowManager.updateViewLayout(it, dollarParams) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoving) {
                        togglePanel()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(dollarView, dollarParams)
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    private fun initPanelView() {
        val inflater = LayoutInflater.from(this)
        panelView = inflater.inflate(R.layout.layout_floating_panel, null)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        val txtUser = panelView?.findViewById<TextView>(R.id.txt_panel_user)
        val userKey = prefManager.userKey
        txtUser?.text = "USER: ${if (userKey.length > 12) userKey.take(12) + "..." else userKey.ifBlank { "ACTIVE" }}"

        // Display-only switches - visual only!
        val switchAimBot = panelView?.findViewById<Switch>(R.id.switch_aim_bot)
        val switchAimLock = panelView?.findViewById<Switch>(R.id.switch_aim_lock)
        val switchBoostAim = panelView?.findViewById<Switch>(R.id.switch_boost_aim)
        val switchSpeedMobile = panelView?.findViewById<Switch>(R.id.switch_speed_mobile)

        val dummyListener = { label: String, isChecked: Boolean ->
            Toast.makeText(applicationContext, "$label: ${if (isChecked) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        }

        switchAimBot?.setOnCheckedChangeResponse { isChecked -> dummyListener("AIM BOT", isChecked) }
        switchAimLock?.setOnCheckedChangeResponse { isChecked -> dummyListener("AIM LOCK", isChecked) }
        switchBoostAim?.setOnCheckedChangeResponse { isChecked -> dummyListener("BOOST AIM", isChecked) }
        switchSpeedMobile?.setOnCheckedChangeResponse { isChecked -> dummyListener("SPEED MOBILE", isChecked) }

        // Close button: hides panel, dollar stays visible
        val btnClose = panelView?.findViewById<ImageView>(R.id.btn_close_panel)
        btnClose?.setOnClickListener {
            hidePanel()
        }

        // Exit overlay: closes service completely
        val btnExit = panelView?.findViewById<Button>(R.id.btn_exit_overlay)
        btnExit?.setOnClickListener {
            stopSelf()
        }

        // Draggable header for the panel
        val header = panelView?.findViewById<View>(R.id.panel_header)
        var pInitX = 0
        var pInitY = 0
        var pTouchX = 0f
        var pTouchY = 0f

        header?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    pInitX = panelParams?.x ?: 0
                    pInitY = panelParams?.y ?: 0
                    pTouchX = event.rawX
                    pTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - pTouchX).toInt()
                    val dy = (event.rawY - pTouchY).toInt()
                    panelParams?.x = pInitX + dx
                    panelParams?.y = pInitY + dy
                    if (isPanelOpen && panelView != null) {
                        windowManager.updateViewLayout(panelView, panelParams)
                    }
                    true
                }
                else -> false
            }
        }

        panelView?.visibility = View.GONE
        windowManager.addView(panelView, panelParams)
    }

    private fun Switch.setOnCheckedChangeResponse(action: (Boolean) -> Unit) {
        this.setOnCheckedChangeListener { _, isChecked ->
            action(isChecked)
        }
    }

    private fun togglePanel() {
        if (isPanelOpen) {
            hidePanel()
        } else {
            showPanel()
        }
    }

    private fun showPanel() {
        panelView?.let {
            it.visibility = View.VISIBLE
            isPanelOpen = true
        }
    }

    private fun hidePanel() {
        panelView?.let {
            it.visibility = View.GONE
            isPanelOpen = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        prefManager.isOverlayActive = false

        dollarView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            dollarView = null
        }

        panelView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            panelView = null
        }
    }
}
