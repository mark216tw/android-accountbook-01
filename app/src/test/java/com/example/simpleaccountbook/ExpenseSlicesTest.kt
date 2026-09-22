package com.example.simpleaccountbook

import com.example.simpleaccountbook.data.LedgerRow
import com.example.simpleaccountbook.data.expenseSlices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseSlicesTest {
    @Test fun emptyAndIncomeOnlyRowsProduceNoSlices() {
        assertTrue(expenseSlices(emptyList()).isEmpty())
        assertTrue(expenseSlices(listOf(row(1, 1, 100, "INCOME"))).isEmpty())
    }

    @Test fun keepsTopFiveAndCombinesRemainderWithoutChangingTotal() {
        val slices = expenseSlices((1L..7L).map { row(it, it, (8 - it) * 100) })
        val expectedTotal: Long = (1L..7L).sumOf { (8 - it) * 100 }
        val actualTotal: Long = slices.sumOf { it.amount }

        assertEquals(6, slices.size)
        assertEquals(expectedTotal, actualTotal)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, null), slices.map { it.categoryId })
        assertTrue(slices.last().isOther)
        assertEquals(360f, slices.fold(0f) { total, slice -> total + slice.sweepAngle }, 0.0001f)
        assertTrue(slices.all { it.sweepAngle.isFinite() })
    }

    @Test fun oneCategoryIsFullCircleAndEqualAmountsSortByCategoryId() {
        val one = expenseSlices(listOf(row(1, 9, 50))).single()
        assertEquals(360f, one.sweepAngle, 0f)
        assertFalse(one.isOther)
        assertEquals(listOf(2L, 4L), expenseSlices(listOf(row(1, 4, 50), row(2, 2, 50))).map { it.categoryId })
    }

    private fun row(id: Long, categoryId: Long, amount: Long, type: String = "EXPENSE") =
        LedgerRow(id, type, amount, categoryId, "分類$categoryId", true, "", 20_000, 0, 0)
}
