package com.coffee.beantracker.bridge

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RoastConsumeDao {

    @Query("SELECT COUNT(*) FROM roast_consumes WHERE roastId = :roastId")
    suspend fun countByRoast(roastId: String): Int

    suspend fun existsRoast(roastId: String): Boolean = countByRoast(roastId) > 0

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: RoastConsumeEntity): Long
}
