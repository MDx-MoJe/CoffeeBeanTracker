package com.coffee.beantracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.coffee.beantracker.databinding.FragmentProfileBinding
import com.coffee.beantracker.theme.AppTheme
import com.coffee.beantracker.theme.DarkMode
import com.coffee.beantracker.theme.ThemeManager
import com.google.android.material.card.MaterialCardView
import android.widget.LinearLayout

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // SAF 导入：必须在 Fragment 创建阶段注册（生命周期契约），放 onViewCreated 会 lateinit 未初始化闪退
    private val importLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) confirmImport(uri) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buildThemePreview()
        updateDarkModeText()

        binding.cvDarkMode.setOnClickListener { showDarkModePicker() }
        binding.cvBrewRecords.setOnClickListener {
            startActivity(Intent(requireContext(), DeductRecordsActivity::class.java))
        }
        binding.cvHelpTutorial.setOnClickListener {
            startActivity(Intent(requireContext(), HelpTutorialActivity::class.java))
        }
        binding.cvPrivacyPolicy.setOnClickListener {
            startActivity(Intent(requireContext(), PrivacyPolicyActivity::class.java))
        }
        binding.tvVersion.text = " " + getVersionNameSafe()
        binding.tvAuthor.text = " " + getString(R.string.author_text)

        // ===== 数据备份/恢复（zip 直写 Download，导入兼容 zip/json）=====
        binding.btnBackupExport.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val json = com.coffee.beantracker.utils.BackupManager.exportAll(requireContext())
                    val zip = com.coffee.beantracker.utils.BackupManager.packZip(json)
                    val name = com.coffee.beantracker.utils.BackupManager.suggestedFileName()
                    val where = com.coffee.beantracker.utils.BackupManager.saveToDownloads(requireContext(), name, zip)
                    showBackupStatus(if (where != null) "✅ 已保存到 $where" else "❌ 保存失败")
                } catch (e: Exception) {
                    showBackupStatus("❌ 导出失败：${e.message?.take(40)}")
                }
            }
        }
        binding.btnBackupImport.setOnClickListener {
            importLauncher.launch(arrayOf("application/zip", "application/json", "application/octet-stream", "*/*"))
        }

        // 支持开发者与姐妹应用
        binding.cvSupportDev.setOnClickListener {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://afdian.com/a/RoastCurve")))
            }
        }
        binding.tvSisterApp.setOnClickListener {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/MDx-MoJe/RoastCurve")))
            }
        }
    }

    private fun confirmImport(uri: android.net.Uri) {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_CoffeeBean_Dialog)
            .setTitle("确认导入？")
            .setMessage("将按 ID 合并备份中的数据（已有 ID 覆盖、新 ID 插入），不删除现有数据。")
            .setPositiveButton(R.string.yes) { _, _ ->
                lifecycleScope.launch {
                    val r = com.coffee.beantracker.utils.BackupManager.importFrom(requireContext(), uri)
                    r.fold(
                        onSuccess = { (b, g, ded) ->
                            showBackupStatus("✅ 导入完成：熟豆 $b / 生豆 $g / 流水 $ded")
                        },
                        onFailure = {
                            showBackupStatus("❌ 导入失败：${com.coffee.beantracker.utils.BackupManager.friendlyError(it)}")
                        },
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getVersionNameSafe(): String {
        val fromBuildConfig: String? = try { BuildConfig.VERSION_NAME } catch (_: Throwable) { null }
        if (fromBuildConfig != null && fromBuildConfig.isNotEmpty()) return fromBuildConfig
        return try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Throwable) {
            "1.0.0"
        }
    }

    private fun buildThemePreview() {
        val ctx = requireContext()
        val container = binding.themeContainer
        container.removeAllViews()
        val themes = AppTheme.displayList()
        val current = ThemeManager.getCurrentTheme()
        val cardParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        cardParams.setMargins(4, 0, 4, 0)

        themes.forEach { (id, name) ->
            val themeEnum = AppTheme.fromId(id)
            val colors = ThemeManager.getThemeColors(ctx, themeEnum)
            val view = LayoutInflater.from(ctx).inflate(R.layout.item_theme_choice, container, false) as MaterialCardView
            view.layoutParams = cardParams
            view.radius = 20f
            view.strokeWidth = if (themeEnum == current) 4 else 0
            view.strokeColor = colors.colorPrimaryDark
            view.setCardBackgroundColor(colors.colorPrimary)
            val title = view.findViewById<android.widget.TextView>(R.id.tvThemeName)
            title?.text = name
            title?.setTextColor(
                when {
                    themeEnum == AppTheme.PURE_WHITE -> colors.titleTextColor
                    themeEnum == AppTheme.SUNSET_ORANGE || themeEnum == AppTheme.ROSE_PINK -> android.graphics.Color.WHITE
                    else -> colors.titleTextColor
                }
            )
            val dot = view.findViewById<View>(R.id.viewThemeDot)
            dot?.setBackgroundColor(colors.colorAccent)
            view.setOnClickListener {
                if (themeEnum != current) {
                    ThemeManager.setTheme(themeEnum)
                    requireActivity().recreate()
                }
            }
            container.addView(view)
        }
    }

    private fun updateDarkModeText() {
        binding.tvDarkMode.text = ThemeManager.getDarkMode().displayName
    }

    private fun showDarkModePicker() {
        val options = DarkMode.displayList()
        val current = ThemeManager.getDarkMode()
        val labels = options.map { it.second }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_CoffeeBean_Dialog)
            .setTitle(R.string.dark_mode)
            .setSingleChoiceItems(labels, options.indexOfFirst { it.first == current.id }) { d, which ->
                val newMode = DarkMode.fromId(options[which].first)
                ThemeManager.setDarkMode(newMode)
                d.dismiss()
                requireActivity().recreate()
            }
            .show()
    }
}
