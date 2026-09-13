package com.haxtech.haxtracker

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.haxtech.haxtracker.core.model.MatchConfig
import com.haxtech.haxtracker.ui.home.HomeScreen
import com.haxtech.haxtracker.ui.match.MatchScreen
import com.haxtech.haxtracker.ui.match.MatchViewModel
import com.haxtech.haxtracker.ui.theme.HaxTrackerTheme
import com.haxtech.haxtracker.ui.theme.PitchBlack

class MainActivity : ComponentActivity() {

    private val matchViewModel: MatchViewModel by viewModels()
    private var isMidGameScreenActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HaxTrackerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = PitchBlack
                ) { innerPadding ->
                    HaxTrackerApp(
                        viewModel = matchViewModel,
                        onScreenChanged = { isMatchScreen ->
                            isMidGameScreenActive = isMatchScreen
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    /**
     * Intercept physical hardware volume keys for mid-game no-look scoring:
     * - Volume Up: Point for current Serving Team
     * - Volume Down: Point for current Receiving Team (Side-Out)
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isMidGameScreenActive) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (matchViewModel.handleVolumeKey(isVolumeUp = true)) {
                        return true
                    }
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (matchViewModel.handleVolumeKey(isVolumeUp = false)) {
                        return true
                    }
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun HaxTrackerApp(
    viewModel: MatchViewModel,
    onScreenChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeMatchConfig by remember { mutableStateOf<MatchConfig?>(null) }

    val isMatchActive = activeMatchConfig != null
    onScreenChanged(isMatchActive)

    if (activeMatchConfig != null) {
        MatchScreen(
            viewModel = viewModel,
            onNavigateBack = {
                activeMatchConfig = null
            },
            modifier = modifier
        )
    } else {
        HomeScreen(
            onStartMatch = { config ->
                viewModel.startMatch(config)
                activeMatchConfig = config
            },
            modifier = modifier
        )
    }
}