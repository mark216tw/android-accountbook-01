package com.example.simpleaccountbook.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleaccountbook.AppUiState
import com.example.simpleaccountbook.AppViewModel
import com.example.simpleaccountbook.BudgetLevel
import com.example.simpleaccountbook.budgetLevel
import com.example.simpleaccountbook.data.EntryType
import com.example.simpleaccountbook.formatMoney
import java.time.LocalDate

@Composable
fun HomeScreen(state: AppUiState, viewModel: AppViewModel, padding: PaddingValues) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var typeName by rememberSaveable { mutableStateOf(state.settings.lastEntryType) }
    var selectedCategory by rememberSaveable { mutableLongStateOf(0L) }
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var pendingSaveId by rememberSaveable { mutableLongStateOf(0L) }
    var note by rememberSaveable { mutableStateOf("") }
    var noteExpanded by rememberSaveable { mutableStateOf(false) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var showBudgetDialog by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val type = EntryType.entries.firstOrNull { it.name == typeName } ?: EntryType.EXPENSE
    val categories = state.categories.filter { it.type == type.name && it.isActive }
    val amount = amountText.toLongOrNull() ?: 0L

    LaunchedEffect(state.categories, state.settings) {
        if (!initialized && state.categories.isNotEmpty()) {
            typeName = state.settings.lastEntryType.takeIf { name -> EntryType.entries.any { it.name == name } } ?: EntryType.EXPENSE.name
            val initialType = EntryType.valueOf(typeName)
            val active = state.categories.filter { it.type == initialType.name && it.isActive }
            selectedCategory = state.recentCategoryId(initialType).takeIf { recent -> active.any { it.id == recent } } ?: active.firstOrNull()?.id ?: 0
            initialized = true
        }
    }
    LaunchedEffect(state.saveResult, pendingSaveId) {
        val result = state.saveResult
        if (pendingSaveId != 0L && result?.requestId == pendingSaveId) {
            if (result.errorMessage == null) {
                amountText = ""
                note = ""
                noteExpanded = false
                selectedEpochDay = LocalDate.now().toEpochDay()
                snackbar.showSnackbar("帳目已儲存")
            } else {
                snackbar.showSnackbar(result.errorMessage)
            }
            viewModel.acknowledgeSaveResult(pendingSaveId)
            pendingSaveId = 0L
        }
    }

    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeButton("支出", type == EntryType.EXPENSE, Color(0xFFD14C43), Modifier.weight(1f)) {
                        typeName = EntryType.EXPENSE.name
                        selectedCategory = preferredCategory(state, EntryType.EXPENSE)
                    }
                    TypeButton("收入", type == EntryType.INCOME, Color(0xFF27835C), Modifier.weight(1f)) {
                        typeName = EntryType.INCOME.name
                        selectedCategory = preferredCategory(state, EntryType.INCOME)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (type == EntryType.EXPENSE) "支出金額" else "收入金額", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (amountText.isBlank()) "NT$ 0" else formatMoney(amount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (amountText.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        )
                    }
                    DateButton(LocalDate.ofEpochDay(selectedEpochDay), { selectedEpochDay = it.toEpochDay() })
                }
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.forEach { category ->
                        val selected = selectedCategory == category.id
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = category.id },
                            label = { Text(category.name, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (selected) ({ Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }) else null,
                        )
                    }
                }
            }
            item {
                if (noteExpanded) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(100) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("備註（選填）") },
                        supportingText = { Text("${note.length}/100") },
                        singleLine = true,
                    )
                } else {
                    TextButton(onClick = { noteExpanded = true }) { Text("＋備註") }
                }
            }
            item {
                NumberPad { key ->
                    when (key) {
                        "⌫" -> amountText = amountText.dropLast(1)
                        "C" -> amountText = ""
                        else -> if (amountText.length < 9) amountText = (amountText + key).trimStart('0').ifEmpty { "0" }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        val requestId = System.nanoTime().let { if (it == 0L) 1L else it }
                        if (viewModel.saveEntry(
                            amount = amount,
                            type = type,
                            categoryId = selectedCategory,
                            note = note,
                            date = LocalDate.ofEpochDay(selectedEpochDay),
                            requestId = requestId,
                        )) pendingSaveId = requestId
                    },
                    enabled = amount > 0 && selectedCategory != 0L && !state.isSaving,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Default.Check, null)
                    Text(if (state.isSaving) "儲存中…" else "儲存這筆${if (type == EntryType.EXPENSE) "支出" else "收入"}", Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                }
            }
            item { MonthSelector(state.selectedMonth, viewModel::previousMonth, viewModel::nextMonth) }
            item { BudgetCard(state, onEdit = { showBudgetDialog = true }) }
            item { MonthlySummary(state) }
            item { ExpenseDonutChart(state.selectedMonth, state.spent, state.expenseSlices) }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (showBudgetDialog) {
        BudgetDialog(state.budget, state.selectedMonth.toString(), onDismiss = { showBudgetDialog = false }) {
            viewModel.setBudget(it)
            showBudgetDialog = false
        }
    }
}

private fun preferredCategory(state: AppUiState, type: EntryType): Long {
    val active = state.categories.filter { it.type == type.name && it.isActive }
    return state.recentCategoryId(type).takeIf { recent -> active.any { it.id == recent } } ?: active.firstOrNull()?.id ?: 0
}

@Composable
private fun TypeButton(label: String, selected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = color)) {
            Icon(Icons.Default.Check, null, Modifier.size(18.dp)); Text(label, Modifier.padding(start = 6.dp), fontWeight = FontWeight.Bold)
        }
    } else OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
}

@Composable
private fun BudgetCard(state: AppUiState, onEdit: () -> Unit) {
    val level = budgetLevel(state.spent, state.budget)
    val color = when (level) {
        BudgetLevel.SAFE -> Color(0xFF2E9D63)
        BudgetLevel.WARNING -> Color(0xFFD49A13)
        BudgetLevel.DANGER -> Color(0xFFD34D45)
    }
    val status = when (level) {
        BudgetLevel.SAFE -> "安全"
        BudgetLevel.WARNING -> "接近預算"
        BudgetLevel.DANGER -> if (state.spent > state.budget && state.budget > 0) "已超支" else "注意支出"
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("預算剩餘 · $status", style = MaterialTheme.typography.labelLarge, color = color)
                    Text(
                        if (state.budget == 0L) "尚未設定預算" else if (state.spent > state.budget) "超支 ${formatMoney(state.spent - state.budget)}" else formatMoney(state.budget - state.spent),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                TextButton(onClick = onEdit) { Icon(Icons.Default.Edit, null); Text(if (state.budget == 0L) "設定" else "修改") }
            }
            if (state.budget > 0) {
                LinearProgressIndicator(
                    progress = { (state.spent.toFloat() / state.budget).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(9.dp), color = color, trackColor = color.copy(alpha = .18f),
                )
                Text("已花 ${formatMoney(state.spent)}／預算 ${formatMoney(state.budget)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MonthlySummary(state: AppUiState) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            SummaryValue("收入", state.income, Color(0xFF27835C))
            SummaryValue("支出", state.spent, Color(0xFFD14C43))
            SummaryValue("結餘", state.balance, MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SummaryValue(label: String, amount: Long, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(formatMoney(amount), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NumberPad(onKey: (String) -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("C", "0", "⌫"))
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    OutlinedButton(onClick = { onKey(key) }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(13.dp)) {
                        if (key == "⌫") Icon(Icons.AutoMirrored.Filled.Backspace, "退格") else Text(key, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetDialog(current: Long, month: String, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var value by rememberSaveable(month) { mutableStateOf(if (current == 0L) "" else current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("設定 $month 預算") },
        text = { OutlinedTextField(value, { value = it.filter(Char::isDigit).take(9) }, label = { Text("預算金額") }, prefix = { Text("NT$ ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) },
        confirmButton = { TextButton(onClick = { onSave(value.toLongOrNull() ?: 0) }) { Text("儲存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
