package com.xlr8.app.data.remote.allanime

/**
 * De-obfuscates AllAnime provider source URLs.
 *
 * Internal provider links arrive hex-encoded and prefixed with "--". ani-cli decodes
 * them by reading the string two hex digits at a time and XOR-ing each byte with 56
 * (0x38) to recover an ASCII path such as `/apivtwo/clock?id=...`. We mirror that exactly.
 *
 * Non-obfuscated entries (already plain `http(s)://...`) are returned unchanged.
 */
object AllAnimeDecoder {

    private const val XOR_KEY = 56

    fun decodeSourceUrl(raw: String): String? {
        if (!raw.startsWith("--")) {
            // Already a usable URL (some providers return direct links).
            return raw.takeIf { it.startsWith("http") }
        }
        val hex = raw.removePrefix("--")
        if (hex.isEmpty() || hex.length % 2 != 0) return null

        val out = StringBuilder(hex.length / 2)
        var i = 0
        while (i < hex.length) {
            val byte = hex.substring(i, i + 2).toIntOrNull(16) ?: return null
            out.append((byte xor XOR_KEY).toChar())
            i += 2
        }
        return out.toString()
    }

    /**
     * Turns a decoded clock path into the JSON endpoint ani-cli fetches, e.g.
     * `/apivtwo/clock?id=X` → `https://allanime.day/apivtwo/clock.json?id=X`.
     * Returns null if [decodedPath] isn't an internal clock provider link.
     */
    fun toClockJsonUrl(decodedPath: String): String? {
        if (!decodedPath.contains("/clock")) return null
        val jsonPath = decodedPath.replaceFirst("/clock", "/clock.json")
        return AllAnimeApi.CLOCK_BASE + jsonPath
    }
}
