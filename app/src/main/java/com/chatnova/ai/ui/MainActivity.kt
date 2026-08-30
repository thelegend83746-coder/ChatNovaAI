package com.chatnova.ai.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.chatnova.ai.ChatNovaApplication
import com.chatnova.ai.domain.model.ThemeMode
import com.chatnova.ai.ui.navigation.NavGraph
import com.chatnova.ai.ui.navigation.Screen
import com.chatnova.ai.ui.theme.ChatNovaTheme
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as ChatNovaApplication).container

        // Check if first launch
        val isFirst = runBlocking {
            val first = appContainer.settingsRepository.isFirstLaunch()
            if (first) {
                appContainer.settingsRepository.setFirstLaunchCompleted()
            }
            first
        }

        setContent {
            val settings by appContainer.settingsRepository.chatSettings.collectAsState(
                initial = com.chatnova.ai.domain.model.ChatSettings()
            )

            ChatNovaTheme(themeMode = settings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        appContainer = appContainer,
                        startDestination = if (isFirst) Screen.Welcome.route else Screen.Chat.route
                    )
                }
            }
        }
    }
}
