package com.example.gamelogger.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val AppDarkColorScheme = darkColorScheme(
    primary = Purple_Light,
    secondary = Purple_Dark,
    tertiary = Purple_Medium,
    background = LGM_Black,
    surface = LGM_Black,
    onPrimary = LGM_White,
    onSecondary = LGM_White,
    onTertiary = LGM_Black,
    onBackground = LGM_White,
    onSurface = LGM_White
)

private val AppLightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = Purple_Medium,
    secondary = Teal_Light,
    tertiary = Teal_Dark,
    background = LGM_White,
    surface = LGM_White,
    onPrimary = LGM_White,
    onSecondary = LGM_White,
    onTertiary = LGM_Black,
    onBackground = LGM_Black,
    onSurface = LGM_Black
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
        else -> AppLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}