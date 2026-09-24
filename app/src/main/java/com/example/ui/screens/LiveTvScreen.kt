package com.example.ui.screens

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.LiveCategory
import com.example.data.model.LiveChannel
import com.example.ui.components.TvTouchButton
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
import kotlinx.coroutines.launch

@Composable
fun LiveTvScreen(
    viewModel: IptvViewModel,
    onPlayChannel: (channel: LiveChannel) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val categories by viewModel.liveCategories.collectAsState()
    val selectedCategory by viewModel.selectedLiveCategory.collectAsState()
    val channels by viewModel.liveChannels.collectAsState()
    val selectedChannel by viewModel.selectedLiveChannel.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLiveChannelsLoading by viewModel.isLiveChannelsLoading.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()
    val streamFormat by viewModel.streamFormat.collectAsState()
    val strings by viewModel.appText.collectAsState()

    // Dialog States
    var showRenameDialog by remember { mutableStateOf(false) }
    var channelToRename by remember { mutableStateOf<LiveChannel?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var showEpgDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val categoriesListState = rememberLazyListState()
    val channelsListState = rememberLazyListState()

    val categoriesFocusRequester = remember { FocusRequester() }
    val channelsFocusRequester = remember { FocusRequester() }

    // Auto-select first category if none selected
    LaunchedEffect(categories) {
        if (selectedCategory == null && categories.isNotEmpty()) {
            viewModel.selectLiveCategory(categories.first())
        }
    }

    // Key Interceptor for Remote Control Actions (Colors & Navigation)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val code = keyEvent.nativeKeyEvent.keyCode
                    when (code) {
                        // RED KEY: Toggle Favorite on current channel
                        KeyEvent.KEYCODE_PROG_RED, 183, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_F1 -> {
                            selectedChannel?.let { viewModel.toggleChannelFavorite(it) }
                            true
                        }
                        // GREEN KEY: Focus categories sidebar
                        KeyEvent.KEYCODE_PROG_GREEN, 184, KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.KEYCODE_F2 -> {
                            try {
                                categoriesFocusRequester.requestFocus()
                            } catch (_: Exception) {}
                            true
                        }
                        // YELLOW KEY: Edit channel name
                        KeyEvent.KEYCODE_PROG_YELLOW, 185, KeyEvent.KEYCODE_F3 -> {
                            selectedChannel?.let { ch ->
                                channelToRename = ch
                                renameInputText = viewModel.getChannelDisplayName(ch)
                                showRenameDialog = true
                            }
                            true
                        }
                        // BLUE KEY: Show EPG page
                        KeyEvent.KEYCODE_PROG_BLUE, 186, KeyEvent.KEYCODE_F4 -> {
                            if (selectedChannel != null) {
                                showEpgDialog = true
                            }
                            true
                        }
                        // BACK / ESCAPE
                        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                            onBack()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .testTag("live_tv_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ================================================================
            // TOP BAR: Only "Live tv" and Back Button (Clean, No Clutter)
            // ================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val backSource = remember { MutableInteractionSource() }
                    val isBackFocused by backSource.collectIsFocusedAsState()

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isBackFocused) TvAccentGold else TvSurfaceHighlight)
                            .border(1.dp, if (isBackFocused) Color.White else TvBorder, CircleShape)
                            .clickable(interactionSource = backSource, indication = null) { onBack() }
                            .focusable(interactionSource = backSource)
                            .testTag("btn_back_live_tv"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                            tint = if (isBackFocused) TvBackground else TvTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Clean Title: ONLY "Live tv"
                    Text(
                        text = strings.liveTv,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                if (selectedCategory != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TvSurfaceHighlight)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = selectedCategory?.categoryName ?: "",
                            color = TvAccentGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ================================================================
            // MAIN 3-PANE LAYOUT: Lateral Categories Sidebar + Channels + Preview
            // ================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // PANE 1: LATERAL CATEGORIES SIDEBAR (القوائم تكون بشكل جانبي)
                Box(
                    modifier = Modifier
                        .width(230.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(TvSurface)
                        .border(1.dp, TvBorder, RoundedCornerShape(14.dp))
                        .padding(8.dp)
                        .testTag("live_categories_sidebar")
                ) {
                    if (categories.isEmpty() && isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TvAccentGold, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else if (categories.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = strings.noCategories,
                                color = TvTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            state = categoriesListState,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(categories, key = { _, cat -> cat.categoryId }) { index, cat ->
                                val isSelected = selectedCategory?.categoryId == cat.categoryId
                                SidebarCategoryItem(
                                    category = cat,
                                    isSelected = isSelected,
                                    modifier = if (index == 0) Modifier.focusRequester(categoriesFocusRequester) else Modifier,
                                    onSelect = {
                                        viewModel.selectLiveCategory(cat)
                                    },
                                    onNavigateRight = {
                                        viewModel.selectLiveCategory(cat)
                                        try {
                                            channelsFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    }
                                )
                            }
                        }
                    }
                }

                // PANE 2: CHANNELS LIST
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(TvSurface)
                        .border(1.dp, TvBorder, RoundedCornerShape(14.dp))
                        .padding(8.dp)
                        .testTag("live_channels_list")
                ) {
                    if (isLiveChannelsLoading && channels.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TvAccentGold, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                        }
                    } else if (channels.isEmpty() && !isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = errorMsg ?: strings.noChannels,
                                color = TvTextMuted,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            state = channelsListState,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(channels, key = { _, it -> it.streamId }) { index, ch ->
                                val isSelected = selectedChannel?.streamId == ch.streamId
                                val isChFav = viewModel.isChannelFavorite(ch.streamId)
                                val displayName = viewModel.getChannelDisplayName(ch)

                                ChannelItem(
                                    channel = ch,
                                    displayName = displayName,
                                    isSelected = isSelected,
                                    isFavorite = isChFav,
                                    modifier = if (index == 0) Modifier.focusRequester(channelsFocusRequester) else Modifier,
                                    onFocus = { viewModel.selectLiveChannel(ch) },
                                    onClick = {
                                        viewModel.selectLiveChannel(ch)
                                        onPlayChannel(ch)
                                    },
                                    onDpadLeft = {
                                        try {
                                            categoriesFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    }
                                )
                            }
                        }
                    }
                }

                // PANE 3: CHANNEL DETAILS & ACTIONS
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(TvSurface)
                        .border(1.dp, TvBorder, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                        .testTag("live_channel_preview_pane")
                ) {
                    if (selectedChannel != null) {
                        val ch = selectedChannel!!
                        val displayName = viewModel.getChannelDisplayName(ch)
                        val isChannelFav = viewModel.isChannelFavorite(ch.streamId)

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Channel Header Card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(TvSurfaceHighlight)
                                        .border(1.dp, TvBorder, RoundedCornerShape(10.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!ch.streamIcon.isNullOrBlank()) {
                                            AsyncImage(
                                                model = ch.streamIcon,
                                                contentDescription = displayName,
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF222B3D)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LiveTv,
                                                    contentDescription = null,
                                                    tint = TvAccentGold,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = displayName,
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                BadgeText("HD")
                                                BadgeText(streamFormat.uppercase())
                                                BadgeText("LIVE")
                                            }
                                        }
                                    }
                                }

                                // Program Info
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = strings.nowPlaying,
                                        color = TvAccentGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = displayName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${ch.num ?: ""} • ${streamFormat.uppercase()} stream",
                                        color = TvTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                // Timeline Bar
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    LinearProgressIndicator(
                                        progress = { 0.5f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = TvAccentGold,
                                        trackColor = TvSurfaceHighlight,
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "LIVE", color = TvAccentGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "ID: ${ch.streamId}", color = TvTextMuted, fontSize = 10.sp)
                                    }
                                }
                            }

                            // Action buttons
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                TvTouchButton(
                                    text = strings.playChannel,
                                    icon = Icons.Default.PlayArrow,
                                    isPrimary = true,
                                    onClick = { onPlayChannel(ch) },
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "btn_play_channel"
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TvTouchButton(
                                        text = if (isChannelFav) strings.favorite else strings.addToFavorite,
                                        icon = Icons.Default.Star,
                                        isPrimary = false,
                                        onClick = { viewModel.toggleChannelFavorite(ch) },
                                        modifier = Modifier.weight(1f),
                                        testTag = "btn_fav_channel"
                                    )
                                    TvTouchButton(
                                        text = strings.renameChannel,
                                        icon = Icons.Default.Edit,
                                        isPrimary = false,
                                        onClick = {
                                            channelToRename = ch
                                            renameInputText = displayName
                                            showRenameDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        testTag = "btn_rename_channel"
                                    )
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = strings.selectChannelPrompt,
                                color = TvTextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // ================================================================
            // COLOR BADGES BAR (رموز أزرار التحكم عن بعد)
            // ================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF131822))
                    .border(1.dp, Color(0xFF202A38), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RemoteColorButton(
                    color = Color(0xFFE53935),
                    label = strings.redKeyFav,
                    onClick = {
                        selectedChannel?.let { viewModel.toggleChannelFavorite(it) }
                    }
                )

                RemoteColorButton(
                    color = Color(0xFF43A047),
                    label = strings.greenKeyCat,
                    onClick = {
                        try {
                            categoriesFocusRequester.requestFocus()
                        } catch (_: Exception) {}
                    }
                )

                RemoteColorButton(
                    color = Color(0xFFF59E0B),
                    label = strings.yellowKeyRename,
                    onClick = {
                        selectedChannel?.let { ch ->
                            channelToRename = ch
                            renameInputText = viewModel.getChannelDisplayName(ch)
                            showRenameDialog = true
                        }
                    }
                )

                RemoteColorButton(
                    color = Color(0xFF1E88E5),
                    label = strings.blueKeyEpg,
                    onClick = {
                        if (selectedChannel != null) {
                            showEpgDialog = true
                        }
                    }
                )
            }
        }

        // ================================================================
        // DIALOG 1: RENAME CHANNEL
        // ================================================================
        if (showRenameDialog && channelToRename != null) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = {
                    Text(
                        text = strings.editChannelTitle,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = strings.enterNewName,
                            color = TvTextSecondary,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = renameInputText,
                            onValueChange = { renameInputText = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TvTextPrimary,
                                unfocusedTextColor = TvTextPrimary,
                                focusedBorderColor = TvAccentGold,
                                unfocusedBorderColor = TvBorder,
                                cursorColor = TvAccentGold
                            ),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TvTouchButton(
                        text = strings.save,
                        isPrimary = true,
                        onClick = {
                            viewModel.renameLiveChannel(channelToRename!!, renameInputText)
                            showRenameDialog = false
                        }
                    )
                },
                dismissButton = {
                    TvTouchButton(
                        text = strings.cancel,
                        isPrimary = false,
                        onClick = { showRenameDialog = false }
                    )
                },
                containerColor = TvSurface,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // ================================================================
        // DIALOG 2: EPG PAGE
        // ================================================================
        if (showEpgDialog && selectedChannel != null) {
            val ch = selectedChannel!!
            val displayName = viewModel.getChannelDisplayName(ch)

            AlertDialog(
                onDismissRequest = { showEpgDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.epgGuide} • $displayName",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E88E5))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "EPG LIVE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TvSurfaceHighlight)
                                .border(1.dp, TvBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = strings.nowPlaying,
                                    color = TvAccentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = displayName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "Schedule:",
                            color = TvTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            EpgScheduleRow(time = "NOW", title = displayName, isCurrent = true)
                            EpgScheduleRow(time = "+30m", title = "$displayName - News / Live Feed", isCurrent = false)
                            EpgScheduleRow(time = "+1h", title = "$displayName - Main Feature", isCurrent = false)
                        }
                    }
                },
                confirmButton = {
                    TvTouchButton(
                        text = strings.cancel,
                        isPrimary = true,
                        onClick = { showEpgDialog = false }
                    )
                },
                containerColor = TvSurface,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun SidebarCategoryItem(
    category: LiveCategory,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
    onNavigateRight: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    val isFav = category.categoryId == IptvViewModel.ID_FAVORITES
    val isRecent = category.categoryId == IptvViewModel.ID_RECENTS

    val bg = when {
        isFocused -> TvAccentGold
        isSelected -> TvSurfaceHighlight
        else -> Color.Transparent
    }

    val textColor = when {
        isFocused -> TvBackground
        isSelected -> TvAccentGold
        isFav || isRecent -> TvAccentGold
        else -> TvTextPrimary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = if (isFocused) 1.8.dp else if (isSelected) 1.dp else 0.dp,
                color = if (isFocused) Color.White else if (isSelected) Color(0xFF2E3D4F) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(interactionSource = interaction, indication = null) {
                onSelect()
            }
            .focusable(interactionSource = interaction)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER -> {
                            onSelect()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            onNavigateRight()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = when {
                    isFav -> Icons.Default.Favorite
                    isRecent -> Icons.Default.History
                    else -> Icons.Default.Tv
                },
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = category.categoryName,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EpgScheduleRow(time: String, title: String, isCurrent: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isCurrent) TvSurfaceHighlight else Color(0xFF131822))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = if (isCurrent) TvAccentGold else TvTextPrimary,
            fontSize = 12.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = time,
            color = TvTextMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun RemoteColorButton(
    color: Color,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isFocused) color.copy(alpha = 0.35f) else Color.Transparent)
            .border(
                width = if (isFocused) 1.5.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                color = if (isFocused) Color.White else TvTextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun ChannelItem(
    channel: LiveChannel,
    displayName: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onFocus: () -> Unit = {},
    onClick: () -> Unit,
    onDpadLeft: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        if (isFocused) {
            delay(120)
            onFocus()
        }
    }

    val bg = when {
        isFocused -> TvAccentGold
        isSelected -> TvSurfaceHighlight
        else -> Color(0xFF131822)
    }

    val textColor = when {
        isFocused -> TvBackground
        isSelected -> TvAccentGold
        else -> TvTextPrimary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = if (isFocused) 1.8.dp else 1.dp,
                color = if (isFocused) Color.White else if (isSelected) Color(0xFF283545) else TvBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_BUTTON_A -> {
                            onClick()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            onDpadLeft()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (!channel.streamIcon.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.streamIcon,
                        contentDescription = displayName,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF222B3D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = if (isFocused) TvBackground else TvAccentGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = displayName,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = if (isFocused) TvBackground else TvAccentGold,
                        modifier = Modifier.size(13.dp)
                    )
                }
                if (isSelected && !isFocused) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Active",
                        tint = TvAccentGold,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BadgeText(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(TvBackground)
            .border(1.dp, TvBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = TvTextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
