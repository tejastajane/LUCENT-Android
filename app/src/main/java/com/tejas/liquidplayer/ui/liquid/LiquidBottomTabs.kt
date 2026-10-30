package com.tejas.liquidplayer.ui.liquid

// Adapted from Kyant0/AndroidLiquidGlass (Apache-2.0): preserves the library's
// combined-backdrop, damped spring, refraction, highlight and shadow interaction.

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.tejas.liquidplayer.ui.liquid.utils.DampedDragAnimation
import com.tejas.liquidplayer.ui.liquid.utils.InteractiveHighlight
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    collapseProgress: Float = 0f,
    onCollapsedClick: (() -> Unit)? = null,
    collapsedContent: (@Composable () -> Unit)? = null,
    activeContent: (@Composable (Int) -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    // Keep the refracted glass itself neutral; only the active SVG is blue in light theme.
    val accentColor = Color.White
    val containerColor = if (isLightTheme) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.055f)

    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val collapse = collapseProgress.coerceIn(0f, 1f)
        val horizontalInset = lerp(4f, 0f, collapse).dp
        val contentWidthPx = constraints.maxWidth.toFloat() - with(density) { (horizontalInset * 2f).toPx() }
        val tabWidth = contentWidthPx / tabsCount
        val selectedPillWidth = with(density) {
            lerp(tabWidth, contentWidthPx, collapse).toDp()
        }
        val containerHeight = lerp(72f, 62f, collapse).dp
        val selectedPillHeight = lerp(64f, 62f, collapse).dp

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        // Keep the animation state stable while the selected tab changes;
        // keying this state by selectedTabIndex would reset it and skip the slide.
        var currentIndex by remember {
            mutableIntStateOf(selectedTabIndex)
        }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex.toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(
                            0f,
                            spring(1f, 300f, 0.5f)
                        )
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }
        LaunchedEffect(selectedTabIndex) {
            currentIndex = selectedTabIndex
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                    onTabSelected(index)
                }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, offset ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        Row(
            Modifier
                .graphicsLayer {
                    translationX = panelOffset
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(50) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(24f.dp.toPx(), 24f.dp.toPx(), depthEffect = true, chromaticAberration = false)
                    },
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .then(interactiveHighlight.modifier)
                .height(containerHeight)
                .fillMaxWidth()
                .padding(4f.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides {
                lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
            }
        ) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(50) },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(
                                24f.dp.toPx() * progress,
                                24f.dp.toPx() * progress
                            )
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(selectedPillHeight)
                    .fillMaxWidth()
                    .padding(horizontal = 4f.dp)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        Box(
            Modifier
                .padding(horizontal = horizontalInset)
                .graphicsLayer {
                    val selectedPosition =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                    // The selected tile returns to the leading edge as it expands into the active button.
                    translationX = selectedPosition * (1f - collapse)
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { RoundedCornerShape(50) },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            depthEffect = true, chromaticAberration = false
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        // Keep a subtle premium rim visible while idle, then lift on press.
                        Highlight.Default.copy(alpha = 0.45f + 0.45f * progress)
                    },
                    shadow = {
                        val progress = dampedDragAnimation.pressProgress
                        Shadow(
                            radius = 10f.dp,
                            offset = androidx.compose.ui.unit.DpOffset(0f.dp, 2f.dp),
                            color = Color.Black.copy(alpha = 0.42f),
                            alpha = 0.55f + 0.35f * progress
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = (4f + 4f * progress).dp,
                            alpha = 0.45f + 0.45f * progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        if (isLightTheme) {
                            // Keep light mode clear and neutral; the blue is only on its active SVG.
                            drawRect(Color.Black.copy(alpha = 0.12f))
                            drawRect(Color.White.copy(alpha = 0.035f + 0.025f * progress))
                            drawRect(Color.Black.copy(alpha = 0.035f * progress))
                        } else {
                            // Dark-only smoked drop: glossy upper edge, dark center, and a deep lower rim
                            // matching the reference while leaving inactive tabs visibly greyer.
                            drawRect(Color.Black.copy(alpha = 0.34f))
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.105f),
                                        Color.White.copy(alpha = 0.035f),
                                        Color.Black.copy(alpha = 0.075f),
                                        Color.Black.copy(alpha = 0.19f)
                                    ),
                                    startY = 0f,
                                    endY = size.height
                                )
                            )
                            drawRect(Color.Black.copy(alpha = 0.07f * progress))
                        }
                    }
                )
                .border(1f.dp, Color.White.copy(alpha = 0.19f), RoundedCornerShape(50.dp))
                .height(selectedPillHeight)
                .width(selectedPillWidth)
        ) {
            if (collapsedContent != null) {
                Box(
                    Modifier.fillMaxSize()
                        .graphicsLayer { alpha = collapse }
                        .then(
                            if (collapse > 0.5f) Modifier.clickable {
                                onCollapsedClick?.invoke()
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    collapsedContent()
                }
            }
        }
        // Keep the selected SVG in its tab slot above the moving glass capsule.
        // The glass capsule has its own spring position and never owns the icon layer.
        if (activeContent != null) {
            val activeIndex = currentIndex.coerceIn(0, tabsCount - 1)
            Row(
                Modifier
                    .height(containerHeight)
                    .fillMaxWidth()
                    .padding(4f.dp)
                    .graphicsLayer { alpha = 1f - collapse },
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(tabsCount) { index ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (index == activeIndex) {
                            Box(
                                Modifier.padding(top = lerp(12f, 18f, collapse).dp).size(26.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                activeContent(activeIndex)
                            }
                        }
                    }
                }
            }
        }

    }
}
