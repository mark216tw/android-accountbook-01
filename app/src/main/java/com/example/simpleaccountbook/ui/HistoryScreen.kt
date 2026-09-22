package com.example.simpleaccountbook.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.simpleaccountbook.AppUiState
import com.example.simpleaccountbook.AppViewModel
import com.example.simpleaccountbook.data.EntryType
import com.example.simpleaccountbook.data.LedgerRow
import com.example.simpleaccountbook.data.LedgerTypeFilter
import com.example.simpleaccountbook.displayDate
import com.example.simpleaccountbook.formatMoney
import com.example.simpleaccountbook.toLocalDate
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HistoryScreen(state: AppUiState, viewModel: AppViewModel, padding: PaddingValues) {
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingSaveId by rememberSaveable { mutableLongStateOf(0L) }
    var exportMonth by remember { mutableStateOf<YearMonth?>(null) }
    var categoryMenu by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val grouped = state.visibleRows.groupBy { it.occurredEpochDay.toLocalDate() }.toSortedMap(compareByDescending { it })
    val historicalCategories = state.monthRows
        .filter { state.ledgerTypeFilter == LedgerTypeFilter.ALL || it.type == state.ledgerTypeFilter.name }
        .distinctBy { it.categoryId }
        .sortedBy { it.categoryName }
    val selectedCategoryName = historicalCategories.firstOrNull { it.categoryId == state.ledgerCategoryFilter }?.categoryName

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val month = exportMonth
        exportMonth = null
        if (uri != null && month != null) scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        OutputStreamWriter(output, Charsets.UTF_8).buffered().use { viewModel.exportMonthCsv(month, it) }
                    } ?: error("無法開啟選取的檔案")
                }
            }
            snackbar.showSnackbar(if (result.isSuccess) "CSV 已匯出" else "匯出失敗，檔案可能不完整")
        }
    }

    LaunchedEffect(state.saveResult, pendingSaveId) {
        val result = state.saveResult
        if (pendingSaveId != 0L && result?.requestId == pendingSaveId) {
            if (result.errorMessage == null) editingId = null else snackbar.showSnackbar(result.errorMessage)
            viewModel.acknowledgeSaveResult(pendingSaveId)
            pendingSaveId = 0L
        }
    }

    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { MonthSelector(state.selectedMonth, viewModel::previousMonth, viewModel::nextMonth) }
                    IconButton(
                        onClick = {
                            exportMonth = state.selectedMonth
                            exportLauncher.launch("accountbook-${state.selectedMonth}.csv")
                        },
                        enabled = !state.isExportingCsv,
                    ) { Icon(Icons.Default.Download, "匯出這個月的 CSV") }
                }
            }
            item {
                OutlinedTextField(
                    value = state.ledgerQuery,
                    onValueChange = viewModel::setLedgerQuery,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜尋分類、備註或金額") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                )
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        LedgerTypeFilter.ALL to "全部",
                        LedgerTypeFilter.EXPENSE to "支出",
                        LedgerTypeFilter.INCOME to "收入",
                    ).forEach { (filter, label) ->
                        FilterChip(
                            selected = state.ledgerTypeFilter == filter,
                            onClick = { viewModel.setLedgerTypeFilter(filter) },
                            label = { Text(label) },
                        )
                    }
                }
            }
            item {
                Box {
                    OutlinedButton(onClick = { categoryMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedCategoryName ?: "全部分類", Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                        DropdownMenuItem(text = { Text("全部分類") }, onClick = {
                            viewModel.setLedgerCategoryFilter(null)
                            categoryMenu = false
                        })
                        historicalCategories.forEach { row ->
                            DropdownMenuItem(
                                text = { Text(row.categoryName + if (row.categoryActive) "" else "（已停用）") },
                                onClick = {
                                    viewModel.setLedgerCategoryFilter(row.categoryId)
                                    categoryMenu = false
                                },
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "${state.visibleRows.size} 筆 · 支出 ${formatMoney(state.visibleExpense)} · 收入 ${formatMoney(state.visibleIncome)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.monthRows.isEmpty()) {
                item { EmptyLedger("這個月還沒有帳目", "可切換月份，或從記帳頁新增收支") }
            } else if (state.visibleRows.isEmpty()) {
                item { EmptyLedger("沒有符合搜尋或篩選條件的帳目", "請調整搜尋文字或篩選條件") }
            } else {
                grouped.forEach { (date, entries) ->
                    item(key = "header-$date") {
                        val expense = entries.filter { it.type == EntryType.EXPENSE.name }.sumOf { it.amount }
                        val income = entries.filter { it.type == EntryType.INCOME.name }.sumOf { it.amount }
                        Column {
                            Text(date.displayDate(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("支出 ${formatMoney(expense)}  ·  收入 ${formatMoney(income)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(entries, key = { it.id }) { entry ->
                        LedgerCard(entry) { editingId = entry.id }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    editingId?.let { id ->
        state.monthRows.firstOrNull { it.id == id }?.let { row ->
            EditEntryDialog(
                row = row,
                state = state,
                onDismiss = { editingId = null },
                onSave = { amount, type, categoryId, note, date ->
                    val requestId = System.nanoTime().let { if (it == 0L) 1L else it }
                    if (viewModel.saveEntry(amount, type, categoryId, note, date, row.id, requestId)) pendingSaveId = requestId
                },
                onDelete = {
                    viewModel.deleteEntry(row)
                    editingId = null
                    scope.launch {
                        if (snackbar.showSnackbar("帳目已刪除", "復原") == SnackbarResult.ActionPerformed) viewModel.restoreEntry(row)
                    }
                },
            )
        }
    }
}

@Composable
private fun EmptyLedger(title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LedgerCard(row: LedgerRow, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(row.categoryName, fontWeight = FontWeight.SemiBold)
                Text(if (row.type == EntryType.EXPENSE.name) "支出" else "收入", style = MaterialTheme.typography.labelSmall)
                if (row.note.isNotBlank()) Text(row.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                (if (row.type == EntryType.EXPENSE.name) "− " else "+ ") + formatMoney(row.amount),
                color = if (row.type == EntryType.EXPENSE.name) Color(0xFFD14C43) else Color(0xFF27835C),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EditEntryDialog(
    row: LedgerRow,
    state: AppUiState,
    onDismiss: () -> Unit,
    onSave: (Long, EntryType, Long, String, LocalDate) -> Unit,
    onDelete: () -> Unit,
) {
    var amount by rememberSaveable(row.id) { mutableStateOf(row.amount.toString()) }
    var note by rememberSaveable(row.id) { mutableStateOf(row.note) }
    var typeName by rememberSaveable(row.id) { mutableStateOf(row.type) }
    var categoryId by rememberSaveable(row.id) { mutableLongStateOf(row.categoryId) }
    var epochDay by rememberSaveable(row.id) { mutableLongStateOf(row.occurredEpochDay) }
    val type = EntryType.valueOf(typeName)
    val categories = state.categories.filter { it.type == type.name && (it.isActive || it.id == row.categoryId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("編輯帳目") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(EntryType.EXPENSE to "支出", EntryType.INCOME to "收入").forEach { (item, label) ->
                        FilterChip(selected = type == item, onClick = {
                            typeName = item.name
                            categoryId = state.categories.firstOrNull { it.type == item.name && it.isActive }?.id ?: 0
                        }, label = { Text(label) })
                    }
                }
                OutlinedTextField(amount, { amount = it.filter(Char::isDigit).take(9) }, label = { Text("金額") }, prefix = { Text("NT$ ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(note, { note = it.take(100) }, modifier = Modifier.fillMaxWidth(), label = { Text("備註（選填）") }, supportingText = { Text("${note.length}/100") }, singleLine = true)
                DateButton(LocalDate.ofEpochDay(epochDay), { epochDay = it.toEpochDay() }, Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { category -> FilterChip(category.id == categoryId, { categoryId = category.id }, { Text(category.name) }) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(amount.toLongOrNull() ?: 0, type, categoryId, note, LocalDate.ofEpochDay(epochDay)) }, enabled = (amount.toLongOrNull() ?: 0) > 0 && categoryId != 0L && !state.isSaving) { Text("儲存") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Icon(Icons.Default.Delete, null); Text("刪除") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}
