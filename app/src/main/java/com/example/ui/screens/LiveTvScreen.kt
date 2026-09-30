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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.LiveCategory
import com.example.data.model.LiveChannel
import com.example.ui.components.TvTouchButton
import com.example.ui.viewmodel.IptvViewModel

private val ColorThemeNeonGreen = Color(0xFF00E676)
private val ColorThemeDarkBg = Color(0xFF050B08)
private val ColorThemeSurface = Color(0xFF0C1611)
private val ColorThemeSurfaceFocused = Color(0xFF0A2618)
private val ColorThemeBorder = Color(0xFF14271E)

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
    val strings by viewModel.appText.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val categoriesListState = rememberLazyListState()
    val channelsListState = rememberLazyListState()

    val backFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    val categoriesFocusRequester = remember { FocusRequester() }
    val channelsFocusRequester = remember { FocusRequester() }

    // Auto-select first category if none selected
    LaunchedEffect(categories) {
        if (selectedCategory == null && categories.isNotEmpty()) {
            viewModel.selectLiveCategory(categories.first())
        }
    }

    val displayedChannels = remember(channels, searchQuery) {
        if (searchQuery.isBlank()) {
            channels
        } else {
            channels.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorThemeDarkBg)
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .testTag("live_tv_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ========================================================
            // PANE 1: LEFT SIDEBAR (Larger Categories & Controls)
            // ========================================================
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Back Button: "← Back" (Enlarged)
                val backInteraction = remember { MutableInteractionSource() }
                val isBackFocused by backInteraction.collectIsFocusedAsState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .focusRequester(backFocusRequester)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isBackFocused) ColorThemeNeonGreen else ColorThemeSurface)
                        .border(
                            1.4.dp,
                            if (isBackFocused) Color.White else ColorThemeBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .focusable(interactionSource = backInteraction)
                        .clickable(interactionSource = backInteraction, indication = null) { onBack() }
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                when (keyEvent.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_CENTER,
                                    KeyEvent.KEYCODE_ENTER,
                                    KeyEvent.KEYCODE_BUTTON_A -> {
                                        onBack()
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                                        searchFocusRequester.requestFocus()
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        channelsFocusRequester.requestFocus()
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isBackFocused) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Back",
                            color = if (isBackFocused) Color.Black else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Search Channel Input (Enlarged)
                val searchInteraction = remember { MutableInteractionSource() }
                val isSearchFocused by searchInteraction.collectIsFocusedAsState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .focusRequester(searchFocusRequester)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ColorThemeSurface)
                        .border(
                            1.4.dp,
                            if (isSearchFocused) ColorThemeNeonGreen else ColorThemeBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchFocused) ColorThemeNeonGreen else Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search channel...",
                                    color = Color(0xFF6B7280),
                                    fontSize = 15.sp
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(ColorThemeNeonGreen),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Categories Vertical List (Enlarged Cards)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (categories.isEmpty() && isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ColorThemeNeonGreen, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                        }
                    } else {
                        LazyColumn(
                            state = categoriesListState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(categories, key = { _, cat -> cat.categoryId }) { index, cat ->
                                val isSelected = selectedCategory?.categoryId == cat.categoryId
                                SidebarCategoryCardEnlarged(
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
            }

            // ========================================================
            // PANE 2: MIDDLE CHANNELS LIST (Enlarged Channel Cards)
            // ========================================================
            Column(
                modifier = Modifier
                    .weight(1.4f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Header: Green Vertical Bar + Category Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(ColorThemeNeonGreen)
                    )
                    Text(
                        text = selectedCategory?.categoryName ?: "All Channels",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${displayedChannels.size} channels",
                        color = ColorThemeNeonGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Channels LazyColumn (Enlarged Cards)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (isLiveChannelsLoading && displayedChannels.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ColorThemeNeonGreen, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                        }
                    } else if (displayedChannels.isEmpty() && !isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = errorMsg ?: strings.noChannels,
                                color = Color(0xFF6B7280),
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            state = channelsListState,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(displayedChannels, key = { _, it -> it.streamId }) { index, ch ->
                                val isSelected = selectedChannel?.streamId == ch.streamId
                                val isFav = viewModel.isChannelFavorite(ch.streamId)
                                val displayName = viewModel.getChannelDisplayName(ch)

                                ChannelRowCardEnlarged(
                                    channel = ch,
                                    displayName = displayName,
                                    isSelected = isSelected,
                                    isFavorite = isFav,
                                    modifier = if (index == 0) Modifier.focusRequester(channelsFocusRequester) else Modifier,
                                    onFocus = { viewModel.selectLiveChannel(ch) },
                                    onClick = {
                                        viewModel.selectLiveChannel(ch)
                                        onPlayChannel(ch)
                                    },
                                    onToggleFav = { viewModel.toggleChannelFavorite(ch) },
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
            }

            // ========================================================
            // PANE 3: RIGHT CHANNEL DETAILS & EPG INFO
            // ========================================================
            Box(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(ColorThemeSurface)
                    .border(1.2.dp, ColorThemeBorder, RoundedCornerShape(18.dp))
                    .padding(22.dp)
                    .testTag("live_channel_preview_pane")
            ) {
                if (selectedChannel != null) {
                    val ch = selectedChannel!!
                    val displayName = viewModel.getChannelDisplayName(ch)
                    val isFav = viewModel.isChannelFavorite(ch.streamId)

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            // Large Channel Title
                            Text(
                                text = displayName,
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Program Schedule / EPG Text
                            Text(
                                text = "Sem programação para este canal.",
                                color = Color(0xFF90A4AE),
                                fontSize = 16.sp,
                                lineHeight = 22.sp
                            )
                        }

                        // Bottom Actions: Play Channel Button
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            TvTouchButton(
                                text = strings.playChannel,
                                icon = Icons.Default.PlayArrow,
                                isPrimary = true,
                                onClick = { onPlayChannel(ch) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                testTag = "btn_play_channel"
                            )

                            TvTouchButton(
                                text = if (isFav) strings.favorite else strings.addToFavorite,
                                icon = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                isPrimary = false,
                                onClick = { viewModel.toggleChannelFavorite(ch) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                testTag = "btn_fav_channel"
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = strings.selectChannelPrompt,
                            color = Color(0xFF6B7280),
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enlarged Category Item Card (Height 56dp, Font 16sp)
 */
@Composable
private fun SidebarCategoryCardEnlarged(
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

    val active = isFocused || isSelected

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) ColorThemeSurfaceFocused else ColorThemeSurface)
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.4.dp else 1.dp,
                color = if (active) ColorThemeNeonGreen else ColorThemeBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(interactionSource = interaction, indication = null) { onSelect() }
            .focusable(interactionSource = interaction)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_BUTTON_A -> {
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
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (isFav) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = ColorThemeNeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (isRecent) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = ColorThemeNeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = category.categoryName,
                    color = if (active) Color.White else Color(0xFFD1D5DB),
                    fontSize = 16.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Enlarged Channel Row Card (Height 74dp, Logo 54dp, Font 16sp)
 */
@Composable
private fun ChannelRowCardEnlarged(
    channel: LiveChannel,
    displayName: String,
    isSelected: Boolean,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    onToggleFav: () -> Unit,
    onDpadLeft: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    val active = isFocused || isSelected

    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) ColorThemeSurfaceFocused else ColorThemeSurface)
            .border(
                width = if (active) 2.2.dp else 1.dp,
                color = if (active) ColorThemeNeonGreen else ColorThemeBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .focusable(interactionSource = interaction)
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
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Large Logo + Number & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Channel Square Logo (54dp x 54dp)
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F1E16)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!channel.streamIcon.isNullOrBlank()) {
                        AsyncImage(
                            model = channel.streamIcon,
                            contentDescription = displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = ColorThemeNeonGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Number + Channel Name
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "#${channel.num ?: channel.streamId}",
                        color = ColorThemeNeonGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right: Heart Icon for Favorite
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onToggleFav() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) ColorThemeNeonGreen else Color(0xFF6B7280),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
