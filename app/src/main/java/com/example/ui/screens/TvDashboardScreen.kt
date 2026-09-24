package com.example.ui.screens

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TvAccentGold
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvBorder
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceHighlight
import com.example.ui.theme.TvTextMuted
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.viewmodel.IptvViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TvDashboardScreen(
    viewModel: IptvViewModel,
    onNavigateToLiveTv: () -> Unit,
    onNavigateToMovies: () -> Unit,
    onNavigateToSeries: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val activeAccount by viewModel.activeAccount.collectAsState()
    val liveCount by viewModel.liveCount.collectAsState()
    val vodCount by viewModel.vodCount.collectAsState()
    val seriesCount by viewModel.seriesCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val strings by viewModel.appText.collectAsState()

    var selectedIndex by remember { mutableIntStateOf(0) }

    val liveTvFocusRequester = remember { FocusRequester() }
    val moviesFocusRequester = remember { FocusRequester() }
    val seriesFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }
    val profileFocusRequester = remember { FocusRequester() }

    // Ensure default focus is ON LIVE TV immediately upon entering Dashboard
    LaunchedEffect(Unit) {
        delay(80)
        try {
            liveTvFocusRequester.requestFocus()
            selectedIndex = 0
        } catch (_: Exception) {}
    }

    // Live clock updater
    var currentTimeString by remember {
        mutableStateOf(SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date()).lowercase())
    }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date()).lowercase()
            delay(30000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF141C21),
                        Color(0xFF0D1216),
                        Color(0xFF080B0D)
                    ),
                    center = Offset(300f, 150f),
                    radius = 1200f
                )
            )
            .padding(horizontal = 40.dp, vertical = 20.dp)
            .testTag("tv_dashboard_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Center Logo / Brand, Right Settings & Avatar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                // Centered App Name / Logo: mluona iptv
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "mluona",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = " iptv",
                        color = TvAccentGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.offset(y = (-2).dp)
                    )
                }

                // Right action buttons
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = TvAccentGold,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }

                    TvCircleIconButton(
                        icon = Icons.Default.Settings,
                        contentDescription = strings.settings,
                        testTag = "btn_settings",
                        focusRequester = settingsFocusRequester,
                        onDown = {
                            when (selectedIndex) {
                                1 -> moviesFocusRequester.requestFocus()
                                2 -> seriesFocusRequester.requestFocus()
                                else -> liveTvFocusRequester.requestFocus()
                            }
                        },
                        onRight = { profileFocusRequester.requestFocus() },
                        onClick = onNavigateToSettings
                    )

                    TvProfileIconButton(
                        onClick = onNavigateToUsers,
                        hasAccount = activeAccount != null,
                        testTag = "btn_profile",
                        focusRequester = profileFocusRequester,
                        onDown = {
                            when (selectedIndex) {
                                1 -> moviesFocusRequester.requestFocus()
                                2 -> seriesFocusRequester.requestFocus()
                                else -> liveTvFocusRequester.requestFocus()
                            }
                        },
                        onLeft = { settingsFocusRequester.requestFocus() }
                    )
                }
            }

            // Main 3 Hero TV Cards (Live TV, Movies, Series)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Live TV Card (Default Focused)
                HeroTvCard(
                    title = strings.liveTv,
                    countLabel = if (liveCount > 0) "+$liveCount ${strings.channels}" else "+5000 ${strings.channels}",
                    updateTimeLabel = "Last Update: Today",
                    iconType = CardIconType.LIVE_TV,
                    isSelected = selectedIndex == 0,
                    focusRequester = liveTvFocusRequester,
                    onFocus = { selectedIndex = 0 },
                    onNavigateRight = {
                        selectedIndex = 1
                        moviesFocusRequester.requestFocus()
                    },
                    onNavigateLeft = null,
                    onNavigateUp = {
                        settingsFocusRequester.requestFocus()
                    },
                    onClick = {
                        selectedIndex = 0
                        onNavigateToLiveTv()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_live_tv")
                )

                // 2. Movies Card
                HeroTvCard(
                    title = strings.movies,
                    countLabel = if (vodCount > 0) "+$vodCount ${strings.movies}" else "+1200 ${strings.movies}",
                    updateTimeLabel = "Last Update: 2 hrs ago",
                    iconType = CardIconType.MOVIES,
                    isSelected = selectedIndex == 1,
                    focusRequester = moviesFocusRequester,
                    onFocus = { selectedIndex = 1 },
                    onNavigateRight = {
                        selectedIndex = 2
                        seriesFocusRequester.requestFocus()
                    },
                    onNavigateLeft = {
                        selectedIndex = 0
                        liveTvFocusRequester.requestFocus()
                    },
                    onNavigateUp = {
                        settingsFocusRequester.requestFocus()
                    },
                    onClick = {
                        selectedIndex = 1
                        onNavigateToMovies()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_movies")
                )

                // 3. Series Card
                HeroTvCard(
                    title = strings.series,
                    countLabel = if (seriesCount > 0) "+$seriesCount ${strings.series}" else "+500 ${strings.series}",
                    updateTimeLabel = "Last Update: 2 hrs ago",
                    iconType = CardIconType.SERIES,
                    isSelected = selectedIndex == 2,
                    focusRequester = seriesFocusRequester,
                    onFocus = { selectedIndex = 2 },
                    onNavigateRight = null,
                    onNavigateLeft = {
                        selectedIndex = 1
                        moviesFocusRequester.requestFocus()
                    },
                    onNavigateUp = {
                        profileFocusRequester.requestFocus()
                    },
                    onClick = {
                        selectedIndex = 2
                        onNavigateToSeries()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_series")
                )
            }

            // Bottom Bar: Timeshift pill (left) and Clock / Weather / Server status (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bottom-left Timeshift pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF13191E))
                        .border(1.dp, Color(0xFF222B32), RoundedCornerShape(20.dp))
                        .clickable { onNavigateToLiveTv() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HandDrawnClockIcon(modifier = Modifier.size(16.dp))
                    Text(
                        text = strings.timeshift,
                        color = TvTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Bottom-right Live Clock & Server/Location status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = currentTimeString,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(18.dp)
                            .background(Color(0xFF28343D))
                    )

                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = activeAccount?.name ?: "Mluona Server",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (activeAccount != null) "${strings.connected} 1080p" else strings.ready,
                            color = TvTextMuted,
                            fontSize = 10.sp
                        )
                    }

                    HandDrawnWeatherIcon(modifier = Modifier.size(24.dp))

                    Text(
                        text = "24°",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

enum class CardIconType {
    LIVE_TV,
    MOVIES,
    SERIES
}

@Composable
fun HeroTvCard(
    title: String,
    countLabel: String,
    updateTimeLabel: String,
    iconType: CardIconType,
    isSelected: Boolean,
    focusRequester: FocusRequester? = null,
    onFocus: () -> Unit,
    onNavigateRight: (() -> Unit)? = null,
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateUp: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val active = isFocused

    val borderColor = if (active) Color(0xFF2DD4BF) else Color(0xFF212930)

    Box(
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .graphicsLayer {
                translationY = if (active) -12f else 0f
                scaleX = if (active) 1.04f else 1.0f
                scaleY = if (active) 1.04f else 1.0f
            }
            .height(205.dp)
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_BUTTON_A -> {
                            onClick()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (onNavigateRight != null) {
                                onNavigateRight()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (onNavigateLeft != null) {
                                onNavigateLeft()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (onNavigateUp != null) {
                                onNavigateUp()
                                true
                            } else false
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {

        // Card Main Body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    if (active) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF193B41), // Luminous teal-cyan glass
                                Color(0xFF132F34),
                                Color(0xFF0D2024)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF151B20), // Charcoal slate glass
                                Color(0xFF101519),
                                Color(0xFF0C1013)
                            )
                        )
                    }
                )
                .border(
                    width = if (active) 1.8.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            // Diffuse warm ambient light ray bleeding down from the top tab when active
            if (active) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.TopCenter)
                ) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x55FFC107),
                                Color(0x15FFC107),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, 0f),
                            radius = size.width * 0.45f
                        )
                    )
                }
            }

            // Card Inner Layout
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Card Title
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )

                // Bottom row: count & update on left, custom hand-drawn icon on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = countLabel,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            HandDrawnRefreshIcon(modifier = Modifier.size(11.dp))
                            Text(
                                text = updateTimeLabel,
                                color = TvTextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Hand-crafted line icon with golden accent inside
                    when (iconType) {
                        CardIconType.LIVE_TV -> HandDrawnTvIcon(modifier = Modifier.size(54.dp))
                        CardIconType.MOVIES -> HandDrawnMovieCameraIcon(modifier = Modifier.size(54.dp))
                        CardIconType.SERIES -> HandDrawnSeriesIcon(modifier = Modifier.size(54.dp))
                    }
                }
            }
        }
    }
}

/**
 * Hand-drawn TV Icon with golden lightning mark inside
 */
@Composable
fun HandDrawnTvIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val strokeColor = Color.White
        val accentColor = Color(0xFFFFBF00)
        val strokeW = 2.4.dp.toPx()

        // 1. Two antenna ears angled from top
        val topCenter = Offset(w * 0.5f, h * 0.28f)
        val leftEar = Offset(w * 0.32f, h * 0.12f)
        val rightEar = Offset(w * 0.68f, h * 0.12f)

        drawLine(
            color = strokeColor,
            start = topCenter,
            end = leftEar,
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = strokeColor,
            start = topCenter,
            end = rightEar,
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // 2. Rounded TV Screen Frame
        val screenLeft = w * 0.12f
        val screenTop = h * 0.28f
        val screenWidth = w * 0.76f
        val screenHeight = h * 0.62f
        val cornerRad = CornerRadius(14.dp.toPx(), 14.dp.toPx())

        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(screenLeft, screenTop),
            size = Size(screenWidth, screenHeight),
            cornerRadius = cornerRad,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 3. Golden lightning bolt in center
        val boltPath = Path().apply {
            moveTo(w * 0.53f, h * 0.44f)
            lineTo(w * 0.43f, h * 0.60f)
            lineTo(w * 0.49f, h * 0.60f)
            lineTo(w * 0.46f, h * 0.74f)
            lineTo(w * 0.57f, h * 0.56f)
            lineTo(w * 0.50f, h * 0.56f)
            close()
        }
        drawPath(path = boltPath, color = accentColor, style = Fill)
    }
}

/**
 * Hand-drawn Movie Camera Icon with golden viewfinder window inside
 */
@Composable
fun HandDrawnMovieCameraIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val strokeColor = Color.White
        val accentColor = Color(0xFFFFBF00)
        val strokeW = 2.4.dp.toPx()

        // 1. Two top film reels (circular rings)
        val reelRadius = w * 0.13f
        val leftReelCenter = Offset(w * 0.32f, h * 0.28f)
        val rightReelCenter = Offset(w * 0.56f, h * 0.28f)

        drawCircle(
            color = strokeColor,
            radius = reelRadius,
            center = leftReelCenter,
            style = Stroke(width = strokeW)
        )
        drawCircle(
            color = strokeColor,
            radius = reelRadius,
            center = rightReelCenter,
            style = Stroke(width = strokeW)
        )

        // 2. Camera rectangular body
        val bodyLeft = w * 0.16f
        val bodyTop = h * 0.42f
        val bodyWidth = w * 0.50f
        val bodyHeight = h * 0.46f
        val bodyCorner = CornerRadius(10.dp.toPx(), 10.dp.toPx())

        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = bodyCorner,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 3. Right lens cone/funnel
        val lensPath = Path().apply {
            moveTo(bodyLeft + bodyWidth, h * 0.52f)
            lineTo(w * 0.84f, h * 0.42f)
            lineTo(w * 0.84f, h * 0.78f)
            lineTo(bodyLeft + bodyWidth, h * 0.68f)
            close()
        }
        drawPath(
            path = lensPath,
            color = strokeColor,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 4. Golden window / lens inside body
        val innerBoxLeft = bodyLeft + bodyWidth * 0.25f
        val innerBoxTop = bodyTop + bodyHeight * 0.26f
        val innerBoxWidth = bodyWidth * 0.48f
        val innerBoxHeight = bodyHeight * 0.48f
        val innerCorner = CornerRadius(4.dp.toPx(), 4.dp.toPx())

        drawRoundRect(
            color = accentColor,
            topLeft = Offset(innerBoxLeft, innerBoxTop),
            size = Size(innerBoxWidth, innerBoxHeight),
            cornerRadius = innerCorner,
            style = Stroke(width = 2.2.dp.toPx())
        )
    }
}

/**
 * Hand-drawn Series Display Icon with golden play button inside
 */
@Composable
fun HandDrawnSeriesIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val strokeColor = Color.White
        val accentColor = Color(0xFFFFBF00)
        val strokeW = 2.4.dp.toPx()

        // 1. Clapper / Screen Rounded Outer Frame
        val frameLeft = w * 0.16f
        val frameTop = h * 0.24f
        val frameWidth = w * 0.68f
        val frameHeight = h * 0.64f
        val corner = CornerRadius(14.dp.toPx(), 14.dp.toPx())

        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(frameLeft, frameTop),
            size = Size(frameWidth, frameHeight),
            cornerRadius = corner,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 2. Clapper divider line near the top
        drawLine(
            color = strokeColor,
            start = Offset(frameLeft, h * 0.40f),
            end = Offset(frameLeft + frameWidth, h * 0.40f),
            strokeWidth = strokeW
        )

        // Vertical tick marks on the clapper head
        drawLine(
            color = strokeColor,
            start = Offset(w * 0.38f, frameTop),
            end = Offset(w * 0.38f, h * 0.40f),
            strokeWidth = 1.8.dp.toPx()
        )
        drawLine(
            color = strokeColor,
            start = Offset(w * 0.62f, frameTop),
            end = Offset(w * 0.62f, h * 0.40f),
            strokeWidth = 1.8.dp.toPx()
        )

        // 3. Golden Play triangle in the center
        val playPath = Path().apply {
            moveTo(w * 0.44f, h * 0.52f)
            lineTo(w * 0.62f, h * 0.64f)
            lineTo(w * 0.44f, h * 0.76f)
            close()
        }
        drawPath(path = playPath, color = accentColor, style = Fill)
    }
}

/**
 * Hand-drawn Clock icon for Timeshift
 */
@Composable
fun HandDrawnClockIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeColor = Color(0xFFA0AEC0)
        val strokeW = 1.6.dp.toPx()
        val r = size.width * 0.42f
        val c = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = strokeColor,
            radius = r,
            center = c,
            style = Stroke(width = strokeW)
        )

        // Clock hands
        drawLine(
            color = strokeColor,
            start = c,
            end = Offset(c.x, c.y - r * 0.55f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = strokeColor,
            start = c,
            end = Offset(c.x + r * 0.45f, c.y),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Hand-drawn Weather / Status Icon
 */
@Composable
fun HandDrawnWeatherIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val goldColor = Color(0xFFFFBF00)
        val cloudColor = Color.White

        // Sun behind cloud
        drawCircle(
            color = goldColor,
            radius = w * 0.22f,
            center = Offset(w * 0.65f, h * 0.35f),
            style = Fill
        )

        // Cloud outline
        val cloudPath = Path().apply {
            moveTo(w * 0.22f, h * 0.68f)
            lineTo(w * 0.70f, h * 0.68f)
            cubicTo(w * 0.85f, h * 0.68f, w * 0.85f, h * 0.48f, w * 0.70f, h * 0.48f)
            cubicTo(w * 0.68f, h * 0.30f, w * 0.42f, h * 0.30f, w * 0.38f, h * 0.46f)
            cubicTo(w * 0.18f, h * 0.46f, w * 0.16f, h * 0.68f, w * 0.22f, h * 0.68f)
            close()
        }
        drawPath(
            path = cloudPath,
            color = cloudColor,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Hand-drawn Refresh Icon
 */
@Composable
fun HandDrawnRefreshIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeColor = Color(0xFF8B98A5)
        val strokeW = 1.4.dp.toPx()
        val r = size.width * 0.4f
        val c = Offset(size.width / 2f, size.height / 2f)

        drawArc(
            color = strokeColor,
            startAngle = 45f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(c.x - r, c.y - r),
            size = Size(r * 2, r * 2),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun TvCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    focusRequester: FocusRequester? = null,
    onDown: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .size(38.dp)
            .clip(CircleShape)
            .background(if (isFocused) TvAccentGold else Color(0xFF161E24))
            .border(
                width = 1.dp,
                color = if (isFocused) Color.White else Color(0xFF26323B),
                shape = CircleShape
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_BUTTON_A -> {
                            onClick()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (onDown != null) {
                                onDown()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (onRight != null) {
                                onRight()
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) TvBackground else TvTextPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun TvProfileIconButton(
    onClick: () -> Unit,
    hasAccount: Boolean,
    testTag: String,
    focusRequester: FocusRequester? = null,
    onDown: (() -> Unit)? = null,
    onLeft: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFF161E24))
            .border(
                width = if (isFocused) 1.8.dp else 1.dp,
                color = if (isFocused) TvAccentGold else Color(0xFF26323B),
                shape = CircleShape
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_BUTTON_A -> {
                            onClick()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (onDown != null) {
                                onDown()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (onLeft != null) {
                                onLeft()
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Profile",
            tint = TvTextPrimary,
            modifier = Modifier.size(20.dp)
        )

        // Status indicator dot (red/amber badge as in reference image)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 1.dp, y = (-1).dp)
                .size(9.dp)
                .clip(CircleShape)
                .background(if (hasAccount) Color(0xFF22C55E) else Color(0xFFEF4444))
                .border(1.dp, Color(0xFF161E24), CircleShape)
        )
    }
}

