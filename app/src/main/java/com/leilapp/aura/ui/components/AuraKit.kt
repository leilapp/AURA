package com.leilapp.aura.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.leilapp.aura.ui.theme.AuraColors

/*
 * Piezas visuales reutilizables del sistema "Dark Premium" de Aura.
 * Pensadas para usarse en todas las pantallas: listas agrupadas, iconos con fondo,
 * anillo de progreso, accesos rápidos y estados de carga.
 */

/** Posición de una fila dentro de un grupo visual (una sola superficie con esquinas compartidas). */
enum class GroupPosition { SINGLE, FIRST, MIDDLE, LAST;
    companion object {
        fun of(index: Int, size: Int) = when {
            size <= 1 -> SINGLE
            index == 0 -> FIRST
            index == size - 1 -> LAST
            else -> MIDDLE
        }
    }
}

private val GroupRadius = 20.dp
private val GroupInner = 6.dp

fun GroupPosition.shape(): Shape = when (this) {
    GroupPosition.SINGLE -> RoundedCornerShape(GroupRadius)
    GroupPosition.FIRST -> RoundedCornerShape(GroupRadius, GroupRadius, GroupInner, GroupInner)
    GroupPosition.MIDDLE -> RoundedCornerShape(GroupInner)
    GroupPosition.LAST -> RoundedCornerShape(GroupInner, GroupInner, GroupRadius, GroupRadius)
}

/**
 * Fila de una lista agrupada. Cada fila puede ser un item independiente de un LazyColumn
 * (para animar altas y bajas) y aun así verse como una sola tarjeta.
 */
@Composable
fun GroupedRow(
    position: GroupPosition,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = position.shape()
    Box(
        modifier
            .fillMaxWidth()
            .padding(top = if (position == GroupPosition.MIDDLE || position == GroupPosition.LAST) 2.dp else 0.dp)
            .clip(shape)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    ) { content() }
}

/** Icono dentro de un fondo suave redondeado: estilo único para toda la app. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    container: Color = tint.copy(alpha = 0.14f),
    size: Dp = 36.dp,
    iconSize: Dp = 18.dp,
    contentDescription: String? = null,
) {
    Box(
        modifier.size(size).background(container, RoundedCornerShape(size * 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Anillo de progreso animado; el contenido se dibuja en el centro (avatar, porcentaje…). */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    stroke: Dp = 3.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "progress",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = stroke.toPx()
            val inset = w / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - w, this.size.height - w)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(w))
            if (animated > 0f) {
                drawArc(color, -90f, 360f * animated, false, topLeft, arcSize, style = Stroke(w, cap = StrokeCap.Round))
            }
        }
        content()
    }
}

/** Reduce ligeramente el elemento mientras se presiona: respuesta táctil discreta. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(120), label = "press")
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Acceso rápido compacto: icono con fondo tintado y etiqueta debajo. */
@Composable
fun QuickActionTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .pressScale(interaction)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
            .heightIn(min = 76.dp)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconBadge(icon, tint = tint, size = 34.dp, iconSize = 18.dp)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Etiqueta pequeña en mayúsculas para encabezar bloques dentro de tarjetas. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

/** Punto de color usado como indicador de prioridad o estado. */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).background(color, CircleShape))
}

@Composable
fun GroupDivider(modifier: Modifier = Modifier, startIndent: Dp = 0.dp) {
    HorizontalDivider(modifier.padding(start = startIndent), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

/** Bloque gris que late suavemente mientras se cargan los datos. */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, height: Dp = 16.dp, shape: Shape = RoundedCornerShape(8.dp)) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { this.alpha = alpha }
            .background(AuraColors.SurfaceHigh, shape),
    )
}

/** Fila horizontal genérica con icono, textos y contenido final. */
@Composable
fun ListRowContent(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    supportingColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!supporting.isNullOrBlank()) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = supportingColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing?.invoke()
    }
}
