package com.teledrive.sky.ui.theme

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

// ─── Dark Color Scheme — Primary ────────────────────────────────────────────
private val TeleDriveDarkColorScheme = darkColorScheme(
    primary = SkyBlue,
    onPrimary = SkyTextOnBlue,
    primaryContainer = SkyBlueContainer,
    onPrimaryContainer = SkyBlueLight,

    secondary = SkyAccent,
    onSecondary = SkyBackground,
    secondaryContainer = SkyAccentDim,
    onSecondaryContainer = SkyAccent,

    tertiary = SkyAccent,
    onTertiary = SkyBackground,
    tertiaryContainer = Color(0xFF1A2E40),
    onTertiaryContainer = SkyAccent,

    error = SkyError,
    onError = Color(0xFFFFFFFF),
    errorContainer = SkyErrorContainer,
    onErrorContainer = SkyError,

    background = SkyBackground,
    onBackground = SkyTextPrimary,

    surface = SkySurface,
    onSurface = SkyTextPrimary,
    surfaceVariant = SkySurfaceElevated,
    onSurfaceVariant = SkyTextSecondary,

    surfaceTint = SkyBlue,
    inverseSurface = SkyTextPrimary,
    inverseOnSurface = SkyBackground,
    inversePrimary = SkyBlueDark,

    outline = SkyBorder,
    outlineVariant = SkyDivider,
    scrim = SkyScrim,
)

// ─── Light Color Scheme (fallback, app is primarily dark) ───────────────────
private val TeleDriveLightColorScheme = lightColorScheme(
    primary = SkyBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE9FF),
    onPrimaryContainer = Color(0xFF001849),

    secondary = Color(0xFF2196F3),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE9FF),
    onSecondaryContainer = Color(0xFF001849),

    tertiary = Color(0xFF1565C0),
    onTertiary = Color.White,

    error = Color(0xFFB00020),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFF5F8FF),
    onBackground = Color(0xFF1A1C1E),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDDE3F0),
    onSurfaceVariant = Color(0xFF44474F),

    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C7D0),
    scrim = Color(0x99000000),
)

@Composable
fun TeleDriveTheme(
    darkTheme: Boolean = true, // Default to dark theme
    dynamicColor: Boolean = false, // Disable dynamic color to keep our brand
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> TeleDriveDarkColorScheme
        else -> TeleDriveLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TeleDriveTypography,
        shapes = TeleDriveShapes,
        content = content
    )
}
