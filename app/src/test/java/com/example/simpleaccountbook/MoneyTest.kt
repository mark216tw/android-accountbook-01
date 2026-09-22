package com.example.simpleaccountbook

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test fun budgetLevelsUseExpectedBoundaries() {
        assertEquals(BudgetLevel.SAFE, budgetLevel(5_999, 10_000))
        assertEquals(BudgetLevel.WARNING, budgetLevel(6_000, 10_000))
        assertEquals(BudgetLevel.WARNING, budgetLevel(7_999, 10_000))
        assertEquals(BudgetLevel.DANGER, budgetLevel(8_000, 10_000))
        assertEquals(BudgetLevel.DANGER, budgetLevel(12_000, 10_000))
    }

    @Test fun missingBudgetIsSafe() {
        assertEquals(BudgetLevel.SAFE, budgetLevel(500, 0))
    }
}
