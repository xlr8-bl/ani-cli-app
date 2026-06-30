package com.xlr8.app.domain.model

/** Full metadata for a single show, used by the Detail page. */
data class AnimeDetail(
    val anime: Anime,
    val durationMinutes: Int?,
    val episodes: List<EpisodeMeta>,
    val characters: List<CharacterRole>,
    val relations: List<RelatedAnime>,
) {
    /** PREQUEL/SEQUEL relations that form the show's season chain (anime only). */
    val seasons: List<RelatedAnime>
        get() = relations.filter {
            it.relationType == "PREQUEL" || it.relationType == "SEQUEL" || it.relationType == "PARENT"
        }

    /** Everything else worth showing in a "Related" row. */
    val otherRelations: List<RelatedAnime>
        get() = relations.filterNot { it in seasons }
}

/** A single episode entry for the grid. */
data class EpisodeMeta(
    val number: Int,
    val title: String?,
    val thumbnailUrl: String?,
    val isNew: Boolean,
)

/** A character paired with its (preferred-language) voice actor. */
data class CharacterRole(
    val characterId: Int,
    val characterName: String,
    val characterImageUrl: String?,
    val role: String?,
    val voiceActorName: String?,
    val voiceActorImageUrl: String?,
    val voiceActorLanguage: String?,
)

/** A related show, used for the season selector and the Related row. */
data class RelatedAnime(
    val anilistId: Int,
    val title: String,
    val coverImageUrl: String?,
    val format: String?,
    val relationType: String?,
)
