package com.coffee.beantracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
