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
            text = PRIVACY_POLICY_TEXT
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
        val urls = listOfNotNull(
            if (PRIVACY_URL_EXTERNAL.isNotEmpty()) "在线完整版（外部渠道）：$PRIVACY_URL_EXTERNAL" else null,
            if (PRIVACY_URL_CN.isNotEmpty()) "在线完整版（国内渠道）：$PRIVACY_URL_CN" else null,
        )
        if (urls.isNotEmpty()) {
            text.text = PRIVACY_POLICY_TEXT + "\n" + urls.joinToString("\n")
        }

        dialog.show()
        return false
    }

    private const val PRIVACY_POLICY_TEXT = """《隐私政策》

更新日期：2026-08-27　政策版本：1

欢迎使用豆袋（CoffeeBeanTracker）。我们深知个人信息对您的重要性，本应用在设计上即遵循"数据不出设备"原则：

一、我们收集什么
本应用不收集、不上传、不分享您的任何个人信息。无需注册账号，不申请通讯录、位置、麦克风等敏感权限。

二、数据存储位置
您的所有生豆、熟豆库存与冲煮记录仅保存在本机应用私有目录中。卸载应用即彻底删除。

三、文件读写
仅当您主动导出或导入备份/日志文件时访问系统存储，文件保存在您指定的位置。备份文件由您自行保管，我们不持有副本。

四、未成年人保护
本应用面向咖啡爱好者，不针对儿童收集任何信息。

五、政策更新
若未来应用引入任何新功能涉及数据处理，我们将更新本政策并通过应用内弹窗重新征求您的同意。
"""
}
