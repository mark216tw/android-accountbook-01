package com.example.simpleaccountbook

import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class BudgetLevel { SAFE, WARNING, DANGER }

fun budgetLevel(spent: Long, budget: Long): BudgetLevel {
    if (budget <= 0L) return BudgetLevel.SAFE
    val ratio = spent.toDouble() / budget
    return when {
        ratio < 0.6 -> BudgetLevel.SAFE
        ratio < 0.8 -> BudgetLevel.WARNING
        else -> BudgetLevel.DANGER
    }
}

fun formatMoney(amount: Long): String = "NT$ ${NumberFormat.getIntegerInstance(Locale.TAIWAN).format(amount)}"

fun Long.toLocalDate(): LocalDate = LocalDate.ofEpochDay(this)

fun LocalDate.toPickerMillis(): Long = toEpochDay() * 86_400_000L

fun pickerMillisToDate(millis: Long): LocalDate = LocalDate.ofEpochDay(Math.floorDiv(millis, 86_400_000L))

fun LocalDate.displayDate(): String = format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.TAIWAN))

fun currentYearMonthKey(): String = YearMonth.now().toString()

fun YearMonth.displayMonth(): String = format(DateTimeFormatter.ofPattern("yyyy年 M月", Locale.TAIWAN))
