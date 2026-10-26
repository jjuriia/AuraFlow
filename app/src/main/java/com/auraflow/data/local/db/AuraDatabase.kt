package com.auraflow.data.local.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SessionEntity::class, QuoteEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    abstract fun quoteDao(): QuoteDao

    companion object {
        fun create(context: Context): AuraDatabase =
            Room.databaseBuilder(context.applicationContext, AuraDatabase::class.java, "auraflow.db")
                .build()
    }
}
