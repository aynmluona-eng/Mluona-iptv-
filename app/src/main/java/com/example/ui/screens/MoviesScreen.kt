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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.VodCategory
import com.example.data.model.VodMovie
import com.example.ui.theme.TvAccentGold
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvBorder
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceHighlight
import com.example.ui.theme.TvTextMuted
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.viewmodel.IptvViewModel

@Composable
fun MoviesScreen(
    viewModel: IptvViewModel,
    onSelectMovie: (movie: VodMovie) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val categories by viewModel.vodCategories.collectAsState()
    val movies by viewModel.vodMovies.collectAsState()
    val isVodLoading by viewModel.isVodLoading.collectAsState()
    val selectedCategory by viewModel.selectedVodCategory.collectAsState()

    // Load VOD categories and initial category on-demand
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadVodContentIfNeeded()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground)
            .padding(horizontal = 32.dp, vertical = 20.dp)
            .testTag("movies_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(TvSurfaceHighlight)
                            .clickable { onBack() }
                            .testTag("btn_back_from_movies"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TvTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Movies (VOD)",
                        color = TvTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${movies.size} Movies",
                    color = TvTextSecondary,
                    fontSize = 14.sp
                )
            }

            // Layout: Category Sidebar + Movie Poster Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Categories List (Left) - Fast per category, starting directly with first category
                Box(
                    modifier = Modifier
                        .width(210.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TvSurface)
                        .border(1.dp, TvBorder, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(categories, key = { it.categoryId }) { cat ->
                            val isSelected = selectedCategory?.categoryId == cat.categoryId
                            val catInteraction = remember { MutableInteractionSource() }
                            val isCatFocused by catInteraction.collectIsFocusedAsState()

                            val isFavCat = cat.categoryId == IptvViewModel.ID_FAVORITES
                            val isRecentCat = cat.categoryId == IptvViewModel.ID_RECENTS

                            val bg = when {
                                isCatFocused -> TvAccentGold
                                isSelected -> TvSurfaceHighlight
                                else -> Color.Transparent
                            }
                            val contentColor = when {
                                isCatFocused -> TvBackground
                                isSelected -> TvAccentGold
                                isFavCat || isRecentCat -> TvAccentGold.copy(alpha = 0.9f)
                                else -> TvTextSecondary
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .border(
                                        width = if (isCatFocused) 2.dp else 0.dp,
                                        color = if (isCatFocused) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .focusable(interactionSource = catInteraction)
                                    .clickable(
                                        interactionSource = catInteraction,
                                        indication = null
                                    ) { viewModel.selectVodCategory(cat) }
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                            when (keyEvent.nativeKeyEvent.keyCode) {
                                                KeyEvent.KEYCODE_DPAD_CENTER,
                                                KeyEvent.KEYCODE_ENTER -> {
                                                    viewModel.selectVodCategory(cat)
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isFavCat) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = contentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else if (isRecentCat) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = contentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = cat.categoryName,
                                        color = contentColor,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCatFocused || isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Poster Grid (Right)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TvSurface)
                        .border(1.dp, TvBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    if (isVodLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TvAccentGold, modifier = Modifier.size(36.dp))
                        }
                    } else if (movies.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = TvTextMuted,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "No movies available in this category",
                                    color = TvTextMuted,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 130.dp),
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(movies, key = { it.streamId }) { movie ->
                                MoviePosterCard(
                                    movie = movie,
                                    onClick = {
                                        viewModel.markMovieWatched(movie)
                                        onSelectMovie(movie)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoviePosterCard(
    movie: VodMovie,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) TvSurfaceHighlight else TvBackground)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) TvAccentGold else TvBorder,
                shape = RoundedCornerShape(8.dp)
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
                        KeyEvent.KEYCODE_ENTER -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(Color(0xFF1E2433)),
                contentAlignment = Alignment.Center
            ) {
                if (!movie.streamIcon.isNullOrBlank()) {
                    AsyncImage(
                        model = movie.streamIcon,
                        contentDescription = movie.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = TvTextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                }

                if (!movie.rating.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = TvAccentGold,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = movie.rating ?: "",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Text(
                text = movie.name,
                color = if (isFocused) TvAccentGold else TvTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}
