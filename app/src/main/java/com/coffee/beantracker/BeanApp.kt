package com.coffee.beantracker

import android.app.Application
import com.coffee.beantracker.history.HistoryTagManager
import com.coffee.beantracker.theme.ThemeManager

class BeanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.init(this)
        ThemeManager.applyOnCreate(this)
        HistoryTagManager.init(this)
    }
}
