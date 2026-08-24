package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = BentoDarkPrimary,
    onPrimary = BentoOnContainer,
    primaryContainer = BentoDarkPrimaryContainer,
    onPrimaryContainer = BentoDarkOnPrimaryContainer,
    secondary = BentoDarkSecondary,
    onSecondary = Color.Black,
    background = BentoDarkBackground,
    onBackground = BentoDarkTextPrimary,
    surface = BentoDarkSurface,
    onSurface = BentoDarkTextPrimary,
    surfaceVariant = BentoDarkSurfaceVariant,
    onSurfaceVariant = BentoDarkTextSecondary,
    outline = BentoDarkOutline
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BentoPrimary,
    onPrimary = BentoOnPrimary,
    primaryContainer = BentoContainer,
    onPrimaryContainer = BentoOnContainer,
    secondary = BentoSecondary,
    onSecondary = Color.White,
    tertiary = BentoPrimaryDark,
    background = BentoBackground,
    onBackground = BentoTextPrimary,
    surface = BentoSurface,
    onSurface = BentoTextPrimary,
    surfaceVariant = BentoSurfaceVariant,
    onSurfaceVariant = BentoTextSecondary,
    outline = BentoOutline
  )

@Composable
fun StatusVaultTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Preserve Bento Grid theme palette
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        window.statusBarColor = colorScheme.background.toArgb()
        window.navigationBarColor = colorScheme.surface.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}


