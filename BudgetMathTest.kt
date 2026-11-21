package com.pocketbite.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BudgetMathTest {
    @Test
    fun remainingDaysIncludesTodayAndDailyLimitUsesRemainingBalance() {
        val month = YearMonth.of(2026, 11)
        val budget = BudgetEntity(monthKey = "2026-11", amount = 2_000.0)
        val expenses = listOf(
            ExpenseEntity(amount = 400.0, category = "Breakfast", date = "2026-11-09", time = "08:00"),
            ExpenseEntity(amount = 250.0, category = "Hobbies", date = "2026-11-10", time = "17:00")
        )

        val summary = calculateSummary(budget, expenses, month, LocalDate.of(2026, 11, 10))

        assertEquals(1_350.0, summary.balance, 0.001)
        assertEquals(21, summary.remainingDays)
        assertEquals(1_350.0 / 21.0, summary.dailyLimit, 0.001)
        assertEquals(650.0 / 10.0, summary.averageDailySpent, 0.001)
    }

    @Test
    fun completedMonthHasNoRemainingDays() {
        val summary = calculateSummary(
            BudgetEntity(monthKey = "2026-10", amount = 1_000.0),
            emptyList(),
            YearMonth.of(2026, 10),
            LocalDate.of(2026, 11, 1)
        )
        assertEquals(0, summary.remainingDays)
        assertEquals(0.0, summary.dailyLimit, 0.001)
    }
}
