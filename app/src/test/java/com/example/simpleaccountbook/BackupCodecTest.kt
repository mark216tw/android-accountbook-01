package com.example.simpleaccountbook

import com.example.simpleaccountbook.data.BackupBudget
import com.example.simpleaccountbook.data.BackupCategory
import com.example.simpleaccountbook.data.BackupCodec
import com.example.simpleaccountbook.data.BackupDocument
import com.example.simpleaccountbook.data.BackupTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream

class BackupCodecTest {
    private val category = BackupCategory(1, "EXPENSE", "餐飲", "food", 0, true, true)
    private val transaction = BackupTransaction(1, "EXPENSE", 120, 1, 20_000, "午餐", 1_000, 1_000)

    @Test fun roundTripPreservesAccountingData() {
        val source = BackupDocument(2, "test", 123, listOf(category), listOf(transaction), listOf(BackupBudget("2026-09", 15_000)))
        val restored = BackupCodec.decodeAndValidate(BackupCodec.encode(source))

        assertEquals(source, restored)
    }

    @Test fun rejectsTransactionWithMissingCategory() {
        val source = BackupDocument(2, "test", 123, listOf(category), listOf(transaction.copy(categoryId = 99)), emptyList())

        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decodeAndValidate(BackupCodec.encode(source))
        }
    }

    @Test fun rejectsUnsupportedFormat() {
        val source = BackupDocument(1, "test", 123, listOf(category), emptyList(), emptyList())

        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decodeAndValidate(BackupCodec.encode(source))
        }
    }

    @Test fun rejectsOversizedInputBeforeParsing() {
        val input = ByteArrayInputStream(ByteArray(17))

        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.readUtf8WithLimit(input, maxBytes = 16)
        }
    }

    @Test fun rejectsIdsThatCouldExhaustSqliteSequence() {
        val source = BackupDocument(2, "test", 123, listOf(category.copy(id = Long.MAX_VALUE)), emptyList(), emptyList())

        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decodeAndValidate(BackupCodec.encode(source))
        }
    }

    @Test fun rejectsNoteLongerThanOneHundredCharacters() {
        val source = BackupDocument(2, "test", 123, listOf(category), listOf(transaction.copy(note = "x".repeat(101))), emptyList())

        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decodeAndValidate(BackupCodec.encode(source))
        }
    }
}
