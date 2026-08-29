package com.coffee.beantracker

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.coffee.beantracker.data.CoffeeBeanDatabase
import com.coffee.beantracker.data.GreenBean
import com.coffee.beantracker.databinding.ActivityAddEditGreenBeanBinding
import com.coffee.beantracker.theme.ThemeManager
import com.coffee.beantracker.utils.GramFormatter
import com.coffee.beantracker.utils.ToastCustom
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddEditGreenBeanActivity : BaseActivity() {

    private lateinit var binding: ActivityAddEditGreenBeanBinding
    private var beanId: Long = 0L
    private var purchaseDate: Long = System.currentTimeMillis()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var db: CoffeeBeanDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyOnCreate(this)
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditGreenBeanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = CoffeeBeanDatabase.getDatabase(this)

        beanId = intent.getLongExtra(EXTRA_BEAN_ID, 0L)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        applyToolbarColors()

        updatePurchaseDateLabel()
        binding.tvPurchaseDate.setOnClickListener { showDatePicker() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
        binding.btnCancel.setOnClickListener { finish() }

        if (beanId > 0) {
            supportActionBar?.title = getString(R.string.edit_green_bean)
            binding.btnDelete.visibility = android.view.View.VISIBLE
            loadBean()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun applyToolbarColors() {
        val colors = ThemeManager.getThemeColors(this)
        binding.appbar.setBackgroundColor(colors.colorPrimary)
        ThemeManager.applyToToolbar(binding.toolbar, colors)
        window.statusBarColor = colors.colorPrimaryDark
    }

    private fun loadBean() {
        lifecycleScope.launch {
            val bean = withContext(Dispatchers.IO) { db.greenBeanDao().getById(beanId) }
            if (bean != null) {
                purchaseDate = bean.purchaseDate
                binding.etName.setText(bean.name)
                binding.etOrigin.setText(bean.origin)
                binding.etVariety.setText(bean.variety)
                binding.etProcessMethod.setText(bean.processMethod)
                binding.etAltitude.setText(bean.altitude)
                binding.etGrade.setText(bean.grade)
                binding.etHarvestYear.setText(bean.harvestYear)
                binding.etPurchaseGrams.setText(if (bean.purchaseGrams > 0) GramFormatter.format(bean.purchaseGrams) else "")
                binding.etRemainingGrams.setText(if (bean.remainingGrams > 0) GramFormatter.format(bean.remainingGrams) else "")
                binding.etNotes.setText(bean.notes)
                updatePurchaseDateLabel()
            }
        }
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.purchase_date))
            .setSelection(purchaseDate)
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            purchaseDate = millis
            updatePurchaseDateLabel()
        }
        picker.show(supportFragmentManager, "date")
    }

    private fun updatePurchaseDateLabel() {
        binding.tvPurchaseDate.text =
            getString(R.string.purchase_date) + ": " + dateFormat.format(Date(purchaseDate))
    }

    private fun save() {
        val name = binding.etName.text?.toString()?.trim() ?: ""
        if (name.isEmpty()) {
            ToastCustom.show(this, getString(R.string.green_bean_name_required), android.widget.Toast.LENGTH_SHORT)
            return
        }
        val purchaseGrams = GramFormatter.parseOr(binding.etPurchaseGrams.text?.toString(), 0.0)
        val remainingGrams = GramFormatter.parseOr(binding.etRemainingGrams.text?.toString(), 0.0)
        val bean = GreenBean(
            id = beanId,
            name = name,
            origin = binding.etOrigin.text?.toString()?.trim() ?: "",
            processMethod = binding.etProcessMethod.text?.toString()?.trim() ?: "",
            variety = binding.etVariety.text?.toString()?.trim() ?: "",
            altitude = binding.etAltitude.text?.toString()?.trim() ?: "",
            grade = binding.etGrade.text?.toString()?.trim() ?: "",
            harvestYear = binding.etHarvestYear.text?.toString()?.trim() ?: "",
            purchaseDate = purchaseDate,
            purchaseGrams = purchaseGrams,
            remainingGrams = remainingGrams,
            notes = binding.etNotes.text?.toString()?.trim() ?: ""
        )
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (beanId > 0) {
                    db.greenBeanDao().update(bean)
                } else {
                    db.greenBeanDao().insert(bean)
                }
            }
            finish()
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.confirm_delete))
            .setMessage(getString(R.string.confirm_delete_msg))
            .setNegativeButton(getString(R.string.no), null)
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) { db.greenBeanDao().deleteById(beanId) }
                    finish()
                }
            }
            .show()
    }

    companion object {
        const val EXTRA_BEAN_ID = "extra_bean_id"
    }
}
