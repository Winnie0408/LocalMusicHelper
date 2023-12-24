package com.hwinzniej.musichelper.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.YesNoDialog
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TextButton
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.lightSaltColors

class ProcessPage

@OptIn(UnstableSaltApi::class)
@Composable
fun ProcessPageUi() {
    val context = LocalContext.current
    var processAllScannedMusic by remember { mutableStateOf(false) }
    var overwriteOriginalTag by remember { mutableStateOf(false) }
    var showSelectTagTypeDialog by remember { mutableStateOf(false) }
    var enableAlbumArtist by remember { mutableStateOf(true) }
    var enableReleaseYear by remember { mutableStateOf(true) }
    var enableGenre by remember { mutableStateOf(true) }
    var enableTrackNumber by remember { mutableStateOf(true) }
    var showProgressBar by remember { mutableStateOf(false) }

    if (showSelectTagTypeDialog) {
        YesNoDialog(onNegative = { },
            onPositive = { },
            onDismiss = { },
            title = context.getString(R.string.select_needed_tag_type_dialog_title),
            content = "",
            noText = context.getString(R.string.cancel_button_text),
            yesText = context.getString(R.string.ok_button_text),
            onlyComposeView = true,
            customContent = {
                ItemSwitcher(
                    state = enableAlbumArtist, onChange = {
                        enableAlbumArtist = it
                    }, text = context.getString(R.string.album_artist_tag_name)
                )
                ItemSwitcher(
                    state = enableReleaseYear, onChange = {
                        enableReleaseYear = it
                    }, text = context.getString(R.string.release_year_tag_name)
                )
                ItemSwitcher(
                    state = enableGenre, onChange = {
                        enableGenre = it
                    }, text = context.getString(R.string.genre_tag_name)
                )
                ItemSwitcher(
                    state = enableTrackNumber, onChange = {
                        enableTrackNumber = it
                    }, text = context.getString(R.string.track_number_tag_name)
                )
            })
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {
        TitleBar(
            onBack = {},
            text = context.getString(R.string.process_function_name),
            showBackBtn = false
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(color = SaltTheme.colors.background)
                .verticalScroll(rememberScrollState())
        ) {
            RoundedColumn {
                ItemTitle(text = context.getString(R.string.process_control))
                ItemSwitcher(
                    state = processAllScannedMusic,
                    onChange = {
                        processAllScannedMusic = it
                    },
                    text = context.getString(R.string.process_all_scanned_music_switch_title),
                    sub = context.getString(R.string.process_all_scanned_music_switch_sub)
                )
                Item(
                    onClick = {

                    },
                    enabled = !processAllScannedMusic,
                    text = context.getString(R.string.select_needed_processing_music_directory_item_title),
                )
//                ItemSpacer()
                ItemSwitcher(
                    state = overwriteOriginalTag,
                    onChange = {
                        overwriteOriginalTag = it
                    },
                    text = context.getString(R.string.overwrite_original_tag_switch_title),
                    sub = context.getString(R.string.overwrite_original_tag_switch_sub)
                )
                Item(
                    onClick = {
                        showSelectTagTypeDialog = true
                    },
                    text = context.getString(R.string.select_needed_tag_type_item_title),
                )

                ItemContainer {
                    TextButton(onClick = {}, text = context.getString(R.string.start_text))
                }
            }

            RoundedColumn {
                ItemTitle(text = context.getString(R.string.processing_result))
//                ItemValue(
//                    text = context.getString(R.string.number_of_total_songs),
//                    sub = progressPercent.value.toString()
//                )

                ItemContainer {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .size((LocalConfiguration.current.screenHeightDp / 4).dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color = SaltTheme.colors.background)
                    ) {
                        item {
                            Box {
                                if (showProgressBar) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .zIndex(1f),
                                        color = SaltTheme.colors.highlight,
                                        trackColor = SaltTheme.colors.background
                                    )
                                }
                                Text(
                                    modifier = Modifier.padding(
                                        top = 3.dp, start = 7.dp, end = 7.dp
                                    ),
                                    text = "//TODO",
                                    fontSize = 16.sp,
                                    style = TextStyle(
                                        lineHeight = 1.5.em, color = SaltTheme.colors.subText
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    SaltTheme(
        colors = lightSaltColors()
    ) {
        ProcessPageUi()
    }
}