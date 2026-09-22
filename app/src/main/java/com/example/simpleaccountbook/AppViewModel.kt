package com.example.simpleaccountbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.simpleaccountbook.data.AppRepository
import com.example.simpleaccountbook.data.BackupDocument
import com.example.simpleaccountbook.data.CategoryEntity
import com.example.simpleaccountbook.data.CsvExporter
import com.example.simpleaccountbook.data.EntryType
import com.example.simpleaccountbook.data.ExpenseSlice
import com.example.simpleaccountbook.data.LedgerRow
import com.example.simpleaccountbook.data.LedgerTypeFilter
import com.example.simpleaccountbook.data.SettingsStore
import com.example.simpleaccountbook.data.ThemeSettings
import com.example.simpleaccountbook.data.TransactionEntity
import com.example.simpleaccountbook.data.expenseSlices
import com.example.simpleaccountbook.data.filterLedgerRows
import java.io.Writer
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EntrySaveResult(val requestId: Long, val errorMessage: String? = null)

data class AppUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val monthRows: List<LedgerRow> = emptyList(),
    val visibleRows: List<LedgerRow> = emptyList(),
    val budget: Long = 0,
    val settings: ThemeSettings = ThemeSettings(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val ledgerQuery: String = "",
    val ledgerTypeFilter: LedgerTypeFilter = LedgerTypeFilter.ALL,
    val ledgerCategoryFilter: Long? = null,
    val isSaving: Boolean = false,
    val isExportingCsv: Boolean = false,
    val saveResult: EntrySaveResult? = null,
) {
    val spent: Long = monthRows.filter { it.type == EntryType.EXPENSE.name }.sumOf { it.amount }
    val income: Long = monthRows.filter { it.type == EntryType.INCOME.name }.sumOf { it.amount }
    val balance: Long = income - spent
    val visibleExpense: Long = visibleRows.filter { it.type == EntryType.EXPENSE.name }.sumOf { it.amount }
    val visibleIncome: Long = visibleRows.filter { it.type == EntryType.INCOME.name }.sumOf { it.amount }
    val expenseSlices: List<ExpenseSlice> = expenseSlices(monthRows)

    fun recentCategoryId(type: EntryType): Long = when (type) {
        EntryType.EXPENSE -> settings.lastExpenseCategoryId
        EntryType.INCOME -> settings.lastIncomeCategoryId
    }
}

sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
}

private data class MonthData(val month: YearMonth, val rows: List<LedgerRow>, val budget: Long)
private data class LedgerFilters(val query: String, val type: LedgerTypeFilter, val categoryId: Long?)
private data class OperationState(val isSaving: Boolean, val isExportingCsv: Boolean, val saveResult: EntrySaveResult?)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AppViewModel(
    private val repository: AppRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val ledgerQuery = MutableStateFlow("")
    private val ledgerTypeFilter = MutableStateFlow(LedgerTypeFilter.ALL)
    private val ledgerCategoryFilter = MutableStateFlow<Long?>(null)
    private val isSaving = MutableStateFlow(false)
    private val isExportingCsv = MutableStateFlow(false)
    private val saveResult = MutableStateFlow<EntrySaveResult?>(null)
    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    private val monthData = selectedMonth.flatMapLatest { month ->
        val start = month.atDay(1).toEpochDay()
        val end = month.plusMonths(1).atDay(1).toEpochDay()
        combine(
            repository.observeMonthLedger(start, end),
            repository.observeBudget(month.toString()),
        ) { rows, budget -> MonthData(month, rows, budget?.amount ?: 0L) }
    }
    private val filters = combine(
        ledgerQuery.debounce(200).map(String::trim).distinctUntilChanged(),
        ledgerTypeFilter,
        ledgerCategoryFilter,
        ::LedgerFilters,
    )
    private val operationState = combine(isSaving, isExportingCsv, saveResult, ::OperationState)

    val uiState = combine(
        repository.categories,
        monthData,
        filters,
        settingsStore.settings,
        operationState,
    ) { categories, month, currentFilters, settings, operation ->
        AppUiState(
            categories = categories,
            monthRows = month.rows,
            visibleRows = filterLedgerRows(month.rows, currentFilters.query, currentFilters.type, currentFilters.categoryId),
            budget = month.budget,
            settings = settings,
            selectedMonth = month.month,
            ledgerQuery = ledgerQuery.value,
            ledgerTypeFilter = currentFilters.type,
            ledgerCategoryFilter = currentFilters.categoryId,
            isSaving = operation.isSaving,
            isExportingCsv = operation.isExportingCsv,
            saveResult = operation.saveResult,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init {
        viewModelScope.launch { repository.seedCategories() }
        viewModelScope.launch {
            monthData.collect { month ->
                val selected = ledgerCategoryFilter.value ?: return@collect
                val type = ledgerTypeFilter.value
                if (month.rows.none { it.categoryId == selected && (type == LedgerTypeFilter.ALL || it.type == type.name) }) {
                    ledgerCategoryFilter.value = null
                }
            }
        }
    }

    fun previousMonth() { selectedMonth.value = selectedMonth.value.minusMonths(1) }
    fun nextMonth() { selectedMonth.value = selectedMonth.value.plusMonths(1) }
    fun setLedgerQuery(query: String) { ledgerQuery.value = query }
    fun setLedgerTypeFilter(filter: LedgerTypeFilter) {
        ledgerTypeFilter.value = filter
        val categoryId = ledgerCategoryFilter.value
        if (categoryId != null && filter != LedgerTypeFilter.ALL && uiState.value.monthRows.none { it.categoryId == categoryId && it.type == filter.name }) {
            ledgerCategoryFilter.value = null
        }
    }
    fun setLedgerCategoryFilter(categoryId: Long?) { ledgerCategoryFilter.value = categoryId }

    fun saveEntry(
        amount: Long,
        type: EntryType,
        categoryId: Long,
        note: String = "",
        date: LocalDate = LocalDate.now(),
        id: Long = 0,
        requestId: Long,
    ): Boolean {
        if (isSaving.value) return false
        isSaving.value = true
        viewModelScope.launch {
            val result = runCatching {
                repository.saveTransaction(
                    TransactionEntity(
                        id = id,
                        type = type.name,
                        amount = amount,
                        categoryId = categoryId,
                        occurredEpochDay = date.toEpochDay(),
                        note = note,
                    ),
                )
            }
            if (result.isSuccess) runCatching { settingsStore.setRecentEntry(type, categoryId) }
            isSaving.value = false
            saveResult.value = EntrySaveResult(requestId, result.exceptionOrNull()?.message)
        }
        return true
    }

    fun acknowledgeSaveResult(requestId: Long) {
        if (saveResult.value?.requestId == requestId) saveResult.value = null
    }

    fun deleteEntry(row: LedgerRow) = viewModelScope.launch {
        repository.transactionById(row.id)?.let { repository.deleteTransaction(it) }
    }
    fun restoreEntry(row: LedgerRow) = viewModelScope.launch {
        repository.restoreTransaction(row.toEntity())
    }
    suspend fun exportMonthCsv(month: YearMonth, writer: Writer) {
        check(!isExportingCsv.value) { "CSV 正在匯出" }
        isExportingCsv.value = true
        try {
            CsvExporter.write(
                repository.monthLedger(month.atDay(1).toEpochDay(), month.plusMonths(1).atDay(1).toEpochDay()),
                writer,
            )
        } finally {
            isExportingCsv.value = false
        }
    }

    fun setBudget(amount: Long) = viewModelScope.launch {
        runCatching { repository.setBudget(selectedMonth.value.toString(), amount.coerceAtLeast(0)) }
            .onFailure { _events.emit(UiEvent.Message(it.message ?: "預算儲存失敗")) }
    }
    fun saveCategory(category: CategoryEntity) = viewModelScope.launch {
        runCatching { repository.saveCategory(category) }
            .onFailure { _events.emit(UiEvent.Message(it.message ?: "分類儲存失敗")) }
    }
    fun deleteCategory(category: CategoryEntity) = viewModelScope.launch { repository.removeCategory(category) }
    fun toggleCategory(category: CategoryEntity) = saveCategory(category.copy(isActive = !category.isActive))
    fun moveCategory(category: CategoryEntity, direction: Int) = viewModelScope.launch {
        val list = uiState.value.categories.filter { it.type == category.type }.sortedBy { it.sortOrder }
        val index = list.indexOfFirst { it.id == category.id }
        val other = list.getOrNull(index + direction) ?: return@launch
        repository.swapCategoryOrder(category, other)
    }

    suspend fun createBackup(appVersion: String): String = repository.createBackup(appVersion)
    suspend fun restoreBackup(content: String): BackupDocument {
        val document = repository.restoreBackup(content)
        runCatching { settingsStore.clearRecentCategories() }
        return document
    }

    fun setDisplayMode(mode: String) = viewModelScope.launch { settingsStore.setDisplayMode(mode) }
    fun setPresetTheme(id: String, hue: Float) = viewModelScope.launch { settingsStore.setPresetTheme(id, hue) }
    fun setCustomHue(hue: Float) = viewModelScope.launch { settingsStore.setCustomHue(hue) }

    class Factory(
        private val repository: AppRepository,
        private val settingsStore: SettingsStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(repository, settingsStore) as T
    }
}

private fun LedgerRow.toEntity() = TransactionEntity(
    id = id,
    type = type,
    amount = amount,
    categoryId = categoryId,
    occurredEpochDay = occurredEpochDay,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
