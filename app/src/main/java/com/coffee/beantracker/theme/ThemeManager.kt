package com.coffee.beantracker.theme

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.coffee.beantracker.R

enum class AppTheme(val id: Int, val displayName: String) {
    PURE_WHITE(6, "纯净白"),
    COFFEE_BROWN(0, "咖啡棕"),
    FOREST_GREEN(1, "森林绿"),
    OCEAN_BLUE(2, "海洋蓝"),
    SUNSET_ORANGE(3, "落日橙"),
    ROSE_PINK(4, "玫瑰粉"),
    LAVENDER_PURPLE(5, "薰衣紫");

    companion object {
        fun fromId(id: Int): AppTheme = values().firstOrNull { it.id == id } ?: PURE_WHITE
        fun displayList(): List<Pair<Int, String>> = values().map { it.id to it.displayName }
    }
}

enum class DarkMode(val id: Int, val displayName: String) {
    FOLLOW_SYSTEM(0, "跟随系统"),
    LIGHT(1, "浅色模式"),
    DARK(2, "深色模式");

    companion object {
        fun fromId(id: Int): DarkMode = values().firstOrNull { it.id == id } ?: FOLLOW_SYSTEM
        fun displayList(): List<Pair<Int, String>> = values().map { it.id to it.displayName }
    }
}

object ThemeManager {
    private const val PREFS_NAME = "app_theme_prefs"
    private const val KEY_THEME = "selected_theme"
    private const val KEY_DARK_MODE = "dark_mode"

    private lateinit var prefs: SharedPreferences
    private var isDarkModeCached: Boolean? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getCurrentTheme(): AppTheme = AppTheme.fromId(prefs.getInt(KEY_THEME, AppTheme.PURE_WHITE.id))
    fun setTheme(theme: AppTheme) {
        prefs.edit().putInt(KEY_THEME, theme.id).apply()
    }

    fun getDarkMode(): DarkMode = DarkMode.fromId(prefs.getInt(KEY_DARK_MODE, DarkMode.FOLLOW_SYSTEM.id))
    fun setDarkMode(mode: DarkMode) {
        prefs.edit().putInt(KEY_DARK_MODE, mode.id).apply()
        applyNightMode(mode)
    }

    fun applyNightMode(mode: DarkMode) {
        val appCompatMode = when (mode) {
            DarkMode.FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            DarkMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            DarkMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(appCompatMode)
        isDarkModeCached = null // 清除缓存，下次重新计算
    }

    fun applyOnCreate(context: Context) {
        if (!::prefs.isInitialized) init(context)
        applyNightMode(getDarkMode())
        isDarkModeCached = null
    }

    /**
     * 判断当前是否处于深色模式。
     * 优先使用缓存，首次调用时通过 configuration 计算。
     */
    fun isDarkMode(context: Context): Boolean {
        isDarkModeCached?.let { return it }
        val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val result = mode == Configuration.UI_MODE_NIGHT_YES
        isDarkModeCached = result
        return result
    }

    data class ThemeColors(
        val colorPrimary: Int,
        val colorPrimaryDark: Int,
        val colorAccent: Int,
        val titleTextColor: Int
    )

    fun getThemeColors(context: Context, theme: AppTheme = getCurrentTheme()): ThemeColors {
        val res = context.resources
        val dark = isDarkMode(context)
        val c = when (theme) {
            AppTheme.PURE_WHITE -> if (dark) {
                // 深色模式：纯净白主题自动适配为深灰暗色
                ThemeColors(
                    colorPrimary = res.getColor(R.color.t_dark_primary),
                    colorPrimaryDark = res.getColor(R.color.t_dark_primary_dark),
                    colorAccent = res.getColor(R.color.t_white_accent), // 保留浅草绿作为强调色
                    titleTextColor = res.getColor(R.color.white)
                )
            } else {
                ThemeColors(
                    colorPrimary = res.getColor(R.color.t_white_primary),
                    colorPrimaryDark = res.getColor(R.color.t_white_primary_dark),
                    colorAccent = res.getColor(R.color.t_white_accent),
                    titleTextColor = res.getColor(R.color.text_primary_light)
                )
            }
            AppTheme.COFFEE_BROWN -> ThemeColors(
                colorPrimary = res.getColor(R.color.t_coffee_primary),
                colorPrimaryDark = res.getColor(R.color.t_coffee_primary_dark),
                colorAccent = res.getColor(R.color.t_coffee_accent),
                titleTextColor = res.getColor(R.color.white)
            )
            AppTheme.FOREST_GREEN -> ThemeColors(
                colorPrimary = res.getColor(R.color.t_forest_primary),
                colorPrimaryDark = res.getColor(R.color.t_forest_primary_dark),
                colorAccent = res.getColor(R.color.t_forest_accent),
                titleTextColor = res.getColor(R.color.white)
            )
            AppTheme.OCEAN_BLUE -> ThemeColors(
                colorPrimary = res.getColor(R.color.t_ocean_primary),
                colorPrimaryDark = res.getColor(R.color.t_ocean_primary_dark),
                colorAccent = res.getColor(R.color.t_ocean_accent),
                titleTextColor = res.getColor(R.color.white)
            )
            AppTheme.SUNSET_ORANGE -> ThemeColors(
                colorPrimary = res.getColor(R.color.t_sunset_primary),
                colorPrimaryDark = res.getColor(R.color.t_sunset_primary_dark),
                colorAccent = res.getColor(R.color.t_sunset_accent),
                titleTextColor = res.getColor(R.color.white)
            )
            AppTheme.ROSE_PINK -> ThemeColors(
                colorPrimary = res.getColor(R.color.t_rose_primary),
                colorPrimaryDark = res.getColor(R.color.t_rose_primary_dark),
                colorAccent = res.getColor(R.color.t_rose_accent),
                titleTextColor = res.getColor(R.color.white)
            )
            AppTheme.LAVENDER_PURPLE -> ThemeColors(
                colorPrimary = res.getColor(R.color.t_lavender_primary),
                colorPrimaryDark = res.getColor(R.color.t_lavender_primary_dark),
                colorAccent = res.getColor(R.color.t_lavender_accent),
                titleTextColor = res.getColor(R.color.white)
            )
        }
        return c
    }

    /**
     * 统一把 Toolbar 的背景、标题文字颜色、返回按钮颜色设置好，
     * 解决白色主题（PURE_WHITE）下默认白字白底看不见问题。
     */
    fun applyToToolbar(toolbar: Toolbar, colors: ThemeColors, backgroundOverride: Int? = null) {
        toolbar.setBackgroundColor(backgroundOverride ?: colors.colorPrimary)
        toolbar.setTitleTextColor(colors.titleTextColor)
        toolbar.navigationIcon?.setTint(colors.titleTextColor)
        toolbar.overflowIcon?.setTint(colors.titleTextColor)
    }
}
