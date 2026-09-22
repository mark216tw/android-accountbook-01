package com.example.simpleaccountbook.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.simpleaccountbook.AppUiState
import com.example.simpleaccountbook.AppViewModel
import com.example.simpleaccountbook.UiEvent

private enum class Screen(val label: String, val icon: ImageVector) {
    HOME("記帳", Icons.Default.AddCircle),
    HISTORY("流水帳", Icons.Default.History),
    SETTINGS("設定", Icons.Default.Settings),
    CATEGORIES("分類管理", Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBookApp(state: AppUiState, viewModel: AppViewModel) {
    var screenName by rememberSaveable { mutableStateOf(Screen.HOME.name) }
    val screen = Screen.valueOf(screenName)
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is UiEvent.Message) snackbar.showSnackbar(event.text)
        }
    }
    if (screen == Screen.SETTINGS || screen == Screen.CATEGORIES) {
        BackHandler { screenName = if (screen == Screen.CATEGORIES) Screen.SETTINGS.name else Screen.HOME.name }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (screen) {
                            Screen.HOME -> "簡單記帳"
                            else -> screen.label
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    if (screen == Screen.SETTINGS || screen == Screen.CATEGORIES) {
                        androidx.compose.material3.IconButton(
                            onClick = { screenName = if (screen == Screen.CATEGORIES) Screen.SETTINGS.name else Screen.HOME.name },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    }
                },
                actions = {
                    if (screen == Screen.HOME) {
                        IconButton(onClick = { screenName = Screen.SETTINGS.name }) {
                            Icon(Icons.Default.Settings, "設定")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (screen == Screen.HOME || screen == Screen.HISTORY) {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding().height(64.dp),
                    windowInsets = WindowInsets(0),
                ) {
                    listOf(Screen.HOME, Screen.HISTORY).forEach { item ->
                        NavigationBarItem(
                            selected = screen == item,
                            onClick = { screenName = item.name },
                            icon = { Icon(item.icon, item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        when (screen) {
            Screen.HOME -> HomeScreen(state, viewModel, padding)
            Screen.HISTORY -> HistoryScreen(state, viewModel, padding)
            Screen.SETTINGS -> SettingsScreen(state, viewModel, padding, onCategories = { screenName = Screen.CATEGORIES.name })
            Screen.CATEGORIES -> CategoryScreen(state, viewModel, padding)
        }
    }
}
