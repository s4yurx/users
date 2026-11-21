package com.example.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BatteryHealthyGreen
import com.example.ui.theme.StatusLiveCoral
import kotlinx.coroutines.delay

/**
 * Continuous internet connectivity overlay rendered across the entire Duet app.
 *
 * - Whenever [isOnline] is `false` (at launch or any repeated disconnect while the app is open),
 *   immediately animates in a prominent frosted glass prompt notifying the user they are not
 *   connected to the internet.
 * - The instant [isOnline] becomes `true`, the offline prompt automatically exits and briefly
 *   shows a "Back online — synced" confirmation pill before disappearing cleanly.
 */
@Composable
fun OfflineConnectivityPrompt(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    var hasBeenOffline by remember { mutableStateOf(!isOnline) }
    var showReconnectedPill by remember { mutableStateOf(false) }

    LaunchedEffect(isOnline) {
        if (!isOnline) {
            hasBeenOffline = true
            showReconnectedPill = false
        } else if (hasBeenOffline) {
            showReconnectedPill = true
            delay(2_200L)
            showReconnectedPill = false
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "offline_pulse")
    val iconPulse by pulseTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offline_icon_scale"
    )

    val frostBlur = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(14.dp)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Offline prompt — automatically appears on EVERY disconnect and exits the moment internet returns
        AnimatedVisibility(
            visible = !isOnline,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(220)),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(220))
        ) {
            val shape = RoundedCornerShape(22.dp)
            Box(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = shape,
                        ambientColor = StatusLiveCoral.copy(alpha = 0.35f),
                        spotColor = Color.Black.copy(alpha = 0.32f)
                    )
                    .clip(shape)
                    .border(
                        width = 1.3.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.82f),
                                StatusLiveCoral.copy(alpha = 0.55f)
                            )
                        ),
                        shape = shape
                    )
                    .testTag("no_internet_prompt")
            ) {
                // Dedicated frosted white glass backdrop (never blurs text!)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF2B111A).copy(alpha = 0.90f),
                                    Color(0xFF4A1926).copy(alpha = 0.88f)
                                )
                            )
                        )
                        .background(Color.White.copy(alpha = 0.14f))
                        .then(frostBlur)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .scale(iconPulse)
                            .clip(CircleShape)
                            .background(StatusLiveCoral.copy(alpha = 0.24f))
                            .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WifiOff,
                            contentDescription = "No internet connection",
                            tint = Color(0xFFFFB2BF),
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "No Internet Connection",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "You are not connected to the internet. Waiting to reconnect automatically…",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.86f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .alpha(if (iconPulse > 0.98f) 1f else 0.45f)
                            .clip(CircleShape)
                            .background(StatusLiveCoral)
                    )
                }
            }
        }

        // Brief auto-dismissing emerald pill right after connection is restored
        AnimatedVisibility(
            visible = isOnline && showReconnectedPill,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(200)),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(280))
        ) {
            val shape = CircleShape
            Box(
                modifier = Modifier
                    .shadow(10.dp, shape)
                    .clip(shape)
                    .border(1.2.dp, Color.White.copy(alpha = 0.75f), shape)
                    .testTag("back_online_prompt")
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFF0E3B2B).copy(alpha = 0.92f))
                        .background(Color.White.copy(alpha = 0.14f))
                        .then(frostBlur)
                )
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Back online",
                        tint = BatteryHealthyGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Back online — synced with Duet",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
