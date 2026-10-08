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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.R
import com.example.data.auth.AuthManager
import com.example.data.auth.AuthResult
import com.example.data.pref.PreferenceManager
import com.example.ui.components.GlassCard
import com.example.ui.components.MaxoSwitch
import com.example.ui.components.ParticlesBackground
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

        if (prefManager.isLoggedIn) {
            startDashboardAndFinish()
            return
        }

        val initialSavedKey = if (prefManager.isSavePasswordEnabled) prefManager.savedKey else ""
        val initialSaveState = prefManager.isSavePasswordEnabled
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"

        setContent {
            MaxoTheme {
                LoginScreen(
                    initialKey = initialSavedKey,
                    initialSavePassword = initialSaveState,
                    deviceId = deviceId,
                    onCopyDeviceId = { copyDeviceIdToClipboard(deviceId) },
                    onLoginAttempt = { enteredKey, saveEnabled, onComplete ->
                        performLogin(enteredKey, saveEnabled, onComplete)
                    }
                )
            }
        }
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

        // Save password preference handling
        prefManager.isSavePasswordEnabled = savePasswordEnabled
        if (savePasswordEnabled) {
            prefManager.savedKey = cleanKey
        } else {
            prefManager.savedKey = ""
        }

        // Authenticate in background
        Thread {
            try {
                val url = java.net.URL("https://raw.githubusercontent.com/mmmmsjwjdjv-alt/Maxo/refs/heads/main/User.json")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val reader = java.io.BufferedReader(java.io.InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                conn.disconnect()

                val jsonArray = org.json.JSONArray(sb.toString())

                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: ""

                var keyExists = false
                var deviceMatches = false
                var expired = false
                var allowOffline = false
                var matchedExpiry = ""

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val jsonKey = obj.getString("key")
                    val jsonDeviceId = obj.getString("device_id")

                    if (jsonKey == cleanKey) {
                        keyExists = true

                        if (jsonDeviceId == deviceId) {
                            deviceMatches = true
                            allowOffline = obj.optBoolean("Allowoffline", false)
                            val expiryStr = obj.getString("expirydate")
                            matchedExpiry = expiryStr

                            val sdf = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.US)
                            val expiryDate = sdf.parse(expiryStr)
                            val now = java.util.Date()

                            if (expiryDate != null && now.after(expiryDate)) {
                                expired = true
                            }
                        }
                        break
                    }
                }

                val finalKeyExists = keyExists
                val finalDeviceMatches = deviceMatches
                val finalExpired = expired
                val finalAllowOffline = allowOffline
                val finalExpiry = matchedExpiry

                runOnUiThread {
                    onComplete(false)
                    if (!finalKeyExists) {
                        Toast.makeText(applicationContext, "پسورد اشتباه است", Toast.LENGTH_LONG).show()
                    } else if (!finalDeviceMatches) {
                        Toast.makeText(applicationContext, "این پسورد مربوط به دستگاه دیگری است", Toast.LENGTH_LONG).show()
                    } else if (finalExpired) {
                        Toast.makeText(applicationContext, "اشتراک شما منقضی شده است", Toast.LENGTH_LONG).show()
                    } else {
                        prefManager.saveAuthSuccess(
                            key = cleanKey,
                            devId = deviceId,
                            expiry = finalExpiry,
                            allowOff = finalAllowOffline
                        )
                        Toast.makeText(applicationContext, "لاگین موفق", Toast.LENGTH_SHORT).show()
                        startDashboardAndFinish()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    onComplete(false)
                    Toast.makeText(applicationContext, "خطا در اتصال به اینترنت", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun startDashboardAndFinish() {
        val intent = Intent(applicationContext, MaxoActivity::class.java)
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
    onLoginAttempt: (String, Boolean, (Boolean) -> Unit) -> Unit
) {
    var enteredKey by remember { mutableStateOf(initialKey) }
    var showPassword by remember { mutableStateOf(false) }
    var savePassword by remember { mutableStateOf(initialSavePassword) }
    var isLoading by remember { mutableStateOf(false) }
    var justCopied by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    ParticlesBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
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
                // Header Branding
                Box(
                    modifier = Modifier
                        .size(68.dp)
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
                        contentDescription = "MAXO Emblem",
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "MAXO",
                    style = androidx.compose.material3.MaterialTheme.typography.displayLarge,
                    color = PureWhite
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
                        // Card Header Indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(PureWhite)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SECURE ACCESS",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                    color = GrayLight,
                                    letterSpacing = 1.5.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = GrayLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Password / Key Input
                        OutlinedTextField(
                            value = enteredKey,
                            onValueChange = { enteredKey = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input"),
                            label = {
                                Text(
                                    text = "PASSWORD / KEY",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                    color = GrayLight
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "Enter License Key",
                                    color = GrayMedium,
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (!isLoading) {
                                        isLoading = true
                                        onLoginAttempt(enteredKey, savePassword) {
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedContainerColor = Color(0xCC0E0E0E),
                                unfocusedContainerColor = Color(0x990E0E0E),
                                focusedBorderColor = PureWhite,
                                unfocusedBorderColor = CardBorder,
                                cursorColor = PureWhite
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Switches section
                        // SHOW PASSWORD switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GraySubtle)
                                .border(1.dp, GrayBorder, RoundedCornerShape(12.dp))
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
                                checked = showPassword,
                                onCheckedChange = { showPassword = it },
                                modifier = Modifier.testTag("show_password_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // SAVE PASSWORD switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GraySubtle)
                                .border(1.dp, GrayBorder, RoundedCornerShape(12.dp))
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
                                .clickable {
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
                                        text = "ANDROID DEVICE ID",
                                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                        color = GrayLight
                                    )
                                }

                                AnimatedVisibility(
                                    visible = justCopied,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(PureWhite)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = PureBlack,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "COPIED",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureBlack
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = deviceId,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF222222))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy ID",
                                        tint = PureWhite,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "COPY",
                                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // LOGIN BUTTON
                        Button(
                            onClick = {
                                if (!isLoading) {
                                    focusManager.clearFocus()
                                    isLoading = true
                                    onLoginAttempt(enteredKey, savePassword) {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
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
                    text = "MAXO SECURE ENGINE • V1.0",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = GrayMedium,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
