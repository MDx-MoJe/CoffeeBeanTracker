package com.coffee.beantracker

import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.coffee.beantracker.databinding.ActivityPrivacyPolicyBinding
import com.coffee.beantracker.theme.ThemeManager
import com.google.android.material.card.MaterialCardView

class PrivacyPolicyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrivacyPolicyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyOnCreate(this)
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.privacy_policy)

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

        container.addView(buildBigTitle(ctx, getString(R.string.privacy_title)))
        container.addView(buildSubTitle(ctx, getString(R.string.privacy_effective_date)))
        container.addView(buildCard(ctx, "", getString(R.string.privacy_intro), 14f))

        container.addView(buildSectionTitle(ctx, getString(R.string.privacy_section_1_title)))
        container.addView(buildCard(ctx, "", getString(R.string.privacy_section_1_content), 13.5f))

        container.addView(buildSectionTitle(ctx, getString(R.string.privacy_section_2_title)))
        container.addView(buildCard(ctx, "", getString(R.string.privacy_section_2_content), 13.5f))

        container.addView(buildSectionTitle(ctx, getString(R.string.privacy_section_3_title)))
        container.addView(buildCard(ctx, "", getString(R.string.privacy_section_3_content), 13.5f))

        container.addView(buildSectionTitle(ctx, getString(R.string.privacy_section_4_title)))
        container.addView(buildCard(ctx, "", getString(R.string.privacy_section_4_content), 13.5f))

        container.addView(buildSectionTitle(ctx, getString(R.string.privacy_section_5_title)))
        container.addView(buildCard(ctx, "", getString(R.string.privacy_section_5_content), 13.5f))

        container.addView(buildSectionTitle(ctx, getString(R.string.privacy_section_6_title)))
        container.addView(buildCard(ctx, "", getString(R.string.privacy_section_6_content), 13.5f))

        container.addView(buildSectionTitle(ctx, getString(R.string.privacy_section_7_title)))
        container.addView(buildCard(ctx, "", getString(R.string.privacy_section_7_content), 13.5f))
    }

    private fun buildBigTitle(ctx: android.content.Context, text: String): View {
        val tv = TextView(ctx).apply {
            this.text = text
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            val out = android.util.TypedValue()
            ctx.theme.resolveAttribute(android.R.attr.textColorPrimary, out, true)
            setTextColor(out.data)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp2px(ctx, 20), dp2px(ctx, 20), dp2px(ctx, 20), dp2px(ctx, 4))
        }
        return tv
    }

    private fun buildSubTitle(ctx: android.content.Context, text: String): View {
        val tv = TextView(ctx).apply {
            this.text = text
            textSize = 12.5f
            setTextColor(resources.getColor(R.color.text_secondary))
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp2px(ctx, 20), dp2px(ctx, 4), dp2px(ctx, 20), dp2px(ctx, 16))
        }
        return tv
    }

    private fun buildSectionTitle(ctx: android.content.Context, text: String): View {
        val tv = TextView(ctx).apply {
            this.text = text
            textSize = 15.5f
            setTypeface(null, android.graphics.Typeface.BOLD)
            val out = android.util.TypedValue()
            ctx.theme.resolveAttribute(android.R.attr.textColorPrimary, out, true)
            setTextColor(out.data)
            setPadding(dp2px(ctx, 20), dp2px(ctx, 16), dp2px(ctx, 20), dp2px(ctx, 6))
        }
        return tv
    }

    private fun buildCard(ctx: android.content.Context, title: String, desc: String, descSp: Float = 13.5f): View {
        val outer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp2px(ctx, 16), 0, dp2px(ctx, 16), dp2px(ctx, 10))
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
            setPadding(dp2px(ctx, 16), dp2px(ctx, 14), dp2px(ctx, 16), dp2px(ctx, 14))
        }
        if (title.isNotEmpty()) {
            val tvTitle = TextView(ctx).apply {
                this.text = title
                textSize = 14.5f
                setTypeface(null, android.graphics.Typeface.BOLD)
                val out = android.util.TypedValue()
                ctx.theme.resolveAttribute(android.R.attr.textColorPrimary, out, true)
                setTextColor(out.data)
            }
            inner.addView(tvTitle)
        }
        val tvDesc = TextView(ctx).apply {
            this.text = desc
            textSize = descSp
            setTextColor(ctx.resources.getColor(R.color.text_secondary))
            setPadding(0, if (title.isNotEmpty()) dp2px(ctx, 6) else 0, 0, 0)
            lineHeight = dp2px(ctx, 21)
        }
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
