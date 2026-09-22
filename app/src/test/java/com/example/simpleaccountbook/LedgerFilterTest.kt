package com.example.simpleaccountbook

import com.example.simpleaccountbook.data.LedgerRow
import com.example.simpleaccountbook.data.LedgerTypeFilter
import com.example.simpleaccountbook.data.filterLedgerRows
import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerFilterTest {
    private val rows = listOf(
        row(1, "EXPENSE", 120, 10, "餐飲", "午餐"),
        row(2, "INCOME", 20_000, 20, "薪資", ""),
    )

    @Test fun searchesCategoryNoteAndAmount() {
        assertEquals(listOf(1L), filterLedgerRows(rows, "餐").map { it.id })
        assertEquals(listOf(1L), filterLedgerRows(rows, "午餐").map { it.id })
        assertEquals(listOf(2L), filterLedgerRows(rows, "20000").map { it.id })
    }

    @Test fun filtersTypeAndCategoryTogether() {
        assertEquals(listOf(1L), filterLedgerRows(rows, typeFilter = LedgerTypeFilter.EXPENSE).map { it.id })
        assertEquals(listOf(2L), filterLedgerRows(rows, categoryId = 20).map { it.id })
        assertEquals(emptyList<LedgerRow>(), filterLedgerRows(rows, typeFilter = LedgerTypeFilter.INCOME, categoryId = 10))
    }

    private fun row(id: Long, type: String, amount: Long, categoryId: Long, category: String, note: String) =
        LedgerRow(id, type, amount, categoryId, category, true, note, 20_000, 0, 0)
}
