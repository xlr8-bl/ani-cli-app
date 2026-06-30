package com.xlr8.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** User-selectable theme preference. The device is the account, so this lives on-device. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = LightError,
    onError = LightOnError,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = DarkError,
    onError = DarkOnError,
)

private val AmoledColors = DarkColors.copy(
    background = AmoledBackground,
    surface = AmoledSurface,
    surfaceVariant = AmoledSurfaceVariant,
    surfaceContainer = AmoledBackground,
    surfaceContainerLow = AmoledBackground,
    surfaceContainerLowest = AmoledBackground,
)

@Composable
fun XLR8Theme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    amoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (dark && amoled) {
                scheme.copy(
                    background = AmoledBackground,
                    surface = AmoledSurface,
                    surfaceContainer = AmoledBackground,
                    surfaceContainerLowest = AmoledBackground,
                )
            } else scheme
        }
        dark && amoled -> AmoledColors
        dark -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val lightStatusIcons = colorScheme.background.luminance() > 0.5f
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightStatusIcons
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = lightStatusIcons
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = XLR8Typography,
        content = content,
    )
}

/** Convenience for previews and brand chips. */
val brandAccent: Color get() = BrandBlue
