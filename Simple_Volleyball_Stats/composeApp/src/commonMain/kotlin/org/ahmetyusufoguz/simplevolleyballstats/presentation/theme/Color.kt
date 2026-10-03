package org.ahmetyusufoguz.simplevolleyballstats.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui. graphics.Color

// Light Theme
val md_theme_light_primary = Color(0xFF00695C)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_secondary = Color(0xFF0288D1)
val md_theme_light_background = Color(0xFFFAFAFA)
val md_theme_light_surface = Color(0xFFFFFFFF)

// Dark Theme
val md_theme_dark_primary = Color(0xFF4DB6AC)
val md_theme_dark_onPrimary = Color(0xFF00251A)
val md_theme_dark_secondary = Color(0xFF4FC3F7)
val md_theme_dark_background = Color(0xFF121212)
val md_theme_dark_surface = Color(0xFF1E1E1E)

// Court Green Theme
fun courtGreenColorScheme() = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF8BC34A),
    background = Color(0xFFF1F8E9),
    surface = Color(0xFFFFFFFF)
)

// Ocean Blue Theme
fun oceanBlueColorScheme() = lightColorScheme(
    primary = Color(0xFF0277BD),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF00ACC1),
    background = Color(0xFFE1F5FE),
    surface = Color(0xFFFFFFFF)
)