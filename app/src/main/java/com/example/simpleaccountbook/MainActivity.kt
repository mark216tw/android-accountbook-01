package com.example.simpleaccountbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.simpleaccountbook.ui.AccountBookApp
import com.example.simpleaccountbook.ui.SimpleAccountBookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val application = application as AccountBookApplication
        setContent {
            val viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(application.repository, application.settingsStore))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            SimpleAccountBookTheme(state.settings) { darkTheme ->
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
                AccountBookApp(state, viewModel)
            }
        }
    }
}
