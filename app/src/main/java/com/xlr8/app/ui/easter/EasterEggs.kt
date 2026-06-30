package com.xlr8.app.ui.easter

import com.xlr8.app.domain.model.Anime
import kotlin.random.Random

/**
 * Hidden community in-jokes. All opt-in to discover — none of this changes the app for
 * someone who doesn't know to look. (One Piece is a manga, so the ribbing is harmless.)
 */
object EasterEggs {

    /** The recurring mantra (verbatim wording), glazed with the bleach emojis. */
    const val BLEACH_MANTRA = "the bleach is peak and the peak is bleach 🤧🔥"

    /** the bleach glaze pool (the mantra lives here too). all of it, peak. */
    val BLEACH_GLAZE = listOf(
        BLEACH_MANTRA,
        "ichigo's drip is undefeated and so is the soundtrack 🤧🔥",
        "every bleach opening goes harder than your favorite song 🤧🔥",
        "bankai reveal then chills for the rest of the week 🤧🔥",
        "the animation comeback was a flex on the entire industry 🤧🔥",
        "no amount of filler can dim that bankai glow 🤧🔥",
        "thousand year blood war said hold my reiatsu 🤧🔥",
        "kubo cooked and never let it simmer 🤧🔥",
    )

    /** a sprinkle of ronaldo glaze. siuuu. */
    val RONALDO_GLAZE = listOf(
        "siuuuu 🐐🤧🔥",
        "the goat never rests and neither does a bankai 🐐🔥",
        "cristiano would watch bleach and go siuuu 🐐🤧🔥",
        "900 goals, zero filler, pure peak 🐐🔥",
        "5 ballon d'ors stacked like ichigo's bankai forms 🐐🤧🔥",
    )

    /** The big three, corrected: bleach is canonically in; one piece is conspicuously not. */
    val REAL_BIG_THREE = listOf("naruto", "bleach", "dragon ball")
    const val REAL_BIG_THREE_CAPTION = "da real BIG three (yes bro)"

    /** Light, rotating jabs shown when a one piece entry is opened. */
    val ONE_PIECE_ROASTS = listOf(
        "a baby born when it aired can legally drink now and they're still at sea 😭💔🙏",
        "every island is 'the most important arc yet' for the 47th time ✌️🫩",
        "gear 5 was peak, the 900 episodes before it were a warmup 💔🥀",
        "spent more time at one tea party than most shows run in total 😭💔🙏",
        "1100 episodes deep and the treasure is still a rumor ✌️🫩",
        "they scream the attack name longer than the fight actually lasts 💔🥀",
        "the pacing is a glacier and the glacier is winning 😭💔🙏",
        "you could learn fluent japanese in the time one arc takes ✌️🫩",
        "two hours of episode, four minutes of plot, eleven flashbacks 💔🥀",
        "still 'almost at the next island' since before you had a phone 😭💔🙏",
    )

    /** naruto's in the big three, no glazing though. mid, but still up there. */
    val NARUTO_TAKES = listOf(
        "naruto: mid, but still up there… still up there ✌️🫩",
        "naruto: 200 episodes of flashbacks, somehow still up there 💔🥀",
        "naruto: peaked at pain, mid otherwise, still up there 😭💔🙏",
        "talk no jutsu carried harder than the big three spot, still up there though ✌️🫩",
    )

    val FRIEND_GROUP_CREDITS = listOf(
        "the xlr8 crew",
        "resident bleach apologist 🤧🔥",
        "the one who still defends one piece (we forgive them) 💔🥀",
        "dragon ball purist",
        "naruto ran into the group chat ✌️🫩",
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

    /** Always glaze bleach on open, with a ~15% sprinkle of ronaldo glaze. */
    fun bleachOpenLine(): String =
        if (Random.nextFloat() < 0.15f) RONALDO_GLAZE.random() else BLEACH_GLAZE.random()

    private fun Anime.matchesAnyTitle(needle: String): Boolean {
        val n = needle.lowercase()
        return listOfNotNull(title, englishTitle, nativeTitle)
            .any { it.lowercase().contains(n) }
    }
}
