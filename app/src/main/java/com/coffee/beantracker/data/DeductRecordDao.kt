package com.coffee.beantracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeductRecordDao {

    @Query("SELECT * FROM deduct_records ORDER BY createdAt DESC LIMIT 99")
    fun getAllLatest(): Flow<List<DeductRecord>>

    @Query("SELECT COUNT(*) FROM deduct_records")
    suspend fun getCount(): Int

    @Insert
    suspend fun insert(record: DeductRecord): Long

    @Delete
    suspend fun delete(record: DeductRecord)

    @Query("DELETE FROM deduct_records WHERE id IN (SELECT id FROM deduct_records ORDER BY createdAt ASC LIMIT :limit)")
    suspend fun deleteOldest(limit: Int)

    @Query("DELETE FROM deduct_records")
    suspend fun deleteAll()
}
