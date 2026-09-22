package com.example.simpleaccountbook.data

data class LedgerRow(
    val id: Long,
    val type: String,
    val amount: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryActive: Boolean,
    val note: String,
    val occurredEpochDay: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class LedgerTypeFilter { ALL, EXPENSE, INCOME }

fun filterLedgerRows(
    rows: List<LedgerRow>,
    query: String = "",
    typeFilter: LedgerTypeFilter = LedgerTypeFilter.ALL,
    categoryId: Long? = null,
): List<LedgerRow> {
    val term = query.trim()
    return rows.filter { row ->
        (typeFilter == LedgerTypeFilter.ALL || row.type == typeFilter.name) &&
            (categoryId == null || row.categoryId == categoryId) &&
            (term.isEmpty() || row.categoryName.contains(term, ignoreCase = true) ||
                row.note.contains(term, ignoreCase = true) || row.amount.toString().contains(term))
    }
}

data class ExpenseSlice(
    val categoryId: Long?,
    val categoryName: String,
    val amount: Long,
    val sweepAngle: Float,
    val isOther: Boolean = false,
)

fun expenseSlices(rows: List<LedgerRow>, maxCategories: Int = 5): List<ExpenseSlice> {
    require(maxCategories > 0) { "顯示分類數必須大於零" }
    val totals = rows.asSequence()
        .filter { it.type == EntryType.EXPENSE.name }
        .groupBy { it.categoryId to it.categoryName }
        .map { (category, entries) -> Triple(category.first, category.second, entries.sumOf { it.amount }) }
        .filter { it.third > 0 }
        .sortedWith(compareByDescending<Triple<Long, String, Long>> { it.third }.thenBy { it.first })
    if (totals.isEmpty()) return emptyList()

    val displayed = totals.take(maxCategories).toMutableList()
    if (totals.size > maxCategories) {
        displayed += Triple(Long.MIN_VALUE, "其他分類合計", totals.drop(maxCategories).sumOf { it.third })
    }
    val total = displayed.sumOf { it.third }
    var usedAngle = 0f
    return displayed.mapIndexed { index, item ->
        val angle = if (index == displayed.lastIndex) 360f - usedAngle else (item.third.toDouble() / total * 360.0).toFloat()
        usedAngle += angle
        ExpenseSlice(
            categoryId = item.first.takeUnless { it == Long.MIN_VALUE },
            categoryName = item.second,
            amount = item.third,
            sweepAngle = angle,
            isOther = item.first == Long.MIN_VALUE,
        )
    }
}
