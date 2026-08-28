package com.coffee.beantracker

import android.content.res.Configuration
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.coffee.beantracker.data.CoffeeBean
import com.coffee.beantracker.data.RoastLevel
import com.coffee.beantracker.databinding.ItemCoffeeBeanBinding
import com.coffee.beantracker.utils.ImageHelper
import com.coffee.beantracker.utils.FlavorPeriodHelper
import com.coffee.beantracker.utils.GramFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.EditText
import android.widget.LinearLayout
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.coffee.beantracker.utils.ToastCustom

data class StatusInfo(
    val statusColor: Int,
    val badgeColor: Int,
    val badgeText: String,
    val progressBg: Int = 0
)

class CoffeeBeanAdapter(
    private val onItemClick: (CoffeeBean) -> Unit,
    private val onDeductPourOver: (CoffeeBean) -> Unit,
    private val onDeductEspresso: (CoffeeBean) -> Unit,
    private val onModifyStock: (CoffeeBean, Double) -> Unit,
    private val onPrintClick: (CoffeeBean) -> Unit
) : ListAdapter<CoffeeBean, CoffeeBeanAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CoffeeBean>() {
            override fun areItemsTheSame(oldItem: CoffeeBean, newItem: CoffeeBean): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: CoffeeBean, newItem: CoffeeBean): Boolean = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCoffeeBeanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCoffeeBeanBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(bean: CoffeeBean) {
            val ctx = binding.root.context
            val now = System.currentTimeMillis()
            val millisInDay = TimeUnit.DAYS.toMillis(1)
            val daysSinceRoast = ((now - bean.roastDate) / millisInDay).toInt()
            val daysLeft = bean.restDays - daysSinceRoast
            val bestBeforeDays = bean.bestBeforeDays - daysSinceRoast

            val status = when {
                daysLeft > 0 -> StatusInfo(
                    R.color.status_not_ready,
                    R.drawable.crisp_badge_not_ready,
                    ctx.getString(R.string.resting_countdown, daysLeft)
                )
                bestBeforeDays >= 0 -> StatusInfo(
                    R.color.status_ready,
                    R.drawable.crisp_badge_ready,
                    ctx.getString(R.string.ready_until, bestBeforeDays)
                )
                else -> StatusInfo(
                    R.color.status_expired,
                    R.drawable.crisp_badge_expired,
                    ctx.getString(R.string.expired_countdown, -bestBeforeDays)
                )
            }

            binding.tvName.text = bean.name

            // 产地 · 烘焙度 · 处理法 拼接显示（缺项自动过滤，整行统一缩放）
            val subParts = listOfNotNull(
                if (bean.origin.isEmpty()) null else bean.origin,
                try {
                    RoastLevel.valueOf(bean.roastLevel).displayName
                } catch (e: Exception) { bean.roastLevel },
                if (bean.processMethod.isEmpty()) null else bean.processMethod
            )
            binding.tvSubInfo.text = if (subParts.isEmpty()) "-" else subParts.joinToString(" · ")

            binding.tvDevelopmentTime.text = if (bean.developmentTime.isEmpty()) "-" else bean.developmentTime
            binding.tvFlavorNotes.text = if (bean.flavorNotes.isEmpty()) ctx.getString(R.string.no_flavor_notes) else bean.flavorNotes
            binding.tvRoastDate.text = dateFormat.format(Date(bean.roastDate))

            val cal = Calendar.getInstance()
            cal.timeInMillis = bean.roastDate + bean.bestBeforeDays * millisInDay
            binding.tvBestBefore.text = dateFormat.format(cal.time)

binding.tvRestBadge.setBackgroundResource(status.badgeColor)
            binding.tvRestBadge.text = status.badgeText

            binding.tvRestBadge.setOnClickListener { v ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
                val rulesText = FlavorPeriodHelper.getFullRulesText()
                MaterialAlertDialogBuilder(ctx, R.style.ThemeOverlay_CoffeeBean_Dialog)
                    .setTitle("赏味期计算规则")
                    .setMessage(rulesText)
                    .setPositiveButton("知道了", null)
                    .show()
            }

            binding.btnPrint.setOnClickListener { v ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
                onPrintClick(bean)
            }

            val hasBgImage = ImageHelper.imageExists(bean.backgroundImagePath)
            val isDarkMode = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES

            if (hasBgImage) {
                binding.ivBackground.visibility = View.VISIBLE
                binding.ivBackground.alpha = 0.85f
                binding.ivBackground.load(File(bean.backgroundImagePath)) {
                    crossfade(true)
                    allowHardware(false)
                }
                binding.glassOverlay.setBackgroundResource(
                    if (isDarkMode) R.drawable.glass_overlay_dark else R.drawable.glass_overlay
                )
            } else {
                binding.ivBackground.visibility = View.GONE
                binding.ivBackground.setImageDrawable(null)
                val bgColor = if (isDarkMode) 0xFF252830.toInt() else 0xFFF4EFE9.toInt()
                binding.glassOverlay.setBackgroundColor(bgColor)
            }
            binding.glassOverlay.visibility = View.VISIBLE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hasBgImage) {
                binding.ivBackground.setRenderEffect(
                    RenderEffect.createBlurEffect(4f, 4f, Shader.TileMode.CLAMP)
                )
            }

            val pourOverGrams = if (bean.pourOverGrams > 0) bean.pourOverGrams else 15.0
            val espressoGrams = if (bean.espressoGrams > 0) bean.espressoGrams else 18.0
            val cupsPourOver = if (pourOverGrams > 0) (bean.stockGrams / pourOverGrams).toInt() else 0
            val cupsEspresso = if (espressoGrams > 0) (bean.stockGrams / espressoGrams).toInt() else 0

            if (bean.stockGrams > 0) {
                binding.tvStockGrams.text = GramFormatter.format(bean.stockGrams)
                binding.tvCupsPourOver.text = ctx.getString(R.string.pour_over_short) + " " +
                        cupsPourOver.toString() + ctx.getString(R.string.cups_unit)
                binding.tvCupsEspresso.text = ctx.getString(R.string.espresso_short) + " " +
                        cupsEspresso.toString() + ctx.getString(R.string.cups_unit)
                binding.llCupsInfo.visibility = View.VISIBLE

                val canPourOver = bean.stockGrams >= pourOverGrams
                binding.btnPourOver.isEnabled = canPourOver
                binding.btnPourOver.alpha = if (canPourOver) 1.0f else 0.4f

                val canEspresso = bean.stockGrams >= espressoGrams
                binding.btnEspresso.isEnabled = canEspresso
                binding.btnEspresso.alpha = if (canEspresso) 1.0f else 0.4f
            } else {
                binding.tvStockGrams.text = if (bean.stockGrams == 0.0) "0" else ctx.getString(R.string.stock_zero)
                binding.llCupsInfo.visibility = View.GONE
                binding.btnPourOver.isEnabled = false
                binding.btnPourOver.alpha = 0.4f
                binding.btnEspresso.isEnabled = false
                binding.btnEspresso.alpha = 0.4f
            }

            binding.tvStockGrams.setOnClickListener { v ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
                val input = EditText(ctx).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                            android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                    setText(GramFormatter.format(bean.stockGrams))
                    setSelection(text.length)
                    hint = ctx.getString(R.string.stock_grams_unit)
                    textSize = 18f
                }
                val container = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    val dp16 = (16 * ctx.resources.displayMetrics.density).toInt()
                    setPadding(dp16, dp16 / 2, dp16, 0)
                    addView(input, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ))
                }
                MaterialAlertDialogBuilder(ctx, R.style.ThemeOverlay_CoffeeBean_Dialog)
                    .setTitle("修改库存克数")
                    .setMessage("请输入新的库存（克）")
                    .setView(container)
                    .setNegativeButton(R.string.no, null)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        val raw = input.text?.toString()?.trim() ?: ""
                        val grams = GramFormatter.parse(raw)
                        if (grams != null && grams >= 0) {
                            onModifyStock(bean, grams)
                        } else {
                            ToastCustom.show(ctx, "请输入有效的克数（支持一位小数）", android.widget.Toast.LENGTH_SHORT)
                        }
                    }
                    .show()
            }

            binding.btnPourOver.setOnClickListener { v ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
                if (bean.stockGrams >= pourOverGrams) {
                    onDeductPourOver(bean)
                }
            }

            binding.btnEspresso.setOnClickListener { v ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
                if (bean.stockGrams >= espressoGrams) {
                    onDeductEspresso(bean)
                }
            }

            binding.root.setOnClickListener { onItemClick(bean) }
        }
    }
}
