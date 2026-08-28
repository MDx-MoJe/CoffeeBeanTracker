package com.coffee.beantracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.coffee.beantracker.data.GreenBean
import com.coffee.beantracker.utils.GramFormatter
import com.coffee.beantracker.databinding.ItemGreenBeanBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 生豆列表适配器（生豆管理 Tab）
 */
class GreenBeanAdapter(
    private val onItemClick: (GreenBean) -> Unit
) : ListAdapter<GreenBean, GreenBeanAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<GreenBean>() {
            override fun areItemsTheSame(oldItem: GreenBean, newItem: GreenBean): Boolean =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: GreenBean, newItem: GreenBean): Boolean =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGreenBeanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemGreenBeanBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(bean: GreenBean) {
            binding.tvName.text = bean.name

            val subParts = listOfNotNull(
                if (bean.origin.isEmpty()) null else bean.origin,
                if (bean.variety.isEmpty()) null else bean.variety,
                if (bean.processMethod.isEmpty()) null else bean.processMethod
            )
            binding.tvSubInfo.text = if (subParts.isEmpty()) "-" else subParts.joinToString(" · ")

            val attrParts = listOfNotNull(
                if (bean.altitude.isEmpty()) null else bean.altitude,
                if (bean.grade.isEmpty()) null else bean.grade,
                if (bean.harvestYear.isEmpty()) null else bean.harvestYear
            )
            binding.tvBeanAttr.text = attrParts.joinToString(" · ")
            binding.tvBeanAttr.visibility = if (attrParts.isEmpty()) View.GONE else View.VISIBLE

            binding.tvPurchaseDate.text = dateFormat.format(Date(bean.purchaseDate))
            binding.tvPurchaseGrams.text = GramFormatter.formatWithUnit(bean.purchaseGrams)
            binding.tvRemainingBadge.text = GramFormatter.formatWithUnit(bean.remainingGrams)

            binding.root.setOnClickListener { onItemClick(bean) }
        }
    }
}
