package com.accessai.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** High-contrast colour schemes. Every text/background pair is well above WCAG AAA (7:1). */
public object AccessAIColors {
    public val HighContrastDark: ColorScheme = darkColorScheme(
        primary = Color(0xFFFFD60A),
        onPrimary = Color.Black,
        secondary = Color(0xFF7FDBFF),
        onSecondary = Color.Black,
        background = Color.Black,
        onBackground = Color.White,
        surface = Color(0xFF121212),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF1E1E1E),
        onSurfaceVariant = Color.White,
        error = Color(0xFFFF8A80),
        onError = Color.Black,
        outline = Color.White,
    )

    public val HighContrastLight: ColorScheme = lightColorScheme(
        primary = Color(0xFF002B80),
        onPrimary = Color.White,
        secondary = Color(0xFF004D40),
        onSecondary = Color.White,
        background = Color.White,
        onBackground = Color.Black,
        surface = Color.White,
        onSurface = Color.Black,
        surfaceVariant = Color(0xFFF0F0F0),
        onSurfaceVariant = Color.Black,
        error = Color(0xFF9B0000),
        onError = Color.White,
        outline = Color.Black,
    )
}

/** Larger type than Material defaults. Sizes are in sp, so the user's system font size still applies on top. */
private val AccessAITypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 20.sp, lineHeight = 30.sp),
    bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    labelLarge = TextStyle(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
)

@Composable
public fun AccessAITheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (dark) AccessAIColors.HighContrastDark else AccessAIColors.HighContrastLight,
        typography = AccessAITypography,
        content = content,
    )
}
