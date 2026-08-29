package com.coffee.beantracker

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.coffee.beantracker.data.CoffeeBeanDatabase
import com.coffee.beantracker.data.DeductRecord
import com.coffee.beantracker.databinding.ActivityDeductRecordsBinding
import com.coffee.beantracker.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.coffee.beantracker.utils.GramFormatter
import com.coffee.beantracker.utils.ToastCustom

class DeductRecordsActivity : BaseActivity() {

    private lateinit var binding: ActivityDeductRecordsBinding
    private lateinit var db: CoffeeBeanDatabase
    private lateinit var adapter: DeductRecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyOnCreate(this)
        super.onCreate(savedInstanceState)
        binding = ActivityDeductRecordsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = CoffeeBeanDatabase.getDatabase(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        applyToolbarColors()

        adapter = DeductRecordAdapter { record -> showDeleteConfirm(record) }
        binding.rvRecords.layoutManager = LinearLayoutManager(this)
        binding.rvRecords.adapter = adapter

        // 一键清除按钮
        binding.btnClearAll.setOnClickListener {
            showClearAllConfirm()
        }

        lifecycleScope.launch {
            db.deductRecordDao().getAllLatest().collectLatest { list ->
                if (list.isNullOrEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.rvRecords.visibility = View.GONE
                    binding.btnClearAll.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.rvRecords.visibility = View.VISIBLE
                    binding.btnClearAll.visibility = View.VISIBLE
                    adapter.submitList(list)
                }
            }
        }
    }

    private fun applyToolbarColors() {
        val colors = ThemeManager.getThemeColors(this)
        binding.appbar.setBackgroundColor(colors.colorPrimary)
        ThemeManager.applyToToolbar(binding.toolbar, colors)
        window.statusBarColor = colors.colorPrimaryDark
    }

    private fun showDeleteConfirm(record: DeductRecord) {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CoffeeBean_Dialog)
            .setTitle(R.string.brew_record_delete_title)
            .setMessage(R.string.brew_record_delete_msg)
            .setNegativeButton(R.string.no, null)
            .setPositiveButton(R.string.yes) { _, _ ->
                lifecycleScope.launch {
                    // 删除记录并返还克数到库存（同一事务，豆子不存在则仅删记录）
                    val refunded = withContext(Dispatchers.IO) {
                        db.withTransaction {
                            db.deductRecordDao().delete(record)
                            val bean = db.coffeeBeanDao().getById(record.beanId)
                            if (bean != null) {
                                db.coffeeBeanDao().updateStockGrams(bean.id, bean.stockGrams + record.gramsDeducted)
                                true
                            } else {
                                false
                            }
                        }
                    }
                    if (refunded) {
                        ToastCustom.show(this@DeductRecordsActivity, getString(R.string.brew_record_deleted_refund, GramFormatter.format(record.gramsDeducted)), android.widget.Toast.LENGTH_SHORT)
                    } else {
                        ToastCustom.show(this@DeductRecordsActivity, getString(R.string.brew_record_deleted), android.widget.Toast.LENGTH_SHORT)
                    }
                }
            }
            .show()
    }

    private fun showClearAllConfirm() {
        // 第一次确认
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CoffeeBean_Dialog)
            .setTitle(R.string.clear_all_records)
            .setMessage(R.string.clear_all_records_msg)
            .setNegativeButton(R.string.no, null)
            .setPositiveButton(R.string.continue_action) { _, _ ->
                // 第二次确认
                MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CoffeeBean_Dialog)
                    .setTitle(R.string.confirm_clear_again)
                    .setMessage(R.string.confirm_clear_again_msg)
                    .setNegativeButton(R.string.no, null)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) { db.deductRecordDao().deleteAll() }
                            ToastCustom.show(this@DeductRecordsActivity, getString(R.string.history_cleared), android.widget.Toast.LENGTH_SHORT)
                        }
                    }
                    .show()
            }
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
