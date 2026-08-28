package com.coffee.beantracker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.coffee.beantracker.data.DeductRecord
import com.coffee.beantracker.utils.GramFormatter
import com.coffee.beantracker.databinding.ItemDeductRecordBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeductRecordAdapter(
    private val onLongClick: (DeductRecord) -> Unit
) : ListAdapter<DeductRecord, DeductRecordAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    inner class ViewHolder(val binding: ItemDeductRecordBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeductRecordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = getItem(position)
        val ctx = holder.binding.root.context
        holder.binding.tvBeanName.text = record.beanName
        holder.binding.tvTime.text = dateFormat.format(Date(record.createdAt))
        holder.binding.tvStockChange.text = ctx.getString(
            R.string.brew_record_format_before,
            record.stockBefore,
            record.stockAfter,
            record.gramsDeducted
        )
        holder.binding.tvBadge.text = "-" + GramFormatter.formatWithUnit(record.gramsDeducted)

        // 显示冲煮类型标签
        when (record.brewType) {
            "POUR_OVER" -> {
                holder.binding.tvBrewType.text = ctx.getString(R.string.brew_type_pour_over)
                holder.binding.tvBrewType.setBackgroundResource(R.drawable.badge_ready)
                holder.binding.tvBrewType.visibility = android.view.View.VISIBLE
            }
            "ESPRESSO" -> {
                holder.binding.tvBrewType.text = ctx.getString(R.string.brew_type_espresso)
                holder.binding.tvBrewType.setBackgroundResource(R.drawable.badge_expired)
                holder.binding.tvBrewType.visibility = android.view.View.VISIBLE
            }
            else -> {
                holder.binding.tvBrewType.visibility = android.view.View.GONE
            }
        }

        // 长按单条删除
        holder.binding.cardRoot.setOnLongClickListener {
            onLongClick(record)
            true
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DeductRecord>() {
            override fun areItemsTheSame(a: DeductRecord, b: DeductRecord) = a.id == b.id
            override fun areContentsTheSame(a: DeductRecord, b: DeductRecord) = a == b
        }
    }
}
