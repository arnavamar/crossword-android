package com.crossword.game

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crossword.game.ui.CrosswordTheme
import com.crossword.game.ui.GameScreen
import com.crossword.game.ui.HomeScreen
import com.crossword.game.ui.LevelsScreen
import com.crossword.game.ui.Palette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Light bars with dark icons, whatever the system theme: the app is always on paper.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { CrosswordTheme { CrosswordApp() } }
    }
}

@Composable
fun CrosswordApp(vm: AppViewModel = viewModel()) {
    val screen = vm.screen
    BackHandler(enabled = screen != Screen.Home, onBack = vm::onSystemBack)
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Paper)
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)),
    ) {
        key(screen) {
            when (screen) {
                Screen.Home -> HomeScreen(vm)
                is Screen.Levels -> LevelsScreen(vm, screen.difficulty)
                is Screen.Game -> GameScreen(vm, screen.difficulty, screen.level)
            }
        }
    }
}
