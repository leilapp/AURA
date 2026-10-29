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
    VIOLETA("Violeta", Color(0xFFA985ED), Color(0xFF3A2D57), Color(0xFFE8DDFB)),
    LAVANDA("Lavanda", Color(0xFFBBA8F0), Color(0xFF3D3656), Color(0xFFEAE3FC)),
    ORQUIDEA("Orquídea", Color(0xFFD9A2E0), Color(0xFF52304F), Color(0xFFF6DDF7)),
    AZUL("Azul noche", Color(0xFF94ACEF), Color(0xFF283858), Color(0xFFDCE5FD)),
    JADE("Jade", Color(0xFF86CBB6), Color(0xFF1F443B), Color(0xFFD3F0E6));

    companion object {
        fun from(name: String) = entries.firstOrNull { it.name == name } ?: VIOLETA
    }
}

/**
 * Paleta base "Dark Premium". Las superficies suben de tono por capas
 * (fondo → barra/sección → tarjeta → elemento elevado) en lugar de usar sombras fuertes.
 */
object AuraColors {
    val Background = Color(0xFF100F14)
    val Surface = Color(0xFF18161E)        // Barra inferior y fondos de sección
    val SurfaceHigh = Color(0xFF211E29)    // Tarjetas
    val SurfaceHighest = Color(0xFF2B2735) // Elementos sobre tarjetas (chips, iconos)
    val Outline = Color(0xFF393342)        // Bordes sutiles
    val OutlineSoft = Color(0xFF2A2631)    // Divisores dentro de tarjetas
    val OnSurface = Color(0xFFF4F0FA)
    val OnSurfaceMuted = Color(0xFFAAA4B5)
    val OnSurfaceFaint = Color(0xFF7A7484) // Información terciaria (pasado, deshabilitado)
    val Child = Color(0xFFF2B48F)          // Melocotón: resalta todo lo de "Mi hijo"
    val ChildContainer = Color(0xFF3F2A20)
    val ChildSurface = Color(0xFF241D1E)   // Fondo cálido de la sección "Mi hijo"
    val Warning = Color(0xFFEFCB80)
    val WarningContainer = Color(0xFF3D3219)
    val Error = Color(0xFFF08F96)
    val ErrorContainer = Color(0xFF42232A)
    val Success = Color(0xFF8DD0A6)
}

/** Espaciado consistente para todas las pantallas (múltiplos de 4). */
object AuraSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
    /** Margen lateral de las pantallas. */
    val screen = 20.dp
}

private val AuraTypography = Typography(
    displaySmall = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
)

private val AuraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AuraTheme(accent: AuraAccent = AuraAccent.VIOLETA, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = accent.primary,
        onPrimary = Color(0xFF1C1430),
        primaryContainer = accent.container,
        onPrimaryContainer = accent.soft,
        secondary = accent.soft,
        onSecondary = Color(0xFF1C1430),
        secondaryContainer = AuraColors.SurfaceHighest,
        onSecondaryContainer = AuraColors.OnSurface,
        tertiary = AuraColors.Child,
        onTertiary = Color(0xFF2B1608),
        tertiaryContainer = AuraColors.ChildContainer,
        onTertiaryContainer = Color(0xFFFCDCC8),
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
        outlineVariant = AuraColors.OutlineSoft,
        error = AuraColors.Error,
        errorContainer = AuraColors.ErrorContainer,
    )
    MaterialTheme(colorScheme = scheme, typography = AuraTypography, shapes = AuraShapes, content = content)
}
