package com.example.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlossyPressIndication

val LocalDuetThemePreset = staticCompositionLocalOf { AppThemePreset.BLUSH_ROSE }

val DuetShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun DuetTheme(
    themePreset: AppThemePreset = AppThemePreset.BLUSH_ROSE,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalDuetThemePreset provides themePreset,
        LocalIndication provides GlossyPressIndication
    ) {
        MaterialTheme(
            colorScheme = themePreset.colorScheme,
            typography = Typography,
            shapes = DuetShapes
        ) {
            CompositionLocalProvider(
                LocalContentColor provides themePreset.colorScheme.onBackground,
                content = content
            )
        }
    }
}
