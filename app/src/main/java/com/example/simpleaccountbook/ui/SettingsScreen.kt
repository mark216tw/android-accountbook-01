package com.example.simpleaccountbook.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.simpleaccountbook.AppUiState
import com.example.simpleaccountbook.AppViewModel
import com.example.simpleaccountbook.data.BackupCodec
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(state: AppUiState, viewModel: AppViewModel, padding: PaddingValues, onCategories: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var pendingRestore by remember { mutableStateOf<String?>(null) }
    val appVersion = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val content = withContext(Dispatchers.IO) { viewModel.createBackup(appVersion) }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }
                        ?: error("無法開啟備份檔")
                }
            }.onSuccess { snackbar.showSnackbar("備份已匯出") }
                .onFailure { snackbar.showSnackbar(it.message ?: "備份匯出失敗") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { BackupCodec.readUtf8WithLimit(it) }
                        ?: error("無法開啟備份檔")
                }
            }.onSuccess { pendingRestore = it }
                .onFailure { snackbar.showSnackbar(it.message ?: "備份讀取失敗") }
        }
    }

    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        item {
            SettingSection("顯示模式") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("SYSTEM" to "系統", "LIGHT" to "淺色", "DARK" to "深色").forEach { (id, label) ->
                        val selected = state.settings.displayMode == id
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setDisplayMode(id) },
                            label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (selected) ({ Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }) else null,
                        )
                    }
                }
            }
        }
        item {
            SettingSection("主題色彩") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemePresets.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { preset ->
                                val selected = state.settings.themeId == preset.id
                                Card(
                                    modifier = Modifier.weight(1f).clickable { viewModel.setPresetTheme(preset.id, preset.hue) },
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(Modifier.size(28.dp).clip(CircleShape).background(preset.color), contentAlignment = Alignment.Center) {
                                            if (selected) Icon(Icons.Default.Check, "已選擇", tint = Color.White)
                                        }
                                        Text(
                                            preset.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            SettingSection("自訂色彩") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.size(30.dp).clip(CircleShape)
                            .background(Color.hsl(state.settings.customHue, .58f, .45f)),
                    )
                    Box(Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(
                                Brush.horizontalGradient(
                                    listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
                                ),
                            ),
                        )
                        Slider(
                            value = state.settings.customHue,
                            onValueChange = viewModel::setCustomHue,
                            valueRange = 0f..360f,
                            modifier = Modifier.semantics { contentDescription = "自訂主題色彩" },
                            colors = SliderDefaults.colors(activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent),
                        )
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().clickable(onClick = onCategories), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Category, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text("分類管理", fontWeight = FontWeight.Bold)
                        Text("新增、改名、排序或停用分類", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                }
            }
        }
        item {
            SettingSection("資料備份") {
                Text("備份包含分類、帳目與每月預算。還原前會先驗證檔案，並完整取代目前帳務資料。", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            val date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                            exportLauncher.launch("accountbook-backup-$date.json")
                        },
                        modifier = Modifier.weight(1f),
                    ) { Icon(Icons.Default.Download, null); Text("匯出 JSON") }
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                        modifier = Modifier.weight(1f),
                    ) { Icon(Icons.Default.Upload, null); Text("還原 JSON") }
                }
            }
        }
        item {
            Text("簡單記帳單機版 1.0.0-prerelease", Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    pendingRestore?.let { content ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("取代目前帳務資料？") },
            text = { Text("系統會先驗證備份檔。驗證成功後，目前所有分類、帳目與預算將被備份內容取代，此操作無法復原。") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    pendingRestore = null
                    scope.launch {
                        runCatching { withContext(Dispatchers.IO) { viewModel.restoreBackup(content) } }
                            .onSuccess { document -> snackbar.showSnackbar("已還原 ${document.transactions.size} 筆帳目") }
                            .onFailure { snackbar.showSnackbar(it.message ?: "備份還原失敗") }
                    }
                }) { Text("確認還原") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { pendingRestore = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        content()
    }
}
