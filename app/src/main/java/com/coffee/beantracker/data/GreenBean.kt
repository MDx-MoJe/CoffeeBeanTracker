package com.coffee.beantracker.data

import kotlinx.serialization.Serializable
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 生豆批次（生豆库存管理）
 *
 * 与熟豆 CoffeeBean 的关系：
 *   生豆 GreenBean ──烘焙一锅──▶ 熟豆 CoffeeBean（登记时扣减生豆剩余克重）
 * 当前阶段先做「生豆库存」的登记与查看，烘焙联动后续升级接入。
 */
@Entity(tableName = "green_beans")
@Serializable
data class GreenBean(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                  // 豆名/批次名
    val origin: String = "",           // 产地
    val processMethod: String = "",    // 处理法（水洗/日晒/蜜处理…）
    val variety: String = "",          // 豆种（如 埃塞耶加雪菲、哥伦比亚慧兰…）
    val altitude: String = "",         // 海拔
    val grade: String = "",            // 等级/品质（如 G1、AA、精品…）
    val harvestYear: String = "",      // 采收年份
    val purchaseDate: Long,            // 购买日期
    val purchaseGrams: Double = 0.0,     // 购买克重（支持一位小数）
    val remainingGrams: Double = 0.0,    // 剩余克重（烘焙时扣减）
    val notes: String = "",            // 备注
    val createdAt: Long = System.currentTimeMillis()
)
