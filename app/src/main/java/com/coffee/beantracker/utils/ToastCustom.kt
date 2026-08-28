package com.coffee.beantracker.utils

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.coffee.beantracker.R

object ToastCustom {

    /**
     * 显示草绿色胶囊 Toast。
     * 位置固定在屏幕中上区域（对应咖啡豆卡片上半部分，避免与底部导航 dock 栏重叠）。
     */
    fun show(context: Context, message: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
        val toast = Toast(context.applicationContext)
        val inflater = LayoutInflater.from(context.applicationContext)
        val layout: View = inflater.inflate(R.layout.toast_custom_layout, null)
        val tv = layout.findViewById<TextView>(R.id.tvToastText)
        tv.text = message
        toast.view = layout
        toast.duration = duration
        // 位置：水平居中 + 顶部往下 280dp（避开 Toolbar，落在卡片中部偏上红箭头指向区域）
        val yPx = dp2px(context, 280f)
        toast.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP, 0, yPx)
        toast.show()
    }

    fun showShort(context: Context, message: CharSequence) = show(context, message, Toast.LENGTH_SHORT)
    fun showLong(context: Context, message: CharSequence) = show(context, message, Toast.LENGTH_LONG)

    private fun dp2px(context: Context, dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
