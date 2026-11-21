package com.pocketbite.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "budgets", indices = [Index(value = ["monthKey"], unique = true)])
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthKey: String,
    val amount: Double,
    val breakfastPercent: Int = 60,
    val eveningPercent: Int = 40
)

@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Breakfast",
    val minPrice: Double,
    val maxPrice: Double,
    val unit: String = "piece",
    val available: Boolean = true
)

val FoodItemEntity.typicalPrice: Double get() = (minPrice + maxPrice) / 2.0

@Entity(tableName = "expenses", indices = [Index(value = ["date"])])
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val category: String,
    val date: String,
    val time: String,
    val description: String = "",
    val foodItemId: Long? = null,
    val notes: String = ""
)

@Entity(tableName = "meal_plans", indices = [Index(value = ["date"])])
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val category: String,
    val itemsJson: String,
    val plannedTotal: Double,
    val actualTotal: Double? = null
)

val expenseCategories = listOf(
    "Breakfast", "Evening Snacks", "Extra Food", "Personal Shopping", "Hobbies", "Other Expenses"
)

val mealCategories = listOf("Breakfast", "Evening Snacks", "Extra Food")
