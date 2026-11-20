package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = InkNavy,
    onPrimary = Color.White,
    primaryContainer = InkNavyContainer,
    onPrimaryContainer = OnInkNavyContainer,
    secondary = Terracotta,
    onSecondary = Color.White,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = OnTerracottaContainer,
    background = PaperIvory,
    onBackground = InkText,
    surface = PaperWhite,
    onSurface = InkText,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = PaperOutline,
    error = UnpaidRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkInkPrimary,
    onPrimary = DarkOnInkPrimary,
    primaryContainer = InkNavy,
    onPrimaryContainer = Color.White,
    secondary = DarkTerracotta,
    onSecondary = OnTerracottaContainer,
    secondaryContainer = Terracotta,
    onSecondaryContainer = Color.White,
    background = DarkPaperBg,
    onBackground = DarkText,
    surface = DarkPaperSurface,
    onSurface = DarkText,
    surfaceVariant = DarkPaperVariant,
    onSurfaceVariant = DarkMuted,
    outline = Color(0xFF454C56),
    error = Color(0xFFFF8A7A),
    onError = Color(0xFF4A0E07)
)

@Composable
fun NewspaperBillingTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
