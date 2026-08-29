package com.coffee.beantracker.utils

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AlertDialog
import com.coffee.beantracker.R
import android.widget.TextView
import android.widget.ScrollView

/**
 * 隐私政策同意门闩（唯一配置点）
 *
 * - POLICY_VERSION 与政策文本实质性变化联动：+1 后所有老用户下次启动自动重弹
 * - 在线链接预留给未来内外网分发：空串 = 不显示入口；
 *   届时外网包填 GitHub Pages 地址、国内包填 Gitee Pages 地址
 */
object ConsentManager {

    const val POLICY_VERSION = 1

    /** 外网分发渠道在线版地址，空 = 不显示该入口 */
    const val PRIVACY_URL_EXTERNAL = ""

    /** 国内分发渠道在线版地址，空 = 不显示该入口 */
    const val PRIVACY_URL_CN = ""

    private const val PREFS = "consent"
    private const val KEY_ACCEPTED_VERSION = "accepted_version"

    private fun prefs(activity: Activity): SharedPreferences =
        activity.getSharedPreferences(PREFS, Application.MODE_PRIVATE)

    fun isAccepted(activity: Activity): Boolean =
        prefs(activity).getInt(KEY_ACCEPTED_VERSION, 0) >= POLICY_VERSION

    /**
     * 未同意则弹窗遮蔽启动流程；同意后写回并回调继续。
     * 拒绝 → 二次确认 → 结束应用。
     *
     * @return 是否已同意（true 时调用方可直接走正常启动逻辑）
     */
    fun ensureConsent(activity: Activity, onAccepted: () -> Unit): Boolean {
        if (isAccepted(activity)) return true

        val scroll = ScrollView(activity)
        val text = TextView(activity).apply {
            setPadding(48, 24, 48, 24)
            textSize = 14f
            setTextIsSelectable(false)
            text = activity.getString(com.coffee.beantracker.R.string.consent_policy_text)
        }
        scroll.addView(text)

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.consent_title)
            .setView(scroll)
            .setCancelable(false)
            .create()

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, activity.getString(R.string.consent_accept)) { _, _ ->
            prefs(activity).edit().putInt(KEY_ACCEPTED_VERSION, POLICY_VERSION).apply()
            onAccepted()
        }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, activity.getString(R.string.consent_decline)) { _, _ ->
            // 拒绝 → 二次确认，确认后退出；取消则重新弹回政策
            AlertDialog.Builder(activity)
                .setTitle(R.string.consent_confirm_title)
                .setMessage(R.string.consent_confirm_message)
                .setCancelable(false)
                .setPositiveButton(R.string.consent_exit) { _, _ -> activity.finishAffinity() }
                .setNegativeButton(R.string.consent_recheck) { _, _ ->
                    ensureConsent(activity, onAccepted)   // 重新弹回
                }
                .show()
        }

        // 在线链接预留入口（配置了才显示）：追加到正文末尾
        val policyText = activity.getString(com.coffee.beantracker.R.string.consent_policy_text)
        val urls = listOfNotNull(
            if (PRIVACY_URL_EXTERNAL.isNotEmpty()) activity.getString(com.coffee.beantracker.R.string.online_full_ext, PRIVACY_URL_EXTERNAL) else null,
            if (PRIVACY_URL_CN.isNotEmpty()) activity.getString(com.coffee.beantracker.R.string.online_full_cn, PRIVACY_URL_CN) else null,
        )
        if (urls.isNotEmpty()) {
            text.text = policyText + "\n" + urls.joinToString("\n")
        } else {
            text.text = policyText
        }

        dialog.show()
        return false
    }
}
