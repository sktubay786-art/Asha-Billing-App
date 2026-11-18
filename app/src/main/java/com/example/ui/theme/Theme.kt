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

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldMint,
    onPrimaryContainer = EmeraldDeep,
    secondary = EmeraldDeep,
    onSecondary = Color.White,
    secondaryContainer = SoftTileSurface,
    onSecondaryContainer = InkDark,
    background = PaperBackground,
    onBackground = InkDark,
    surface = SurfaceWhite,
    onSurface = InkDark,
    surfaceVariant = SoftTileSurface,
    onSurfaceVariant = MutedSlate,
    outline = HairlineBorder,
    outlineVariant = HairlineBorder,
    error = CrimsonDue,
    onError = Color.White,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonDue
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = EmeraldMintBright,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF490006),
    errorContainer = Color(0xFF6B141A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val AshaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(15.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun AshaBillingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryColor: Color = EmeraldPrimary,
    deepColor: Color = EmeraldDeep,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme.copy(
            primaryContainer = deepColor
        )
    } else {
        LightColorScheme.copy(
            primary = primaryColor,
            secondary = deepColor
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AshaTypography,
        shapes = AshaShapes,
        content = content
    )
}
