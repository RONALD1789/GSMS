package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GsmsLightColorScheme = lightColorScheme(
    primary = CobaltBlue,
    onPrimary = PureWhite,
    primaryContainer = CobaltLight,
    onPrimaryContainer = OxfordNavy,
    secondary = EmeraldTeal,
    onSecondary = PureWhite,
    secondaryContainer = EmeraldSoft,
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = VioletAi,
    onTertiary = PureWhite,
    tertiaryContainer = VioletSoft,
    onTertiaryContainer = Color(0xFF4C1D95),
    background = AlabasterBg,
    onBackground = InkPrimary,
    surface = PureWhite,
    onSurface = InkPrimary,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = SlateBorder,
    error = CrimsonRed,
    onError = PureWhite,
    errorContainer = CrimsonSoft,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val GsmsDarkColorScheme = darkColorScheme(
    primary = CobaltBright,
    onPrimary = OxfordNavyDark,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = CobaltLight,
    secondary = EmeraldBright,
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = EmeraldSoft,
    tertiary = Color(0xFFA78BFA),
    onTertiary = Color(0xFF2E1065),
    tertiaryContainer = Color(0xFF4C1D95),
    onTertiaryContainer = VioletSoft,
    background = MidnightBg,
    onBackground = SnowPrimary,
    surface = MidnightCard,
    onSurface = SnowPrimary,
    surfaceVariant = MidnightElevated,
    onSurfaceVariant = SnowSecondary,
    outline = MidnightBorder,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = CrimsonSoft
)

val GsmsShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GsmsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    brandAccentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) GsmsDarkColorScheme else GsmsLightColorScheme
    val colorScheme = if (brandAccentColor != null && !darkTheme) {
        baseScheme.copy(primary = brandAccentColor)
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GsmsTypography,
        shapes = GsmsShapes,
        content = content
    )
}
