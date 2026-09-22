package com.example.simpleaccountbook.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: AppDao

    @Before fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        dao = database.appDao()
    }

    @After @Throws(IOException::class) fun closeDatabase() = database.close()

    @Test fun replaceAllDataRestoresRelationsAtomically() = runTest {
        val category = CategoryEntity(4, EntryType.EXPENSE.name, "交通", "transport", 0)
        val transaction = TransactionEntity(8, EntryType.EXPENSE.name, 250, 4, 20_000, note = "公車")

        dao.replaceAllData(listOf(category), listOf(transaction), listOf(MonthlyBudgetEntity("2026-09", 10_000)))

        assertEquals(listOf(category), dao.allCategories())
        assertEquals(listOf(transaction), dao.allTransactions())
        assertEquals(10_000, dao.observeBudget("2026-09").first()?.amount)
    }

    @Test fun seedCategoriesOnlyRunsOnce() = runTest {
        val category = CategoryEntity(type = EntryType.EXPENSE.name, name = "餐飲", iconKey = "food", sortOrder = 0)
        dao.seedCategoriesIfEmpty(listOf(category))
        dao.seedCategoriesIfEmpty(listOf(category.copy(name = "重複")))

        assertEquals(1, dao.categoryCount())
        assertTrue(dao.allCategories().none { it.name == "重複" })
    }

    @Test fun failedRestoreRollsBackExistingData() = runTest {
        val original = CategoryEntity(1, EntryType.EXPENSE.name, "餐飲", "food", 0)
        dao.insertCategories(listOf(original))
        val invalidTransaction = TransactionEntity(2, EntryType.EXPENSE.name, 100, 999, 20_000)

        runCatching { dao.replaceAllData(listOf(original.copy(name = "已改名")), listOf(invalidTransaction), emptyList()) }

        assertEquals(listOf(original), dao.allCategories())
        assertTrue(dao.allTransactions().isEmpty())
    }

    @Test fun monthLedgerUsesHalfOpenRangeJoinsInactiveCategoryAndSorts() = runTest {
        val category = CategoryEntity(1, EntryType.EXPENSE.name, "舊分類", "other", 0, isActive = false)
        dao.insertCategories(listOf(category))
        val decemberStart = LocalDate.of(2026, 12, 1).toEpochDay()
        val januaryStart = LocalDate.of(2027, 1, 1).toEpochDay()
        dao.insertTransactions(
            listOf(
                TransactionEntity(id = 1, type = "EXPENSE", amount = 1, categoryId = 1, occurredEpochDay = decemberStart),
                TransactionEntity(id = 2, type = "EXPENSE", amount = 2, categoryId = 1, occurredEpochDay = januaryStart - 1, note = "月底"),
                TransactionEntity(id = 3, type = "EXPENSE", amount = 3, categoryId = 1, occurredEpochDay = januaryStart - 1),
                TransactionEntity(id = 4, type = "EXPENSE", amount = 4, categoryId = 1, occurredEpochDay = januaryStart),
            ),
        )

        val rows = dao.monthLedger(decemberStart, januaryStart)
        assertEquals(listOf(3L, 2L, 1L), rows.map { it.id })
        assertEquals("舊分類", rows.first().categoryName)
        assertTrue(rows.none { it.categoryActive })
        assertEquals("月底", rows.first { it.id == 2L }.note)
        assertEquals(rows, dao.observeMonthLedger(decemberStart, januaryStart).first())
    }

    @Test fun leapDayIsIncludedAndRepositoryEditPreservesCreatedAt() = runTest {
        dao.insertCategories(listOf(CategoryEntity(1, "EXPENSE", "餐飲", "food", 0)))
        val leapDay = LocalDate.of(2028, 2, 29).toEpochDay()
        dao.insertTransaction(TransactionEntity(1, "EXPENSE", 100, 1, leapDay, createdAt = 123, updatedAt = 123))
        val repository = AppRepository(dao)

        assertEquals(listOf(1L), repository.monthLedger(LocalDate.of(2028, 2, 1).toEpochDay(), LocalDate.of(2028, 3, 1).toEpochDay()).map { it.id })
        repository.saveTransaction(TransactionEntity(1, "EXPENSE", 200, 1, leapDay, note = "修改", createdAt = 999))

        val edited = repository.transactionById(1)!!
        assertEquals(123, edited.createdAt)
        assertEquals("修改", edited.note)
        assertTrue(edited.updatedAt >= 123)
    }

    @Test fun repositoryRejectsOverlongNote() = runTest {
        dao.insertCategories(listOf(CategoryEntity(1, "EXPENSE", "餐飲", "food", 0)))

        val result = runCatching {
            AppRepository(dao).saveTransaction(TransactionEntity(type = "EXPENSE", amount = 100, categoryId = 1, occurredEpochDay = 20_000, note = "x".repeat(101)))
        }

        assertTrue(result.isFailure)
        assertTrue(dao.allTransactions().isEmpty())
    }
}
