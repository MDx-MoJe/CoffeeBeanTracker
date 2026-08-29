package com.coffee.beantracker.data

import kotlinx.serialization.Serializable
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RoastLevel(val displayName: String) {
    LIGHT("浅烘"),
    MEDIUM_LIGHT("中浅烘"),
    MEDIUM("中烘"),
    MEDIUM_DARK("中深烘"),
    DARK("深烘");

    /** 本地化名称：跟随系统语言（en→Light/Medium/...，其余回退中文） */
    fun localizedName(context: android.content.Context): String {
        val resId = when (this) {
            LIGHT -> com.coffee.beantracker.R.string.roast_level_light
            MEDIUM_LIGHT -> com.coffee.beantracker.R.string.roast_level_medium_light
            MEDIUM -> com.coffee.beantracker.R.string.roast_level_medium
            MEDIUM_DARK -> com.coffee.beantracker.R.string.roast_level_medium_dark
            DARK -> com.coffee.beantracker.R.string.roast_level_dark
        }
        return context.getString(resId)
    }
}

@Entity(tableName = "coffee_beans")
@Serializable
data class CoffeeBean(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val roastDate: Long,
    val restDays: Int,
    val bestBeforeDays: Int,
    val processMethod: String = "",
    val roastLevel: String = RoastLevel.MEDIUM.name,
    val origin: String = "",
    val flavorNotes: String = "",
    val developmentTime: String = "",
    val stockGrams: Double = 0.0,
    val deductGrams: Double = 18.0,
    val pourOverGrams: Double = 15.0,
    val espressoGrams: Double = 18.0,
    val imagePath: String = "",
    val backgroundImagePath: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun getRoastLevels(context: android.content.Context): List<Pair<String, String>> {
            return RoastLevel.values().map { it.name to it.localizedName(context) }
        }
    }
}