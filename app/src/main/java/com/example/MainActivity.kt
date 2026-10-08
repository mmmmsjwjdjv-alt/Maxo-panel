package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.data.pref.PreferenceManager
import com.example.ui.dashboard.MaxoActivity
import com.example.ui.login.LoginActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefManager = PreferenceManager.getInstance(this)
        val targetClass = if (prefManager.isLoggedIn) {
            MaxoActivity::class.java
        } else {
            LoginActivity::class.java
        }

        startActivity(Intent(this, targetClass))
        finish()
    }
}
