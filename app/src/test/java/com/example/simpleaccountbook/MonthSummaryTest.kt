package com.example.simpleaccountbook

import com.example.simpleaccountbook.data.EntryType
import com.example.simpleaccountbook.data.LedgerRow
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthSummaryTest {
    @Test fun filtersSelectedMonthAcrossYearBoundary() {
        val state = AppUiState(
            monthRows = listOf(
                entry(2, EntryType.INCOME, 2_000, LocalDate.of(2027, 1, 1)),
                entry(3, EntryType.EXPENSE, 300, LocalDate.of(2027, 1, 31)),
            ),
            selectedMonth = YearMonth.of(2027, 1),
        )

        assertEquals(2, state.monthRows.size)
        assertEquals(2_000, state.income)
        assertEquals(300, state.spent)
        assertEquals(1_700, state.balance)
    }

    private fun entry(id: Long, type: EntryType, amount: Long, date: LocalDate) = LedgerRow(
        id = id,
        type = type.name,
        amount = amount,
        categoryId = if (type == EntryType.EXPENSE) 1 else 2,
        categoryName = if (type == EntryType.EXPENSE) "支出" else "收入",
        categoryActive = true,
        note = "",
        occurredEpochDay = date.toEpochDay(),
        createdAt = 0,
        updatedAt = 0,
    )
}
