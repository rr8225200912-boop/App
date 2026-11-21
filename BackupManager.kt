package com.pocketbite.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {
    suspend fun exportJson(context: Context, database: PocketBiteDatabase, uri: Uri) = withContext(Dispatchers.IO) {
        val dao = database.dao()
        val root = JSONObject().put("app", "PocketBite").put("schemaVersion", 1)
        val budgets = JSONArray().apply {
            dao.getAllBudgets().forEach { b -> put(JSONObject()
                .put("id", b.id).put("monthKey", b.monthKey).put("amount", b.amount)
                .put("breakfastPercent", b.breakfastPercent).put("eveningPercent", b.eveningPercent)) }
        }
        val expenses = JSONArray().apply {
            dao.getAllExpenses().forEach { e -> put(JSONObject()
                .put("id", e.id).put("amount", e.amount).put("category", e.category)
                .put("date", e.date).put("time", e.time).put("description", e.description)
                .put("foodItemId", e.foodItemId ?: JSONObject.NULL).put("notes", e.notes)) }
        }
        val foods = JSONArray().apply {
            dao.getAllFoodItems().forEach { f -> put(JSONObject()
                .put("id", f.id).put("name", f.name).put("category", f.category)
                .put("minPrice", f.minPrice).put("maxPrice", f.maxPrice)
                .put("unit", f.unit).put("available", f.available)) }
        }
        val plans = JSONArray().apply {
            dao.getAllMealPlans().forEach { p -> put(JSONObject()
                .put("id", p.id).put("date", p.date).put("category", p.category)
                .put("itemsJson", p.itemsJson).put("plannedTotal", p.plannedTotal)
                .put("actualTotal", p.actualTotal ?: JSONObject.NULL)) }
        }
        root.put("budgets", budgets).put("expenses", expenses).put("foodItems", foods).put("mealPlans", plans)
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(root.toString(2)) }
            ?: error("Could not open the selected backup destination")
    }

    suspend fun importJson(context: Context, database: PocketBiteDatabase, uri: Uri) = withContext(Dispatchers.IO) {
        val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Could not read the selected backup file")
        require(raw.length <= 20_000_000) { "Backup file is too large" }
        val root = JSONObject(raw)
        require(root.optString("app") == "PocketBite") { "This is not a PocketBite backup" }
        require(root.optInt("schemaVersion", -1) == 1) { "Unsupported backup version" }
        val budgets = root.getJSONArray("budgets").toList { o -> BudgetEntity(
            id = o.optLong("id"), monthKey = o.getString("monthKey"), amount = o.getDouble("amount"),
            breakfastPercent = o.optInt("breakfastPercent", 60), eveningPercent = o.optInt("eveningPercent", 40)
        ).also {
            require(it.amount >= 0 && it.amount.isFinite() && it.breakfastPercent in 0..100 && it.eveningPercent in 0..100)
            require(runCatching { java.time.YearMonth.parse(it.monthKey) }.isSuccess)
        } }
        val expenses = root.getJSONArray("expenses").toList { o -> ExpenseEntity(
            id = o.optLong("id"), amount = o.getDouble("amount"), category = o.getString("category"),
            date = o.getString("date"), time = o.optString("time", "00:00"),
            description = o.optString("description", ""),
            foodItemId = if (o.isNull("foodItemId")) null else o.getLong("foodItemId"),
            notes = o.optString("notes", "")
        ).also {
            require(it.amount > 0 && it.amount.isFinite())
            require(runCatching { java.time.LocalDate.parse(it.date) }.isSuccess)
            require(runCatching { java.time.LocalTime.parse(it.time, java.time.format.DateTimeFormatter.ofPattern("HH:mm")) }.isSuccess)
        } }
        val foods = root.getJSONArray("foodItems").toList { o -> FoodItemEntity(
            id = o.optLong("id"), name = o.getString("name"), category = o.optString("category", "Breakfast"),
            minPrice = o.getDouble("minPrice"), maxPrice = o.getDouble("maxPrice"),
            unit = o.optString("unit", "piece"), available = o.optBoolean("available", true)
        ).also {
            require(it.name.isNotBlank() && it.minPrice >= 0 && it.minPrice.isFinite())
            require(it.maxPrice >= it.minPrice && it.maxPrice.isFinite())
        } }
        val plans = root.getJSONArray("mealPlans").toList { o -> MealPlanEntity(
            id = o.optLong("id"), date = o.getString("date"), category = o.getString("category"),
            itemsJson = o.optString("itemsJson", "[]"), plannedTotal = o.getDouble("plannedTotal"),
            actualTotal = if (o.isNull("actualTotal")) null else o.getDouble("actualTotal")
        ).also {
            require(runCatching { java.time.LocalDate.parse(it.date) }.isSuccess)
            require(it.plannedTotal >= 0 && it.plannedTotal.isFinite())
            require(it.actualTotal == null || it.actualTotal >= 0 && it.actualTotal.isFinite())
            org.json.JSONArray(it.itemsJson)
        } }
        require(budgets.map { it.monthKey }.distinct().size == budgets.size) { "Backup contains duplicate monthly budgets" }

        database.withTransaction {
            val dao = database.dao()
            dao.clearBudgets(); dao.clearExpenses(); dao.clearFoodItems(); dao.clearMealPlans()
            if (budgets.isNotEmpty()) dao.insertBudgets(budgets)
            if (expenses.isNotEmpty()) dao.insertExpenses(expenses)
            if (foods.isNotEmpty()) dao.insertFoodItems(foods)
            if (plans.isNotEmpty()) dao.insertMealPlans(plans)
        }
    }

    suspend fun exportCsv(database: PocketBiteDatabase, context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val lines = mutableListOf("id,amount,category,date,time,description,foodItemId,notes")
        database.dao().getAllExpenses().forEach { e ->
            lines += listOf(e.id, e.amount, e.category, e.date, e.time, e.description, e.foodItemId ?: "", e.notes)
                .joinToString(",") { csv(it.toString()) }
        }
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(lines.joinToString("\n")) }
            ?: error("Could not open the selected CSV destination")
    }

    private fun csv(s: String): String = "\"${s.replace("\"", "\"\"")}\""

    private inline fun <T> JSONArray.toList(mapper: (JSONObject) -> T): List<T> =
        (0 until length()).map { mapper(getJSONObject(it)) }
}
