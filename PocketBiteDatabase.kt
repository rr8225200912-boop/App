package com.pocketbite.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PocketBiteDao {
    @Query("SELECT * FROM budgets ORDER BY monthKey DESC")
    fun observeBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey LIMIT 1")
    suspend fun getBudget(monthKey: String): BudgetEntity?

    @Query("SELECT * FROM budgets ORDER BY monthKey DESC")
    suspend fun getAllBudgets(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity): Long

    @Query("SELECT * FROM expenses ORDER BY date DESC, time DESC")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY date DESC, time DESC")
    suspend fun getAllExpenses(): List<ExpenseEntity>

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)

    @Query("SELECT * FROM food_items ORDER BY available DESC, name COLLATE NOCASE")
    fun observeFoodItems(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items ORDER BY name COLLATE NOCASE")
    suspend fun getAllFoodItems(): List<FoodItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFoodItem(item: FoodItemEntity): Long

    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteFoodItem(id: Long)

    @Query("SELECT * FROM meal_plans ORDER BY date DESC, id DESC")
    fun observeMealPlans(): Flow<List<MealPlanEntity>>

    @Query("SELECT * FROM meal_plans ORDER BY date DESC, id DESC")
    suspend fun getAllMealPlans(): List<MealPlanEntity>

    @Insert
    suspend fun insertMealPlan(plan: MealPlanEntity): Long

    @Query("UPDATE meal_plans SET actualTotal = :actual WHERE id = :id")
    suspend fun updateMealPlanActual(id: Long, actual: Double)

    @Query("DELETE FROM budgets")
    suspend fun clearBudgets()

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    @Query("DELETE FROM food_items")
    suspend fun clearFoodItems()

    @Query("DELETE FROM meal_plans")
    suspend fun clearMealPlans()

    @Insert
    suspend fun insertBudgets(items: List<BudgetEntity>)

    @Insert
    suspend fun insertExpenses(items: List<ExpenseEntity>)

    @Insert
    suspend fun insertFoodItems(items: List<FoodItemEntity>)

    @Insert
    suspend fun insertMealPlans(items: List<MealPlanEntity>)
}

@Database(
    entities = [BudgetEntity::class, FoodItemEntity::class, ExpenseEntity::class, MealPlanEntity::class],
    version = 1,
    exportSchema = true
)
abstract class PocketBiteDatabase : RoomDatabase() {
    abstract fun dao(): PocketBiteDao

    companion object {
        @Volatile private var INSTANCE: PocketBiteDatabase? = null

        fun get(context: Context): PocketBiteDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                PocketBiteDatabase::class.java,
                "pocketbite.db"
            ).build().also { INSTANCE = it }
        }
    }
}
