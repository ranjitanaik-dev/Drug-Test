package com.ncb.drugtestcompanion.data.repository

import com.ncb.drugtestcompanion.data.local.dao.TestRecordDao
import com.ncb.drugtestcompanion.data.local.entity.TestRecordEntity
import com.ncb.drugtestcompanion.domain.model.TestRecord
import com.ncb.drugtestcompanion.domain.repository.TestRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestRecordRepositoryImpl @Inject constructor(
    private val testRecordDao: TestRecordDao
) : TestRecordRepository {

    override suspend fun saveTestRecord(record: TestRecord) {
        val entity = TestRecordEntity.fromDomain(record)
        testRecordDao.insertTestRecord(entity)
    }

    override fun getAllTestRecords(): Flow<List<TestRecord>> {
        return testRecordDao.getAllTestRecords().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTestRecordById(testId: String): TestRecord? {
        return testRecordDao.getTestRecordById(testId)?.toDomain()
    }
}
