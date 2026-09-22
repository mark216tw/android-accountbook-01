package com.example.simpleaccountbook.data

import java.io.Writer
import java.time.Instant
import java.time.LocalDate

object CsvExporter {
    private val header = listOf(
        "date", "type", "category", "note", "amount", "currency", "transaction_id", "category_id", "created_at", "updated_at",
    )

    fun write(rows: Iterable<LedgerRow>, writer: Writer) {
        writer.write('\uFEFF'.code)
        writeRow(writer, header)
        rows.forEach { row ->
            writeRow(
                writer,
                listOf(
                    LocalDate.ofEpochDay(row.occurredEpochDay).toString(),
                    row.type,
                    neutralizeFormula(row.categoryName),
                    neutralizeFormula(row.note),
                    row.amount.toString(),
                    "TWD",
                    row.id.toString(),
                    row.categoryId.toString(),
                    Instant.ofEpochMilli(row.createdAt).toString(),
                    Instant.ofEpochMilli(row.updatedAt).toString(),
                ),
            )
        }
        writer.flush()
    }

    private fun writeRow(writer: Writer, fields: List<String>) {
        fields.forEachIndexed { index, field ->
            if (index > 0) writer.write(','.code)
            writer.write('"'.code)
            writer.write(field.replace("\"", "\"\""))
            writer.write('"'.code)
        }
        writer.write("\r\n")
    }

    private fun neutralizeFormula(value: String): String =
        if (value.isNotEmpty() && value.first() in charArrayOf('=', '+', '-', '@', '\t', '\r')) "'$value" else value
}
