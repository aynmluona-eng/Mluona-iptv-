package com.example.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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

/**
 * Universal Button supporting both TV Remote (D-Pad & Enter) and Touch Screen clicks.
 */
@Composable
fun TvTouchButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = false,
    isLoading: Boolean = false,
    testTag: String = "tv_touch_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bg = when {
        isFocused -> TvAccentGold
        isPrimary -> TvAccentGold.copy(alpha = 0.85f)
        else -> TvSurfaceHighlight
    }

    val contentColor = when {
        isFocused -> TvBackground
        isPrimary -> TvBackground
        else -> TvTextPrimary
    }

    val borderCol = when {
        isFocused -> Color.White
        isPrimary -> TvAccentGold
        else -> TvBorder
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderCol,
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (!isLoading) onClick()
            }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_BUTTON_A -> {
                            if (!isLoading) onClick()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .padding(horizontal = 20.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Universal Input field supporting TV remote typing/focus and touch keyboard interaction.
 */
@Composable
fun TvTouchInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier,
    testTag: String = "tv_input_field"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = if (isFocused) TvAccentGold else TvBorder
    val bgColor = if (isFocused) TvSurfaceHighlight else TvSurface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(
                color = TvTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            ),
            cursorBrush = SolidColor(TvAccentGold),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder.ifEmpty { label },
                            color = TvTextMuted,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
