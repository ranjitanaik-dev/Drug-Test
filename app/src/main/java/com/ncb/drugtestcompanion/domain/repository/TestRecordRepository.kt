package com.ncb.drugtestcompanion.domain.repository

import com.ncb.drugtestcompanion.domain.model.TestRecord
import kotlinx.coroutines.flow.Flow

interface TestRecordRepository {
    suspend fun saveTestRecord(record: TestRecord)
    fun getAllTestRecords(): Flow<List<TestRecord>>
    suspend fun getTestRecordById(testId: String): TestRecord?
}
