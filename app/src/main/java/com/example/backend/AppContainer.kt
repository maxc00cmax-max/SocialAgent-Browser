package com.example.backend

import android.app.Application
import com.example.ai.GeminiProvider
import com.example.bridge.AppBridgeManager
import com.example.browser.AndroidBrowserManager
import com.example.database.AppDatabase
import com.example.database.repository.SocialAgentRepository
import com.example.scraper.MockSocialMediaScraper
import com.example.scraper.OpenGraphMetadataScraper
import com.example.scraper.ProductionSocialScraper
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Centralized Application-scoped Dependency Container.
 * Ensures ViewModels, ContentProviders, and WorkManager tasks share
 * a single connection-pooled OkHttpClient, Room Database, BrowserManager,
 * and AgentOrchestrator instance.
 */
class AppContainer(application: Application) {

    val sharedOkHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val database: AppDatabase = AppDatabase.getDatabase(application)
    val repository: SocialAgentRepository = SocialAgentRepository(database)
    val browserManager: AndroidBrowserManager = AndroidBrowserManager(application)
    val geminiProvider: GeminiProvider = GeminiProvider(sharedOkHttpClient)
    val openGraphScraper: OpenGraphMetadataScraper = OpenGraphMetadataScraper(repository, sharedOkHttpClient)
    val socialScraper: ProductionSocialScraper = ProductionSocialScraper(
        repository = repository,
        openGraphScraper = openGraphScraper,
        browserManager = browserManager
    )
    val compatibilityScraper: MockSocialMediaScraper = MockSocialMediaScraper(repository, openGraphScraper)
    val bridgeManager: AppBridgeManager = AppBridgeManager.getInstance(application)

    val orchestrator: AgentOrchestrator = AgentOrchestrator(
        repository = repository,
        browserManager = browserManager,
        geminiProvider = geminiProvider,
        openGraphScraper = openGraphScraper,
        socialScraper = socialScraper,
        bridgeManager = bridgeManager
    )
}
