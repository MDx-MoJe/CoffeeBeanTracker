package com.coffee.beantracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GreenBeanDao {

    @Query("SELECT * FROM green_beans ORDER BY purchaseDate DESC")
    fun getAllGreenBeans(): Flow<List<GreenBean>>

    /** 供 ContentProvider（内容提供器）同步调用：一次性查询，不订阅流 */
    @Query("SELECT * FROM green_beans ORDER BY purchaseDate DESC")
    suspend fun getAllGreenBeansOnce(): List<GreenBean>

    @Query("SELECT * FROM green_beans WHERE id = :id")
    suspend fun getById(id: Long): GreenBean?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bean: GreenBean): Long

    @Update
    suspend fun update(bean: GreenBean)

    @Delete
    suspend fun delete(bean: GreenBean)

    @Query("DELETE FROM green_beans WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE green_beans SET remainingGrams = :grams WHERE id = :id")
    suspend fun updateRemainingGrams(id: Long, grams: Double)
}
