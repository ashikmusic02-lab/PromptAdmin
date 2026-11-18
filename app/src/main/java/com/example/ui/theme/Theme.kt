package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val EnterpriseAdminColorScheme = darkColorScheme(
    primary = VioletPrimary,
    onPrimary = TextPrimary,
    primaryContainer = VioletContainer,
    onPrimaryContainer = VioletBright,
    secondary = ChampagneGold,
    onSecondary = ObsidianBg,
    secondaryContainer = ChampagneContainer,
    onSecondaryContainer = ChampagneLight,
    tertiary = StatusEmerald,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = CharcoalSurface,
    onSurface = TextPrimary,
    surfaceVariant = GraphiteCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = CharcoalSurface,
    surfaceContainer = GraphiteCard,
    surfaceContainerHigh = GraphiteElevated,
    surfaceContainerHighest = GraphiteSubtle,
    outline = GraphiteBorder,
    outlineVariant = GraphiteSubtle,
    error = StatusRose,
    onError = TextPrimary,
    errorContainer = StatusRoseBg,
    onErrorContainer = StatusRose
)

val AdminShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun PromptAdminTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EnterpriseAdminColorScheme,
        typography = Typography,
        shapes = AdminShapes,
        content = content
    )
}
