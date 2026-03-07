package com.ashutosh.flowtimer.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for Flow Time session history.
 *
 * Uses a singleton pattern (manual DI — no Hilt) so every consumer
 * shares the same connection.
 */
@Database(entities = [FlowSession::class], version = 1, exportSchema = false)
abstract class FlowTimerDatabase : RoomDatabase() {

    abstract fun flowSessionDao(): FlowSessionDao

    companion object {
        @Volatile
        private var INSTANCE: FlowTimerDatabase? = null

        /**
         * Returns the singleton database instance, creating it on first call.
         *
         * @param context Application context (avoids Activity leaks).
         */
        fun getInstance(context: Context): FlowTimerDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FlowTimerDatabase::class.java,
                    "flow_timer.db"
                ).build().also { INSTANCE = it }
            }
    }
}
