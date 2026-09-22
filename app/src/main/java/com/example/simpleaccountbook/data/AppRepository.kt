package com.example.simpleaccountbook.data

import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

class AppRepository(private val dao: AppDao) {
    val categories = dao.observeCategories()
    val transactions = dao.observeTransactions()

    fun observeMonthLedger(startEpochDay: Long, endEpochDayExclusive: Long): Flow<List<LedgerRow>> {
        require(startEpochDay < endEpochDayExclusive) { "月份日期範圍無效" }
        return dao.observeMonthLedger(startEpochDay, endEpochDayExclusive)
    }

    suspend fun monthLedger(startEpochDay: Long, endEpochDayExclusive: Long): List<LedgerRow> {
        require(startEpochDay < endEpochDayExclusive) { "月份日期範圍無效" }
        return dao.monthLedger(startEpochDay, endEpochDayExclusive)
    }

    suspend fun transactionById(id: Long): TransactionEntity? = dao.transactionById(id)

    suspend fun seedCategories() {
        val expenseNames = listOf(
            "餐飲" to "food", "購物" to "shopping", "居住" to "home", "交通" to "transport",
            "娛樂" to "fun", "醫療" to "medical", "教育" to "education", "其他" to "other",
        )
        val incomeNames = listOf(
            "薪資" to "salary", "獎金" to "bonus", "投資" to "investment",
            "兼職" to "work", "禮金" to "gift", "其他" to "other",
        )
        dao.seedCategoriesIfEmpty(
            expenseNames.mapIndexed { index, item ->
                CategoryEntity(type = EntryType.EXPENSE.name, name = item.first, iconKey = item.second, sortOrder = index, isDefault = true)
            } + incomeNames.mapIndexed { index, item ->
                CategoryEntity(type = EntryType.INCOME.name, name = item.first, iconKey = item.second, sortOrder = index, isDefault = true)
            },
        )
    }

    suspend fun saveTransaction(transaction: TransactionEntity) {
        require(transaction.amount > 0) { "金額必須大於零" }
        require(transaction.note.length <= 100) { "備註不可超過 100 個字元" }
        LocalDate.ofEpochDay(transaction.occurredEpochDay)
        val category = requireNotNull(dao.categoryById(transaction.categoryId)) { "找不到所選分類" }
        require(category.isActive || transaction.id != 0L) { "所選分類已停用" }
        require(category.type == transaction.type) { "帳目與分類類型不一致" }
        if (transaction.id == 0L) {
            dao.insertTransaction(transaction)
        } else {
            val existing = requireNotNull(dao.transactionById(transaction.id)) { "找不到要更新的帳目" }
            check(dao.updateTransaction(transaction.copy(createdAt = existing.createdAt, updatedAt = System.currentTimeMillis())) == 1) {
                "找不到要更新的帳目"
            }
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = dao.deleteTransaction(transaction)

    suspend fun restoreTransaction(transaction: TransactionEntity) = dao.restoreTransaction(transaction)

    suspend fun saveCategory(category: CategoryEntity) {
        require(category.name.isNotBlank()) { "分類名稱不可空白" }
        require(category.type in EntryType.entries.map { it.name }) { "分類類型無效" }
        if (category.id == 0L) dao.insertCategory(category) else check(dao.updateCategory(category) == 1) { "找不到分類" }
    }

    suspend fun removeCategory(category: CategoryEntity) {
        if (dao.categoryUsageCount(category.id) == 0) dao.deleteCategory(category)
        else dao.updateCategory(category.copy(isActive = false))
    }

    fun observeBudget(yearMonth: String): Flow<MonthlyBudgetEntity?> = dao.observeBudget(yearMonth)

    suspend fun setBudget(yearMonth: String, amount: Long) {
        YearMonth.parse(yearMonth)
        require(amount >= 0) { "預算不可小於零" }
        dao.setBudget(MonthlyBudgetEntity(yearMonth, amount))
    }

    suspend fun swapCategoryOrder(first: CategoryEntity, second: CategoryEntity) = dao.swapCategoryOrder(first, second)

    suspend fun createBackup(appVersion: String): String {
        val snapshot = dao.snapshot()
        val document = BackupDocument(
            appVersion = appVersion,
            exportedAt = System.currentTimeMillis(),
            categories = snapshot.categories.map {
                BackupCategory(it.id, it.type, it.name, it.iconKey, it.sortOrder, it.isDefault, it.isActive)
            },
            transactions = snapshot.transactions.map {
                BackupTransaction(it.id, it.type, it.amount, it.categoryId, it.occurredEpochDay, it.note, it.createdAt, it.updatedAt)
            },
            budgets = snapshot.budgets.map { BackupBudget(it.yearMonth, it.amount) },
        )
        val content = BackupCodec.encode(document)
        BackupCodec.decodeAndValidate(content)
        require(content.toByteArray(Charsets.UTF_8).size <= BackupCodec.MAX_BACKUP_BYTES) { "備份資料超過 10 MB，無法建立可還原的備份" }
        return content
    }

    suspend fun restoreBackup(content: String): BackupDocument {
        val document = BackupCodec.decodeAndValidate(content)
        dao.replaceAllData(
            categories = document.categories.map {
                CategoryEntity(it.id, it.type, it.name, it.iconKey, it.sortOrder, it.isDefault, it.isActive)
            },
            transactions = document.transactions.map {
                TransactionEntity(it.id, it.type, it.amount, it.categoryId, it.occurredEpochDay, it.note, it.createdAt, it.updatedAt)
            },
            budgets = document.budgets.map { MonthlyBudgetEntity(it.yearMonth, it.amount) },
        )
        return document
    }
}
