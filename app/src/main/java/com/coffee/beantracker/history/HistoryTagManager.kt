package com.coffee.beantracker.history

import android.content.Context
import android.content.SharedPreferences

object HistoryTagManager {
    private const val PREFS_NAME = "history_tags"
    private const val MAX_HISTORY = 20
    private const val SEPARATOR = "|||"

    enum class TagType(val key: String) {
        NAME("bean_name"),
        PROCESS_METHOD("process_method"),
        ORIGIN("origin"),
        FLAVOR("flavor_notes"),
        ROAST_LEVEL("roast_level"),
        DEVELOPMENT_TIME("development_time")
    }

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getList(type: TagType): MutableList<String> {
        if (!::prefs.isInitialized) return mutableListOf()
        val raw = prefs.getString(type.key, "") ?: ""
        if (raw.isBlank()) return mutableListOf()
        return raw.split(SEPARATOR).filter { it.isNotBlank() }.toMutableList()
    }

    private fun saveList(type: TagType, list: List<String>) {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(type.key, list.joinToString(SEPARATOR)).apply()
    }

    fun add(type: TagType, value: String) = addTag(type, value)

    fun addTag(type: TagType, value: String) {
        val v = value.trim()
        if (v.isBlank()) return
        val list = getList(type)
        list.remove(v)
        list.add(0, v)
        saveList(type, list.take(MAX_HISTORY))
    }

    fun get(type: TagType): List<String> = getList(type)

    fun getTags(type: TagType): List<String> = getList(type)

    fun clearAll() {
        if (!::prefs.isInitialized) return
        prefs.edit().clear().apply()
    }
}
