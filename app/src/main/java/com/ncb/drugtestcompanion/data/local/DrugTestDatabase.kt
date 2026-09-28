package com.ncb.drugtestcompanion.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ncb.drugtestcompanion.data.local.dao.TestRecordDao
import com.ncb.drugtestcompanion.data.local.entity.TestRecordEntity

@Database(
    entities = [TestRecordEntity::class],
    version = 4,
    exportSchema = false
)
abstract class DrugTestDatabase : RoomDatabase() {
    abstract fun testRecordDao(): TestRecordDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE test_records ADD COLUMN signature TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE test_records ADD COLUMN signatureAlgorithm TEXT NOT NULL DEFAULT 'SHA256withRSA'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE test_records ADD COLUMN imagePath TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE test_records ADD COLUMN address TEXT DEFAULT NULL")
            }
        }
    }
}
