package com.coffee.beantracker.data

import kotlinx.serialization.Serializable
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 「做一杯」扣减操作记录，上限 99 条
 */
@Entity(tableName = "deduct_records")
@Serializable
data class DeductRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val beanId: Long,           // 关联的咖啡豆ID
    val beanName: String,       // 冗余咖啡豆名（删除原豆后也能显示）
    val gramsDeducted: Double,   // 本次扣减克数
    val stockBefore: Double,     // 扣减前库存
    val stockAfter: Double,      // 扣减后库存
    val brewType: String = "",   // 冲煮类型: POUR_OVER / ESPRESSO
    val createdAt: Long = System.currentTimeMillis()
)
