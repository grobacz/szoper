package com.szopper.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Custom color palette for shopping list app
object SzopperColors {
    // Green shades for shopping/success states
    val Green50 = Color(0xFFE8F5E8)
    val Green100 = Color(0xFFC8E6C9)
    val Green200 = Color(0xFFA5D6A7)
    val Green300 = Color(0xFF81C784)
    val Green400 = Color(0xFF66BB6A)
    val Green500 = Color(0xFF4CAF50)
    val Green600 = Color(0xFF43A047)
    val Green700 = Color(0xFF388E3C)
    val Green800 = Color(0xFF2E7D32)
    val Green900 = Color(0xFF1B5E20)
    
    // Neutral grays for backgrounds and surfaces
    val Gray50 = Color(0xFFFAFAFA)
    val Gray100 = Color(0xFFF5F5F5)
    val Gray200 = Color(0xFFEEEEEE)
    val Gray300 = Color(0xFFE0E0E0)
    val Gray800 = Color(0xFF424242)
    val Gray900 = Color(0xFF212121)
    
    // Dark theme specific colors
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkBackground = Color(0xFF121212)
    val DarkSurfaceVariant = Color(0xFF2A2A2A)
    val DarkSurfaceContainer = Color(0xFF262626)
    
    // Error colors
    val Error = Color(0xFFD32F2F)
    val ErrorContainer = Color(0xFFFFEBEE)
    val OnErrorContainer = Color(0xFFB71C1C)
}

private val DarkColorScheme = darkColorScheme(
    primary = SzopperColors.Green400,
    onPrimary = Color.Black,
    primaryContainer = SzopperColors.Green800,
    onPrimaryContainer = SzopperColors.Green100,
    secondary = SzopperColors.Green300,
    onSecondary = Color.Black,
    secondaryContainer = SzopperColors.Green900,
    onSecondaryContainer = SzopperColors.Green200,
    
    background = SzopperColors.DarkBackground,
    onBackground = Color.White,
    surface = SzopperColors.DarkSurface,
    onSurface = Color.White,
    surfaceVariant = SzopperColors.DarkSurfaceVariant,
    onSurfaceVariant = SzopperColors.Gray300,
    
    error = SzopperColors.Error,
    errorContainer = Color(0xFF5F1A1A),
    onError = Color.White,
    onErrorContainer = Color(0xFFFFDAD6),
    
    outline = SzopperColors.Gray800,
    outlineVariant = Color(0xFF404040)
)

private val LightColorScheme = lightColorScheme(
    primary = SzopperColors.Green600,
    onPrimary = Color.White,
    primaryContainer = SzopperColors.Green100,
    onPrimaryContainer = SzopperColors.Green900,
    secondary = SzopperColors.Green500,
    onSecondary = Color.White,
    secondaryContainer = SzopperColors.Green50,
    onSecondaryContainer = SzopperColors.Green800,
    
    background = SzopperColors.Gray50,
    onBackground = SzopperColors.Gray900,
    surface = Color.White,
    onSurface = SzopperColors.Gray900,
    surfaceVariant = SzopperColors.Gray100,
    onSurfaceVariant = SzopperColors.Gray800,
    
    error = SzopperColors.Error,
    errorContainer = SzopperColors.ErrorContainer,
    onError = Color.White,
    onErrorContainer = SzopperColors.OnErrorContainer,
    
    outline = SzopperColors.Gray300,
    outlineVariant = SzopperColors.Gray200
)

@Composable
fun SzopperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}