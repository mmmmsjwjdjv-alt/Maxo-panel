package com.example.ui.dashboard

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.pref.PreferenceManager
import com.example.service.FloatingOverlayService
import com.example.ui.components.GlassCard
import com.example.ui.components.MaxoSwitch
import com.example.ui.components.ParticlesBackground
import com.example.ui.login.LoginActivity
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardBorderSubtle
import com.example.ui.theme.GrayBorder
import com.example.ui.theme.GrayLight
import com.example.ui.theme.GrayMedium
import com.example.ui.theme.GraySubtle
import com.example.ui.theme.MaxoTheme
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.util.DeviceInfoUtils
import com.example.util.DeviceSnapshot
import com.example.util.SecurityGuard
import com.example.util.SubscriptionDetails
import com.example.util.SubscriptionUtils
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MaxoActivity : ComponentActivity() {

    private lateinit var prefManager: PreferenceManager

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingService()
            } else {
                Toast.makeText(this, "Overlay permission is required for On The Panel", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefManager = PreferenceManager.getInstance(this)

        if (!prefManager.isLoggedIn) {
            startLoginActivityAndFinish()
            return
        }

        // 1. Session Token Integrity Check
        val sessionToken = intent.getStringExtra("EXTRA_SESSION_TOKEN")
        if (!SecurityGuard.isSessionValid(sessionToken)) {
            Toast.makeText(this, getString(R.string.security_token_invalid), Toast.LENGTH_LONG).show()
            SecurityGuard.invalidateSession()
            prefManager.logout()
            startLoginActivityAndFinish()
            return
        }

        // 2. Anti-Tamper & Debugger Check
        if (SecurityGuard.isDebuggerAttached() || SecurityGuard.isHookFrameworkPresent()) {
            Toast.makeText(this, getString(R.string.security_tamper_detected), Toast.LENGTH_LONG).show()
            SecurityGuard.invalidateSession()
            prefManager.logout()
            finishAffinity()
            return
        }

        // 3. Network Check
        if (!SecurityGuard.isNetworkAvailable(this) && !prefManager.allowOffline) {
            Toast.makeText(this, getString(R.string.security_network_required), Toast.LENGTH_LONG).show()
            SecurityGuard.invalidateSession()
            prefManager.logout()
            startLoginActivityAndFinish()
            return
        }

        // 4. Periodic 30-Second Cloud License Heartbeat
        lifecycleScope.launch {
            while (isActive) {
                delay(30_000L)
                if (!SecurityGuard.isNetworkAvailable(this@MaxoActivity)) {
                    if (!prefManager.allowOffline) {
                        Toast.makeText(this@MaxoActivity, getString(R.string.security_network_required), Toast.LENGTH_LONG).show()
                        stopFloatingService()
                        SecurityGuard.invalidateSession()
                        prefManager.logout()
                        startLoginActivityAndFinish()
                        break
                    }
                } else {
                    val authResult = SecurityGuard.verifyRemoteLicense(this@MaxoActivity, prefManager.userKey)
                    when (authResult) {
                        is com.example.data.auth.AuthResult.Success -> {
                            // License verified and active
                        }
                        else -> {
                            Toast.makeText(this@MaxoActivity, getString(R.string.security_license_revoked), Toast.LENGTH_LONG).show()
                            stopFloatingService()
                            SecurityGuard.invalidateSession()
                            prefManager.logout()
                            startLoginActivityAndFinish()
                            break
                        }
                    }
                }
            }
        }

        MaxoUiRenderer.setup(
            activity = this,
            userKey = prefManager.userKey,
            expiryDate = prefManager.expiryDate,
            checkServiceRunning = { isServiceRunning(FloatingOverlayService::class.java) },
            onToggleOverlay = { enable ->
                if (enable) {
                    requestOverlayAndStart()
                } else {
                    stopFloatingService()
                }
            },
            onLogout = {
                stopFloatingService()
                prefManager.logout()
                startLoginActivityAndFinish()
            }
        )
    }

    private fun requestOverlayAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            startFloatingService()
        }
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "FLOATING MENU: ACTIVATED", Toast.LENGTH_SHORT).show()
    }

    private fun stopFloatingService() {
        val intent = Intent(this, FloatingOverlayService::class.java)
        stopService(intent)
        prefManager.isAimBotEnabled = false
        prefManager.isAimLockEnabled = false
        prefManager.isBoostAimEnabled = false
        prefManager.isSpeedMobileEnabled = false
        Toast.makeText(this, "FLOATING MENU: STOPPED", Toast.LENGTH_SHORT).show()
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun startLoginActivityAndFinish() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@Composable
fun DashboardScreen(
    userKey: String,
    expiryDate: String,
    isOverlayRunning: Boolean,
    onToggleOverlay: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var subscriptionDetails by remember {
        mutableStateOf(SubscriptionUtils.calculateSubscription(userKey, expiryDate))
    }
    var deviceSnapshot by remember {
        mutableStateOf(DeviceInfoUtils.getDeviceSnapshot(context))
    }

    // Refresh live time and device info every second
    LaunchedEffect(Unit) {
        while (true) {
            subscriptionDetails = SubscriptionUtils.calculateSubscription(userKey, expiryDate)
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            deviceSnapshot = DeviceInfoUtils.getDeviceSnapshot(context)
            delay(4000)
        }
    }

    ParticlesBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PureBlack)
                                .border(1.dp, CardBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.app_launcher_foreground_image),
                                contentDescription = "DRAGON",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DRAGON",
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = PureWhite,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "CONTROL DASHBOARD",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                color = GrayLight,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Logout Button
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GraySubtle)
                                .border(1.dp, GrayBorder, CircleShape)
                                .testTag("logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. SUBSCRIPTION CARD
                SubscriptionCard(details = subscriptionDetails)

                Spacer(modifier = Modifier.height(16.dp))

                // 2. DEVICE INFORMATION SECTION
                DeviceInformationCard(snapshot = deviceSnapshot)

                Spacer(modifier = Modifier.height(16.dp))

                // 3. ON THE PANEL CONTROL CARD
                OnThePanelCard(
                    isRunning = isOverlayRunning,
                    onToggle = onToggleOverlay
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SubscriptionCard(details: SubscriptionDetails) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subscription_card"),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Status badge & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LICENSE SUBSCRIPTION",
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Active badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PureWhite)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(PureBlack)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ACTIVE",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = PureBlack,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // User Row
            InfoRow(
                label = "USER",
                value = details.userKey.ifBlank { "ACTIVE_USER" },
                isMono = true,
                valueColor = PureWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subscription summary
            InfoRow(
                label = "SUBSCRIPTION",
                value = details.subscriptionLabel,
                isMono = true,
                valueColor = PureWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Date & Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DATE",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = GrayLight
                    )
                    Text(
                        text = details.currentDate,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = PureWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TIME",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = GrayLight
                    )
                    Text(
                        text = details.currentTime,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = PureWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Grid of Remaining units: Days, Hours, Minutes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPill(
                    title = "DAYS REMAINING",
                    value = details.daysRemaining,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    title = "HOURS REMAINING",
                    value = details.hoursRemaining,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    title = "MINUTES REMAINING",
                    value = details.minutesRemaining,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatPill(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GraySubtle)
            .border(1.dp, GrayBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = if (value == "PERMANENT") 11.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = GrayMedium,
            fontSize = 8.sp,
            maxLines = 1
        )
    }
}

@Composable
fun DeviceInformationCard(snapshot: DeviceSnapshot) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("device_info_card"),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DEVICE INFORMATION",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Model
            DeviceMetricRow(
                icon = Icons.Default.PhoneAndroid,
                title = "DEVICE MODEL",
                value = snapshot.deviceModel,
                secondary = snapshot.androidVersion
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Storage with Progress Bar
            DeviceProgressRow(
                icon = Icons.Default.SdStorage,
                title = "STORAGE",
                value = snapshot.storageText,
                fraction = snapshot.storageFraction
            )

            Spacer(modifier = Modifier.height(12.dp))

            // RAM with Progress Bar
            DeviceProgressRow(
                icon = Icons.Default.Memory,
                title = "RAM",
                value = snapshot.ramText,
                fraction = snapshot.ramFraction
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Battery and Temperature row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Battery
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GraySubtle)
                        .border(1.dp, GrayBorder, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryFull,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "BATTERY",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = GrayLight
                        )
                        Text(
                            text = if (snapshot.isCharging) "${snapshot.batteryText} ⚡" else snapshot.batteryText,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PureWhite
                        )
                    }
                }

                // Temperature
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GraySubtle)
                        .border(1.dp, GrayBorder, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeviceThermostat,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "TEMPERATURE",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = GrayLight
                        )
                        Text(
                            text = snapshot.temperatureText,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PureWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceMetricRow(
    icon: ImageVector,
    title: String,
    value: String,
    secondary: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GraySubtle)
            .border(1.dp, GrayBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PureWhite,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = GrayLight
            )
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = PureWhite
            )
            if (secondary != null) {
                Text(
                    text = secondary,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = GrayMedium
                )
            }
        }
    }
}

@Composable
fun DeviceProgressRow(
    icon: ImageVector,
    title: String,
    value: String,
    fraction: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GraySubtle)
            .border(1.dp, GrayBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = GrayLight
                )
            }
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = PureWhite
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = PureWhite,
            trackColor = Color(0xFF2C2C2C),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun OnThePanelCard(
    isRunning: Boolean,
    onToggle: (Boolean) -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("on_the_panel_card"),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isRunning) PureWhite else Color(0xFF1E1E1E))
                            .border(1.dp, CardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = if (isRunning) PureBlack else PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "FLOATING MENU",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = PureWhite,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isRunning) "FLOATING MENU ACTIVE" else "FLOATING MENU DISABLED",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = if (isRunning) PureWhite else GrayMedium
                        )
                    }
                }

                MaxoSwitch(
                    checked = isRunning,
                    onCheckedChange = { onToggle(it) },
                    modifier = Modifier.testTag("overlay_toggle_switch")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Enables the floating dollar emblem and draggable glassmorphic control panel above all applications. Tap the dollar icon anytime to access instant toggles.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = GrayLight,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { onToggle(!isRunning) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("on_the_panel_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFF222222) else PureWhite,
                    contentColor = if (isRunning) PureWhite else PureBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isRunning) "DEACTIVATE FLOATING MENU" else "ACTIVATE FLOATING MENU",
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    color = if (isRunning) PureWhite else PureBlack,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    isMono: Boolean = false,
    valueColor: Color = PureWhite
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = GrayLight
        )
        Text(
            text = value,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
