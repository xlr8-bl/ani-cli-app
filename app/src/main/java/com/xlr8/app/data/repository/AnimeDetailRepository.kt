package com.xlr8.app.data.repository

import com.xlr8.app.data.remote.anilist.AniListService
import com.xlr8.app.domain.model.AnimeDetail

/** Fetches full metadata for a single show. AllAnime source resolution is layered on later. */
class AnimeDetailRepository(private val anilist: AniListService) {

    suspend fun detail(anilistId: Int): AnimeDetail = anilist.mediaDetail(anilistId)
}
