package com.xlr8.app.ui.easter

import com.xlr8.app.domain.model.Anime
import kotlin.random.Random

/**
 * Hidden community in-jokes. All opt-in to discover — none of this changes the app for
 * someone who doesn't know to look. (One Piece is a manga, so the ribbing is harmless.)
 */
object EasterEggs {

    /** The recurring mantra. Wording is verbatim and must stay that way. */
    const val BLEACH_MANTRA = "the bleach is peak and the peak is bleach"

    /** The Big Three, corrected: Bleach is canonically in; One Piece is conspicuously not. */
    val REAL_BIG_THREE = listOf("Naruto", "Bleach", "Dragon Ball")
    const val REAL_BIG_THREE_CAPTION = "The REAL Big Three ✊"

    /** Light, rotating jabs shown when a One Piece entry is opened. */
    val ONE_PIECE_ROASTS = listOf(
        "1000+ episodes and they're still on a boat 💀",
        "filler: the anime",
        "still no ending in sight",
        "two-hour episodes, four minutes of plot",
        "the real One Piece was the filler we made along the way",
        "wake me up when they reach the island",
    )

    /** Naruto's in the Big Three — no glazing though. Mid, but still up there. */
    val NARUTO_TAKES = listOf(
        "Naruto: mid, but still up there… still up there",
        "Naruto: 200 episodes of flashbacks, somehow still up there",
        "Naruto: peaked at Pain, mid otherwise, still up there",
        "talk no jutsu carried harder than the Big Three spot — still up there though",
    )

    val FRIEND_GROUP_CREDITS = listOf(
        "The XLR8 Crew",
        "Resident Bleach apologist",
        "The one who still defends One Piece (we forgive them)",
        "Dragon Ball purist",
        "Naruto-ran into the group chat",
    )

    fun isOnePiece(anime: Anime): Boolean = anime.matchesAnyTitle("one piece")

    fun isBleach(anime: Anime): Boolean = anime.matchesAnyTitle("bleach")

    fun isNaruto(anime: Anime): Boolean = anime.matchesAnyTitle("naruto")

    fun randomRoast(): String = ONE_PIECE_ROASTS.random()

    /** A roast string ~70% of the time, null otherwise — so it only shows "occasionally". */
    fun onePieceRoastOrNull(chance: Float = 0.7f): String? =
        if (Random.nextFloat() < chance) randomRoast() else null

    /** A Naruto "mid, but still up there" take, shown occasionally. */
    fun narutoTakeOrNull(chance: Float = 0.7f): String? =
        if (Random.nextFloat() < chance) NARUTO_TAKES.random() else null

    private fun Anime.matchesAnyTitle(needle: String): Boolean {
        val n = needle.lowercase()
        return listOfNotNull(title, englishTitle, nativeTitle)
            .any { it.lowercase().contains(n) }
    }
}
