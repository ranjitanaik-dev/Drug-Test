package com.ncb.drugtestcompanion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ncb.drugtestcompanion.data.local.entity.TestRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TestRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestRecord(record: TestRecordEntity)

    @Query("SELECT * FROM test_records ORDER BY timestamp DESC")
    fun getAllTestRecords(): Flow<List<TestRecordEntity>>

    @Query("SELECT * FROM test_records WHERE testId = :testId LIMIT 1")
    suspend fun getTestRecordById(testId: String): TestRecordEntity?
}
