package com.ncb.drugtestcompanion.domain.usecase

import com.ncb.drugtestcompanion.domain.model.TestRecord
import com.ncb.drugtestcompanion.domain.repository.TestRecordRepository
import javax.inject.Inject

class GetTestRecordUseCase @Inject constructor(
    private val testRecordRepository: TestRecordRepository
) {
    suspend operator fun invoke(testId: String): TestRecord? {
        return testRecordRepository.getTestRecordById(testId)
    }
}
