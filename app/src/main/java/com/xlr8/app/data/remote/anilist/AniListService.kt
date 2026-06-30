package com.xlr8.app.data.remote.anilist

import com.xlr8.app.data.remote.XLR8Json
import com.xlr8.app.domain.model.Anime
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Thin AniList GraphQL client. Posts query + variables to https://graphql.anilist.co
 * and maps the response into domain [Anime] models.
 */
class AniListService(private val client: HttpClient) {

    private suspend fun rawPost(query: String, variables: JsonObject): String {
        val response = client.post(ENDPOINT) {
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Accept, "application/json")
            }
            setBody(GraphQLRequest(query = query, variables = variables))
        }
        return response.bodyAsText()
    }

    private suspend fun executePage(query: String, variables: JsonObject): Page {
        val raw = rawPost(query, variables)
        val parsed = XLR8Json.decodeFromString(
            GraphQLResponse.serializer(PageData.serializer()),
            raw,
        )
        parsed.errors?.firstOrNull()?.let { error ->
            throw AniListException(error.message)
        }
        return parsed.data?.page ?: throw AniListException("Empty AniList response")
    }

    suspend fun mediaPage(
        sort: List<String>,
        page: Int = 1,
        perPage: Int = 20,
        season: String? = null,
        seasonYear: Int? = null,
        status: String? = null,
    ): List<Anime> {
        val variables = buildJsonObject {
            put("page", JsonPrimitive(page))
            put("perPage", JsonPrimitive(perPage))
            put("sort", kotlinx.serialization.json.JsonArray(sort.map { JsonPrimitive(it) }))
            season?.let { put("season", JsonPrimitive(it)) }
            seasonYear?.let { put("seasonYear", JsonPrimitive(it)) }
            status?.let { put("status", JsonPrimitive(it)) }
        }
        return executePage(AniListQueries.MEDIA_PAGE, variables).media.map { it.toDomain() }
    }

    /** Episodes that aired in [airingAtGreater, airingAtLesser] (epoch seconds). */
    suspend fun airingSchedule(
        airingAtGreater: Long,
        airingAtLesser: Long,
        page: Int = 1,
        perPage: Int = 30,
    ): List<AiringScheduleDto> {
        val variables = buildJsonObject {
            put("page", JsonPrimitive(page))
            put("perPage", JsonPrimitive(perPage))
            put("airingAtGreater", JsonPrimitive(airingAtGreater))
            put("airingAtLesser", JsonPrimitive(airingAtLesser))
        }
        return executePage(AniListQueries.AIRING_SCHEDULE, variables).airingSchedules
    }

    /** Full detail for a single show by AniList id. */
    suspend fun mediaDetail(id: Int): com.xlr8.app.domain.model.AnimeDetail {
        val variables = buildJsonObject { put("id", JsonPrimitive(id)) }
        val raw = rawPost(AniListQueries.MEDIA_DETAIL, variables)
        val parsed = XLR8Json.decodeFromString(
            GraphQLResponse.serializer(MediaData.serializer()),
            raw,
        )
        parsed.errors?.firstOrNull()?.let { throw AniListException(it.message) }
        val media = parsed.data?.media ?: throw AniListException("Show not found on AniList")
        return media.toDetail()
    }

    suspend fun search(
        query: String,
        page: Int = 1,
        perPage: Int = 25,
        genre: String? = null,
        seasonYear: Int? = null,
        format: String? = null,
    ): List<Anime> {
        val variables = buildJsonObject {
            put("page", JsonPrimitive(page))
            put("perPage", JsonPrimitive(perPage))
            if (query.isNotBlank()) put("search", JsonPrimitive(query))
            genre?.let { put("genre", JsonPrimitive(it)) }
            seasonYear?.let { put("seasonYear", JsonPrimitive(it)) }
            format?.let { put("format", JsonPrimitive(it)) }
        }
        return executePage(AniListQueries.SEARCH, variables).media.map { it.toDomain() }
    }

    companion object {
        const val ENDPOINT = "https://graphql.anilist.co"
    }
}

class AniListException(message: String) : Exception(message)
