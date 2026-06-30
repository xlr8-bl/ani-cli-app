package com.xlr8.app.data.remote.anilist

import com.xlr8.app.domain.model.AnimeDetail
import com.xlr8.app.domain.model.CharacterRole
import com.xlr8.app.domain.model.EpisodeMeta
import com.xlr8.app.domain.model.RelatedAnime

/** Strips AniList's "Episode 12 - " / "12. " prefixes from a streaming-episode title. */
private val EPISODE_PREFIX = Regex("""^\s*(?:episode\s*)?\d+\s*[-.:]?\s*""", RegexOption.IGNORE_CASE)

private fun cleanEpisodeTitle(raw: String?): String? =
    raw?.replace(EPISODE_PREFIX, "")?.trim()?.takeIf { it.isNotEmpty() }

/** Maps a fully-populated [MediaDto] into the Detail-page domain model. */
fun MediaDto.toDetail(): AnimeDetail {
    val base = toDomain()

    // The most recently aired episode number, when the show is still releasing.
    val lastAired = nextAiringEpisode?.episode?.let { it - 1 }?.takeIf { it >= 1 }

    val episodeList: List<EpisodeMeta> = when {
        streamingEpisodes.isNotEmpty() -> streamingEpisodes.mapIndexed { index, ep ->
            val number = index + 1
            EpisodeMeta(
                number = number,
                title = cleanEpisodeTitle(ep.title),
                thumbnailUrl = ep.thumbnail,
                isNew = lastAired != null && number == lastAired,
            )
        }
        (episodes ?: 0) > 0 -> (1..episodes!!).map { number ->
            EpisodeMeta(
                number = number,
                title = null,
                thumbnailUrl = null,
                isNew = lastAired != null && number == lastAired,
            )
        }
        else -> emptyList()
    }

    val characterRoles: List<CharacterRole> = characters?.edges.orEmpty().mapNotNull { edge ->
        val node = edge.node ?: return@mapNotNull null
        // Prefer the Japanese VA, falling back to whichever is listed first.
        val va = edge.voiceActors.firstOrNull { it.languageV2.equals("Japanese", ignoreCase = true) }
            ?: edge.voiceActors.firstOrNull()
        CharacterRole(
            characterId = node.id,
            characterName = node.name?.full ?: "Unknown",
            characterImageUrl = node.image?.large,
            role = edge.role,
            voiceActorName = va?.name?.full,
            voiceActorImageUrl = va?.image?.large,
            voiceActorLanguage = va?.languageV2,
        )
    }

    val relatedShows: List<RelatedAnime> = relations?.edges.orEmpty().mapNotNull { edge ->
        val node = edge.node ?: return@mapNotNull null
        // Season chains and the Related row are anime-only; skip manga/novel source rows.
        if (node.type != null && node.type != "ANIME") return@mapNotNull null
        RelatedAnime(
            anilistId = node.id,
            title = node.title?.romaji ?: node.title?.english ?: "Untitled",
            coverImageUrl = node.coverImage?.extraLarge ?: node.coverImage?.large,
            format = node.format,
            relationType = edge.relationType,
        )
    }

    return AnimeDetail(
        anime = base,
        durationMinutes = duration,
        episodes = episodeList,
        characters = characterRoles,
        relations = relatedShows,
    )
}
