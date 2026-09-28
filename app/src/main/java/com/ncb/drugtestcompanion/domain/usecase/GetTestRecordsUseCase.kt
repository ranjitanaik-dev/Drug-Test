package com.ncb.drugtestcompanion.domain.usecase

import com.ncb.drugtestcompanion.domain.model.TestRecord
import com.ncb.drugtestcompanion.domain.repository.TestRecordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTestRecordsUseCase @Inject constructor(
    private val testRecordRepository: TestRecordRepository
) {
    operator fun invoke(): Flow<List<TestRecord>> {
        return testRecordRepository.getAllTestRecords()
    }
}
