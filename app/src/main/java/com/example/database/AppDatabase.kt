package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        RecentPageEntity::class,
        CachedMetadataEntity::class
    ],
    version = 7,
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
    abstract fun cachedMetadataDao(): CachedMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Deduplicate recent_pages by url before applying unique index
                db.execSQL(
                    """
                    DELETE FROM recent_pages
                    WHERE rowid NOT IN (
                        SELECT MAX(rowid) FROM recent_pages GROUP BY url
                    )
                    """.trimIndent()
                )

                // ScrapedPostMetadataEntity indexes
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scraped_posts_metadata_platform ON scraped_posts_metadata (platform)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scraped_posts_metadata_scrapedTimestamp ON scraped_posts_metadata (scrapedTimestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scraped_posts_metadata_originalPostUrl ON scraped_posts_metadata (originalPostUrl)")

                // RecentPageEntity indexes
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_recent_pages_url ON recent_pages (url)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recent_pages_platform ON recent_pages (platform)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recent_pages_lastVisitedTimestamp ON recent_pages (lastVisitedTimestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recent_pages_isPinned ON recent_pages (isPinned)")

                // TabTaskLogEntity indexes
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tab_task_logs_tabId ON tab_task_logs (tabId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tab_task_logs_timestamp ON tab_task_logs (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tab_task_logs_status ON tab_task_logs (status)")

                // OrchestrationTaskEntity indexes
                db.execSQL("CREATE INDEX IF NOT EXISTS index_orchestration_tasks_status ON orchestration_tasks (status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_orchestration_tasks_createdAt ON orchestration_tasks (createdAt)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_scraped_metadata` (
                        `id` TEXT NOT NULL,
                        `requestedUrl` TEXT NOT NULL,
                        `resolvedUrl` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `ogImageUrl` TEXT,
                        `siteName` TEXT,
                        `ogType` TEXT,
                        `faviconUrl` TEXT,
                        `platform` TEXT NOT NULL,
                        `author` TEXT,
                        `hashtagsCsv` TEXT NOT NULL,
                        `extractedTagsCount` INTEGER NOT NULL,
                        `scrapedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cached_scraped_metadata_requestedUrl ON cached_scraped_metadata (requestedUrl)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cached_scraped_metadata_resolvedUrl ON cached_scraped_metadata (resolvedUrl)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cached_scraped_metadata_scrapedAt ON cached_scraped_metadata (scrapedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cached_scraped_metadata_platform ON cached_scraped_metadata (platform)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "social_agent_database"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
