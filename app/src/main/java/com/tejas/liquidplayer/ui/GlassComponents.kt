package com.tejas.liquidplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens

/**
 * Real backdrop-based glass surface using Kyant0's Backdrop library.
 * The surface and reflections are neutral white/grey only; do not add color tints.
 */
@Composable
fun GlassSurface(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    surfaceAlpha: Float = 0.055f,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                blur(14.dp.toPx())
                // Neutral refraction: chromatic aberration is intentionally disabled.
                lens(12.dp.toPx(), 7.dp.toPx())
            },
            onDrawSurface = {
                drawRect(Color.White.copy(alpha = surfaceAlpha))
                drawRect(Color.Black.copy(alpha = 0.025f))
            }
        ),
        content = content
    )
}

@Composable
fun GlassIconButton(
    backdrop: Backdrop,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val alpha = if (selected) 0.13f else 0.045f
    GlassSurface(
        backdrop = backdrop,
        modifier = modifier.size(48.dp).clickable(onClick = onClick),
        cornerRadius = 50.dp,
        surfaceAlpha = alpha
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.align(Alignment.Center).size(22.dp)
        )
    }
}
