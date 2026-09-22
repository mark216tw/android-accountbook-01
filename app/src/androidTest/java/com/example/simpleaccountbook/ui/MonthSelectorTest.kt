package com.example.simpleaccountbook.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MonthSelectorTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun displaysMonthAndHandlesBothDirections() {
        var previousClicks = 0
        var nextClicks = 0
        composeRule.setContent {
            MaterialTheme {
                MonthSelector(
                    month = YearMonth.of(2026, 9),
                    onPrevious = { previousClicks++ },
                    onNext = { nextClicks++ },
                )
            }
        }

        composeRule.onNodeWithText("2026年 9月").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("上一個月").performClick()
        composeRule.onNodeWithContentDescription("下一個月").performClick()
        composeRule.runOnIdle {
            assertEquals(1, previousClicks)
            assertEquals(1, nextClicks)
        }
    }
}
