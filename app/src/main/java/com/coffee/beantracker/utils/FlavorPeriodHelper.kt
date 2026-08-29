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

    fun getRange(context: android.content.Context, roastLevel: RoastLevel): FlavorRange {
        return when (roastLevel) {
            RoastLevel.LIGHT -> FlavorRange(
                restMin = 7, restMax = 14,
                flavorMin = 10, flavorMax = 30,
                description = context.getString(com.coffee.beantracker.R.string.roast_level_light_desc)
            )
            RoastLevel.MEDIUM_LIGHT -> FlavorRange(
                restMin = 6, restMax = 12,
                flavorMin = 8, flavorMax = 25,
                description = context.getString(com.coffee.beantracker.R.string.roast_level_medium_light_desc)
            )
            RoastLevel.MEDIUM -> FlavorRange(
                restMin = 5, restMax = 10,
                flavorMin = 7, flavorMax = 21,
                description = context.getString(com.coffee.beantracker.R.string.roast_level_medium_desc)
            )
            RoastLevel.MEDIUM_DARK -> FlavorRange(
                restMin = 3, restMax = 7,
                flavorMin = 5, flavorMax = 14,
                description = context.getString(com.coffee.beantracker.R.string.roast_level_medium_dark_desc)
            )
            RoastLevel.DARK -> FlavorRange(
                restMin = 3, restMax = 7,
                flavorMin = 5, flavorMax = 14,
                description = context.getString(com.coffee.beantracker.R.string.roast_level_dark_desc)
            )
        }
    }

    fun getSuggestionText(context: android.content.Context, roastLevel: RoastLevel): String {
        val r = getRange(context, roastLevel)
        return context.getString(com.coffee.beantracker.R.string.suggestion_suffix, r.restMin.toString() + "-" + r.restMax, r.flavorMin.toString() + "-" + r.flavorMax)
            .let { r.description + it }
    }

    /** 推荐养豆天数（区间中值，四舍五入） */
    fun getDefaultRestDays(roastLevel: RoastLevel): Int {
        val r = rangeOf(roastLevel)
        return Math.round((r.restMin + r.restMax) / 2f)
    }

    /** 推荐赏味天数（区间中值，四舍五入） */
    fun getDefaultBestBefore(roastLevel: RoastLevel): Int {
        val r = rangeOf(roastLevel)
        return Math.round((r.flavorMin + r.flavorMax) / 2f)
    }

    /** 数值专用（不含描述文案），供纯计算使用 */
    private fun rangeOf(roastLevel: RoastLevel): FlavorRange {
        return when (roastLevel) {
            RoastLevel.LIGHT -> FlavorRange(7, 14, 10, 30, "")
            RoastLevel.MEDIUM_LIGHT -> FlavorRange(6, 12, 8, 25, "")
            RoastLevel.MEDIUM -> FlavorRange(5, 10, 7, 21, "")
            RoastLevel.MEDIUM_DARK -> FlavorRange(3, 7, 5, 14, "")
            RoastLevel.DARK -> FlavorRange(3, 7, 5, 14, "")
        }
    }

    fun getFullRulesText(context: android.content.Context): String {
        val sb = StringBuilder()
        sb.append(context.getString(com.coffee.beantracker.R.string.flavor_rules_header)).append("\n\n")
        sb.append(context.getString(com.coffee.beantracker.R.string.flavor_rules_intro)).append("\n\n")

        for (level in RoastLevel.values()) {
            val r = getRange(context, level)
            sb.append("◆ ").append(r.description).append("\n")
            sb.append("  ").append(context.getString(com.coffee.beantracker.R.string.rest_label))
                .append(r.restMin).append("-").append(r.restMax).append(" ").append(context.getString(com.coffee.beantracker.R.string.days_unit_short)).append("\n")
            sb.append("  ").append(context.getString(com.coffee.beantracker.R.string.flavor_window_label, r.flavorMin.toString() + "-" + r.flavorMax))
            if (r.maxRecommend < 30) {
                sb.append(context.getString(com.coffee.beantracker.R.string.max_recommend, r.maxRecommend))
            }
            sb.append("\n\n")
        }

        sb.append(context.getString(com.coffee.beantracker.R.string.flavor_rules_tip))
        return sb.toString()
    }
}
