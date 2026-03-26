package com.example.snapeats.ui.theme

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
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ── Static fallback palette (used on Android 11 and below) ───────────────────
// Primary: a warm food-inspired green
private val FallbackPrimaryLight       = Color(0xFF2E7D32)   // deep green
private val FallbackOnPrimaryLight     = Color(0xFFFFFFFF)
private val FallbackPrimaryContainer   = Color(0xFFA5D6A7)
private val FallbackOnPrimaryContainer = Color(0xFF00210B)

private val FallbackSecondaryLight       = Color(0xFFF57C00)   // warm orange
private val FallbackOnSecondaryLight     = Color(0xFFFFFFFF)
private val FallbackSecondaryContainer   = Color(0xFFFFCC80)
private val FallbackOnSecondaryContainer = Color(0xFF2B1700)

private val FallbackTertiaryLight       = Color(0xFF0277BD)   // blue accent
private val FallbackOnTertiaryLight     = Color(0xFFFFFFFF)
private val FallbackTertiaryContainer   = Color(0xFFB3E5FC)
private val FallbackOnTertiaryContainer = Color(0xFF001E2E)

private val FallbackErrorLight     = Color(0xFFB00020)
private val FallbackOnErrorLight   = Color(0xFFFFFFFF)
private val FallbackBackgroundLight = Color(0xFFFAFAFA)
private val FallbackSurfaceLight    = Color(0xFFFFFFFF)

// Dark equivalents
private val FallbackPrimaryDark       = Color(0xFF81C784)
private val FallbackOnPrimaryDark     = Color(0xFF003A16)
private val FallbackPrimaryContainerDark   = Color(0xFF1B5E20)
private val FallbackOnPrimaryContainerDark = Color(0xFFA5D6A7)

private val FallbackSecondaryDark       = Color(0xFFFFB74D)
private val FallbackOnSecondaryDark     = Color(0xFF4A2800)
private val FallbackSecondaryContainerDark   = Color(0xFF7A4F00)
private val FallbackOnSecondaryContainerDark = Color(0xFFFFDDB3)

private val FallbackTertiaryDark       = Color(0xFF4FC3F7)
private val FallbackOnTertiaryDark     = Color(0xFF00344D)
private val FallbackTertiaryContainerDark   = Color(0xFF004C6E)
private val FallbackOnTertiaryContainerDark = Color(0xFFB3E5FC)

private val FallbackErrorDark     = Color(0xFFCF6679)
private val FallbackOnErrorDark   = Color(0xFF690019)
private val FallbackBackgroundDark = Color(0xFF121212)
private val FallbackSurfaceDark    = Color(0xFF1E1E1E)

// ── Static colour schemes ─────────────────────────────────────────────────────
private val StaticLightColorScheme = lightColorScheme(
    primary             = FallbackPrimaryLight,
    onPrimary           = FallbackOnPrimaryLight,
    primaryContainer    = FallbackPrimaryContainer,
    onPrimaryContainer  = FallbackOnPrimaryContainer,
    secondary           = FallbackSecondaryLight,
    onSecondary         = FallbackOnSecondaryLight,
    secondaryContainer  = FallbackSecondaryContainer,
    onSecondaryContainer = FallbackOnSecondaryContainer,
    tertiary            = FallbackTertiaryLight,
    onTertiary          = FallbackOnTertiaryLight,
    tertiaryContainer   = FallbackTertiaryContainer,
    onTertiaryContainer = FallbackOnTertiaryContainer,
    error               = FallbackErrorLight,
    onError             = FallbackOnErrorLight,
    background          = FallbackBackgroundLight,
    surface             = FallbackSurfaceLight,
)

private val StaticDarkColorScheme = darkColorScheme(
    primary             = FallbackPrimaryDark,
    onPrimary           = FallbackOnPrimaryDark,
    primaryContainer    = FallbackPrimaryContainerDark,
    onPrimaryContainer  = FallbackOnPrimaryContainerDark,
    secondary           = FallbackSecondaryDark,
    onSecondary         = FallbackOnSecondaryDark,
    secondaryContainer  = FallbackSecondaryContainerDark,
    onSecondaryContainer = FallbackOnSecondaryContainerDark,
    tertiary            = FallbackTertiaryDark,
    onTertiary          = FallbackOnTertiaryDark,
    tertiaryContainer   = FallbackTertiaryContainerDark,
    onTertiaryContainer = FallbackOnTertiaryContainerDark,
    error               = FallbackErrorDark,
    onError             = FallbackOnErrorDark,
    background          = FallbackBackgroundDark,
    surface             = FallbackSurfaceDark,
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
 * SnapEatsTheme wraps all app content.
 *
 * - On Android 12+ (API 31+): uses Material You dynamic colour derived from the
 *   user's wallpaper via [dynamicLightColorScheme] / [dynamicDarkColorScheme].
 * - On Android 8–11: falls back to the hand-crafted green/orange static palette.
 *
 * The status bar colour is updated to match [MaterialTheme.colorScheme.background]
 * so the system UI blends seamlessly with the app chrome on every theme variant.
 *
 * @param darkTheme     Whether to apply the dark variant. Defaults to the system setting.
 * @param dynamicColor  Whether to attempt dynamic colour (requires API 31+). Defaults to true.
 * @param content       The composable tree to render inside this theme.
 */
@Composable
fun SnapEatsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        // Dynamic colour: Android 12 (API 31) and above only
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }

        // Static fallback for older devices
        darkTheme -> StaticDarkColorScheme
        else      -> StaticLightColorScheme
    }

    // Update status bar to match app background so there is no colour clash
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use background colour for the status bar
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            // Light icons on dark background; dark icons on light background
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = SnapEatsTypography,
        content     = content,
    )
}
