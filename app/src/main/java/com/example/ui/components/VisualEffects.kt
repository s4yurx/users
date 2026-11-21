package com.example.ui.components

import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppIconPreset
import com.example.ui.theme.DuetAppearanceState
import com.example.ui.theme.LocalDuetThemePreset
import java.io.File
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Global Liquid-Glass Press Indication provided via LocalIndication in DuetTheme.
 * Gives every clickable card, row, pill, and icon a gleaming touch bloom, specular sweep,
 * and glossy top-rim shimmer whenever pressed.
 */
object GlossyPressIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        GlossyPressIndicationNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = 161701
}

private class GlossyPressIndicationNode(
    private val interactionSource: InteractionSource
) : Modifier.Node(), DrawModifierNode {

    private var pressPosition = Offset.Unspecified
    private val glossAlpha = Animatable(0f)
    private val sweepProgress = Animatable(-0.4f)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        pressPosition = interaction.pressPosition
                        launch { glossAlpha.animateTo(1f, tween(130, easing = FastOutSlowInEasing)) }
                        launch {
                            sweepProgress.snapTo(-0.35f)
                            sweepProgress.animateTo(1.35f, tween(420, easing = FastOutSlowInEasing))
                        }
                    }

                    is PressInteraction.Release,
                    is PressInteraction.Cancel -> {
                        launch { glossAlpha.animateTo(0f, tween(260, easing = FastOutSlowInEasing)) }
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val alpha = glossAlpha.value
        if (alpha <= 0.01f || size.width <= 0f || size.height <= 0f) return

        val center = if (pressPosition != Offset.Unspecified) pressPosition
        else Offset(size.width * 0.5f, size.height * 0.5f)
        val radius = max(size.width, size.height) * 0.85f

        // Frosted milky-white glass mist fill on press
        drawRect(color = Color.White.copy(alpha = 0.16f * alpha))

        // Radial specular touch bloom
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.46f * alpha),
                    Color.White.copy(alpha = 0.18f * alpha),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            )
        )

        // Diagonal glossy glass sheen sweep
        val sweep = sweepProgress.value
        val bandWidth = size.width * 0.48f
        val startX = (sweep * (size.width + bandWidth)) - bandWidth
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.46f * alpha),
                    Color.White.copy(alpha = 0.12f * alpha),
                    Color.Transparent
                ),
                start = Offset(startX, 0f),
                end = Offset(startX + bandWidth, size.height)
            )
        )

        // Luminous glass top rim
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.36f * alpha),
                    Color.Transparent
                ),
                startY = 0f,
                endY = size.height * 0.48f
            )
        )
    }
}

/**
 * Draws an animated shimmering metallic border strictly when an EXCLUSIVE theme is active.
 * For Classic themes (`!preset.isExclusive`), zero animation runs and an optional static fallback
 * border is drawn instead.
 */
fun Modifier.exclusiveAnimatedBorder(
    cornerRadius: Dp = 20.dp,
    strokeWidth: Dp = 1.6.dp,
    fallbackColor: Color = Color.Transparent,
    fallbackWidth: Dp = 0.dp
): Modifier = composed {
    val preset = LocalDuetThemePreset.current
    val shape = RoundedCornerShape(cornerRadius)
    if (!preset.isExclusive) {
        if (fallbackWidth > 0.dp && fallbackColor.alpha > 0f) {
            this.border(fallbackWidth, fallbackColor, shape)
        } else {
            this
        }
    } else {
        val transition = rememberInfiniteTransition(label = "exclusive_global_border")
        val phase by transition.animateFloat(
            initialValue = -220f,
            targetValue = 920f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "exclusive_border_sweep"
        )
        val brush = Brush.linearGradient(
            colors = listOf(
                preset.exclusiveAccent.copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.96f),
                preset.bubbleAccentB.copy(alpha = 0.90f),
                preset.swatchPrimary.copy(alpha = 0.78f),
                preset.exclusiveAccent.copy(alpha = 0.85f)
            ),
            start = Offset(phase - 200f, 0f),
            end = Offset(phase + 200f, 160f)
        )
        this.border(width = strokeWidth, brush = brush, shape = shape)
    }
}

/**
 * Non-consuming Liquid-Glass Glossy Press Modifier designed specifically to layer cleanly on
 * Material 3 Button / FilledTonalButton / OutlinedButton / IconButton / FAB / interactive cards.
 *
 * Uses PointerEventPass.Initial without consuming events so M3 click handling stays 100% intact
 * while providing tactile spring squeeze + radial bloom + diagonal liquid-glass sheen on press.
 * When an EXCLUSIVE theme is active, also renders the signature animated border shimmer on the rim!
 */
fun Modifier.glossyPressEffect(
    cornerRadius: Dp = 18.dp,
    pressScale: Float = 0.96f,
    restingSheenAlpha: Float = 0.14f
): Modifier = composed {
    val preset = LocalDuetThemePreset.current
    var pressed by remember { mutableStateOf(false) }
    var touchPoint by remember { mutableStateOf(Offset.Unspecified) }
    val sweepAnim = remember { Animatable(-0.4f) }

    val exclusiveSweepPhase = if (preset.isExclusive) {
        val transition = rememberInfiniteTransition(label = "exclusive_btn_rim")
        val phase by transition.animateFloat(
            initialValue = -160f,
            targetValue = 680f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "exclusive_btn_rim_phase"
        )
        phase
    } else {
        0f
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (pressed) pressScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "glossy_press_scale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = if (pressed) 120 else 280),
        label = "glossy_press_alpha"
    )

    LaunchedEffect(pressed) {
        if (pressed) {
            sweepAnim.snapTo(-0.35f)
            sweepAnim.animateTo(1.35f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
        }
    }

    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                touchPoint = down.position
                pressed = true
                waitForUpOrCancellation(pass = PointerEventPass.Initial)
                pressed = false
            }
        }
        .drawWithContent {
            val radiusPx = if (size.width > 0f && size.height > 0f) {
                cornerRadius.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
            } else {
                0f
            }

            // Frosted blurry-white glass backdrop visible behind transparent/outlined/icon buttons & tabs
            if (size.width > 0f && size.height > 0f && restingSheenAlpha > 0f) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.24f + 0.16f * glowAlpha),
                            Color.White.copy(alpha = 0.10f + 0.10f * glowAlpha)
                        )
                    ),
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
            }

            drawContent()
            if (size.width <= 0f || size.height <= 0f) return@drawWithContent

            val clipShapePath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(Offset.Zero, size),
                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                    )
                )
            }

            clipPath(clipShapePath) {
                // Soft permanent blurry-white frosted glass veil across top half + luminous mist
                val topAlpha = (restingSheenAlpha + 0.10f) + (0.30f * glowAlpha)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = topAlpha.coerceAtMost(0.56f)),
                            Color.White.copy(alpha = (topAlpha * 0.34f).coerceAtMost(0.24f)),
                            Color.White.copy(alpha = 0.03f + 0.08f * glowAlpha)
                        ),
                        startY = 0f,
                        endY = size.height
                    )
                )

                // Soft misty frosted white cloud at top-left corner for frosted-glass depth
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (0.18f + 0.18f * glowAlpha).coerceAtMost(0.40f)),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.18f, size.height * 0.12f),
                        radius = max(size.width, size.height) * 0.72f
                    )
                )

                if (glowAlpha > 0.01f) {
                    val center = if (touchPoint != Offset.Unspecified) touchPoint
                    else Offset(size.width * 0.5f, size.height * 0.5f)
                    val radialRadius = max(size.width, size.height) * 0.9f

                    // Radial glossy touch bloom
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.48f * glowAlpha),
                                Color.White.copy(alpha = 0.18f * glowAlpha),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radialRadius
                        )
                    )

                    // Diagonal specular glass sweep across button
                    val sweep = sweepAnim.value
                    val bandWidth = size.width * 0.55f
                    val startX = (sweep * (size.width + bandWidth)) - bandWidth
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.56f * glowAlpha),
                                Color.White.copy(alpha = 0.20f * glowAlpha),
                                Color.Transparent
                            ),
                            start = Offset(startX, 0f),
                            end = Offset(startX + bandWidth, size.height)
                        )
                    )
                }

                // Crisp frosted-white glass rim (or animated shimmering border when an EXCLUSIVE theme is active!)
                if (preset.isExclusive) {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                preset.exclusiveAccent.copy(alpha = 0.82f),
                                Color.White.copy(alpha = 0.96f),
                                preset.bubbleAccentB.copy(alpha = 0.84f),
                                preset.exclusiveAccent.copy(alpha = 0.82f)
                            ),
                            start = Offset(exclusiveSweepPhase - 140f, 0f),
                            end = Offset(exclusiveSweepPhase + 140f, size.height)
                        ),
                        cornerRadius = CornerRadius(radiusPx, radiusPx),
                        style = Stroke(width = (1.5f + 0.5f * glowAlpha).dp.toPx())
                    )
                } else {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (0.44f + 0.36f * glowAlpha).coerceAtMost(0.86f)),
                                Color.White.copy(alpha = (0.14f + 0.16f * glowAlpha).coerceAtMost(0.36f))
                            )
                        ),
                        cornerRadius = CornerRadius(radiusPx, radiusPx),
                        style = Stroke(width = (1.1f + 0.5f * glowAlpha).dp.toPx())
                    )
                }
            }
        }
}

private data class BubbleSpec(
    val xFraction: Float,
    val radiusDp: Float,
    val speedFractionPerSec: Float,
    val phaseOffset: Float,
    val swayAmplitudeDp: Float,
    val swayFreqHz: Float,
    val zoomFactor: Float
)

private val DefaultZoomBubbles = listOf(
    BubbleSpec(xFraction = 0.14f, radiusDp = 30f, speedFractionPerSec = 0.068f, phaseOffset = 0.08f, swayAmplitudeDp = 16f, swayFreqHz = 0.34f, zoomFactor = 1.24f),
    BubbleSpec(xFraction = 0.36f, radiusDp = 44f, speedFractionPerSec = 0.052f, phaseOffset = 0.37f, swayAmplitudeDp = 20f, swayFreqHz = 0.25f, zoomFactor = 1.28f),
    BubbleSpec(xFraction = 0.62f, radiusDp = 26f, speedFractionPerSec = 0.082f, phaseOffset = 0.61f, swayAmplitudeDp = 14f, swayFreqHz = 0.42f, zoomFactor = 1.22f),
    BubbleSpec(xFraction = 0.84f, radiusDp = 38f, speedFractionPerSec = 0.058f, phaseOffset = 0.21f, swayAmplitudeDp = 18f, swayFreqHz = 0.29f, zoomFactor = 1.26f),
    BubbleSpec(xFraction = 0.25f, radiusDp = 22f, speedFractionPerSec = 0.090f, phaseOffset = 0.78f, swayAmplitudeDp = 12f, swayFreqHz = 0.48f, zoomFactor = 1.20f),
    BubbleSpec(xFraction = 0.74f, radiusDp = 50f, speedFractionPerSec = 0.046f, phaseOffset = 0.89f, swayAmplitudeDp = 22f, swayFreqHz = 0.21f, zoomFactor = 1.30f),
    BubbleSpec(xFraction = 0.50f, radiusDp = 34f, speedFractionPerSec = 0.064f, phaseOffset = 0.50f, swayAmplitudeDp = 17f, swayFreqHz = 0.32f, zoomFactor = 1.25f)
)

/**
 * Wraps screen content in a Compose GraphicsLayer and floats translucent 3D soap/glass bubbles
 * smoothly from bottom to top. Every bubble optically ZOOMS / MAGNIFIES the real underlying UI
 * content passing behind it using clipped scaled `drawLayer(contentLayer)`, topped with
 * iridescent refraction edges and specular highlights. Non-touch-blocking!
 */
@Composable
fun RisingZoomBubblesContainer(
    enabled: Boolean,
    accentA: Color,
    accentB: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        Box(modifier = modifier.fillMaxSize()) {
            content()
        }
        return
    }

    val contentLayer = rememberGraphicsLayer()
    val bubblePath = remember { Path() }
    var elapsedSeconds by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(enabled) {
        var firstFrameNanos = -1L
        while (true) {
            withFrameNanos { frameNanos ->
                if (firstFrameNanos < 0L) firstFrameNanos = frameNanos
                elapsedSeconds = ((frameNanos - firstFrameNanos) / 1_000_000_000.0).toFloat()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                // 1. Record current frame of the app UI into hardware-accelerated GraphicsLayer
                contentLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(contentLayer)

                // 2. Render rising bottom-to-top optical magnifying bubbles over live content
                val t = elapsedSeconds
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@drawWithContent
                val hasLayer = contentLayer.size.width > 0 && contentLayer.size.height > 0

                for (spec in DefaultZoomBubbles) {
                    val radius = spec.radiusDp.dp.toPx()
                    val travelSpan = h + radius * 2.6f
                    val progress = (spec.phaseOffset + t * spec.speedFractionPerSec) % 1f
                    // Bottom -> Top movement
                    val cy = (h + radius * 1.3f) - (progress * travelSpan)
                    val swayPx = sin((t * spec.swayFreqHz * 2.0 * PI) + spec.phaseOffset * 10.0)
                        .toFloat() * spec.swayAmplitudeDp.dp.toPx()
                    val cx = (w * spec.xFraction + swayPx).coerceIn(radius * 0.4f, w - radius * 0.4f)

                    // Soft fade in at bottom & fade out at top edge
                    val edgeFade = when {
                        progress < 0.08f -> (progress / 0.08f).coerceIn(0f, 1f)
                        progress > 0.90f -> ((1f - progress) / 0.10f).coerceIn(0f, 1f)
                        else -> 1f
                    }
                    if (edgeFade <= 0.02f) continue

                    val center = Offset(cx, cy)
                    val effectiveZoom = 1f + (spec.zoomFactor - 1f) * edgeFade
                    bubblePath.reset()
                    bubblePath.addOval(
                        Rect(
                            left = cx - radius,
                            top = cy - radius,
                            right = cx + radius,
                            bottom = cy + radius
                        )
                    )

                    // Optical magnifying lens: zoom real live underlying app UI inside the bubble!
                    if (hasLayer) {
                        clipPath(bubblePath) {
                            scale(
                                scaleX = effectiveZoom,
                                scaleY = effectiveZoom,
                                pivot = center
                            ) {
                                drawLayer(contentLayer)
                            }

                            // Subtle spherical glass refraction tint
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.06f * edgeFade),
                                        accentA.copy(alpha = 0.09f * edgeFade),
                                        accentB.copy(alpha = 0.19f * edgeFade)
                                    ),
                                    center = Offset(cx - radius * 0.18f, cy - radius * 0.18f),
                                    radius = radius * 1.05f
                                ),
                                radius = radius,
                                center = center
                            )
                        }
                    }

                    // Soft outer glow halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                accentA.copy(alpha = 0.11f * edgeFade),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 1.16f
                        ),
                        radius = radius * 1.16f,
                        center = center
                    )

                    // Iridescent soap-bubble outer rim
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.74f * edgeFade),
                                accentA.copy(alpha = 0.58f * edgeFade),
                                Color(0xFF9AEBFF).copy(alpha = 0.46f * edgeFade),
                                accentB.copy(alpha = 0.54f * edgeFade),
                                Color.White.copy(alpha = 0.74f * edgeFade)
                            ),
                            center = center
                        ),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 1.6.dp.toPx())
                    )

                    // Top-left glossy crescent reflection arc
                    val arcInset = radius * 0.22f
                    drawArc(
                        color = Color.White.copy(alpha = 0.74f * edgeFade),
                        startAngle = 202f,
                        sweepAngle = 56f,
                        useCenter = false,
                        topLeft = Offset(cx - radius + arcInset, cy - radius + arcInset),
                        size = Size((radius - arcInset) * 2f, (radius - arcInset) * 2f),
                        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Specular highlight dot
                    drawCircle(
                        color = Color.White.copy(alpha = 0.82f * edgeFade),
                        radius = radius * 0.11f,
                        center = Offset(cx - radius * 0.38f, cy - radius * 0.36f)
                    )

                    // Counter-reflection glint bottom-right
                    drawCircle(
                        color = accentB.copy(alpha = 0.42f * edgeFade),
                        radius = radius * 0.08f,
                        center = Offset(cx + radius * 0.40f, cy + radius * 0.38f)
                    )
                }
            }
    ) {
        content()
    }
}

data class FloatingNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Apple Photos-style floating liquid-glass pill navigation bar paired with a detached circular
 * floating contextual glass orb. Hovers over scrolling content with luminous top rim, spring-sliding
 * active capsule, and glossy press feedback on every button.
 */
@Composable
fun AppleStyleFloatingNavBar(
    items: List<FloatingNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    actionIcon: ImageVector,
    actionContentDescription: String,
    actionTestTag: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePreset = LocalDuetThemePreset.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.25f
    val glassBorderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.62f else 0.95f),
            Color.White.copy(alpha = if (isDark) 0.22f else 0.55f)
        )
    )
    val pillShape = RoundedCornerShape(34.dp)
    val frostBlurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(14.dp)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .widthIn(max = 520.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Main Floating Blurry-White Frosted Glass Navigation Capsule
        Box(
            modifier = Modifier
                .weight(1f)
                .height(62.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = pillShape,
                    ambientColor = Color.White.copy(alpha = 0.32f),
                    spotColor = Color.Black.copy(alpha = 0.26f)
                )
                .clip(pillShape)
                .then(
                    if (activePreset.isExclusive) {
                        Modifier.exclusiveAnimatedBorder(cornerRadius = 34.dp, strokeWidth = 1.7.dp)
                    } else {
                        Modifier.border(1.3.dp, glassBorderBrush, pillShape)
                    }
                )
        ) {
            // Dedicated blurry white frosted glass backdrop layer (never blurs foreground text/icons!)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color.White.copy(alpha = 0.24f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                                    Color.White.copy(alpha = 0.14f)
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.92f),
                                    Color.White.copy(alpha = 0.76f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.80f)
                                )
                            }
                        )
                    )
                    .then(frostBlurModifier)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.30f else 0.75f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.22f, size.height * 0.15f),
                            radius = size.width * 0.45f
                        ),
                        radius = size.width * 0.45f,
                        center = Offset(size.width * 0.22f, size.height * 0.15f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.22f else 0.65f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.80f, size.height * 0.85f),
                            radius = size.width * 0.42f
                        ),
                        radius = size.width * 0.42f,
                        center = Offset(size.width * 0.80f, size.height * 0.85f)
                    )
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
            ) {
                val count = items.size.coerceAtLeast(1)
                val slotWidth = maxWidth / count
                val indicatorOffset by animateDpAsState(
                    targetValue = slotWidth * selectedIndex,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "floating_nav_indicator_offset"
                )

                // Sliding Frosted Blurry-White Active Tab Pill
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(slotWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp))
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.72f else 0.96f),
                                    Color.White.copy(alpha = if (isDark) 0.20f else 0.45f)
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = if (isDark) {
                                        listOf(
                                            Color.White.copy(alpha = 0.34f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
                                        )
                                    } else {
                                        listOf(
                                            Color.White.copy(alpha = 0.94f),
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                                        )
                                    }
                                )
                            )
                            .then(frostBlurModifier)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val selected = index == selectedIndex
                        val tint by animateColorAsState(
                            targetValue = if (selected) {
                                if (isDark) Color.White else MaterialTheme.colorScheme.primary
                            } else {
                                if (isDark) Color.White.copy(alpha = 0.76f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(220),
                            label = "floating_nav_tint_$index"
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .glossyPressEffect(cornerRadius = 28.dp, pressScale = 0.94f, restingSheenAlpha = 0.08f)
                                .clip(RoundedCornerShape(28.dp))
                                .clickable(
                                    role = Role.Tab,
                                    onClick = { onSelect(index) }
                                )
                                .testTag("nav_tab_${item.route}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = tint,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = tint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // Detached Sleek Compact Apple Photos-Style Floating Action Orb (48dp crisp target)
        Box(
            modifier = Modifier
                .size(48.dp)
                .glossyPressEffect(cornerRadius = 24.dp, pressScale = 0.91f, restingSheenAlpha = 0.24f)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    ambientColor = Color.White.copy(alpha = 0.35f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .then(
                    if (activePreset.isExclusive) {
                        Modifier.exclusiveAnimatedBorder(cornerRadius = 24.dp, strokeWidth = 1.8.dp)
                    } else {
                        Modifier.border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.88f),
                                    Color.White.copy(alpha = 0.28f)
                                )
                            ),
                            shape = CircleShape
                        )
                    }
                )
                .clickable(
                    role = Role.Button,
                    onClick = onActionClick
                )
                .semantics { contentDescription = actionContentDescription }
                .testTag(actionTestTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = actionIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Renders the user's selected App Icon Preset or uploaded custom gallery photo logo in a glossy squircle badge.
 */
@Composable
fun DuetBrandLogoBadge(
    appearance: DuetAppearanceState,
    size: Dp = 44.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val customBitmap = remember(appearance.customLogoPath, appearance.customLogoVersion) {
        appearance.customLogoPath?.let { path ->
            runCatching {
                val f = File(path)
                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
            }.getOrNull()
        }
    }

    val shape = RoundedCornerShape(size * 0.30f)
    val baseModifier = modifier
        .size(size)
        .then(
            if (onClick != null) {
                Modifier.glossyPressEffect(cornerRadius = size * 0.30f, pressScale = 0.92f)
            } else {
                Modifier
            }
        )
        .clip(shape)
        .border(
            width = 1.4.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.7f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                )
            ),
            shape = shape
        )
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center
    ) {
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap.asImageBitmap(),
                contentDescription = "Custom app logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AppIconPresetArtwork(
                preset = appearance.iconPreset,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun AppIconPresetArtwork(
    preset: AppIconPreset,
    modifier: Modifier = Modifier
) {
    val bgColors = when (preset) {
        AppIconPreset.CLASSIC -> listOf(Color(0xFF3E182A), Color(0xFF7B263F), Color(0xFFA83E53))
        AppIconPreset.ROSE -> listOf(Color(0xFF5C0E26), Color(0xFFA83E53), Color(0xFFE8647C))
        AppIconPreset.MIDNIGHT -> listOf(Color(0xFF071226), Color(0xFF143563), Color(0xFF285F9E))
        AppIconPreset.VELVET -> listOf(Color(0xFF1B0912), Color(0xFF47182E), Color(0xFF7B2A4A))
        AppIconPreset.SUNSET -> listOf(Color(0xFF6B1E18), Color(0xFFC84E2C), Color(0xFFF6A263))
        AppIconPreset.MINIMAL -> listOf(Color(0xFF101116), Color(0xFF232630), Color(0xFF393E4D))
    }

    Box(
        modifier = modifier.background(
            Brush.linearGradient(colors = bgColors)
        ),
        contentAlignment = Alignment.Center
    ) {
        if (preset.isClassicPhoto && preset.fgDrawableRes != null) {
            Image(
                painter = painterResource(id = preset.fgDrawableRes),
                contentDescription = preset.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (preset.fgDrawableRes != null) {
            Image(
                painter = painterResource(id = preset.fgDrawableRes),
                contentDescription = preset.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(0.86f)
            )
        }
    }
}
