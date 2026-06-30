package com.xlr8.app.data.remote.anilist

/**
 * GraphQL query strings for AniList (https://graphql.anilist.co).
 * Kept as plain constants so they're easy to tweak without a codegen step.
 */
object AniListQueries {

    /** Shared media field selection reused by the discovery queries. */
    private const val MEDIA_FIELDS = """
        id
        title { romaji english native }
        coverImage { extraLarge large color }
        bannerImage
        description(asHtml: false)
        genres
        averageScore
        status
        season
        seasonYear
        format
        episodes
        studios(isMain: true) { nodes { name isAnimationStudio } }
        nextAiringEpisode { airingAt episode }
    """

    /**
     * Generic media page sorted by a caller-provided sort, optionally constrained to a
     * season/year. Used for Trending, Popular This Season, Top 100, Recently Updated, etc.
     */
    val MEDIA_PAGE = """
        query MediaPage(${'$'}page: Int, ${'$'}perPage: Int, ${'$'}sort: [MediaSort], ${'$'}season: MediaSeason, ${'$'}seasonYear: Int, ${'$'}status: MediaStatus) {
          Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            media(type: ANIME, sort: ${'$'}sort, season: ${'$'}season, seasonYear: ${'$'}seasonYear, status: ${'$'}status, isAdult: false) {
              $MEDIA_FIELDS
            }
          }
        }
    """.trimIndent()

    /**
     * Episodes that have aired within a time window — powers "Just Aired" and NEW badges.
     * airingAt_greater / _lesser are epoch-second bounds.
     */
    val AIRING_SCHEDULE = """
        query AiringSchedule(${'$'}page: Int, ${'$'}perPage: Int, ${'$'}airingAtGreater: Int, ${'$'}airingAtLesser: Int) {
          Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            airingSchedules(airingAt_greater: ${'$'}airingAtGreater, airingAt_lesser: ${'$'}airingAtLesser, sort: TIME_DESC) {
              id
              airingAt
              episode
              media {
                $MEDIA_FIELDS
              }
            }
          }
        }
    """.trimIndent()

    /**
     * Full single-show detail: streaming episodes (real thumbnails + titles),
     * characters with their voice actors, and relations for the season selector
     * and Related row.
     */
    val MEDIA_DETAIL = """
        query MediaDetail(${'$'}id: Int) {
          Media(id: ${'$'}id, type: ANIME) {
            id
            type
            title { romaji english native }
            coverImage { extraLarge large color }
            bannerImage
            description(asHtml: false)
            genres
            averageScore
            status
            season
            seasonYear
            format
            episodes
            duration
            studios(isMain: true) { nodes { name isAnimationStudio } }
            nextAiringEpisode { airingAt episode }
            streamingEpisodes { title thumbnail url site }
            characters(sort: [ROLE, RELEVANCE], perPage: 16) {
              edges {
                role
                node { id name { full native } image { large medium } }
                voiceActors(sort: RELEVANCE) { id name { full } image { large } languageV2 }
              }
            }
            relations {
              edges {
                relationType(version: 2)
                node {
                  id
                  type
                  format
                  status
                  seasonYear
                  title { romaji english }
                  coverImage { extraLarge large }
                }
              }
            }
          }
        }
    """.trimIndent()

    /** Free-text search used by the Search screen. */
    val SEARCH = """
        query Search(${'$'}page: Int, ${'$'}perPage: Int, ${'$'}search: String, ${'$'}genre: String, ${'$'}seasonYear: Int, ${'$'}format: MediaFormat) {
          Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            media(type: ANIME, search: ${'$'}search, genre: ${'$'}genre, seasonYear: ${'$'}seasonYear, format: ${'$'}format, sort: SEARCH_MATCH, isAdult: false) {
              $MEDIA_FIELDS
            }
          }
        }
    """.trimIndent()
}
