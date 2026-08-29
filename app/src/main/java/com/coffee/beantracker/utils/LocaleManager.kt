package com.coffee.beantracker.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

/**
 * 应用内语言切换（中英双语）
 *
 * - AppLocale.SYSTEM  跟随系统（默认）
 * - AppLocale.ZH_CN   强制中文
 * - AppLocale.EN_US   强制英文
 *
 * 通过覆写 Activity.attachBaseContext 应用语言，切换后需重建 Activity。
 */
enum class AppLocale(val id: Int, val displayName: String) {
    SYSTEM(0, "跟随系统"),
    ZH_CN(1, "中文"),
    EN_US(2, "English");

    companion object {
        fun fromId(id: Int): AppLocale = values().firstOrNull { it.id == id } ?: SYSTEM
        fun displayList(): List<Pair<Int, String>> = values().map { it.id to it.displayName }
    }
}

object LocaleManager {
    private const val PREFS = "locale_prefs"
    private const val KEY_LOCALE = "app_locale"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun getLocale(): AppLocale = AppLocale.fromId(prefs.getInt(KEY_LOCALE, AppLocale.SYSTEM.id))

    fun setLocale(locale: AppLocale) {
        prefs.edit().putInt(KEY_LOCALE, locale.id).apply()
    }

    /** 当前生效的 Locale 对象（SYSTEM 时返回系统默认） */
    fun effectiveLocale(context: Context): Locale {
        return when (getLocale()) {
            AppLocale.SYSTEM -> Locale.getDefault()
            AppLocale.ZH_CN -> Locale.SIMPLIFIED_CHINESE
            AppLocale.EN_US -> Locale.US
        }
    }

    /**
     * 在 Activity.attachBaseContext 中调用，把应用语言注入 Configuration。
     * 返回包装后的 context，调用方应 super.attachBaseContext(LocaleManager.wrap(this, newBase))
     */
    fun wrap(base: Context): Context {
        val locale = effectiveLocale(base)
        if (locale == Locale.getDefault()) return base
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
