package org.ahmetyusufoguz.simplevolleyballstats.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime. Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppTheme {
    LIGHT,
    DARK,
    COURT_GREEN,
    OCEAN_BLUE
}

val LocalAppTheme = staticCompositionLocalOf { AppTheme.LIGHT }

@Composable
fun SimpleVolleyballStatsTheme(
    appTheme: AppTheme = AppTheme.LIGHT,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme. LIGHT -> lightColorScheme(
            primary = md_theme_light_primary,
            onPrimary = md_theme_light_onPrimary,
            secondary = md_theme_light_secondary,
            background = md_theme_light_background,
            surface = md_theme_light_surface
        )

        AppTheme.DARK -> darkColorScheme(
            primary = md_theme_dark_primary,
            onPrimary = md_theme_dark_onPrimary,
            secondary = md_theme_dark_secondary,
            background = md_theme_dark_background,
            surface = md_theme_dark_surface
        )

        AppTheme.COURT_GREEN -> courtGreenColorScheme()
        AppTheme.OCEAN_BLUE -> oceanBlueColorScheme()
    }

    CompositionLocalProvider(LocalAppTheme provides appTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content
        )
    }
}