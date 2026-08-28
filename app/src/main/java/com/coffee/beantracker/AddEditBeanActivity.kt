package com.coffee.beantracker

import android.app.Activity
import com.google.android.material.datepicker.MaterialDatePicker
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.coffee.beantracker.data.CoffeeBean
import com.coffee.beantracker.data.CoffeeBeanDatabase
import com.coffee.beantracker.utils.GramFormatter
import com.coffee.beantracker.data.RoastLevel
import com.coffee.beantracker.databinding.ActivityAddEditBinding
import com.coffee.beantracker.theme.ThemeManager
import com.coffee.beantracker.utils.ImageHelper
import com.coffee.beantracker.utils.FlavorPeriodHelper
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AddEditBeanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBinding
    private var beanId: Long = 0L
    private var roastDate: Long = System.currentTimeMillis()
    private var currentRoastLevel: RoastLevel = RoastLevel.MEDIUM
    private var backgroundImagePath: String = ""
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var db: CoffeeBeanDatabase

    private val processMethodOptions = listOf(
        "水洗", "日晒", "厌氧日晒", "密处理", "湿刨法", "黑蜜", "红蜜",
        "半水洗", "酵素水洗", "半日晒", "双重厌氧", "自定义"
    )
    private val originOptions = listOf(
        "中国", "埃塞俄比亚", "哥伦比亚", "哥斯达黎加", "巴拿马", "印度尼西亚",
        "巴西", "萨尔瓦多", "洪都拉斯", "危地马拉", "肯尼亚", "秘鲁", "墨西哥",
        "巴布亚新几内亚", "越南", "自定义"
    )
    private val flavorNoteOptions = listOf(
        "柑橘","柠檬","青柠","西柚","橙子","蜜橘",
        "莓果","草莓","树莓","蓝莓","黑醋栗","红醋栗","樱桃",
        "桃子","水蜜桃","油桃","李子","杏",
        "苹果","青苹果","红苹果","梨",
        "葡萄","红葡萄","白葡萄",
        "芒果","菠萝","百香果","番石榴","荔枝","龙眼","哈密瓜","香蕉","无花果","石榴",
        "茉莉","玫瑰","橙花","洋甘菊","栀子花","紫罗兰",
        "花蜜","蜂蜜","蜜饯","焦糖","枫糖","红糖","黄糖","麦芽糖",
        "香草","奶油","太妃","黄油",
        "牛奶巧克力","黑巧克力","可可","可可壳",
        "烤榛子","杏仁","烤杏仁","核桃","夏威夷果","花生",
        "麦芽","谷物","吐司","烤面包","饼干","曲奇","燕麦",
        "红茶","绿茶","乌龙茶","花草茶",
        "柑橘皮","杏干","葡萄干","果脯",
        "烟熏","香料","肉桂","坚果酱",
        "果汁感","丝滑","醇厚","清爽",
        "自定义"
    )

    private val CUSTOM_PROCESS_TAG = "__CUSTOM_PROCESS__"
    private val CUSTOM_ORIGIN_TAG = "__CUSTOM_ORIGIN__"
    private val CUSTOM_FLAVOR_TAG = "__CUSTOM_FLAVOR__"

    // 自定义已添加的标签集合（避免重复）
    private val customProcessChips = hashSetOf<String>()
    private val customOriginChips = hashSetOf<String>()
    private val customFlavorChips = hashSetOf<String>()

    // 编辑模式下的已保存自定义值
    private var savedProcessMethod: String = ""
    private var savedOrigin: String = ""
    private var savedFlavorNotes: String = ""

    private val pickBackgroundLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = ImageHelper.saveImage(this, it, true)
            if (path != null) {
                backgroundImagePath = path
                updateBackgroundDisplay()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyOnCreate(this)
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = CoffeeBeanDatabase.getDatabase(this)

        beanId = intent.getLongExtra(EXTRA_BEAN_ID, 0L)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        applyToolbarColors()

        // 先初始化所有 Chip 组
        setupRoastLevelChips()
        setupProcessMethodChips()
        setupOriginChips()
        setupFlavorNoteChips()
        setupDevelopmentTimeSlider()

        setupListeners()

        if (beanId > 0L) {
            loadBeanForEdit()
            supportActionBar?.setTitle(R.string.edit_bean)
            binding.btnDelete.visibility = View.VISIBLE
        } else {
            supportActionBar?.setTitle(R.string.add_bean)
            updateRoastDateDisplay()
            // 默认中烘（自动填推荐养豆/赏味天数）
            selectRoastLevelChip(RoastLevel.MEDIUM)
            updateRoastLevelDisplay(autoFill = true)
            binding.etPourOverGrams.setText(getString(R.string.default_pour_over_value))
            binding.etEspressoGrams.setText(getString(R.string.default_espresso_value))
        }
        updateBackgroundDisplay()
    }

    private fun applyToolbarColors() {
        val colors = ThemeManager.getThemeColors(this)
        binding.appbar.setBackgroundColor(colors.colorPrimary)
        ThemeManager.applyToToolbar(binding.toolbar, colors)
        window.statusBarColor = colors.colorPrimaryDark
    }

    // ===== Chip Groups Setup =====

    private fun setupDevelopmentTimeSlider() {
        // 发展时间：滑块 20~180 秒，拖动时同步更新右侧数值
        binding.slDevelopmentTime.addOnChangeListener { _, value, _ ->
            binding.tvDevTimeValue.text = getString(R.string.dev_time_seconds, value.toInt())
        }
    }

    private fun setupRoastLevelChips() {
        val cg = binding.cgRoastLevel
        cg.removeAllViews()
        RoastLevel.values().forEach { level ->
            val chip = createChip(level.displayName, singleSelect = true)
            chip.setOnClickListener {
                currentRoastLevel = level
                updateRoastLevelDisplay(autoFill = true)
            }
            cg.addView(chip)
        }
    }

    private fun selectRoastLevelChip(target: RoastLevel) {
        val cg = binding.cgRoastLevel
        for (i in 0 until cg.childCount) {
            val chip = cg.getChildAt(i) as Chip
            if (chip.text == target.displayName) {
                chip.isChecked = true
                break
            }
        }
    }

    private fun setupProcessMethodChips() {
        val cg = binding.cgProcessMethod
        cg.removeAllViews()
        cg.isSingleSelection = false
        processMethodOptions.forEach { option ->
            val chip = createChip(option, singleSelect = false)
            if (option == "自定义") {
                chip.setOnClickListener {
                    if (chip.isChecked) {
                        binding.tilProcessCustom.visibility = View.VISIBLE
                        binding.etProcessCustom.requestFocus()
                        chip.isChecked = false
                    }
                }
            }
            cg.addView(chip)
        }
        // 自定义输入框按回车确认
        binding.etProcessCustom.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                confirmCustomProcess()
                true
            } else false
        }
    }

    private fun confirmCustomProcess() {
        val text = binding.etProcessCustom.text?.toString()?.trim() ?: ""
        if (text.isEmpty()) return
        val cg = binding.cgProcessMethod
        // 如果这个自定义值还没加入选项，则添加
        if (text !in processMethodOptions && text !in customProcessChips) {
            val chip = createChip(text, singleSelect = false)
            // 插到"自定义"chip之前
            val insertIdx = cg.childCount - 1
            cg.addView(chip, insertIdx)
            customProcessChips.add(text)
        }
        // 找到并选中
        for (i in 0 until cg.childCount) {
            val ch = cg.getChildAt(i) as Chip
            if (ch.text == text) {
                ch.isChecked = true
                break
            }
        }
        binding.tilProcessCustom.visibility = View.GONE
        binding.etProcessCustom.text = null
    }

    private fun setupOriginChips() {
        val cg = binding.cgOrigin
        cg.removeAllViews()
        cg.isSingleSelection = false
        originOptions.forEach { option ->
            val chip = createChip(option, singleSelect = false)
            if (option == "自定义") {
                chip.setOnClickListener {
                    if (chip.isChecked) {
                        binding.tilOriginCustom.visibility = View.VISIBLE
                        binding.etOriginCustom.requestFocus()
                        chip.isChecked = false
                    }
                }
            }
            cg.addView(chip)
        }
        binding.etOriginCustom.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                confirmCustomOrigin()
                true
            } else false
        }
    }

    private fun confirmCustomOrigin() {
        val text = binding.etOriginCustom.text?.toString()?.trim() ?: ""
        if (text.isEmpty()) return
        val cg = binding.cgOrigin
        if (text !in originOptions && text !in customOriginChips) {
            val chip = createChip(text, singleSelect = false)
            val insertIdx = cg.childCount - 1
            cg.addView(chip, insertIdx)
            customOriginChips.add(text)
        }
        for (i in 0 until cg.childCount) {
            val ch = cg.getChildAt(i) as Chip
            if (ch.text == text) {
                ch.isChecked = true
                break
            }
        }
        binding.tilOriginCustom.visibility = View.GONE
        binding.etOriginCustom.text = null
    }

    private fun setupFlavorNoteChips() {
        val cg = binding.cgFlavorNotes
        cg.removeAllViews()
        flavorNoteOptions.forEach { option ->
            val chip = createChip(option, singleSelect = false)
            if (option == "自定义") {
                chip.setOnClickListener {
                    if (chip.isChecked) {
                        binding.tilFlavorCustom.visibility = View.VISIBLE
                        binding.etFlavorCustom.requestFocus()
                        // 保持"自定义"chip可反复触发输入框
                        chip.isChecked = false
                    }
                }
            }
            cg.addView(chip)
        }

        binding.etFlavorCustom.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                confirmCustomFlavor()
                true
            } else false
        }
    }

    private fun confirmCustomFlavor() {
        val text = binding.etFlavorCustom.text?.toString()?.trim() ?: ""
        if (text.isEmpty()) return
        val cg = binding.cgFlavorNotes
        if (text !in flavorNoteOptions && text !in customFlavorChips) {
            val chip = createChip(text, singleSelect = false)
            val insertIdx = cg.childCount - 1
            cg.addView(chip, insertIdx)
            customFlavorChips.add(text)
        }
        // 选中
        for (i in 0 until cg.childCount) {
            val ch = cg.getChildAt(i) as Chip
            if (ch.text == text) {
                ch.isChecked = true
                break
            }
        }
        binding.etFlavorCustom.text = null
    }

    /**
     * 创建一个 Chip 实例
     * singleSelect=true → 类似单选（带圆点指示器），使用 filter 样式
     * singleSelect=false → 多选，使用 filter 样式但可多勾选
     */
    private fun createChip(text: String, singleSelect: Boolean): Chip {
        // 使用带主题的 Context 构造 Chip，样式继承 MaterialTheme 中的 chipStyle
        val chip = com.google.android.material.chip.Chip(
            androidx.appcompat.view.ContextThemeWrapper(
                this,
                com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter
            )
        )
        chip.text = text
        chip.isClickable = true
        chip.isCheckable = true
        chip.textSize = 14f
        chip.chipMinHeight = resources.getDimensionPixelSize(R.dimen.chip_min_height).toFloat()
        chip.setTextColor(android.graphics.Color.parseColor("#2B1D14"))
        chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor("#F5EEE6")
        )
        chip.chipStrokeColor = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor("#D8C9B8")
        )
        chip.chipStrokeWidth = 1f
        return chip
    }

    // ===== 读取选中值 =====

    private fun getSelectedProcessMethod(): String {
        val cg = binding.cgProcessMethod
        val checked = cg.checkedChipIds.mapNotNull { id ->
            val chip = cg.findViewById<Chip>(id)
            val t = chip.text?.toString()
            if (t == null || t == "自定义") null else t
        }.toMutableList()
        val draft = binding.etProcessCustom.text?.toString()?.trim() ?: ""
        if (draft.isNotEmpty() && draft !in checked) checked.add(draft)
        return checked.joinToString("，")
    }
    private fun getSelectedOrigin(): String {
        val cg = binding.cgOrigin
        val checked = cg.checkedChipIds.mapNotNull { id ->
            val chip = cg.findViewById<Chip>(id)
            val t = chip.text?.toString()
            if (t == null || t == "自定义") null else t
        }.toMutableList()
        val draft = binding.etOriginCustom.text?.toString()?.trim() ?: ""
        if (draft.isNotEmpty() && draft !in checked) checked.add(draft)
        return checked.joinToString("，")
    }
    private fun getSelectedFlavorNotes(): String {
        val cg = binding.cgFlavorNotes
        val checked = cg.checkedChipIds.mapNotNull { id ->
            val chip = cg.findViewById<Chip>(id)
            val t = chip.text?.toString()
            if (t == null || t == "自定义") null else t
        }.toMutableList()
        // 加上额外备注
        val extra = binding.etFlavorNotesExtra.text?.toString()?.trim() ?: ""
        if (extra.isNotEmpty()) {
            checked.add(extra)
        }
        return checked.joinToString("，")
    }

    // ===== 编辑模式：回填选中 =====

    private fun restoreProcessChip(value: String) {
        if (value.isEmpty()) return
        val cg = binding.cgProcessMethod
        val tokens = value.split("，", ",", "、", ";", ";", " ", "\n", "\t")
            .map { it.trim() }.filter { it.isNotEmpty() }
        for (t in tokens) {
            var found = false
            for (i in 0 until cg.childCount) {
                val ch = cg.getChildAt(i) as Chip
                if (ch.text == t) {
                    ch.isChecked = true
                    found = true
                    break
                }
            }
            if (!found) {
                val chip = createChip(t, singleSelect = false)
                val insertIdx = cg.childCount - 1
                cg.addView(chip, insertIdx)
                customProcessChips.add(t)
                chip.isChecked = true
            }
        }
    }
    private fun restoreOriginChip(value: String) {
        if (value.isEmpty()) return
        val cg = binding.cgOrigin
        val tokens = value.split("，", ",", "、", ";", ";", " ", "\n", "\t")
            .map { it.trim() }.filter { it.isNotEmpty() }
        for (t in tokens) {
            var found = false
            for (i in 0 until cg.childCount) {
                val ch = cg.getChildAt(i) as Chip
                if (ch.text == t) {
                    ch.isChecked = true
                    found = true
                    break
                }
            }
            if (!found) {
                val chip = createChip(t, singleSelect = false)
                val insertIdx = cg.childCount - 1
                cg.addView(chip, insertIdx)
                customOriginChips.add(t)
                chip.isChecked = true
            }
        }
    }
    private fun restoreFlavorChips(value: String) {
        if (value.isEmpty()) return
        // 按常见分隔符拆分
        val tokens = value.split("，", ",", "、", ";", ";", " ", "\n", "\t")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        // 前N个作为chip勾选，剩下的（如果有很长的文本）放到额外备注
        val cg = binding.cgFlavorNotes
        val chipCandidate = mutableListOf<String>()
        val extraNotes = mutableListOf<String>()
        for (t in tokens) {
            // 过长的文本放入备注，不要作为chip
            if (t.length > 8) extraNotes.add(t) else chipCandidate.add(t)
        }
        for (t in chipCandidate) {
            var found = false
            for (i in 0 until cg.childCount) {
                val ch = cg.getChildAt(i) as Chip
                if (ch.text == t) {
                    ch.isChecked = true
                    found = true
                    break
                }
            }
            if (!found) {
                val chip = createChip(t, singleSelect = false)
                val insertIdx = cg.childCount - 1
                cg.addView(chip, insertIdx)
                customFlavorChips.add(t)
                chip.isChecked = true
            }
        }
        if (extraNotes.isNotEmpty()) {
            binding.etFlavorNotesExtra.setText(extraNotes.joinToString("，"))
        }
    }

    // ===== Listeners =====

    private fun setupListeners() {
        binding.cvRoastDate.setOnClickListener { showDatePicker() }
        binding.btnSave.setOnClickListener { saveBean() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnDelete.setOnClickListener { deleteBean() }

        // 图片选择（仅背景图）
        binding.cvBackgroundImage.setOnClickListener { pickBackgroundLauncher.launch("image/*") }
        binding.btnRemoveBg.setOnClickListener {
            if (backgroundImagePath.isNotEmpty()) {
                ImageHelper.deleteImage(backgroundImagePath)
                backgroundImagePath = ""
                updateBackgroundDisplay()
            }
        }
    }

    private fun updateBackgroundDisplay() {
        if (backgroundImagePath.isNotEmpty() && ImageHelper.imageExists(backgroundImagePath)) {
            binding.ivBackgroundImage.load(File(backgroundImagePath)) { crossfade(true) }
            binding.llBgPlaceholder.visibility = View.GONE
            binding.btnRemoveBg.visibility = View.VISIBLE
        } else {
            binding.ivBackgroundImage.setImageResource(android.R.color.transparent)
            binding.llBgPlaceholder.visibility = View.VISIBLE
            binding.btnRemoveBg.visibility = View.GONE
        }
    }

    private fun showDatePicker() {
        // MaterialDatePicker 内部使用 UTC 时间，需要先把本地日期转成 UTC 表示
        val local = Calendar.getInstance()
        local.timeInMillis = roastDate
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.please_select_date)
            .setSelection(utc.timeInMillis)
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            val sel = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selection }
            val localCal = Calendar.getInstance().apply {
                clear()
                set(sel.get(Calendar.YEAR), sel.get(Calendar.MONTH), sel.get(Calendar.DAY_OF_MONTH))
            }
            roastDate = localCal.timeInMillis
            updateRoastDateDisplay()
        }
        picker.show(supportFragmentManager, "roast_date_picker")
    }

    private fun updateRoastDateDisplay() {
        binding.tvRoastDate.text = dateFormat.format(Date(roastDate))
    }

    private fun updateRoastLevelDisplay(autoFill: Boolean = false) {
        // 根据烘焙深度显示建议养豆/赏味期天数
        val hint = FlavorPeriodHelper.getSuggestionText(currentRoastLevel)
        binding.tvFlavorHint.text = hint
        binding.tvFlavorHint.visibility = View.VISIBLE
        // 选择烘焙度时自动填入推荐天数（仍可手动修改）
        if (autoFill) {
            binding.etRestDays.setText(FlavorPeriodHelper.getDefaultRestDays(currentRoastLevel).toString())
            binding.etBestBefore.setText(FlavorPeriodHelper.getDefaultBestBefore(currentRoastLevel).toString())
        }
    }

    private fun loadBeanForEdit() {
        lifecycleScope.launch {
            val bean = withContext(Dispatchers.IO) { db.coffeeBeanDao().getById(beanId) }
            bean?.let {
                beanId = it.id
                binding.etBeanName.setText(it.name)
                roastDate = it.roastDate
                updateRoastDateDisplay()
                binding.etRestDays.setText(it.restDays.toString())
                binding.etBestBefore.setText(it.bestBeforeDays.toString())
                currentRoastLevel = try { RoastLevel.valueOf(it.roastLevel) } catch (e: Exception) { RoastLevel.MEDIUM }
                selectRoastLevelChip(currentRoastLevel)
                updateRoastLevelDisplay()

                savedProcessMethod = it.processMethod
                restoreProcessChip(savedProcessMethod)

                savedOrigin = it.origin
                restoreOriginChip(savedOrigin)

                savedFlavorNotes = it.flavorNotes
                restoreFlavorChips(savedFlavorNotes)

                val devTimeInt = it.developmentTime.toIntOrNull()
                binding.slDevelopmentTime.value = if (devTimeInt != null && devTimeInt in 10..180) devTimeInt.toFloat() else 70f
                binding.tvDevTimeValue.text = getString(R.string.dev_time_seconds, binding.slDevelopmentTime.value.toInt())
                backgroundImagePath = it.backgroundImagePath
                binding.etStockGrams.setText(if (it.stockGrams > 0) GramFormatter.format(it.stockGrams) else "")
                binding.etPourOverGrams.setText(if (it.pourOverGrams > 0) GramFormatter.format(it.pourOverGrams) else getString(R.string.default_pour_over_value))
                binding.etEspressoGrams.setText(if (it.espressoGrams > 0) GramFormatter.format(it.espressoGrams) else getString(R.string.default_espresso_value))
                updateBackgroundDisplay()
            }
        }
    }

    private fun saveBean() {
        val name = binding.etBeanName.text?.toString()?.trim() ?: ""
        if (name.isEmpty()) {
            binding.tilBeanName.error = getString(R.string.error_name_required)
            binding.etBeanName.requestFocus()
            return
        }
        val restDaysText = binding.etRestDays.text?.toString() ?: ""
        val bestBeforeText = binding.etBestBefore.text?.toString() ?: ""
        val restDays = restDaysText.toIntOrNull() ?: 0
        val bestBefore = bestBeforeText.toIntOrNull() ?: 0

        val processMethod = getSelectedProcessMethod()
        val origin = getSelectedOrigin()
        val flavor = getSelectedFlavorNotes()
        val devTime = binding.slDevelopmentTime.value.toInt().toString()

        val stockGrams = GramFormatter.parseOr(binding.etStockGrams.text?.toString(), 0.0)
        val pourOverGrams = GramFormatter.parseOr(binding.etPourOverGrams.text?.toString(), 15.0)
        val safePourOverGrams = if (pourOverGrams <= 0) 15.0 else pourOverGrams
        val espressoGrams = GramFormatter.parseOr(binding.etEspressoGrams.text?.toString(), 18.0)
        val safeEspressoGrams = if (espressoGrams <= 0) 18.0 else espressoGrams

        val bean = CoffeeBean(
            id = beanId,
            name = name,
            roastDate = roastDate,
            restDays = restDays,
            bestBeforeDays = bestBefore,
            processMethod = processMethod,
            roastLevel = currentRoastLevel.name,
            origin = origin,
            flavorNotes = flavor,
            developmentTime = devTime,
            imagePath = "",
            backgroundImagePath = backgroundImagePath,
            stockGrams = stockGrams,
            deductGrams = safePourOverGrams,
            pourOverGrams = safePourOverGrams,
            espressoGrams = safeEspressoGrams
        )

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (beanId > 0L) db.coffeeBeanDao().update(bean)
                else {
                    val newId = db.coffeeBeanDao().insert(bean)
                    bean.copy(id = newId)
                }
            }
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun deleteBean() {
        if (beanId <= 0L) return
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CoffeeBean_Dialog)
            .setTitle(R.string.confirm_delete)
            .setMessage(R.string.confirm_delete_msg)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.yes) { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val b = db.coffeeBeanDao().getById(beanId)
                        if (b != null) {
                            ImageHelper.deleteImage(b.backgroundImagePath)
                            db.coffeeBeanDao().delete(b)
                        }
                    }
                    setResult(RESULT_OK, Intent().apply { putExtra("deletedId", beanId) })
                    finish()
                }
            }
            .show()
    }

    override fun onBackPressed() {
        val focused = currentFocus
        if (focused is android.widget.EditText ||
            focused is android.widget.AutoCompleteTextView) {
            focused.clearFocus()
            return
        }
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val EXTRA_BEAN_ID = "extra_bean_id"
    }
}
