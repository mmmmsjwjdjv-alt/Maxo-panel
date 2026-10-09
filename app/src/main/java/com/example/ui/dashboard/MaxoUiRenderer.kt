package com.example.ui.dashboard

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.service.FloatingOverlayService
import com.example.ui.theme.MaxoTheme

object MaxoUiRenderer {

    fun setup(
        activity: ComponentActivity,
        userKey: String,
        expiryDate: String,
        checkServiceRunning: () -> Boolean,
        onToggleOverlay: (Boolean) -> Unit,
        onLogout: () -> Unit
    ) {
        activity.setContent {
            MaxoTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                var isOverlayRunning by remember { mutableStateOf(checkServiceRunning()) }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isOverlayRunning = checkServiceRunning()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                DashboardScreen(
                    userKey = userKey,
                    expiryDate = expiryDate,
                    isOverlayRunning = isOverlayRunning,
                    onToggleOverlay = onToggleOverlay,
                    onLogout = onLogout
                )
            }
        }
    }
}
