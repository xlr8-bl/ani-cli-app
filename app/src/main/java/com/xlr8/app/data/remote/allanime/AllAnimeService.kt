package com.xlr8.app.data.remote.allanime

import com.xlr8.app.data.remote.XLR8Json
import com.xlr8.app.domain.model.AllAnimeShow
import com.xlr8.app.domain.model.TranslationType
import com.xlr8.app.domain.model.VideoSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Talks to AllAnime's GraphQL API to turn a show + episode into playable [VideoSource]s.
 * Mirrors ani-cli's request shape: GET with `variables` + `query` params and the
 * AllAnime Referer/User-Agent headers.
 */
class AllAnimeService(private val client: HttpClient) {

    private suspend fun <T> query(
        gql: String,
        variables: JsonObject,
        serializer: KSerializer<T>,
    ): T {
        val response = client.get(AllAnimeApi.API_ENDPOINT) {
            url {
                parameters.append(
                    "variables",
                    XLR8Json.encodeToString(JsonObject.serializer(), variables),
                )
                parameters.append("query", gql)
            }
            applyAllAnimeHeaders()
        }
        val raw = response.bodyAsText()
        val parsed = XLR8Json.decodeFromString(AllAnimeResponse.serializer(serializer), raw)
        return parsed.data ?: throw AllAnimeException("Empty AllAnime response")
    }

    /** Searches AllAnime for a show by title. */
    suspend fun search(
        title: String,
        translation: TranslationType = TranslationType.SUB,
    ): List<AllAnimeShow> {
        val variables = buildJsonObject {
            putJsonObject("search") {
                put("allowAdult", false)
                put("allowUnknown", false)
                put("query", title)
            }
            put("limit", 40)
            put("page", 1)
            put("translationType", translation.apiValue)
            put("countryOrigin", "ALL")
        }
        return query(AllAnimeApi.SEARCH, variables, ShowsData.serializer())
            .shows.edges
            .map {
                AllAnimeShow(
                    id = it.id,
                    name = it.name,
                    subEpisodes = it.availableEpisodes?.sub ?: 0,
                    dubEpisodes = it.availableEpisodes?.dub ?: 0,
                )
            }
    }

    /** Returns the available episode numbers (as strings) for a show and translation. */
    suspend fun episodeNumbers(showId: String, translation: TranslationType): List<String> {
        val variables = buildJsonObject { put("showId", showId) }
        val detail = query(AllAnimeApi.EPISODES_DETAIL, variables, ShowDetailData.serializer())
            .show?.availableEpisodesDetail ?: return emptyList()
        val list = when (translation) {
            TranslationType.SUB -> detail.sub
            TranslationType.DUB -> detail.dub
        }
        // AllAnime returns these unsorted and as strings; sort numerically where possible.
        return list.sortedBy { it.toDoubleOrNull() ?: Double.MAX_VALUE }
    }

    /**
     * Resolves every playable [VideoSource] for an episode across providers, including
     * the de-obfuscated internal "clock" providers. Higher-priority providers first.
     */
    suspend fun episodeSources(
        showId: String,
        translation: TranslationType,
        episode: String,
    ): List<VideoSource> {
        val variables = buildJsonObject {
            put("showId", showId)
            put("translationType", translation.apiValue)
            put("episodeString", episode)
        }
        val sourceUrls = query(AllAnimeApi.EPISODE_SOURCES, variables, EpisodeData.serializer())
            .episode?.sourceUrls.orEmpty()
            .sortedByDescending { it.priority }

        val headers = mapOf(
            "Referer" to AllAnimeApi.REFERER,
            "User-Agent" to AllAnimeApi.USER_AGENT,
        )
        val resolved = mutableListOf<VideoSource>()
        for (source in sourceUrls) {
            val decoded = AllAnimeDecoder.decodeSourceUrl(source.sourceUrl) ?: continue
            val clockUrl = AllAnimeDecoder.toClockJsonUrl(decoded)
            if (clockUrl != null) {
                resolved += resolveClock(clockUrl, source.sourceName, headers)
            } else if (decoded.startsWith("http")) {
                resolved += VideoSource(
                    url = decoded,
                    quality = "auto",
                    isHls = decoded.contains(".m3u8"),
                    providerName = source.sourceName,
                    headers = headers,
                )
            }
        }
        return resolved
    }

    /** Fetches an internal provider's `clock.json` and maps its links to [VideoSource]s. */
    private suspend fun resolveClock(
        clockUrl: String,
        providerName: String?,
        headers: Map<String, String>,
    ): List<VideoSource> = try {
        val response = client.get(clockUrl) { applyAllAnimeHeaders() }
        val raw = response.bodyAsText()
        val clock = XLR8Json.decodeFromString(ClockResponse.serializer(), raw)
        clock.links
            .filter { it.link.isNotBlank() }
            .map { link ->
                VideoSource(
                    url = link.link,
                    quality = link.resolutionStr ?: "auto",
                    isHls = link.hls == true || link.link.contains(".m3u8"),
                    providerName = providerName,
                    headers = headers,
                )
            }
    } catch (_: Throwable) {
        // A single dead provider shouldn't fail the whole episode; skip it.
        emptyList()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyAllAnimeHeaders() {
        header("Referer", AllAnimeApi.REFERER)
        header("User-Agent", AllAnimeApi.USER_AGENT)
        header("Accept", "application/json")
    }
}

class AllAnimeException(message: String) : Exception(message)
