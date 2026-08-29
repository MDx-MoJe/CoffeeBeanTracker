package com.coffee.beantracker

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.coffee.beantracker.databinding.ActivityHelpTutorialBinding
import com.coffee.beantracker.theme.ThemeManager
import com.google.android.material.card.MaterialCardView

class HelpTutorialActivity : BaseActivity() {

    private lateinit var binding: ActivityHelpTutorialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyOnCreate(this)
        super.onCreate(savedInstanceState)
        binding = ActivityHelpTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.help_and_tutorial)

        val colors = ThemeManager.getThemeColors(this)
        binding.appbar.setBackgroundColor(colors.colorPrimary)
        ThemeManager.applyToToolbar(binding.toolbar, colors)
        window.statusBarColor = colors.colorPrimaryDark

        setupContent()
    }

    private fun setupContent() {
        val ctx = this
        val container = binding.contentContainer
        container.removeAllViews()

        // ========== 一、主要功能介绍（新文案：4 项） ==========
        container.addView(buildSectionTitle(ctx, getString(R.string.help_feature_intro)))
        container.addView(buildFeatureCard(ctx, getString(R.string.help_feature_1_title), getString(R.string.help_feature_1_desc)))
        container.addView(buildFeatureCard(ctx, getString(R.string.help_feature_2_title), getString(R.string.help_feature_2_desc)))
        container.addView(buildFeatureCard(ctx, getString(R.string.help_feature_3_title), getString(R.string.help_feature_3_desc)))
        container.addView(buildFeatureCard(ctx, getString(R.string.help_feature_4_title), getString(R.string.help_feature_4_desc)))

        // ========== 二、使用提示（新文案：4 项） ==========
        container.addView(buildSectionTitle(ctx, getString(R.string.help_usage_tips)))
        container.addView(buildFeatureCard(ctx, getString(R.string.help_tips_1_title), getString(R.string.help_tips_1_desc)))
        container.addView(buildFeatureCard(ctx, getString(R.string.help_tips_2_title), getString(R.string.help_tips_2_desc)))
        container.addView(buildFeatureCard(ctx, getString(R.string.help_tips_3_title), getString(R.string.help_tips_3_desc)))
        container.addView(buildFeatureCard(ctx, getString(R.string.help_tips_4_title), getString(R.string.help_tips_4_desc)))
    }

    private fun buildSectionTitle(ctx: android.content.Context, text: String): View {
        val tv = TextView(ctx).apply {
            this.text = text
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            val out = android.util.TypedValue()
            ctx.theme.resolveAttribute(android.R.attr.textColorPrimary, out, true)
            setTextColor(out.data)
            setPadding(dp2px(ctx, 20), dp2px(ctx, 20), dp2px(ctx, 20), dp2px(ctx, 8))
        }
        return tv
    }

    private fun buildFeatureCard(ctx: android.content.Context, title: String, desc: String): View {
        val outer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp2px(ctx, 16), 0, dp2px(ctx, 16), dp2px(ctx, 12))
        }
        val card = MaterialCardView(ctx).apply {
            radius = dp2px(ctx, 24).toFloat()
            cardElevation = 0f
            strokeWidth = dp2px(ctx, 1)
            strokeColor = ctx.resources.getColor(R.color.panel_stroke_light)
            setCardBackgroundColor(ctx.resources.getColor(R.color.card_bg_warm))
        }
        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp2px(ctx, 16), dp2px(ctx, 16), dp2px(ctx, 16), dp2px(ctx, 16))
        }
        val tvTitle = TextView(ctx).apply {
            this.text = title
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            val out = android.util.TypedValue()
            ctx.theme.resolveAttribute(android.R.attr.textColorPrimary, out, true)
            setTextColor(out.data)
        }
        val tvDesc = TextView(ctx).apply {
            this.text = desc
            textSize = 13.5f
            setTextColor(ctx.resources.getColor(R.color.text_secondary))
            setPadding(0, dp2px(ctx, 6), 0, 0)
            lineHeight = dp2px(ctx, 20)
        }
        inner.addView(tvTitle)
        inner.addView(tvDesc)
        card.addView(inner)
        outer.addView(card)
        return outer
    }

    private fun dp2px(ctx: android.content.Context, dp: Int): Int {
        val density = ctx.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
