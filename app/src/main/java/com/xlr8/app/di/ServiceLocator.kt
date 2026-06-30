package com.xlr8.app.di

import android.content.Context
import com.xlr8.app.data.local.XLR8Database
import com.xlr8.app.data.remote.allanime.AllAnimeService
import com.xlr8.app.data.remote.anilist.AniListService
import com.xlr8.app.data.remote.buildHttpClient
import com.xlr8.app.data.repository.AllAnimeRepository
import com.xlr8.app.data.repository.AnimeDetailRepository
import com.xlr8.app.data.repository.DiscoveryRepository
import com.xlr8.app.data.repository.PlaybackRepository
import io.ktor.client.HttpClient

/**
 * Minimal manual dependency container. The app is single-module and deliberately
 * avoids a DI framework at this stage; swap for Hilt if/when modules are split.
 *
 * Call [init] once from the Application before anything touches device-scoped state.
 */
object ServiceLocator {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** Application context for components that genuinely need one (e.g. ExoPlayer). */
    val applicationContext: Context get() = appContext

    val httpClient: HttpClient by lazy { buildHttpClient() }

    private val aniListService: AniListService by lazy { AniListService(httpClient) }
    private val allAnimeService: AllAnimeService by lazy { AllAnimeService(httpClient) }

    private val database: XLR8Database by lazy { XLR8Database.build(appContext) }

    val discoveryRepository: DiscoveryRepository by lazy { DiscoveryRepository(aniListService) }

    val animeDetailRepository: AnimeDetailRepository by lazy { AnimeDetailRepository(aniListService) }

    val allAnimeRepository: AllAnimeRepository by lazy {
        AllAnimeRepository(allAnimeService, database.sourceMappingDao())
    }

    val playbackRepository: PlaybackRepository by lazy {
        PlaybackRepository(database.watchProgressDao())
    }
}
