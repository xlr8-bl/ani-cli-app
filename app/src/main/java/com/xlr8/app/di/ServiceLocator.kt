package com.xlr8.app.di

import com.xlr8.app.data.remote.anilist.AniListService
import com.xlr8.app.data.remote.buildHttpClient
import com.xlr8.app.data.repository.AnimeDetailRepository
import com.xlr8.app.data.repository.DiscoveryRepository
import io.ktor.client.HttpClient

/**
 * Minimal manual dependency container. The app is single-module and deliberately
 * avoids a DI framework at this stage; swap for Hilt if/when modules are split.
 */
object ServiceLocator {

    val httpClient: HttpClient by lazy { buildHttpClient() }

    private val aniListService: AniListService by lazy { AniListService(httpClient) }

    val discoveryRepository: DiscoveryRepository by lazy { DiscoveryRepository(aniListService) }

    val animeDetailRepository: AnimeDetailRepository by lazy { AnimeDetailRepository(aniListService) }
}
