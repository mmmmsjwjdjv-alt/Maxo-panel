package com.example.ui.login

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalFocusManager
import com.example.ui.theme.MaxoTheme

object LoginUiRenderer {

    fun setup(
        activity: ComponentActivity,
        initialSavedKey: String,
        initialSaveState: Boolean,
        deviceId: String,
        onCopyDeviceId: () -> Unit,
        onLoginAttempt: (String, Boolean, (Boolean) -> Unit) -> Unit
    ) {
        activity.setContent {
            MaxoTheme {
                LoginScreen(
                    initialKey = initialSavedKey,
                    initialSavePassword = initialSaveState,
                    deviceId = deviceId,
                    onCopyDeviceId = onCopyDeviceId,
                    onLoginAttempt = onLoginAttempt
                )
            }
        }
    }
}
