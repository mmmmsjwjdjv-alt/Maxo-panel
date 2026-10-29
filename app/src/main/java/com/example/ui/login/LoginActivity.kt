package com.example.ui.login

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.R
import com.example.data.auth.AuthManager
import com.example.data.auth.AuthResult
import com.example.data.pref.PreferenceManager
import com.example.ui.components.GlassCard
import com.example.ui.components.MaxoSwitch
import com.example.ui.components.ParticlesBackground
import com.example.ui.components.bouncyClick
import com.example.ui.dashboard.MaxoActivity
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GrayBorder
import com.example.ui.theme.GrayLight
import com.example.ui.theme.GrayMedium
import com.example.ui.theme.GraySubtle
import com.example.ui.theme.MaxoTheme
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.util.SecurityGuard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    private lateinit var prefManager: PreferenceManager
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefManager = PreferenceManager.getInstance(this)
        authManager = AuthManager(this)

        // Strict anti-tamper and environment check
        if (SecurityGuard.isSecurityCompromised()) {
            Toast.makeText(this, getString(R.string.security_tamper_detected), Toast.LENGTH_LONG).show()
            finishAffinity()
            return
        }

        if (prefManager.isLoggedIn) {
            startDashboardAndFinish()
            return
        }

        val initialSavedKey = if (prefManager.isSavePasswordEnabled) prefManager.savedKey else ""
        val initialSaveState = prefManager.isSavePasswordEnabled
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"

        LoginUiRenderer.setup(
            activity = this,
            initialSavedKey = initialSavedKey,
            initialSaveState = initialSaveState,
            deviceId = deviceId,
            onCopyDeviceId = { copyDeviceIdToClipboard(deviceId) },
            onLoginAttempt = { enteredKey, saveEnabled, onComplete ->
                performLogin(enteredKey, saveEnabled, onComplete)
            }
        )
    }

    private fun copyDeviceIdToClipboard(deviceId: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("device_id", deviceId)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(applicationContext, "YOUR ID COPY ✔️: $deviceId", Toast.LENGTH_LONG).show()

        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(40)
                }
            }
        } catch (_: Exception) {}
    }

    private fun performLogin(
        enteredKey: String,
        savePasswordEnabled: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        val cleanKey = enteredKey.trim()
        if (cleanKey.isEmpty()) {
            Toast.makeText(applicationContext, "لطفاً کلید را وارد کنید", Toast.LENGTH_SHORT).show()
            onComplete(false)
            return
        }

        // Anti-tamper & environment verification before processing login
        if (SecurityGuard.isSecurityCompromised()) {
            Toast.makeText(this, getString(R.string.security_tamper_detected), Toast.LENGTH_LONG).show()
            onComplete(false)
            finishAffinity()
            return
        }

        // Save password preference handling
        prefManager.isSavePasswordEnabled = savePasswordEnabled
        if (savePasswordEnabled) {
            prefManager.savedKey = cleanKey
        } else {
            prefManager.savedKey = ""
        }

        lifecycleScope.launch {
            val result = authManager.authenticate(cleanKey)
            onComplete(false)
            when (result) {
                is AuthResult.Success -> {
                    Toast.makeText(applicationContext, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                    startDashboardAndFinish()
                }
                is AuthResult.KeyNotFound -> {
                    Toast.makeText(applicationContext, getString(R.string.err_wrong_key), Toast.LENGTH_LONG).show()
                }
                is AuthResult.DeviceMismatch -> {
                    Toast.makeText(applicationContext, getString(R.string.err_device_mismatch), Toast.LENGTH_LONG).show()
                }
                is AuthResult.Expired -> {
                    Toast.makeText(applicationContext, getString(R.string.err_expired), Toast.LENGTH_LONG).show()
                }
                is AuthResult.AccountBanned -> {
                    Toast.makeText(applicationContext, "خطا: دسترسی این حساب مسدود شده است", Toast.LENGTH_LONG).show()
                }
                is AuthResult.NetworkError -> {
                    Toast.makeText(applicationContext, getString(R.string.err_network), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun startDashboardAndFinish() {
        val token = SecurityGuard.generateNewSessionToken()
        val intent = Intent(applicationContext, MaxoActivity::class.java).apply {
            putExtra("EXTRA_SESSION_TOKEN", token)
        }
        startActivity(intent)
        finish()
    }
}

@Composable
fun LoginScreen(
    initialKey: String,
    initialSavePassword: Boolean,
    deviceId: String,
    onCopyDeviceId: () -> Unit,
    onLoginAttempt: (key: String, savePassword: Boolean, onComplete: (Boolean) -> Unit) -> Unit
) {
    var keyText by remember { mutableStateOf(initialKey) }
    var passwordVisible by remember { mutableStateOf(false) }
    var savePassword by remember { mutableStateOf(initialSavePassword) }
    var isLoading by remember { mutableStateOf(false) }
    var justCopied by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    ParticlesBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Circular 3D Red John R Emblem
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF2E2E2E), PureBlack)
                            )
                        )
                        .border(1.5.dp, CardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_launcher_foreground_image),
                        contentDescription = "Red John Emblem",
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "RED JOHN",
                    style = androidx.compose.material3.MaterialTheme.typography.displayLarge,
                    color = PureWhite,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "CONTROL SYSTEM",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = GrayLight,
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Centered Glass Login Card
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Key Input Field
                        OutlinedTextField(
                            value = keyText,
                            onValueChange = { keyText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("license_key_input"),
                            label = {
                                Text(
                                    text = "Enter License Key",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                    color = GrayLight
                                )
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (!isLoading && keyText.isNotBlank()) {
                                        isLoading = true
                                        onLoginAttempt(keyText, savePassword) {
                                            isLoading = false
                                        }
                                    }
                                }
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = if (passwordVisible) PureWhite else GrayMedium,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PureWhite,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                cursorColor = PureWhite,
                                focusedContainerColor = Color(0xCC000000),
                                unfocusedContainerColor = Color(0xAA0A0A0A)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // SHOW PASSWORD Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F0F0F))
                                .border(1.dp, GrayBorder, RoundedCornerShape(12.dp))
                                .clickable { passwordVisible = !passwordVisible }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SHOW PASSWORD",
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                color = PureWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            MaxoSwitch(
                                checked = passwordVisible,
                                onCheckedChange = { passwordVisible = it },
                                modifier = Modifier.testTag("show_password_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // SAVE PASSWORD Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F0F0F))
                                .border(1.dp, GrayBorder, RoundedCornerShape(12.dp))
                                .clickable { savePassword = !savePassword }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SAVE PASSWORD",
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                color = PureWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            MaxoSwitch(
                                checked = savePassword,
                                onCheckedChange = { savePassword = it },
                                modifier = Modifier.testTag("save_password_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // DEVICE ID Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xE6080808))
                                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                                .bouncyClick(scaleDown = 0.96f) {
                                    onCopyDeviceId()
                                    justCopied = true
                                    coroutineScope.launch {
                                        delay(2200)
                                        justCopied = false
                                    }
                                }
                                .padding(14.dp)
                                .testTag("device_id_card")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = GrayLight,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "DEVICE ID",
                                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                        color = GrayLight,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AnimatedVisibility(
                                        visible = justCopied,
                                        enter = fadeIn(tween(150)),
                                        exit = fadeOut(tween(150))
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Copied",
                                                tint = PureWhite,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "COPIED",
                                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                                color = PureWhite,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (!justCopied) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Tap to Copy",
                                                tint = GrayLight,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "TAP TO COPY",
                                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                                color = GrayLight
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = deviceId,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        // Login Action Button
                        Button(
                            onClick = {
                                if (!isLoading) {
                                    focusManager.clearFocus()
                                    if (keyText.isNotBlank()) {
                                        isLoading = true
                                        onLoginAttempt(keyText, savePassword) {
                                            isLoading = false
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .bouncyClick(scaleDown = 0.94f) {
                                    if (!isLoading) {
                                        focusManager.clearFocus()
                                        if (keyText.isNotBlank()) {
                                            isLoading = true
                                            onLoginAttempt(keyText, savePassword) {
                                                isLoading = false
                                            }
                                        }
                                    }
                                }
                                .testTag("login_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PureWhite,
                                contentColor = PureBlack,
                                disabledContainerColor = Color(0xFF666666),
                                disabledContentColor = Color(0xFF222222)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = PureBlack,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "AUTHENTICATING...",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                                    color = PureBlack
                                )
                            } else {
                                Text(
                                    text = "LOGIN",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                                    color = PureBlack,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer note
                Text(
                    text = "RED JOHN SECURE ENGINE • V2.0",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = GrayMedium,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Faint subtle owner watermark requested by user
                Text(
                    text = "MAXO",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = GrayMedium.copy(alpha = 0.35f),
                    letterSpacing = 4.sp
                )
            }
        }
    }
}
