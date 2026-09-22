package com.example.simpleaccountbook.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM categories ORDER BY type, sortOrder, id")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun categoryCount(): Int

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun categoryById(id: Long): CategoryEntity?

    @Insert
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Transaction
    suspend fun seedCategoriesIfEmpty(categories: List<CategoryEntity>) {
        if (categoryCount() == 0) insertCategories(categories)
    }

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity): Int

    @Transaction
    suspend fun swapCategoryOrder(first: CategoryEntity, second: CategoryEntity) {
        updateCategory(first.copy(sortOrder = second.sortOrder))
        updateCategory(second.copy(sortOrder = first.sortOrder))
    }

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun categoryUsageCount(categoryId: Long): Int

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM transactions ORDER BY occurredEpochDay DESC, id DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query(
        """SELECT t.id, t.type, t.amount, t.categoryId, c.name AS categoryName,
            c.isActive AS categoryActive, t.note, t.occurredEpochDay, t.createdAt, t.updatedAt
            FROM transactions AS t INNER JOIN categories AS c ON c.id = t.categoryId
            WHERE t.occurredEpochDay >= :startEpochDay AND t.occurredEpochDay < :endEpochDayExclusive
            ORDER BY t.occurredEpochDay DESC, t.id DESC""",
    )
    fun observeMonthLedger(startEpochDay: Long, endEpochDayExclusive: Long): Flow<List<LedgerRow>>

    @Query(
        """SELECT t.id, t.type, t.amount, t.categoryId, c.name AS categoryName,
            c.isActive AS categoryActive, t.note, t.occurredEpochDay, t.createdAt, t.updatedAt
            FROM transactions AS t INNER JOIN categories AS c ON c.id = t.categoryId
            WHERE t.occurredEpochDay >= :startEpochDay AND t.occurredEpochDay < :endEpochDayExclusive
            ORDER BY t.occurredEpochDay DESC, t.id DESC""",
    )
    suspend fun monthLedger(startEpochDay: Long, endEpochDayExclusive: Long): List<LedgerRow>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun transactionById(id: Long): TransactionEntity?

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity): Int

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth LIMIT 1")
    fun observeBudget(yearMonth: String): Flow<MonthlyBudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBudget(budget: MonthlyBudgetEntity)

    @Query("SELECT * FROM categories ORDER BY id")
    suspend fun allCategories(): List<CategoryEntity>

    @Query("SELECT * FROM transactions ORDER BY id")
    suspend fun allTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM monthly_budgets ORDER BY yearMonth")
    suspend fun allBudgets(): List<MonthlyBudgetEntity>

    @Transaction
    suspend fun snapshot(): DatabaseSnapshot = DatabaseSnapshot(allCategories(), allTransactions(), allBudgets())

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM monthly_budgets")
    suspend fun clearBudgets()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Insert
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Insert
    suspend fun insertBudgets(budgets: List<MonthlyBudgetEntity>)

    @Transaction
    suspend fun replaceAllData(
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<MonthlyBudgetEntity>,
    ) {
        clearTransactions()
        clearBudgets()
        clearCategories()
        insertCategories(categories)
        insertBudgets(budgets)
        insertTransactions(transactions)
    }
}
