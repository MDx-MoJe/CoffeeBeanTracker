package com.coffee.beantracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CoffeeBeanDao {
    @Query("SELECT * FROM coffee_beans ORDER BY roastDate DESC")
    fun getAllBeans(): Flow<List<CoffeeBean>>

    @Query("SELECT * FROM coffee_beans WHERE id = :id")
    suspend fun getById(id: Long): CoffeeBean?

    @Query("SELECT * FROM coffee_beans WHERE id = :id")
    suspend fun getBeanById(id: Long): CoffeeBean?

    /** 按名称精确查（豆袋互联：熟豆入库同名累加用） */
    @Query("SELECT * FROM coffee_beans WHERE name = :name LIMIT 1")
    suspend fun getByNameOnce(name: String): CoffeeBean?

    /** 只更新库存（豆袋互联：熟豆累加） */
    @Query("UPDATE coffee_beans SET stockGrams = :stockGrams WHERE id = :id")
    suspend fun updateStock(id: Long, stockGrams: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bean: CoffeeBean): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBean(bean: CoffeeBean): Long

    @Update
    suspend fun update(bean: CoffeeBean)

    @Update
    suspend fun updateBean(bean: CoffeeBean)

    @Delete
    suspend fun delete(bean: CoffeeBean)

    @Delete
    suspend fun deleteBean(bean: CoffeeBean)

    @Query("DELETE FROM coffee_beans WHERE id = :id")
    suspend fun deleteBeanById(id: Long)

    @Query("UPDATE coffee_beans SET stockGrams = :grams WHERE id = :id")
    suspend fun updateStockGrams(id: Long, grams: Double)
}