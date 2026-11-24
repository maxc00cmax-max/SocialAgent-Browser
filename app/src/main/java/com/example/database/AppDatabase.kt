package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.database.dao.*
import com.example.database.entity.*

@Database(
    entities = [
        ScrapedPostMetadataEntity::class,
        BrowserTabEntity::class,
        BrowserDataEntity::class,
        TokenLogEntity::class,
        OrchestrationTaskEntity::class,
        ToolEntity::class,
        ResearchEntity::class,
        MediaEntity::class,
        PostDraftEntity::class,
        TabTaskLogEntity::class,
        RecentPageEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun browserTabDao(): BrowserTabDao
    abstract fun browserDataDao(): BrowserDataDao
    abstract fun tokenLogDao(): TokenLogDao
    abstract fun toolDao(): ToolDao
    abstract fun postMetadataDao(): PostMetadataDao
    fun scrapedPostMetadataDao(): ScrapedPostMetadataDao = postMetadataDao()
    abstract fun orchestrationTaskDao(): OrchestrationTaskDao
    abstract fun researchDao(): ResearchDao
    abstract fun mediaDao(): MediaDao
    abstract fun postDraftDao(): PostDraftDao
    abstract fun tabTaskLogDao(): TabTaskLogDao
    abstract fun recentPageDao(): RecentPageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "social_agent_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
