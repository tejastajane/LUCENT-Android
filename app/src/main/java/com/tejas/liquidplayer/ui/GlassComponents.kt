package com.tejas.liquidplayer.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
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
    navMaterial: Boolean = false,
    flatNavMaterial: Boolean = false,
    clampNavLensToSurfaceHeight: Boolean = false,
    dropShadowElevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isLightSurface = MaterialTheme.colorScheme.background.luminance() > 0.5f
    // Same low-alpha surface paint as the inactive background in LiquidBottomTabs.
    val navbarInactiveMaterial = if (isLightSurface) {
        Color.Black.copy(alpha = 0.04f)
    } else {
        Color.White.copy(alpha = 0.055f)
    }
    val shape = RoundedCornerShape(cornerRadius)
    val surfaceModifier = if (dropShadowElevation > 0.dp) {
        modifier.shadow(
            elevation = dropShadowElevation,
            shape = shape,
            clip = false
        )
    } else {
        modifier
    }
    Box(
        surfaceModifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                if (navMaterial) {
                    blur(8.dp.toPx())
                    lens(
                        refractionHeight = if (clampNavLensToSurfaceHeight) {
                            minOf(
                                24.dp.toPx(),
                                (size.height / 2f - 1.dp.toPx()).coerceAtLeast(0f)
                            )
                        } else {
                            24.dp.toPx()
                        },
                        refractionAmount = 24.dp.toPx(),
                        depthEffect = true,
                        chromaticAberration = false
                    )
                } else {
                    blur(0.dp.toPx())
                    lens(
                        refractionHeight = size.minDimension * 0.1f,
                        refractionAmount = size.minDimension * 0.2f,
                        depthEffect = true,
                        chromaticAberration = false
                    )
                }
            },
            highlight = {
                // The inactive navbar surface itself has no additional bright bevel.
                Highlight.Plain.copy(
                    width = 1.1.dp,
                    blurRadius = 1.25.dp,
                    alpha = if (flatNavMaterial) 0f else 0.9f
                )
            },
            shadow = {
                Shadow(
                    radius = 14.dp,
                    offset = DpOffset(0.dp, 2.dp),
                    color = Color.Black.copy(alpha = 0.42f),
                    alpha = if (flatNavMaterial) 0f else 0.95f
                )
            },
            innerShadow = {
                InnerShadow(
                    radius = 2.dp,
                    offset = DpOffset(0.dp, (-0.75).dp),
                    color = Color.White.copy(alpha = 0.14f),
                    alpha = if (flatNavMaterial) 0f else 0.85f
                )
            },
            onDrawSurface = {
                if (navMaterial) {
                    // Translucent and theme-aware; never add a solid underlay.
                    drawRect(navbarInactiveMaterial)
                } else {
                    drawRect(Color.White.copy(alpha = surfaceAlpha))
                    drawRect(Color.Black.copy(alpha = 0.018f))
                }
            }
        ).then(
            if (flatNavMaterial) {
                // Fine reflective rim for the Play/Resume pill only; keep the navbar untouched.
                Modifier.border(
                    width = 1.dp,
                    color = if (isLightSurface) Color.Black.copy(alpha = 0.14f)
                    else Color.White.copy(alpha = 0.18f),
                    shape = shape
                )
            } else {
                Modifier
            }
        ),
        content = content
    )
}


/**
 * Dialog-matched Kyant0 glass material for player panels and overlays.
 * Callers provide their own shape so existing panel geometry stays unchanged.
 */
@Composable
fun KyantGlassSurface(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    shape: Shape,
    content: @Composable BoxScope.() -> Unit
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val surfaceTint = if (isLightTheme) {
        Color(0xFFFAFAFA).copy(alpha = 0.6f)
    } else {
        Color(0xFF121212).copy(alpha = 0.4f)
    }

    Box(
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                colorControls(
                    brightness = if (isLightTheme) 0.2f else 0f,
                    saturation = 1.5f
                )
                blur(if (isLightTheme) 16.dp.toPx() else 8.dp.toPx())
                lens(24.dp.toPx(), 48.dp.toPx(), depthEffect = true)
            },
            highlight = { Highlight.Plain },
            onDrawSurface = { drawRect(surfaceTint) }
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
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val view = LocalView.current
    val base = modifier.size(width = 68.dp, height = 48.dp).clip(RoundedCornerShape(50.dp))
    val inputModifier = if (onLongClick != null) {
        base.combinedClickable(
            interactionSource = null,
            indication = null,
            role = Role.Button,
            onClickLabel = contentDescription,
            onLongClickLabel = "Open aspect ratio options",
            onClick = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onClick() },
            onLongClick = { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); onLongClick() }
        )
    } else {
        base.clickable(role = Role.Button) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick()
        }
    }
    GlassSurface(
        backdrop = backdrop,
        modifier = inputModifier,
        cornerRadius = 50.dp,
        surfaceAlpha = 0.075f,
        navMaterial = true
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.align(Alignment.Center).size(22.dp)
        )
    }
}
