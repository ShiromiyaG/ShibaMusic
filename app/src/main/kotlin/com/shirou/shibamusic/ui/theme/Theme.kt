package com.shirou.shibamusic.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Color palette
private val md_theme_dark_primary = Color(0xFF90CAF9)
private val md_theme_dark_onPrimary = Color(0xFF003258)
private val md_theme_dark_primaryContainer = Color(0xFF00497D)
private val md_theme_dark_onPrimaryContainer = Color(0xFFCEE5FF)
private val md_theme_dark_secondary = Color(0xFFBCC7DC)
private val md_theme_dark_onSecondary = Color(0xFF263141)
private val md_theme_dark_secondaryContainer = Color(0xFF3D4758)
private val md_theme_dark_onSecondaryContainer = Color(0xFFD8E3F8)
private val md_theme_dark_tertiary = Color(0xFFD7BEE4)
private val md_theme_dark_onTertiary = Color(0xFF3A2948)
private val md_theme_dark_tertiaryContainer = Color(0xFF523F5F)
private val md_theme_dark_onTertiaryContainer = Color(0xFFF3DAFF)
private val md_theme_dark_error = Color(0xFFFFB4AB)
private val md_theme_dark_errorContainer = Color(0xFF93000A)
private val md_theme_dark_onError = Color(0xFF690005)
private val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
private val md_theme_dark_background = Color(0xFF001F25)
private val md_theme_dark_onBackground = Color(0xFFA6EEFF)
private val md_theme_dark_surface = Color(0xFF001F25)
private val md_theme_dark_onSurface = Color(0xFFA6EEFF)
private val md_theme_dark_surfaceVariant = Color(0xFF41484D)
private val md_theme_dark_onSurfaceVariant = Color(0xFFC1C7CE)
private val md_theme_dark_outline = Color(0xFF8B9297)
private val md_theme_dark_inverseOnSurface = Color(0xFF001F25)
private val md_theme_dark_inverseSurface = Color(0xFFA6EEFF)
private val md_theme_dark_inversePrimary = Color(0xFF006399)
private val md_theme_dark_shadow = Color(0xFF000000)
private val md_theme_dark_surfaceTint = Color(0xFF90CAF9)
private val md_theme_dark_outlineVariant = Color(0xFF41484D)
private val md_theme_dark_scrim = Color(0xFF000000)

private val md_theme_light_primary = Color(0xFF006399)
private val md_theme_light_onPrimary = Color(0xFFFFFFFF)
private val md_theme_light_primaryContainer = Color(0xFFCEE5FF)
private val md_theme_light_onPrimaryContainer = Color(0xFF001D32)
private val md_theme_light_secondary = Color(0xFF545F70)
private val md_theme_light_onSecondary = Color(0xFFFFFFFF)
private val md_theme_light_secondaryContainer = Color(0xFFD8E3F8)
private val md_theme_light_onSecondaryContainer = Color(0xFF111C2B)
private val md_theme_light_tertiary = Color(0xFF6B5778)
private val md_theme_light_onTertiary = Color(0xFFFFFFFF)
private val md_theme_light_tertiaryContainer = Color(0xFFF3DAFF)
private val md_theme_light_onTertiaryContainer = Color(0xFF251431)
private val md_theme_light_error = Color(0xFFBA1A1A)
private val md_theme_light_errorContainer = Color(0xFFFFDAD6)
private val md_theme_light_onError = Color(0xFFFFFFFF)
private val md_theme_light_onErrorContainer = Color(0xFF410002)
private val md_theme_light_background = Color(0xFFFBFCFF)
private val md_theme_light_onBackground = Color(0xFF191C1E)
private val md_theme_light_surface = Color(0xFFFBFCFF)
private val md_theme_light_onSurface = Color(0xFF191C1E)
private val md_theme_light_surfaceVariant = Color(0xFFDFE2EB)
private val md_theme_light_onSurfaceVariant = Color(0xFF43474E)
private val md_theme_light_outline = Color(0xFF73777F)
private val md_theme_light_inverseOnSurface = Color(0xFFF0F1F3)
private val md_theme_light_inverseSurface = Color(0xFF2E3133)
private val md_theme_light_inversePrimary = Color(0xFF90CAF9)
private val md_theme_light_shadow = Color(0xFF000000)
private val md_theme_light_surfaceTint = Color(0xFF006399)
private val md_theme_light_outlineVariant = Color(0xFFC3C6CF)
private val md_theme_light_scrim = Color(0xFF000000)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

@Composable
fun ShibaMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themePref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        .getString(com.shirou.shibamusic.util.Preferences.THEME, com.shirou.shibamusic.helper.ThemeHelper.DEFAULT_MODE)
        ?: com.shirou.shibamusic.helper.ThemeHelper.DEFAULT_MODE
    
    val useDarkTheme = when (themePref) {
        com.shirou.shibamusic.helper.ThemeHelper.LIGHT_MODE -> false
        com.shirou.shibamusic.helper.ThemeHelper.DARK_MODE -> true
        else -> darkTheme
    }
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
