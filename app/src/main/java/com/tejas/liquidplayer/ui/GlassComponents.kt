package com.tejas.liquidplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/**
 * A real Kyant0 Backdrop surface.
 *
 * The source must be a useful rendered backdrop (not a flat black placeholder).
 * Keep this component neutral: no blue/cyan tint and no chromatic aberration.
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
                // Match Kyant0's GlassPlayground reset values: blur 0dp, height 0.2
                // of half the element's minimum dimension, amount 0.2 of that dimension,
                // and chromatic aberration off. The proportional lens scales naturally
                // from small player buttons to larger panels.
                vibrancy()
                blur(0.dp.toPx())
                lens(
                    refractionHeight = size.minDimension * 0.1f,
                    refractionAmount = size.minDimension * 0.2f,
                    depthEffect = true,
                    chromaticAberration = false
                )
            },
            highlight = {
                // The library's GlassPlayground uses a plain, neutral highlight.
                Highlight.Plain.copy(
                    width = 1.1.dp,
                    blurRadius = 1.25.dp,
                    alpha = 0.9f
                )
            },
            shadow = {
                Shadow(
                    radius = 14.dp,
                    offset = DpOffset(0.dp, 2.dp),
                    color = Color.Black.copy(alpha = 0.42f),
                    alpha = 0.95f
                )
            },
            innerShadow = {
                InnerShadow(
                    radius = 2.dp,
                    offset = DpOffset(0.dp, (-0.75).dp),
                    color = Color.White.copy(alpha = 0.14f),
                    alpha = 0.85f
                )
            },
            onDrawSurface = {
                // Neutral clear material; optical highlights come from Backdrop itself.
                drawRect(Color.White.copy(alpha = surfaceAlpha))
                drawRect(Color.Black.copy(alpha = 0.018f))
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
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val view = LocalView.current
    val base = modifier.size(48.dp).clip(RoundedCornerShape(50.dp))
    val inputModifier = if (onLongClick != null) {
        base.pointerInput(onClick, onLongClick) {
            detectTapGestures(
                onTap = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onClick() },
                onLongPress = { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); onLongClick() }
            )
        }
    } else {
        base.clickable { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onClick() }
    }
    GlassSurface(
        backdrop = backdrop,
        modifier = inputModifier,
        cornerRadius = 50.dp,
        surfaceAlpha = 0.075f
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.align(Alignment.Center).size(22.dp)
        )
    }
}
