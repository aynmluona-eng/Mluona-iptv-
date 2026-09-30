package com.example.ui.screens

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.IptvViewModel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.ui.theme.TvAccentGold
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvBorder
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceHighlight
import com.example.ui.theme.TvTextMuted
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ResizeModeChoice(val label: String, val mode: Int) {
    FIT("تناسب الشاشة", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("ملء كامل", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("تكبير (Zoom)", AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
}

@OptIn(UnstableApi::class)
@Composable
fun TvPlayerScreen(
    title: String,
    streamUrl: String,
    isLive: Boolean = false,
    channelNumber: Int? = null,
    epgInfo: String? = null,
    frequencyInfo: String? = null,
    viewModel: IptvViewModel? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentLiveChannel by (viewModel?.selectedLiveChannel ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState()

    val currentTitle = if (isLive && currentLiveChannel != null) currentLiveChannel!!.name else title
    val currentChannelNumber = if (isLive && currentLiveChannel != null) currentLiveChannel!!.num else channelNumber
    val currentEpgInfo = if (isLive && currentLiveChannel != null) (currentLiveChannel!!.epgChannelId ?: "البث الحي المباشر") else epgInfo
    val resolvedStreamUrl = if (isLive && currentLiveChannel != null) {
        viewModel?.getLiveStreamUrl(currentLiveChannel!!) ?: streamUrl
    } else {
        streamUrl
    }

    // Double-click OK detection state (for VOD play/pause)
    var lastOkPressTime by remember { mutableLongStateOf(0L) }
    val doubleClickThreshold = 400L

    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }
    var showBottomDetails by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    // Live TV Receiver-style states
    var liveOverlayVisible by remember { mutableStateOf(true) }
    var numberInputBuffer by remember { mutableStateOf("") }
    var autoHideJob by remember { mutableStateOf<Job?>(null) }
    var numberTuneJob by remember { mutableStateOf<Job?>(null) }

    fun showLiveReceiverBanner() {
        liveOverlayVisible = true
        autoHideJob?.cancel()
        autoHideJob = coroutineScope.launch {
            delay(3500)
            liveOverlayVisible = false
        }
    }

    LaunchedEffect(Unit) {
        if (isLive) {
            showLiveReceiverBanner()
        }
    }

    // Playback Speed (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x) for Movies
    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    var currentSpeedIndex by remember { mutableIntStateOf(2) } // default 1.0f

    // Resize Mode
    var currentResizeModeIndex by remember { mutableIntStateOf(0) }
    val resizeModes = listOf(
        ResizeModeChoice.FIT,
        ResizeModeChoice.FILL,
        ResizeModeChoice.ZOOM
    )

    // Current Time for Live OSD Clock (only runs when overlays are visible to avoid unnecessary recompositions)
    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(showControls || showBottomDetails) {
        if (showControls || showBottomDetails) {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            while (true) {
                currentTimeString = timeFormat.format(Date())
                delay(1000)
            }
        }
    }

    // Auto-hide controls & bottom details for VOD
    LaunchedEffect(showControls, isPlaying, isLive) {
        if (!isLive && showControls && isPlaying) {
            delay(5000)
            showControls = false
        }
    }

    LaunchedEffect(showBottomDetails, isLive) {
        if (!isLive && showBottomDetails) {
            delay(7000)
            showBottomDetails = false
        }
    }

    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var activeStreamUrl by remember { mutableStateOf(resolvedStreamUrl) }
    var fallbackAttempted by remember(activeStreamUrl) { mutableStateOf(false) }

    LaunchedEffect(resolvedStreamUrl) {
        if (resolvedStreamUrl.isNotBlank() && resolvedStreamUrl != activeStreamUrl) {
            activeStreamUrl = resolvedStreamUrl
            fallbackAttempted = false
            playerError = null
            isBuffering = true
            showBottomDetails = true
            showControls = true
            if (isLive) {
                showLiveReceiverBanner()
            }
        }
    }

    val exoPlayer = remember {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 1000,
                /* maxBufferMs = */ 5000,
                /* bufferForPlaybackMs = */ 250,
                /* bufferForPlaybackAfterRebufferMs = */ 500
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(1000, false)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("IPTVSmartersPro")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(4000)
            .setReadTimeoutMs(8000)
            .setKeepPostFor302Redirects(true)
            .setDefaultRequestProperties(
                mapOf(
                    "User-Agent" to "IPTVSmartersPro",
                    "Accept" to "*/*",
                    "Connection" to "keep-alive"
                )
            )
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
            .apply {
                playWhenReady = true
                val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build()
                setAudioAttributes(audioAttributes, true)
            }
    }

    LaunchedEffect(activeStreamUrl) {
        if (activeStreamUrl.isNotBlank()) {
            isBuffering = true
            playerError = null
            exoPlayer.stop()
            val mediaItem = MediaItem.fromUri(activeStreamUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    // Update playback speed when changed
    LaunchedEffect(currentSpeedIndex) {
        val speed = speedOptions[currentSpeedIndex]
        exoPlayer.playbackParameters = PlaybackParameters(speed)
    }

    // Update aspect ratio / resize mode when changed
    LaunchedEffect(currentResizeModeIndex, playerViewRef) {
        playerViewRef?.resizeMode = resizeModes[currentResizeModeIndex].mode
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    playerError = null
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                if (!fallbackAttempted && isLive) {
                    fallbackAttempted = true
                    if (activeStreamUrl.endsWith(".m3u8", ignoreCase = true)) {
                        activeStreamUrl = activeStreamUrl.replace(".m3u8", ".ts")
                        return
                    } else if (activeStreamUrl.endsWith(".ts", ignoreCase = true)) {
                        activeStreamUrl = activeStreamUrl.replace(".ts", ".m3u8")
                        return
                    }
                }
                playerError = "تعذر تشغيل البث: تأكد من صحة الرابط أو عمل السيرفر"
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            playerViewRef?.player = null
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    // Periodic time update (only for VOD / Movies when controls are visible)
    LaunchedEffect(isPlaying, isLive, showControls) {
        if (!isLive && isPlaying && showControls) {
            while (isPlaying && showControls) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
                delay(1000)
            }
        }
    }

    // Handle Remote Back key directly: close channel and exit player immediately
    BackHandler {
        if (isLive && numberInputBuffer.isNotEmpty()) {
            numberTuneJob?.cancel()
            numberInputBuffer = ""
        } else {
            onBack()
        }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val keyCode = keyEvent.nativeKeyEvent.keyCode

                    if (isLive) {
                        // Live TV: Receiver Behavior
                        when (keyCode) {
                            // Back button: clear number buffer or return
                            KeyEvent.KEYCODE_BACK,
                            KeyEvent.KEYCODE_ESCAPE,
                            KeyEvent.KEYCODE_WINDOW -> {
                                if (numberInputBuffer.isNotEmpty()) {
                                    numberTuneJob?.cancel()
                                    numberInputBuffer = ""
                                    true
                                } else {
                                    onBack()
                                    true
                                }
                            }

                            // OK / DPAD_CENTER: Tune immediately if buffer exists, or toggle receiver banner
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                if (numberInputBuffer.isNotEmpty()) {
                                    numberTuneJob?.cancel()
                                    val num = numberInputBuffer.toIntOrNull()
                                    if (num != null && viewModel != null) {
                                        viewModel.playChannelByNumber(num)
                                    }
                                    numberInputBuffer = ""
                                    showLiveReceiverBanner()
                                } else {
                                    if (liveOverlayVisible) {
                                        liveOverlayVisible = false
                                    } else {
                                        showLiveReceiverBanner()
                                    }
                                }
                                true
                            }

                            // CH+ / Page Up / D-pad Up: Next channel
                            KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_DPAD_UP -> {
                                if (viewModel != null) {
                                    viewModel.playNextLiveChannel()
                                    showLiveReceiverBanner()
                                }
                                true
                            }

                            // CH- / Page Down / D-pad Down: Previous channel
                            KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (viewModel != null) {
                                    viewModel.playPreviousLiveChannel()
                                    showLiveReceiverBanner()
                                }
                                true
                            }

                            // Number keys: 0 to 9 on TV remote
                            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4,
                            KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9,
                            KeyEvent.KEYCODE_NUMPAD_0, KeyEvent.KEYCODE_NUMPAD_1, KeyEvent.KEYCODE_NUMPAD_2, KeyEvent.KEYCODE_NUMPAD_3,
                            KeyEvent.KEYCODE_NUMPAD_4, KeyEvent.KEYCODE_NUMPAD_5, KeyEvent.KEYCODE_NUMPAD_6, KeyEvent.KEYCODE_NUMPAD_7,
                            KeyEvent.KEYCODE_NUMPAD_8, KeyEvent.KEYCODE_NUMPAD_9 -> {
                                val digit = when (keyCode) {
                                    in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> (keyCode - KeyEvent.KEYCODE_0).toString()
                                    in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> (keyCode - KeyEvent.KEYCODE_NUMPAD_0).toString()
                                    else -> ""
                                }
                                if (digit.isNotEmpty() && numberInputBuffer.length < 5) {
                                    numberInputBuffer += digit
                                    liveOverlayVisible = true
                                    numberTuneJob?.cancel()
                                    numberTuneJob = coroutineScope.launch {
                                        delay(1200)
                                        val targetNum = numberInputBuffer.toIntOrNull()
                                        if (targetNum != null && viewModel != null) {
                                            viewModel.playChannelByNumber(targetNum)
                                        }
                                        numberInputBuffer = ""
                                        showLiveReceiverBanner()
                                    }
                                }
                                true
                            }

                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                exoPlayer.play()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                exoPlayer.pause()
                                true
                            }
                            else -> false
                        }
                    } else {
                        // VOD / Movies / Series
                        when (keyCode) {
                            KeyEvent.KEYCODE_BACK,
                            KeyEvent.KEYCODE_ESCAPE,
                            KeyEvent.KEYCODE_WINDOW -> {
                                onBack()
                                true
                            }

                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                val now = System.currentTimeMillis()
                                if (now - lastOkPressTime < doubleClickThreshold) {
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    lastOkPressTime = 0L
                                } else {
                                    lastOkPressTime = now
                                    showBottomDetails = !showBottomDetails
                                    showControls = true
                                }
                                true
                            }

                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                                showControls = true
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                                exoPlayer.seekTo(newPos)
                                showControls = true
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                exoPlayer.play()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                exoPlayer.pause()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                exoPlayer.seekTo((exoPlayer.currentPosition + 15000).coerceAtMost(durationMs))
                                showControls = true
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                                exoPlayer.seekTo((exoPlayer.currentPosition - 15000).coerceAtLeast(0L))
                                showControls = true
                                true
                            }
                            else -> false
                        }
                    }
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isLive) {
                    if (liveOverlayVisible) {
                        liveOverlayVisible = false
                    } else {
                        showLiveReceiverBanner()
                    }
                } else {
                    val now = System.currentTimeMillis()
                    if (now - lastOkPressTime < doubleClickThreshold) {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        lastOkPressTime = 0L
                    } else {
                        lastOkPressTime = now
                        showControls = !showControls
                        if (showControls) showBottomDetails = true
                    }
                }
            }
            .testTag("tv_player_screen")
    ) {
        // Video View with PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = resizeModes[currentResizeModeIndex].mode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    playerViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator
        if (isBuffering && playerError == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF00E676),
                        modifier = Modifier.size(52.dp),
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = "جارٍ تحميل البث...",
                        color = TvTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Error message overlay
        if (playerError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = playerError ?: "",
                        color = Color(0xFFFF6B6B),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    var isRetryFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .onFocusChanged { isRetryFocused = it.isFocused }
                            .focusable()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isRetryFocused) Color(0xFF00E676) else Color(0xFF00E676).copy(alpha = 0.85f))
                            .border(
                                width = if (isRetryFocused) 2.dp else 0.dp,
                                color = if (isRetryFocused) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                playerError = null
                                isBuffering = true
                                val mediaItem = MediaItem.fromUri(activeStreamUrl)
                                exoPlayer.setMediaItem(mediaItem)
                                exoPlayer.prepare()
                                exoPlayer.play()
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "إعادة المحاولة",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // =========================================================================
        // LIVE TV OVERLAY (RECEIVER STYLE: Simple, top right back, center bottom bar)
        // =========================================================================
        if (isLive) {
            // 1. Top Right Back Button ("وخيار الرجوع في اعلى اليمين")
            AnimatedVisibility(
                visible = liveOverlayVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp, end = 24.dp)
            ) {
                val backSource = remember { MutableInteractionSource() }
                val isBackFocused by backSource.collectIsFocusedAsState()

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isBackFocused) Color(0xFF00E676) else Color.Black.copy(alpha = 0.55f))
                        .border(
                            width = 1.dp,
                            color = if (isBackFocused) Color.White else Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(interactionSource = backSource, indication = null) {
                            onBack()
                        }
                        .focusable(interactionSource = backSource)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("btn_live_back_top_right"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "رجوع",
                            color = if (isBackFocused) Color.Black else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = if (isBackFocused) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. Channel Number Input Indicator (when typing digits on remote: e.g. "12")
            if (numberInputBuffer.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 28.dp, top = 24.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(1.5.dp, Color(0xFF00E676), RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "CH: $numberInputBuffer -",
                        color = Color(0xFF00E676),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // 3. Small semi-transparent center bottom bar containing channel number and name merged
            // ("بالنسبة للشريط السفلي للمشغل ادمج معه رقم قناة")
            val liveChannelsList by (viewModel?.liveChannels ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
            val computedIndex = remember(currentLiveChannel, liveChannelsList) {
                val idx = liveChannelsList.indexOfFirst { it.streamId == currentLiveChannel?.streamId }
                if (idx >= 0) idx + 1 else null
            }
            val displayChannelNum = currentChannelNumber ?: computedIndex

            AnimatedVisibility(
                visible = liveOverlayVisible,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.60f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .testTag("live_receiver_channel_bar")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (displayChannelNum != null && displayChannelNum > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF00E676).copy(alpha = 0.22f))
                                    .border(1.dp, Color(0xFF00E676).copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$displayChannelNum",
                                    color = Color(0xFF00E676),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Text(
                            text = currentTitle,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // =========================================================================
        // VOD / MOVIES / SERIES OVERLAYS (Full controls, seekbar, speed, etc.)
        // =========================================================================
        if (!isLive) {
            // Top Header Bar: Back button (Top Left) & Aspect Ratio/Zoom (Top Right)
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.88f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Left: Back Button + Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val backSource = remember { MutableInteractionSource() }
                            val isBackFocused by backSource.collectIsFocusedAsState()

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isBackFocused) TvAccentGold else TvSurface.copy(alpha = 0.85f))
                                    .border(1.dp, if (isBackFocused) TvAccentGold else TvBorder, CircleShape)
                                    .clickable(
                                        interactionSource = backSource,
                                        indication = null
                                    ) { onBack() }
                                    .focusable(interactionSource = backSource)
                                    .testTag("btn_player_back"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "رجوع",
                                    tint = if (isBackFocused) TvBackground else TvTextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Text(
                                text = currentTitle,
                                color = TvTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Top Right: Zoom & Speed selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val speedSource = remember { MutableInteractionSource() }
                            val isSpeedFocused by speedSource.collectIsFocusedAsState()

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSpeedFocused) TvAccentGold else TvSurface.copy(alpha = 0.85f))
                                    .border(1.dp, if (isSpeedFocused) TvAccentGold else TvBorder, RoundedCornerShape(8.dp))
                                    .clickable(
                                        interactionSource = speedSource,
                                        indication = null
                                    ) {
                                        currentSpeedIndex = (currentSpeedIndex + 1) % speedOptions.size
                                    }
                                    .focusable(interactionSource = speedSource)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("btn_player_speed"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = "سرعة التشغيل",
                                        tint = if (isSpeedFocused) TvBackground else TvAccentGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${speedOptions[currentSpeedIndex]}x",
                                        color = if (isSpeedFocused) TvBackground else TvTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            val zoomSource = remember { MutableInteractionSource() }
                            val isZoomFocused by zoomSource.collectIsFocusedAsState()

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isZoomFocused) TvAccentGold else TvSurface.copy(alpha = 0.85f))
                                    .border(1.dp, if (isZoomFocused) TvAccentGold else TvBorder, RoundedCornerShape(8.dp))
                                    .clickable(
                                        interactionSource = zoomSource,
                                        indication = null
                                    ) {
                                        currentResizeModeIndex = (currentResizeModeIndex + 1) % resizeModes.size
                                    }
                                    .focusable(interactionSource = zoomSource)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("btn_player_zoom"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AspectRatio,
                                        contentDescription = "تكبير الشاشة",
                                        tint = if (isZoomFocused) TvBackground else TvAccentGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = resizeModes[currentResizeModeIndex].label,
                                        color = if (isZoomFocused) TvBackground else TvTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Compact Dynamic Bottom Controls Bar for VOD
            AnimatedVisibility(
                visible = showBottomDetails,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xE60F141D))
                            .border(1.dp, Color(0xFF263345), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (durationMs > 0) {
                                var sliderPosition by remember { mutableFloatStateOf(0f) }
                                var isDragging by remember { mutableStateOf(false) }

                                val currentSec = (if (isDragging) (sliderPosition * durationMs / 1000).toLong() else currentPositionMs / 1000)
                                val totalSec = durationMs / 1000

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = formatDuration(currentSec),
                                        color = TvAccentGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Slider(
                                        value = if (isDragging) sliderPosition else (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
                                        onValueChange = {
                                            isDragging = true
                                            sliderPosition = it
                                        },
                                        onValueChangeFinished = {
                                            isDragging = false
                                            val targetMs = (sliderPosition * durationMs).toLong()
                                            exoPlayer.seekTo(targetMs)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(18.dp),
                                        colors = SliderDefaults.colors(
                                            thumbColor = TvAccentGold,
                                            activeTrackColor = TvAccentGold,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                        )
                                    )

                                    Text(
                                        text = formatDuration(totalSec),
                                        color = TvTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = currentTitle,
                                    color = TvTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PlayerControlIconButton(
                                        icon = Icons.Default.Replay10,
                                        contentDescription = "رجوع 10 ثواني",
                                        buttonSize = 32,
                                        iconSize = 18,
                                        onClick = {
                                            val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                                            exoPlayer.seekTo(newPos)
                                        }
                                    )

                                    val playBtnSource = remember { MutableInteractionSource() }
                                    val isPlayBtnFocused by playBtnSource.collectIsFocusedAsState()

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isPlayBtnFocused) Color(0xFFFFD54F) else TvAccentGold)
                                            .border(2.dp, if (isPlayBtnFocused) Color.White else Color.Transparent, CircleShape)
                                            .clickable(interactionSource = playBtnSource, indication = null) {
                                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                            }
                                            .focusable(interactionSource = playBtnSource)
                                            .testTag("btn_player_toggle_play"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                                            tint = TvBackground,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    PlayerControlIconButton(
                                        icon = Icons.Default.Forward10,
                                        contentDescription = "تقديم 10 ثواني",
                                        buttonSize = 32,
                                        iconSize = 18,
                                        onClick = {
                                            val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                                            exoPlayer.seekTo(newPos)
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DynamicAudioVisualizer(isPlaying = isPlaying)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicAudioVisualizer(isPlaying: Boolean, tint: Color = TvAccentGold) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        modifier = Modifier.height(16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(if (isPlaying) h1.dp else 4.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(tint)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(if (isPlaying) h2.dp else 7.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(tint)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(if (isPlaying) h3.dp else 5.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(tint)
        )
    }
}

@Composable
private fun PlayerControlIconButton(
    icon: ImageVector,
    contentDescription: String,
    buttonSize: Int = 34,
    iconSize: Int = 18,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .size(buttonSize.dp)
            .clip(CircleShape)
            .background(if (isFocused) TvAccentGold else TvSurface.copy(alpha = 0.85f))
            .border(1.dp, if (isFocused) Color.White else TvBorder, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) TvBackground else TvTextPrimary,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }
}
