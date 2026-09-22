package com.example.simpleaccountbook

import com.example.simpleaccountbook.data.CsvExporter
import com.example.simpleaccountbook.data.LedgerRow
import java.io.IOException
import java.io.StringWriter
import java.io.Writer
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {
    @Test fun writesBomHeaderCrLfAndEscapedLocalizedRow() {
        val output = StringWriter()
        CsvExporter.write(
            listOf(
                LedgerRow(7, "EXPENSE", 1234, 3, "餐飲,\"外食\"", false, "第一行\n第二行", LocalDate.of(2026, 9, 2).toEpochDay(), 0, 1000),
            ),
            output,
        )

        val csv = output.toString()
        assertTrue(csv.startsWith("\uFEFF\"date\",\"type\",\"category\",\"note\",\"amount\",\"currency\",\"transaction_id\",\"category_id\",\"created_at\",\"updated_at\"\r\n"))
        assertTrue(csv.contains("\"餐飲,\"\"外食\"\"\",\"第一行\n第二行\",\"1234\",\"TWD\""))
        assertTrue(csv.endsWith("\"1970-01-01T00:00:00Z\",\"1970-01-01T00:00:01Z\"\r\n"))
        assertTrue(csv.lineSequence().none { "NT$" in it || "1,234" in it })
    }

    @Test fun neutralizesFormulaTextAndEmptyExportStillHasHeader() {
        val output = StringWriter()
        CsvExporter.write(listOf(LedgerRow(1, "EXPENSE", 1, 1, "=cmd", true, "\tformula", 0, 0, 0)), output)
        assertTrue(output.toString().contains("\"'=cmd\",\"'\tformula\""))

        val empty = StringWriter()
        CsvExporter.write(emptyList(), empty)
        assertEquals(2, empty.toString().count { it == '\n' } + 1)
    }

    @Test fun writerFailurePropagates() {
        val failing = object : Writer() {
            override fun write(buffer: CharArray, offset: Int, count: Int) = throw IOException("disk full")
            override fun flush() = Unit
            override fun close() = Unit
        }
        assertThrows(IOException::class.java) { CsvExporter.write(emptyList(), failing) }
    }
}
