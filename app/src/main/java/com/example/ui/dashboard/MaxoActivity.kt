package com.example.ui.dashboard

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.lifecycleScope
import com.example.R
import com.example.data.pref.PreferenceManager
import com.example.service.FloatingOverlayService
import com.example.ui.components.GlassCard
import com.example.ui.components.MaxoSwitch
import com.example.ui.components.ParticlesBackground
import com.example.ui.components.bouncyClick
import com.example.ui.login.LoginActivity
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GrayBorder
import com.example.ui.theme.GrayLight
import com.example.ui.theme.GrayMedium
import com.example.ui.theme.GraySubtle
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.util.DeviceInfoUtils
import com.example.util.DeviceSnapshot
import com.example.util.SecurityGuard
import com.example.util.SubscriptionDetails
import com.example.util.SubscriptionUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MaxoActivity : ComponentActivity() {

    private lateinit var prefManager: PreferenceManager
    private var onOverlayStateChangedCallback: ((Boolean) -> Unit)? = null

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingService()
                onOverlayStateChangedCallback?.invoke(true)
            } else {
                Toast.makeText(this, "Overlay permission is required for Floating Menu", Toast.LENGTH_SHORT).show()
                onOverlayStateChangedCallback?.invoke(false)
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

        // 2. Comprehensive Anti-Tamper, Root, Frida & Debugger Check
        if (SecurityGuard.isSecurityCompromised()) {
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

        // 4. Periodic 20-Second Cloud License Heartbeat
        lifecycleScope.launch {
            while (isActive) {
                delay(20_000L)
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
            checkServiceRunning = {
                prefManager.isOverlayActive || isServiceRunning(FloatingOverlayService::class.java)
            },
            onToggleOverlay = { enable, callback ->
                onOverlayStateChangedCallback = callback
                if (enable) {
                    requestOverlayAndStart()
                } else {
                    stopFloatingService()
                    callback(false)
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
            onOverlayStateChangedCallback?.invoke(true)
        }
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        prefManager.isOverlayActive = true
        Toast.makeText(this, "FLOATING MENU: ACTIVATED", Toast.LENGTH_SHORT).show()
    }

    private fun stopFloatingService() {
        val intent = Intent(this, FloatingOverlayService::class.java)
        stopService(intent)
        prefManager.isOverlayActive = false
        prefManager.resetFloatingSwitches()
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

    // Refresh live time and device info continuously
    LaunchedEffect(Unit) {
        while (true) {
            subscriptionDetails = SubscriptionUtils.calculateSubscription(userKey, expiryDate)
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            deviceSnapshot = DeviceInfoUtils.getDeviceSnapshot(context)
            delay(3500)
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
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TOP HEADER BAR: LUXURY HUD BAR
                DashboardHeader(onLogout = onLogout)

                Spacer(modifier = Modifier.height(14.dp))

                // 1. HERO LICENSE & TELEMETRY CARD
                HeroLicenseCard(details = subscriptionDetails, userKey = userKey)

                Spacer(modifier = Modifier.height(14.dp))

                // 2. FLOATING MENU MASTER CONTROLLER
                FloatingMenuCard(
                    isRunning = isOverlayRunning,
                    onToggle = onToggleOverlay
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. HARDWARE & PERFORMANCE MATRIX
                HardwareMatrixCard(snapshot = deviceSnapshot)

                Spacer(modifier = Modifier.height(14.dp))

                // 4. SECURITY AUDIT BADGE
                SecurityAuditBadge()

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DashboardHeader(onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x70141414))
            .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PureBlack)
                    .border(1.dp, CardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_launcher_foreground_image),
                    contentDescription = "Red John",
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "RED JOHN",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = PureWhite,
                    letterSpacing = 3.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FF66))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "ONLINE • SYSTEM ENCRYPTED",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00FF66),
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

        // Tactile Bouncy Logout
        IconButton(
            onClick = onLogout,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF202020))
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .bouncyClick(scaleDown = 0.88f) { onLogout() }
                .testTag("logout_button")
        ) {
            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = "Logout",
                tint = PureWhite,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
fun HeroLicenseCard(details: SubscriptionDetails, userKey: String) {
    val context = LocalContext.current
    var copiedRecently by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subscription_card"),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
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
                        modifier = Modifier.size(18.dp)
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
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(PureBlack)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "ACTIVE",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = PureBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // License Key Pill with Copy Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0C0C0C))
                    .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(12.dp))
                    .bouncyClick(scaleDown = 0.96f) {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Red John Key", userKey))
                        copiedRecently = true
                        Toast.makeText(context, "LICENSE KEY COPIED", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "AUTHORIZED KEY",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = GrayLight
                    )
                    Text(
                        text = if (userKey.length > 20) userKey.take(20) + "..." else userKey.ifBlank { "VIP-AUTHORIZED" },
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Key",
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time Clock and Expiry Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoPill(
                    label = "EXPIRATION DATE",
                    value = details.expiryDateFormatted
                )
                InfoPill(
                    label = "CURRENT CLOCK",
                    value = details.currentTime
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Duration Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF1E1E1E),
                                Color(0xFF121212)
                            )
                        )
                    )
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (details.isPermanent) "★ PERMANENT UNLIMITED ACCESS ★" else "REMAINING: ${details.subscriptionLabel}",
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = PureWhite
                )
            }
        }
    }
}

@Composable
private fun InfoPill(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F0F0F))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = GrayLight
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = PureWhite
        )
    }
}

@Composable
fun FloatingMenuCard(
    isRunning: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val cardBorderColor by animateColorAsState(
        targetValue = if (isRunning) Color(0x80FFFFFF) else Color(0x22FFFFFF),
        animationSpec = tween(220),
        label = "floatingCardBorder"
    )

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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isRunning) PureWhite else Color(0xFF1A1A1A))
                            .border(1.dp, CardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = if (isRunning) PureBlack else PureWhite,
                            modifier = Modifier.size(22.dp)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isRunning) Color(0xFF00FF66) else Color(0xFF888888))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isRunning) "FLOATING MENU ACTIVE" else "FLOATING MENU DISABLED",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = if (isRunning) Color(0xFF00FF66) else GrayMedium
                            )
                        }
                    }
                }

                // Interactive Responsive Switch
                MaxoSwitch(
                    checked = isRunning,
                    onCheckedChange = { onToggle(it) },
                    modifier = Modifier.testTag("overlay_toggle_switch")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick feature pill preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniFeatureChip(icon = Icons.Default.Bolt, title = "COMBAT")
                MiniFeatureChip(icon = Icons.Default.Speed, title = "BOOST")
                MiniFeatureChip(icon = Icons.Default.QueryStats, title = "HUD")
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Enables the floating 3D dollar emblem and draggable multi-tab control suite above all games and applications. Tap the dollar button anytime to toggle Aim Bot, Speed, and Hardware Telemetry.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = GrayLight,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Responsive Action Button with Spring Bounce
            Button(
                onClick = { onToggle(!isRunning) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .bouncyClick(scaleDown = 0.94f) { onToggle(!isRunning) }
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
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

@Composable
private fun MiniFeatureChip(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141414))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PureWhite,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = title,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite
        )
    }
}

@Composable
fun HardwareMatrixCard(snapshot: DeviceSnapshot) {
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DEVICE TELEMETRY",
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = snapshot.deviceModel,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = PureWhite
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Telemetry Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    modifier = Modifier.weight(1f),
                    label = "BATTERY TEMP",
                    value = snapshot.temperatureText,
                    icon = Icons.Default.Thermostat
                )
                MetricTile(
                    modifier = Modifier.weight(1f),
                    label = "BATTERY LEVEL",
                    value = snapshot.batteryText,
                    icon = Icons.Default.Speed
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    modifier = Modifier.weight(1f),
                    label = "DEVICE STORAGE",
                    value = snapshot.storageText,
                    icon = Icons.Default.Memory
                )
                MetricTile(
                    modifier = Modifier.weight(1f),
                    label = "OS PLATFORM",
                    value = snapshot.androidVersion,
                    icon = Icons.Default.Security
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // RAM Usage Bar
            Text(
                text = "RAM ALLOCATION (${snapshot.ramText})",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = GrayLight
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { snapshot.ramFraction.coerceIn(0.1f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PureWhite,
                trackColor = Color(0xFF282828),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun MetricTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F0F0F))
            .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(12.dp))
            .padding(11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GrayLight,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.5.sp,
                color = GrayLight
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp,
            color = PureWhite
        )
    }
}

@Composable
private fun SecurityAuditBadge() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x40121212))
            .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = Color(0xFF00FF66),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = "ANTI-TAMPER & DYNAMIC TOKEN SHIELD ACTIVE",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )
        }
        Text(
            text = "20s SYNC",
            fontFamily = FontFamily.Monospace,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00FF66)
        )
    }
}
