package com.xlr8.app.util

private val HTML_TAG = Regex("<[^>]+>")

/**
 * AniList synopses contain light HTML (`<br>`, `<i>`, `<b>`, source notes). Strip tags
 * and decode the handful of entities that show up, leaving readable plain text.
 */
fun String.stripHtml(): String =
    replace("<br>", "\n", ignoreCase = true)
        .replace("<br/>", "\n", ignoreCase = true)
        .replace("<br />", "\n", ignoreCase = true)
        .replace(HTML_TAG, "")
        .replace("&mdash;", "—")
        .replace("&ndash;", "–")
        .replace("&hellip;", "…")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
