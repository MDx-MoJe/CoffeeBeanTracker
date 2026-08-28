package com.coffee.beantracker.utils

import java.util.Calendar
import java.util.concurrent.TimeUnit

object DateUtils {
    fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getDaysBetween(startTimestamp: Long, endTimestamp: Long): Long {
        val start = getStartOfDay(startTimestamp)
        val end = getStartOfDay(endTimestamp)
        val diff = end - start
        return TimeUnit.MILLISECONDS.toDays(diff)
    }

    fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format("%d-%02d-%02d", year, month, day)
    }
}

enum class BeanStatus {
    RESTING,
    READY,
    BEST_PERIOD,
    PAST_BEST
}

data class BeanStatusInfo(
    val status: BeanStatus,
    val displayText: String,
    val daysDisplay: String,
    val progress: Int
)

object BeanStatusCalculator {
    fun calculateStatus(bean: com.coffee.beantracker.data.CoffeeBean): BeanStatusInfo {
        val today = System.currentTimeMillis()
        val daysSinceRoast = DateUtils.getDaysBetween(bean.roastDate, today)
        val restDays = bean.restDays.toLong()
        val bestBefore = bean.bestBeforeDays.toLong()

        return when {
            daysSinceRoast < restDays -> {
                val remaining = restDays - daysSinceRoast
                val progress = ((daysSinceRoast.toDouble() / restDays) * 100).toInt().coerceIn(0, 100)
                BeanStatusInfo(
                    status = BeanStatus.RESTING,
                    displayText = "养豆中",
                    daysDisplay = "还剩 ${remaining} 天",
                    progress = progress
                )
            }
            daysSinceRoast < bestBefore -> {
                val daysReady = daysSinceRoast - restDays
                val totalBest = bestBefore - restDays
                val bestProgress = if (totalBest > 0) {
                    ((daysReady.toDouble() / totalBest) * 50 + 50).toInt().coerceIn(50, 99)
                } else 50
                BeanStatusInfo(
                    status = BeanStatus.BEST_PERIOD,
                    displayText = "最佳赏味期",
                    daysDisplay = "已养 ${daysSinceRoast} 天",
                    progress = bestProgress
                )
            }
            else -> {
                val pastDays = daysSinceRoast - bestBefore
                BeanStatusInfo(
                    status = BeanStatus.PAST_BEST,
                    displayText = "已过赏味期",
                    daysDisplay = "超过 ${pastDays} 天",
                    progress = 100
                )
            }
        }
    }
}
