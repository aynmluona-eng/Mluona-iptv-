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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

@Composable
fun TvSettingsScreen(
    viewModel: IptvViewModel,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val currentLang by viewModel.currentLanguage.collectAsState()
    val streamFormat by viewModel.streamFormat.collectAsState()
    val audioType by viewModel.audioType.collectAsState()
    val strings by viewModel.appText.collectAsState()

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground)
            .padding(horizontal = 40.dp, vertical = 24.dp)
            .testTag("tv_settings_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header: Back Button & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val backInteraction = remember { MutableInteractionSource() }
                val isBackFocused by backInteraction.collectIsFocusedAsState()

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isBackFocused) TvAccentGold else TvSurfaceHighlight)
                        .border(1.dp, if (isBackFocused) Color.White else TvBorder, CircleShape)
                        .clickable(interactionSource = backInteraction, indication = null) { onBack() }
                        .focusable(interactionSource = backInteraction)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                 keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)
                            ) {
                                onBack()
                                true
                            } else false
                        }
                        .testTag("btn_back_settings"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = if (isBackFocused) TvBackground else TvTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = strings.settings,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.simpleSettingsDesc,
                        color = TvTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // 1. Language Selection Card (اختيار تغيير لغات: الإنجليزية و الإسبانية + دعم كامل للتطبيق)
            SettingsSectionCard(
                icon = Icons.Default.Language,
                title = strings.language,
                testTag = "section_language"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingOptionPill(
                        label = strings.english,
                        isSelected = currentLang == "en",
                        modifier = Modifier.weight(1f),
                        testTag = "opt_lang_en",
                        onClick = { viewModel.setLanguage("en") }
                    )
                    SettingOptionPill(
                        label = strings.spanish,
                        isSelected = currentLang == "es",
                        modifier = Modifier.weight(1f),
                        testTag = "opt_lang_es",
                        onClick = { viewModel.setLanguage("es") }
                    )
                    SettingOptionPill(
                        label = strings.arabic,
                        isSelected = currentLang == "ar",
                        modifier = Modifier.weight(1f),
                        testTag = "opt_lang_ar",
                        onClick = { viewModel.setLanguage("ar") }
                    )
                }
            }

            // 2. Stream Format Selection Card (تغيير صيغة البث: HLS .m3u8 أو MPEG-TS .ts)
            SettingsSectionCard(
                icon = Icons.Default.LiveTv,
                title = strings.streamFormat,
                testTag = "section_stream_format"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingOptionPill(
                        label = strings.formatHls,
                        isSelected = streamFormat == "m3u8",
                        modifier = Modifier.weight(1f),
                        testTag = "opt_format_m3u8",
                        onClick = { viewModel.setStreamFormat("m3u8") }
                    )
                    SettingOptionPill(
                        label = strings.formatTs,
                        isSelected = streamFormat == "ts",
                        modifier = Modifier.weight(1f),
                        testTag = "opt_format_ts",
                        onClick = { viewModel.setStreamFormat("ts") }
                    )
                }
            }

            // 3. Audio Type Selection Card (نوع الصوت)
            SettingsSectionCard(
                icon = Icons.Default.Audiotrack,
                title = strings.audioType,
                testTag = "section_audio_type"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingOptionPill(
                        label = strings.audioAuto,
                        isSelected = audioType == "auto",
                        modifier = Modifier.weight(1f),
                        testTag = "opt_audio_auto",
                        onClick = { viewModel.setAudioType("auto") }
                    )
                    SettingOptionPill(
                        label = strings.audioStereo,
                        isSelected = audioType == "stereo",
                        modifier = Modifier.weight(1f),
                        testTag = "opt_audio_stereo",
                        onClick = { viewModel.setAudioType("stereo") }
                    )
                    SettingOptionPill(
                        label = strings.audioSurround,
                        isSelected = audioType == "surround",
                        modifier = Modifier.weight(1f),
                        testTag = "opt_audio_surround",
                        onClick = { viewModel.setAudioType("surround") }
                    )
                    SettingOptionPill(
                        label = strings.audioHardware,
                        isSelected = audioType == "hardware",
                        modifier = Modifier.weight(1f),
                        testTag = "opt_audio_hardware",
                        onClick = { viewModel.setAudioType("hardware") }
                    )
                }
            }

            // 4. App Version Card (وفي الأخير نوع الإصدار التطبيق 1.0)
            SettingsSectionCard(
                icon = Icons.Default.Info,
                title = strings.versionTitle,
                testTag = "section_app_version"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10151C))
                        .border(1.dp, Color(0xFF1E2835), RoundedCornerShape(12.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Mluona IPTV",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Android TV • Ultra Low Latency Engine",
                            color = TvTextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TvAccentGold)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "v${strings.appVersion}",
                            color = TvBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    testTag: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TvSurface)
            .border(1.dp, TvBorder, RoundedCornerShape(16.dp))
            .padding(18.dp)
            .testTag(testTag)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(TvSurfaceHighlight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TvAccentGold,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            content()
        }
    }
}

@Composable
private fun SettingOptionPill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    val active = isFocused || isSelected
    val bgColor = if (isSelected) Color(0xFF1B2F38) else if (isFocused) Color(0xFF1E2833) else Color(0xFF131920)
    val borderColor = if (isFocused) TvAccentGold else if (isSelected) Color(0xFF2DD4BF) else Color(0xFF232D38)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (active) 1.8.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .focusable(interactionSource = interaction)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                    (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                     keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER ||
                     keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BUTTON_A)
                ) {
                    onClick()
                    true
                } else false
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2DD4BF),
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                color = if (isSelected) Color.White else if (isFocused) Color.White else TvTextSecondary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
