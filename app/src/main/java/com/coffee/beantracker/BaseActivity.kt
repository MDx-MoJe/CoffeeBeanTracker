package com.coffee.beantracker

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.coffee.beantracker.utils.LocaleManager

/**
 * 所有 Activity 的基类：注入应用内语言（attachBaseContext 覆写）。
 * 主题（深色模式等）由各 Activity 自行在 onCreate 调用 ThemeManager，保持原有行为。
 */
open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
