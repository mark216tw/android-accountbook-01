package com.example.simpleaccountbook.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class EntryType { EXPENSE, INCOME }

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val name: String,
    val iconKey: String,
    val sortOrder: Int,
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("categoryId"), Index("occurredEpochDay")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Long,
    val categoryId: Long,
    val occurredEpochDay: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "monthly_budgets")
data class MonthlyBudgetEntity(
    @PrimaryKey val yearMonth: String,
    val amount: Long,
)

data class DatabaseSnapshot(
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<MonthlyBudgetEntity>,
)

data class ThemeSettings(
    val displayMode: String = "SYSTEM",
    val themeId: String = "PURPLE",
    val customHue: Float = 275f,
    val lastEntryType: String = EntryType.EXPENSE.name,
    val lastExpenseCategoryId: Long = 0,
    val lastIncomeCategoryId: Long = 0,
)
