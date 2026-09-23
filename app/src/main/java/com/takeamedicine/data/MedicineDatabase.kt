package com.takeamedicine.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MedicineEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MedicineDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao

    companion object {
        @Volatile
        private var INSTANCE: MedicineDatabase? = null

        fun getInstance(context: Context): MedicineDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedicineDatabase::class.java,
                    "medicine.db"
                ).addMigrations(MIGRATION_1_2)
                .build().also { INSTANCE = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE medicines ADD COLUMN unit TEXT NOT NULL DEFAULT '片'"
                )
            }
        }
    }
}
