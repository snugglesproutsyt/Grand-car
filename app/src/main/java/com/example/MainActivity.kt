package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.engine.GameStateStatus
import com.example.engine.GameViewModel
import com.example.ui.ArmoryScreen
import com.example.ui.GameScreen
import com.example.ui.MainMenuScreen
import com.example.ui.theme.GunmetalDark
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    MENU,
    GAME,
    ARMORY
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GunmetalDark),
                    color = GunmetalDark
                ) {
                    val gameViewModel: GameViewModel = viewModel()
                    ApexTriggerApp(viewModel = gameViewModel)
                }
            }
        }
    }
}

@Composable
fun ApexTriggerApp(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf(AppScreen.MENU) }

    // If game transitions to MENU from inside GameScreen, update local state
    if (uiState.status == GameStateStatus.MENU && currentScreen == AppScreen.GAME) {
        currentScreen = AppScreen.MENU
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            AppScreen.MENU -> {
                MainMenuScreen(
                    viewModel = viewModel,
                    onStartGame = { currentScreen = AppScreen.GAME },
                    onNavigateArmory = { currentScreen = AppScreen.ARMORY }
                )
            }
            AppScreen.GAME -> {
                GameScreen(
                    viewModel = viewModel,
                    onNavigateArmory = { currentScreen = AppScreen.ARMORY }
                )
            }
            AppScreen.ARMORY -> {
                ArmoryScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = AppScreen.MENU }
                )
            }
        }
    }
}
