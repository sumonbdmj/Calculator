package com.simplecalculatordeign.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- palette

object Brand {
    val Purple = Color(0xFF7B2FF7)
    val DeepPurple = Color(0xFF4A00E0)
    val Violet = Color(0xFF8E2DE2)
    val Blue = Color(0xFF2F80ED)
    val Indigo = Color(0xFF4F6BFF)
    val Cyan = Color(0xFF00D2FF)
    val Teal = Color(0xFF00B4DB)
    val Orange = Color(0xFFFF8A00)
    val Amber = Color(0xFFFFB347)
    val Pink = Color(0xFFFF4D8D)
    val Coral = Color(0xFFFF5E62)
    val Green = Color(0xFF00C48C)
    val Navy = Color(0xFF0B1026)
    val NavySurface = Color(0xFF151B3A)
    val NavySurfaceHigh = Color(0xFF1E2650)
    val Snow = Color(0xFFF6F5FF)
}

private val LightColors = lightColorScheme(
    primary = Brand.Purple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF25005A),
    secondary = Brand.Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCF4FF),
    onSecondaryContainer = Color(0xFF002A35),
    tertiary = Brand.Orange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0C2),
    onTertiaryContainer = Color(0xFF3A1D00),
    background = Brand.Snow,
    onBackground = Color(0xFF1A1B2E),
    surface = Color.White,
    onSurface = Color(0xFF1A1B2E),
    surfaceVariant = Color(0xFFEFEBFA),
    onSurfaceVariant = Color(0xFF5B5873),
    surfaceContainer = Color(0xFFF3F0FC),
    surfaceContainerHigh = Color(0xFFEDE9F8),
    outline = Color(0xFFCBC5DD),
    outlineVariant = Color(0xFFE3DEF0),
    error = Color(0xFFE5484D),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB794FF),
    onPrimary = Color(0xFF2A0068),
    primaryContainer = Color(0xFF4B1FA8),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Brand.Cyan,
    onSecondary = Color(0xFF00313D),
    secondaryContainer = Color(0xFF0B4A5C),
    onSecondaryContainer = Color(0xFFCCF4FF),
    tertiary = Brand.Amber,
    onTertiary = Color(0xFF3A1D00),
    tertiaryContainer = Color(0xFF6A3A00),
    onTertiaryContainer = Color(0xFFFFE0C2),
    background = Brand.Navy,
    onBackground = Color(0xFFE8E6FF),
    surface = Brand.NavySurface,
    onSurface = Color(0xFFE8E6FF),
    surfaceVariant = Color(0xFF232B57),
    onSurfaceVariant = Color(0xFFB5B3D6),
    surfaceContainer = Brand.NavySurface,
    surfaceContainerHigh = Brand.NavySurfaceHigh,
    outline = Color(0xFF3B4475),
    outlineVariant = Color(0xFF2A3263),
    error = Color(0xFFFF6B6F),
    onError = Color(0xFF3B0003),
)

/** Calculator-specific colours and gradients not covered by Material roles. */
@Immutable
data class CalcColors(
    val isDark: Boolean,
    val background: Brush,
    val display: Brush,
    val displayText: Color,
    val displaySubtle: Color,
    val numberKey: Color,
    val numberKeyText: Color,
    val functionKey: Color,
    val functionKeyText: Color,
    val operatorKey: Brush,
    val equalsKey: Brush,
    val clearKey: Brush,
    val memoryChip: Color,
    val memoryChipText: Color,
    val card: Color,
    val cardBorder: Color,
    val accentGradient: Brush,
    val success: Color,
    val warning: Color,
)

private val LightCalcColors = CalcColors(
    isDark = false,
    background = Brush.verticalGradient(listOf(Color(0xFFF4F0FF), Color(0xFFEFF6FF), Color(0xFFF9F7FF))),
    display = Brush.linearGradient(listOf(Brand.Violet, Brand.DeepPurple, Brand.Blue)),
    displayText = Color.White,
    displaySubtle = Color.White.copy(alpha = 0.72f),
    numberKey = Color.White,
    numberKeyText = Color(0xFF1F1D3A),
    functionKey = Color(0xFFEDE7FF),
    functionKeyText = Brand.DeepPurple,
    operatorKey = Brush.linearGradient(listOf(Brand.Cyan, Brand.Blue)),
    equalsKey = Brush.linearGradient(listOf(Brand.Amber, Brand.Orange)),
    clearKey = Brush.linearGradient(listOf(Brand.Pink, Brand.Coral)),
    memoryChip = Color(0xFFE2F7FF),
    memoryChipText = Color(0xFF00708A),
    card = Color.White,
    cardBorder = Color(0xFFE6E0F6),
    accentGradient = Brush.linearGradient(listOf(Brand.Purple, Brand.Blue)),
    success = Color(0xFF00A877),
    warning = Color(0xFFE08600),
)

private val DarkCalcColors = CalcColors(
    isDark = true,
    background = Brush.verticalGradient(listOf(Color(0xFF0B1026), Color(0xFF121A3D), Color(0xFF0B1026))),
    display = Brush.linearGradient(listOf(Color(0xFF5B1FC7), Color(0xFF3A0CA3), Color(0xFF1F5FD1))),
    displayText = Color.White,
    displaySubtle = Color.White.copy(alpha = 0.7f),
    numberKey = Color(0xFF1C2448),
    numberKeyText = Color(0xFFF1EFFF),
    functionKey = Color(0xFF2A2462),
    functionKeyText = Color(0xFFCDB8FF),
    operatorKey = Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF2F6FED))),
    equalsKey = Brush.linearGradient(listOf(Color(0xFFFFA62B), Color(0xFFFF6A00))),
    clearKey = Brush.linearGradient(listOf(Color(0xFFFF4D8D), Color(0xFFE5484D))),
    memoryChip = Color(0xFF12364A),
    memoryChipText = Color(0xFF7FE3FF),
    card = Brand.NavySurface,
    cardBorder = Color(0xFF29315F),
    accentGradient = Brush.linearGradient(listOf(Color(0xFF9D5CFF), Brand.Cyan)),
    success = Color(0xFF3DDC97),
    warning = Color(0xFFFFB347),
)

val LocalCalcColors = staticCompositionLocalOf { LightCalcColors }

// ---------------------------------------------------------------- typography & shapes

private val Display = FontFamily.SansSerif

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Light, fontSize = 56.sp, lineHeight = 64.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Light, fontSize = 44.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun SimpleCalculatorTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCalcColors provides if (darkTheme) DarkCalcColors else LightCalcColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object CalcTheme {
    val colors: CalcColors
        @Composable get() = LocalCalcColors.current
}
