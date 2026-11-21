package com.pocketbite.app.data

import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

fun money(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = if (value % 1.0 == 0.0) 0 else 2
        maximumFractionDigits = 2
    }
    return "৳${formatter.format(value)}"
}

data class BudgetSummary(
    val budget: Double,
    val spent: Double,
    val balance: Double,
    val remainingDays: Int,
    val dailyLimit: Double,
    val todaySpent: Double,
    val elapsedDays: Int,
    val averageDailySpent: Double
)

fun calculateSummary(
    budget: BudgetEntity?,
    expenses: List<ExpenseEntity>,
    month: YearMonth,
    today: LocalDate = LocalDate.now()
): BudgetSummary {
    val monthKey = month.toString()
    val monthly = expenses.filter { it.date.startsWith(monthKey) }
    val spent = monthly.sumOf { it.amount }
    val balance = (budget?.amount ?: 0.0) - spent
    val elapsedDays = when {
        month.isBefore(YearMonth.from(today)) -> month.lengthOfMonth()
        month.isAfter(YearMonth.from(today)) -> 0
        else -> today.dayOfMonth
    }
    val remainingDays = when {
        month.isBefore(YearMonth.from(today)) -> 0
        month.isAfter(YearMonth.from(today)) -> month.lengthOfMonth()
        else -> month.lengthOfMonth() - today.dayOfMonth + 1
    }
    val daily = if (remainingDays > 0) balance / remainingDays else 0.0
    val todaySpent = monthly.filter { it.date == today.toString() }.sumOf { it.amount }
    val average = if (elapsedDays > 0) spent / elapsedDays else 0.0
    return BudgetSummary(budget?.amount ?: 0.0, spent, balance, remainingDays, daily, todaySpent, elapsedDays, average)
}
