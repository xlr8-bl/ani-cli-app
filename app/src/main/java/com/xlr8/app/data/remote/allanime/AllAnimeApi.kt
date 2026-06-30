package com.xlr8.app.data.remote.allanime

/**
 * AllAnime endpoints, headers and GraphQL queries — ported directly from ani-cli
 * (https://github.com/pystardust/ani-cli, GPL-3.0). The host, Referer and
 * User-Agent must match what ani-cli sends or the API rejects the request.
 *
 * If AllAnime moves hosts or changes the schema, these are the values to update.
 */
object AllAnimeApi {

    const val BASE_HOST = "allanime.day"
    const val API_ENDPOINT = "https://api.$BASE_HOST/api"
    const val REFERER = "https://allanime.to"
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    /** Clock provider links (the de-obfuscated internal sources) live on the base host. */
    const val CLOCK_BASE = "https://$BASE_HOST"

    // NOTE: "Vaild..." typos below are intentional — that's how AllAnime's schema spells them.

    val SEARCH = """
        query(${'$'}search: SearchInput, ${'$'}limit: Int, ${'$'}page: Int, ${'$'}translationType: VaildTranslationTypeEnumType, ${'$'}countryOrigin: VaildCountryOriginEnumType) {
          shows(search: ${'$'}search, limit: ${'$'}limit, page: ${'$'}page, translationType: ${'$'}translationType, countryOrigin: ${'$'}countryOrigin) {
            edges {
              _id
              name
              availableEpisodes
              __typename
            }
          }
        }
    """.trimIndent()

    val EPISODES_DETAIL = """
        query(${'$'}showId: String!) {
          show(_id: ${'$'}showId) {
            _id
            availableEpisodesDetail
          }
        }
    """.trimIndent()

    val EPISODE_SOURCES = """
        query(${'$'}showId: String!, ${'$'}translationType: VaildTranslationTypeEnumType!, ${'$'}episodeString: String!) {
          episode(showId: ${'$'}showId, translationType: ${'$'}translationType, episodeString: ${'$'}episodeString) {
            episodeString
            sourceUrls
          }
        }
    """.trimIndent()
}
