package com.example.gamelogger.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val AppDarkColorScheme = darkColorScheme(
    primary = LGM_Dark_Blue,
    secondary = LGM_Grey_Blue,
    tertiary = LGM_Light_Grey,
    background = LGM_Near_Black,
    surface = LGM_Near_Black,
    onPrimary = LGM_Off_White,
    onSecondary = LGM_Off_White,
    onTertiary = LGM_Off_White,
    onBackground = LGM_Off_White,
    onSurface = LGM_Off_White
)

@Composable
fun GameLoggerTheme(
    darkTheme: Boolean = true, // Default to dark theme
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color to use our palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> AppDarkColorScheme
        else -> AppDarkColorScheme // Use dark for light theme as well
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}