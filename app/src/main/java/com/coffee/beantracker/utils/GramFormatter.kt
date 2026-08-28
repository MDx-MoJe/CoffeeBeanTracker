package com.coffee.beantracker.utils

import java.util.Locale

/**
 * 克数格式化/解析工具
 *
 * 规则：支持一位小数；显示时整数不带小数点，小数保留一位；
 * 解析时四舍五入到一位小数。
 */
object GramFormatter {

    /** 格式化为显示文本：0 → "0"，12.5 → "12.5"，12.0 → "12" */
    fun format(grams: Double): String {
        return if (grams == Math.floor(grams) && !grams.isInfinite()) {
            grams.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", grams)
        }
    }

    /** 格式化并带单位：12.5 → "12.5g" */
    fun formatWithUnit(grams: Double): String = format(grams) + "g"

    /** 解析输入文本到克数，非法输入返回 null；结果四舍五入到一位小数 */
    fun parse(text: String?): Double? {
        val t = text?.trim() ?: return null
        if (t.isEmpty()) return null
        val v = t.toDoubleOrNull() ?: return null
        if (v < 0 || !v.isFinite()) return null
        // 四舍五入到一位小数
        val rounded = Math.round(v * 10.0) / 10.0
        return rounded
    }

    /** 解析带默认值版本 */
    fun parseOr(text: String?, default: Double): Double = parse(text) ?: default
}
