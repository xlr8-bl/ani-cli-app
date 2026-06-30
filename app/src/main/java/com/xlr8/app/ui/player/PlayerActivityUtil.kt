package com.xlr8.app.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager

/** Walks the ContextWrapper chain to find the hosting Activity (needed for PiP, brightness). */
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** Adjusts media volume by [delta] steps and returns the new 0f..1f fraction. */
fun AudioManager.nudgeVolume(delta: Int): Float {
    val max = getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val current = getStreamVolume(AudioManager.STREAM_MUSIC)
    val next = (current + delta).coerceIn(0, max)
    setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
    return if (max == 0) 0f else next.toFloat() / max
}

/** Formats milliseconds as m:ss or h:mm:ss. */
fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
