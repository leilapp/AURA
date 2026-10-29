package com.leilapp.aura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leilapp.aura.data.AuraSettings
import com.leilapp.aura.ui.navigation.AuraNavHost
import com.leilapp.aura.ui.theme.AuraAccent
import com.leilapp.aura.ui.theme.AuraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val settings = (application as AuraApp).container.settings
        setContent {
            val prefs by settings.settings.collectAsStateWithLifecycle(initialValue = AuraSettings())
            AuraTheme(accent = AuraAccent.from(prefs.accent)) {
                AuraNavHost()
            }
        }
    }
}
