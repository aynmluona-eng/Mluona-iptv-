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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MluonaLogo
import com.example.ui.theme.TvAccentGold
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvBorder
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceHighlight
import com.example.ui.theme.TvTextMuted
import com.example.ui.theme.TvTextPrimary
import com.example.ui.viewmodel.IptvViewModel

/**
 * TV Portal Screen supporting remote control and touch.
 * Displays connection options and quick active session entry.
 */
@Composable
fun TvPortalScreen(
    viewModel: IptvViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToXtreamLogin: () -> Unit,
    onNavigateToM3u: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onBackToSplash: () -> Unit
) {
    BackHandler {
        onBackToSplash()
    }

    val activeAccount by viewModel.activeAccount.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground)
            .padding(horizontal = 48.dp, vertical = 28.dp)
            .testTag("tv_portal_screen"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(680.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(TvSurface)
                .border(1.dp, TvBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 40.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Top Monogram Logo
                MluonaLogo(
                    size = 72.dp,
                    animated = false
                )

                // Active Account Banner if available
                if (activeAccount != null) {
                    TvRemoteButton(
                        text = "Continue with: ${activeAccount?.name} (${activeAccount?.status})",
                        isHighlighted = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_continue_active"),
                        onClick = onNavigateToDashboard
                    )
                }

                // Primary 2x2 Remote Action Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TvRemoteButton(
                        text = "Load Your Playlist Or File/URL",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_load_playlist"),
                        onClick = onNavigateToM3u
                    )
                    TvRemoteButton(
                        text = "Load Your Data From Device",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_load_device"),
                        onClick = onNavigateToM3u
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TvRemoteButton(
                        text = "Login with Xtream Codes API",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_xtream_login"),
                        isHighlighted = activeAccount == null,
                        onClick = onNavigateToXtreamLogin
                    )
                    TvRemoteButton(
                        text = "Play Single Stream",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_single_stream"),
                        onClick = onNavigateToM3u
                    )
                }

                // Centered Secondary Button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TvRemoteButton(
                        text = "List Users / Switch Account",
                        modifier = Modifier
                            .width(260.dp)
                            .testTag("btn_list_users"),
                        onClick = onNavigateToUsers
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Press OK or Tap to select • Remote & Touch Supported",
                    fontSize = 12.sp,
                    color = TvTextMuted,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun TvRemoteButton(
    text: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val backgroundColor = when {
        isFocused -> TvAccentGold
        isHighlighted -> TvSurfaceHighlight
        else -> Color(0xFF1E2433)
    }

    val textColor = when {
        isFocused -> TvBackground
        isHighlighted -> TvAccentGold
        else -> TvTextPrimary
    }

    val borderColor = when {
        isFocused -> Color.White
        isHighlighted -> TvAccentGold.copy(alpha = 0.6f)
        else -> TvBorder
    }

    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
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
                        else -> false
                    }
                } else {
                    false
                }
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isFocused || isHighlighted) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
