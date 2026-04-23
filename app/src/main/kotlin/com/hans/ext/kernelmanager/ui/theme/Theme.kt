package com.hans.ext.kernelmanager.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AppColorScheme = darkColorScheme(
    // Primary
    primary                = Primary,
    primaryContainer       = PrimaryContainer,
    onPrimary              = OnPrimary,
    onPrimaryContainer     = OnPrimaryContainer,
    inversePrimary         = PrimaryDim,

    // Secondary
    secondary              = Secondary,
    secondaryContainer     = SecondaryContainer,
    onSecondary            = OnSecondary,
    onSecondaryContainer   = OnSecondaryContainer,

    // Tertiary
    tertiary               = Tertiary,
    tertiaryContainer      = TertiaryContainer,
    onTertiary             = OnTertiary,
    onTertiaryContainer    = OnTertiaryContainer,

    // Error
    error                  = Error,
    errorContainer         = ErrorContainer,
    onError                = OnError,
    onErrorContainer       = OnErrorContainer,

    // Surfaces
    background             = BackgroundDark,
    onBackground           = OnSurfaceDark,
    surface                = SurfaceDark,
    surfaceVariant         = SurfaceVariant,
    onSurface              = OnSurfaceDark,
    onSurfaceVariant       = OnSurfaceMuted,
    surfaceTint            = Primary,

    // Inverse (snackbars)
    inverseSurface         = InverseSurface,
    inverseOnSurface       = InverseOnSurface,

    // Outline
    outline                = OutlineDark,
    outlineVariant         = OutlineVariant,

    scrim                  = Color(0x99000000)
)

@Composable
fun ExtKernelManagerTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AppColorScheme.background.toArgb()
            window.navigationBarColor = AppColorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
