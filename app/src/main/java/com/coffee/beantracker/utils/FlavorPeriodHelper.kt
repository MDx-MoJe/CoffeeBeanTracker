package com.coffee.beantracker.utils

import com.coffee.beantracker.data.RoastLevel

object FlavorPeriodHelper {

    data class FlavorRange(
        val restMin: Int,
        val restMax: Int,
        val flavorMin: Int,
        val flavorMax: Int,
        val description: String,
        val maxRecommend: Int = 30
    )

    fun getRange(roastLevel: RoastLevel): FlavorRange {
        return when (roastLevel) {
            RoastLevel.LIGHT -> FlavorRange(
                restMin = 7, restMax = 14,
                flavorMin = 10, flavorMax = 30,
                description = "浅烘（手冲花果调）"
            )
            RoastLevel.MEDIUM_LIGHT -> FlavorRange(
                restMin = 6, restMax = 12,
                flavorMin = 8, flavorMax = 25,
                description = "中浅烘"
            )
            RoastLevel.MEDIUM -> FlavorRange(
                restMin = 5, restMax = 10,
                flavorMin = 7, flavorMax = 21,
                description = "中烘（均衡型，峰值窗口）"
            )
            RoastLevel.MEDIUM_DARK -> FlavorRange(
                restMin = 3, restMax = 7,
                flavorMin = 5, flavorMax = 14,
                description = "中深烘（意式油脂调）"
            )
            RoastLevel.DARK -> FlavorRange(
                restMin = 3, restMax = 7,
                flavorMin = 5, flavorMax = 14,
                description = "深烘（意式油脂调）"
            )
        }
    }

    fun getSuggestionText(roastLevel: RoastLevel): String {
        val r = getRange(roastLevel)
        return r.description + "：养豆 " + r.restMin + "-" + r.restMax + " 天，赏味期约 " + r.flavorMin + "-" + r.flavorMax + " 天"
    }

    /** 推荐养豆天数（区间中值，四舍五入） */
    fun getDefaultRestDays(roastLevel: RoastLevel): Int {
        val r = getRange(roastLevel)
        return Math.round((r.restMin + r.restMax) / 2f)
    }

    /** 推荐赏味天数（区间中值，四舍五入） */
    fun getDefaultBestBefore(roastLevel: RoastLevel): Int {
        val r = getRange(roastLevel)
        return Math.round((r.flavorMin + r.flavorMax) / 2f)
    }

    fun getFullRulesText(): String {
        val sb = StringBuilder()
        sb.append("【赏味期计算规则】\n\n")
        sb.append("不同烘焙深度的咖啡豆，养豆时间和赏味期各不相同：\n\n")

        for (level in RoastLevel.values()) {
            val r = getRange(level)
            sb.append("◆ ").append(r.description).append("\n")
            sb.append("  养豆：").append(r.restMin).append("-").append(r.restMax).append(" 天\n")
            sb.append("  赏味期：第 ").append(r.flavorMin).append("-").append(r.flavorMax).append(" 天")
            if (r.maxRecommend < 30) {
                sb.append("（最长不建议超 ").append(r.maxRecommend).append(" 天）")
            }
            sb.append("\n\n")
        }

        sb.append("提示：以上为建议范围，具体可根据个人口味和豆子特性微调。")
        return sb.toString()
    }
}
