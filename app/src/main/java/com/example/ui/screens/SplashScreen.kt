package com.example.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.MluonaLogo
import com.example.ui.theme.TvBackground
import kotlinx.coroutines.delay

/**
 * TV Splash screen displaying strictly the modern minimalist monogram logo.
 * NO TEXT is shown, complying exactly with user requirements.
 * Fully supports Android TV remote D-Pad navigation and key events.
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        // Seamless smooth TV splash pause then proceed to TV Portal
        delay(2200)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground)
            .testTag("splash_screen_root")
            .focusRequester(focusRequester)
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onSplashComplete()
            }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_BUTTON_A,
                        KeyEvent.KEYCODE_SPACE -> {
                            onSplashComplete()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Logo ONLY - NO TEXT
        MluonaLogo(
            size = 190.dp,
            animated = true
        )
    }
}
