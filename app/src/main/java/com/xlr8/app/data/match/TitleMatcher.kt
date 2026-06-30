package com.xlr8.app.data.match

import com.xlr8.app.domain.model.AllAnimeShow
import com.xlr8.app.domain.model.Anime

/**
 * Heuristically matches an AniList show to an AllAnime search result. Titles diverge
 * between the two sources (romaji vs english, punctuation, season suffixes), so we
 * normalize and score by token overlap rather than requiring an exact string match.
 */
object TitleMatcher {

    /** Lowercase, drop punctuation, collapse whitespace. */
    fun normalize(text: String): String =
        text.lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    private fun tokens(text: String): Set<String> =
        normalize(text).split(' ').filter { it.isNotBlank() }.toSet()

    /** Jaccard similarity over word tokens, 0.0–1.0. */
    private fun similarity(a: String, b: String): Double {
        val ta = tokens(a)
        val tb = tokens(b)
        if (ta.isEmpty() || tb.isEmpty()) return 0.0
        val intersection = ta.intersect(tb).size.toDouble()
        val union = ta.union(tb).size.toDouble()
        return intersection / union
    }

    /**
     * Picks the best AllAnime candidate for [anime], comparing against romaji, english
     * and native titles. Returns null when nothing clears [threshold] so callers can fall
     * back to a manual link.
     */
    fun bestMatch(
        anime: Anime,
        candidates: List<AllAnimeShow>,
        threshold: Double = 0.34,
    ): AllAnimeShow? {
        if (candidates.isEmpty()) return null
        val targets = listOfNotNull(anime.title, anime.englishTitle, anime.nativeTitle)
            .map { normalize(it) }
            .filter { it.isNotBlank() }
        if (targets.isEmpty()) return null

        // Exact normalized match wins outright.
        candidates.firstOrNull { normalize(it.name) in targets }?.let { return it }

        var best: AllAnimeShow? = null
        var bestScore = 0.0
        for (candidate in candidates) {
            val score = targets.maxOf { similarity(it, candidate.name) }
            if (score > bestScore) {
                bestScore = score
                best = candidate
            }
        }
        return best?.takeIf { bestScore >= threshold }
    }
}
