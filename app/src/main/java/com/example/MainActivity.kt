package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ThemePreferences
import com.example.ui.navigation.ArkAppRoot
import com.example.ui.theme.ArkSocialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePrefs = remember { ThemePreferences(applicationContext) }
            val storedDarkMode by themePrefs.darkModeFlow.collectAsStateWithLifecycle(initialValue = null)
            val useDarkTheme = storedDarkMode ?: isSystemInDarkTheme()

            ArkSocialTheme(darkTheme = useDarkTheme) {
                ArkAppRoot(
                    themePreferences = themePrefs,
                    isDarkMode = useDarkTheme
                )
            }
        }
    }
}
