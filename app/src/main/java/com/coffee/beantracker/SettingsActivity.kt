package com.coffee.beantracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.coffee.beantracker.data.RoastLevel
import com.coffee.beantracker.databinding.ActivitySettingsBinding
import com.coffee.beantracker.history.HistoryTagManager
import com.coffee.beantracker.theme.AppTheme
import com.coffee.beantracker.theme.DarkMode
import com.coffee.beantracker.theme.ThemeManager
import com.coffee.beantracker.utils.ToastCustom
import com.google.android.material.card.MaterialCardView

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private lateinit var backupLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private lateinit var importLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>

    private fun doExport(uri: android.net.Uri) {
        lifecycleScope.launch {
            try {
                val json = com.coffee.beantracker.utils.BackupManager.exportAll(this@SettingsActivity)
                contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                showBackupStatus(getString(R.string.backup_saved))
            } catch (e: Exception) {
                showBackupStatus(getString(R.string.export_failed, e.message?.take(40) ?: ""))
            }
        }
    }

    private fun confirmImport(uri: android.net.Uri) {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CoffeeBean_Dialog)
            .setTitle(R.string.import_confirm_title)
            .setMessage(R.string.import_confirm_msg)
            .setPositiveButton(R.string.yes) { _, _ ->
                lifecycleScope.launch {
                    val r = com.coffee.beantracker.utils.BackupManager.importFrom(this@SettingsActivity, uri)
                    r.fold(
                        onSuccess = { (b, g, ded) ->
                            showBackupStatus(getString(R.string.import_done, b, g, ded))
                        },
                        onFailure = { showBackupStatus(getString(R.string.import_failed, it.message?.take(40) ?: "")) },
                    )
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showBackupStatus(msg: String) {
        binding.tvBackupStatus.visibility = View.VISIBLE
        binding.tvBackupStatus.text = msg
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyOnCreate(this)
        applyThemeOverlay()
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        applyToolbarColors()
        buildThemePreview()
        updateDarkModeText()

        binding.cvDarkMode.setOnClickListener { showDarkModePicker() }
        binding.btnClearHistory.setOnClickListener { showClearHistoryConfirm() }
        binding.cvBrewRecords.setOnClickListener {
            startActivity(Intent(this, DeductRecordsActivity::class.java))
        }
        binding.cvHelpTutorial.setOnClickListener {
            startActivity(Intent(this, HelpTutorialActivity::class.java))
        }
        binding.cvPrivacyPolicy.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }
        binding.tvVersion.text = " " + getVersionNameSafe()
        binding.tvAuthor.text = " " + getString(R.string.author_text)

        // 支持开发者：跳爱发电（与烤豆共用赞助渠道）
        binding.cvSupportDev.setOnClickListener {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://afdian.com/a/RoastCurve")))
            }
        }
        // 姐妹应用：跳烤豆 GitHub 仓库
        binding.tvSisterApp.setOnClickListener {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/MDx-MoJe/RoastCurve")))
            }
        }

        // ===== 数据备份/恢复 =====
        backupLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
        ) { uri -> if (uri != null) doExport(uri) }
        importLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
        ) { uri -> if (uri != null) confirmImport(uri) }

        binding.btnBackupExport.setOnClickListener {
            backupLauncher.launch(com.coffee.beantracker.utils.BackupManager.suggestedFileName())
        }
        binding.btnBackupImport.setOnClickListener {
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        }
    }

    private fun getVersionNameSafe(): String {
        val fromBuildConfig: String? = try { BuildConfig.VERSION_NAME } catch (_: Throwable) { null }
        if (fromBuildConfig != null && fromBuildConfig.isNotEmpty()) return fromBuildConfig
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Throwable) {
            "1.0.0"
        }
    }

    private fun applyThemeOverlay() {
        val colors = ThemeManager.getThemeColors(this)
        theme.applyStyle(android.R.style.Theme_Material_Light_NoActionBar, false)
        val typedArray = obtainStyledAttributes(intArrayOf(
            androidx.appcompat.R.attr.colorPrimary,
            androidx.appcompat.R.attr.colorPrimaryDark,
            androidx.appcompat.R.attr.colorAccent
        ))
        typedArray.recycle()
    }

    private fun applyToolbarColors() {
        val colors = ThemeManager.getThemeColors(this)
        binding.appbar.setBackgroundColor(colors.colorPrimary)
        ThemeManager.applyToToolbar(binding.toolbar, colors)
        window.statusBarColor = colors.colorPrimaryDark
        binding.btnClearHistory.setTextColor(colors.colorPrimary)
    }

    private fun buildThemePreview() {
        val container = binding.themeContainer
        container.removeAllViews()
        val themes = AppTheme.displayList(this)
        val current = ThemeManager.getCurrentTheme()
        val cardParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        cardParams.setMargins(4, 0, 4, 0)

        themes.forEach { (id, _) ->
            val themeEnum = AppTheme.fromId(id)
            val colors = ThemeManager.getThemeColors(this, themeEnum)
            val view = LayoutInflater.from(this).inflate(R.layout.item_theme_choice, container, false) as MaterialCardView
            view.layoutParams = cardParams
            view.radius = 20f
            view.strokeWidth = if (themeEnum == current) 4 else 0
            view.strokeColor = colors.colorPrimaryDark
            view.setCardBackgroundColor(colors.colorPrimary)
            // 白色主题：主色浅，dot 绿色在白背景上突兀 → 隐藏 dot
            val dot = view.findViewById<View>(R.id.viewThemeDot)
            if (themeEnum == AppTheme.PURE_WHITE) {
                dot?.visibility = View.GONE
            } else {
                dot?.setBackgroundColor(colors.colorAccent)
            }
            view.setOnClickListener {
                if (themeEnum != current) {
                    ThemeManager.setTheme(themeEnum)
                    applyNewTheme()
                }
            }
            container.addView(view)
        }
    }

    private fun applyNewTheme() {
        val intent = intent
        finish()
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun updateDarkModeText() {
        binding.tvDarkMode.text = ThemeManager.getDarkMode().localizedName(this)
    }

    private fun showDarkModePicker() {
        val options = DarkMode.displayList(this)
        val current = ThemeManager.getDarkMode()
        val labels = options.map { it.second }.toTypedArray()
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CoffeeBean_Dialog)
            .setTitle(R.string.dark_mode)
            .setSingleChoiceItems(labels, options.indexOfFirst { it.first == current.id }) { d, which ->
                val newMode = DarkMode.fromId(options[which].first)
                ThemeManager.setDarkMode(newMode)
                d.dismiss()
                applyNewTheme()
            }
            .show()
    }

    private fun showClearHistoryConfirm() {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CoffeeBean_Dialog)
            .setTitle(R.string.history_clear)
            .setMessage(R.string.clear_fill_history_confirm)
            .setPositiveButton(R.string.yes) { _, _ ->
                HistoryTagManager.clearAll()
                ToastCustom.show(this, getString(R.string.history_cleared))
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
