package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.QCEmailLog

@Database(entities = [QCEmailLog::class], version = 1, exportSchema = false)
abstract class QCDatabase : RoomDatabase() {
    abstract fun qcEmailLogDao(): QCEmailLogDao

    companion object {
        @Volatile
        private var INSTANCE: QCDatabase? = null

        fun getDatabase(context: Context): QCDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QCDatabase::class.java,
                    "qc_workflow_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
