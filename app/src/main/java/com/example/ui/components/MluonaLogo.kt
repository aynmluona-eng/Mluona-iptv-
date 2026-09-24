package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TvAccentGold
import com.example.ui.theme.TvLogoWhite

/**
 * Modern minimalist monogram logo for "Mluona IPTV" combining 'M' and 'L'.
 * Strictly pure geometry, zero text, classic and sharp for television displays.
 */
@Composable
fun MluonaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    animated: Boolean = true,
    leftColor: Color = TvLogoWhite,
    rightColor: Color = TvAccentGold
) {
    var startAnim by remember { mutableStateOf(!animated) }

    LaunchedEffect(Unit) {
        if (animated) {
            startAnim = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "logoAlpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.88f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "logoScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("mluona_logo_container"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .testTag("mluona_logo_canvas")
        ) {
            val canvasW = this.size.width
            val canvasH = this.size.height
            val scaleX = (canvasW / 108f) * scale
            val scaleY = (canvasH / 108f) * scale

            val offsetX = (canvasW - (108f * scaleX)) / 2f
            val offsetY = (canvasH - (108f * scaleY)) / 2f

            // Left wing of the 'M' monogram
            val pathM = Path().apply {
                moveTo(offsetX + 24f * scaleX, offsetY + 82f * scaleY)
                lineTo(offsetX + 24f * scaleX, offsetY + 26f * scaleY)
                lineTo(offsetX + 36f * scaleX, offsetY + 26f * scaleY)
                lineTo(offsetX + 54f * scaleX, offsetY + 58f * scaleY)
                lineTo(offsetX + 54f * scaleX, offsetY + 70f * scaleY)
                lineTo(offsetX + 38f * scaleX, offsetY + 44f * scaleY)
                lineTo(offsetX + 38f * scaleX, offsetY + 82f * scaleY)
                close()
            }

            // Right wing of 'M' and the base of 'L'
            val pathL = Path().apply {
                moveTo(offsetX + 54f * scaleX, offsetY + 70f * scaleY)
                lineTo(offsetX + 70f * scaleX, offsetY + 44f * scaleY)
                lineTo(offsetX + 70f * scaleX, offsetY + 26f * scaleY)
                lineTo(offsetX + 84f * scaleX, offsetY + 26f * scaleY)
                lineTo(offsetX + 84f * scaleX, offsetY + 70f * scaleY)
                lineTo(offsetX + 94f * scaleX, offsetY + 70f * scaleY)
                lineTo(offsetX + 94f * scaleX, offsetY + 82f * scaleY)
                lineTo(offsetX + 72f * scaleX, offsetY + 82f * scaleY)
                lineTo(offsetX + 72f * scaleX, offsetY + 38f * scaleY)
                lineTo(offsetX + 54f * scaleX, offsetY + 58f * scaleY)
                close()
            }

            drawPath(path = pathM, color = leftColor.copy(alpha = alpha))
            drawPath(path = pathL, color = rightColor.copy(alpha = alpha))
        }
    }
}
