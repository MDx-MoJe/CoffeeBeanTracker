package com.coffee.beantracker.bridge

import kotlinx.serialization.Serializable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 烘焙消耗幂等键：一个 roastId 只允许成功扣减一次（烤豆端一炉一个 UUID）
 */
@Entity(
    tableName = "roast_consumes",
    indices = [Index(value = ["roastId"], unique = true)]
)
@Serializable
data class RoastConsumeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roastId: String,
    val greenBeanId: Long,
    val createdAt: Long = System.currentTimeMillis()
)
