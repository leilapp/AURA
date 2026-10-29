package com.leilapp.aura.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Acentos disponibles. Para añadir uno nuevo basta con agregarlo aquí. */
enum class AuraAccent(val label: String, val primary: Color, val container: Color, val soft: Color) {
    VIOLETA("Violeta", Color(0xFFB79CFF), Color(0xFF3B2C6B), Color(0xFFD9CCFF)),
    LAVANDA("Lavanda", Color(0xFFC8B6FF), Color(0xFF41395E), Color(0xFFE6DEFF)),
    ORQUIDEA("Orquídea", Color(0xFFE3A6E8), Color(0xFF5A2E5E), Color(0xFFF4D4F6)),
    AZUL("Azul noche", Color(0xFF9DB7FF), Color(0xFF263A6B), Color(0xFFD3DFFF)),
    JADE("Jade", Color(0xFF8FD6C0), Color(0xFF1F4A40), Color(0xFFC9EFE3));

    companion object {
        fun from(name: String) = entries.firstOrNull { it.name == name } ?: VIOLETA
    }
}

object AuraColors {
    val Background = Color(0xFF0E0E12)
    val Surface = Color(0xFF16151B)
    val SurfaceHigh = Color(0xFF1F1D26)
    val SurfaceHighest = Color(0xFF292632)
    val Outline = Color(0xFF3A3645)
    val OnSurface = Color(0xFFECE9F3)
    val OnSurfaceMuted = Color(0xFFB2ADBF)
    val Child = Color(0xFFFFB38A)       // Melocotón: resalta todo lo de "Mi hijo"
    val ChildContainer = Color(0xFF4A2C1E)
    val Warning = Color(0xFFFFD27A)
    val WarningContainer = Color(0xFF4A3A16)
    val Error = Color(0xFFFF8A8A)
    val Success = Color(0xFF8FD6A8)
}

private val AuraTypography = Typography(
    displaySmall = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

private val AuraShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun AuraTheme(accent: AuraAccent = AuraAccent.VIOLETA, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = accent.primary,
        onPrimary = Color(0xFF1A1230),
        primaryContainer = accent.container,
        onPrimaryContainer = accent.soft,
        secondary = accent.soft,
        onSecondary = Color(0xFF1A1230),
        secondaryContainer = AuraColors.SurfaceHighest,
        onSecondaryContainer = AuraColors.OnSurface,
        tertiary = AuraColors.Child,
        tertiaryContainer = AuraColors.ChildContainer,
        onTertiaryContainer = Color(0xFFFFDCC8),
        background = AuraColors.Background,
        onBackground = AuraColors.OnSurface,
        surface = AuraColors.Background,
        onSurface = AuraColors.OnSurface,
        surfaceVariant = AuraColors.SurfaceHigh,
        onSurfaceVariant = AuraColors.OnSurfaceMuted,
        surfaceContainerLowest = AuraColors.Background,
        surfaceContainerLow = AuraColors.Surface,
        surfaceContainer = AuraColors.Surface,
        surfaceContainerHigh = AuraColors.SurfaceHigh,
        surfaceContainerHighest = AuraColors.SurfaceHighest,
        outline = AuraColors.Outline,
        outlineVariant = Color(0xFF2B2833),
        error = AuraColors.Error,
    )
    MaterialTheme(colorScheme = scheme, typography = AuraTypography, shapes = AuraShapes, content = content)
}
