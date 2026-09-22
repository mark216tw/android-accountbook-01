package com.example.simpleaccountbook.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.simpleaccountbook.data.ThemeSettings

data class ThemePreset(val id: String, val label: String, val hue: Float, val color: Color)

val ThemePresets = listOf(
    ThemePreset("PURPLE", "活力紫", 275f, Color(0xFF7856A8)),
    ThemePreset("CORAL", "珊瑚橘", 15f, Color(0xFFE05A47)),
    ThemePreset("SKY", "天空藍", 205f, Color(0xFF1976A8)),
    ThemePreset("MINT", "薄荷綠", 155f, Color(0xFF168568)),
    ThemePreset("LEMON", "檸檬黃", 48f, Color(0xFF9A7410)),
    ThemePreset("BERRY", "莓果粉", 330f, Color(0xFFB33F72)),
)

fun themeSeed(settings: ThemeSettings): Color =
    ThemePresets.firstOrNull { it.id == settings.themeId }?.color
        ?: Color.hsl(settings.customHue, 0.58f, 0.45f)

@Composable
fun SimpleAccountBookTheme(settings: ThemeSettings, content: @Composable (darkTheme: Boolean) -> Unit) {
    val dark = when (settings.displayMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }
    val seed = themeSeed(settings)
    val hue = settings.customHue
    val scheme = if (dark) {
        darkColorScheme(
            primary = seed.copy(alpha = 1f).lighten(0.28f),
            onPrimary = Color(0xFF1D1028),
            primaryContainer = Color.hsl(hue, 0.40f, 0.28f),
            onPrimaryContainer = Color.hsl(hue, 0.45f, 0.90f),
            secondary = Color.hsl((hue + 45f) % 360f, 0.45f, 0.72f),
            background = Color(0xFF151218),
            surface = Color(0xFF151218),
            surfaceVariant = Color(0xFF332E36),
        )
    } else {
        lightColorScheme(
            primary = seed,
            onPrimary = Color.White,
            primaryContainer = Color.hsl(hue, 0.55f, 0.90f),
            onPrimaryContainer = Color.hsl(hue, 0.55f, 0.20f),
            secondary = Color.hsl((hue + 45f) % 360f, 0.45f, 0.38f),
            background = Color(0xFFFFF9FD),
            surface = Color(0xFFFFF9FD),
            surfaceVariant = Color(0xFFF0EAF1),
        )
    }
    MaterialTheme(colorScheme = scheme) { content(dark) }
}

private fun Color.lighten(amount: Float): Color = Color(
    red = red + (1f - red) * amount,
    green = green + (1f - green) * amount,
    blue = blue + (1f - blue) * amount,
    alpha = alpha,
)
