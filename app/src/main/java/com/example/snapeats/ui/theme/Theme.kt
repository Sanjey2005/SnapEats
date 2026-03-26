package com.example.snapeats.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ── Dark glassmorphic colour scheme ──────────────────────────────────────────
private val SnapEatsDarkColorScheme = darkColorScheme(
    primary             = Color(0xFF4CAF50),   // green accent
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFF1B5E20),
    onPrimaryContainer  = Color(0xFFE6EDF3),
    secondary           = Color(0xFF00BCD4),   // cyan
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFF004D5C),
    onSecondaryContainer = Color(0xFFE6EDF3),
    tertiary            = Color(0xFF00BCD4),
    onTertiary          = Color.White,
    tertiaryContainer   = Color(0xFF004D5C),
    onTertiaryContainer = Color(0xFFE6EDF3),
    error               = Color(0xFFFF5252),
    onError             = Color.White,
    errorContainer      = Color(0xFF5C1A1A),
    onErrorContainer    = Color(0xFFFF8A80),
    background          = Color(0xFF0D1117),
    onBackground        = Color(0xFFE6EDF3),
    surface             = Color(0xFF161B22),
    onSurface           = Color(0xFFE6EDF3),
    surfaceVariant      = Color(0xFF21262D),
    onSurfaceVariant    = Color(0xFF8B949E),
    outline             = Color(0xFF30363D),
    outlineVariant      = Color(0xFF21262D),
    inverseSurface      = Color(0xFFE6EDF3),
    inverseOnSurface    = Color(0xFF0D1117),
    inversePrimary      = Color(0xFF1B5E20),
    surfaceTint         = Color(0xFF4CAF50),
    scrim               = Color(0xFF0D1117),
)

// ── Typography ────────────────────────────────────────────────────────────────
val SnapEatsTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

// ── Public theme entry-point ──────────────────────────────────────────────────
/**
 * SnapEatsTheme always applies the dark glassmorphic colour scheme.
 *
 * @param darkTheme    Accepted for signature compatibility but not used — always dark.
 * @param dynamicColor Accepted for signature compatibility but not used — no dynamic colour.
 * @param content      The composable tree to render inside this theme.
 */
@Composable
fun SnapEatsTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = SnapEatsDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Color(0xFF0D1117).toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = SnapEatsTypography,
        content     = content,
    )
}
