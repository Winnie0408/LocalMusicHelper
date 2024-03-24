package com.hwinzniej.musichelper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.activity.TagPage
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(UnstableSaltApi::class)
@Composable
fun TagPageUi(
    tagPage: TagPage,
    enableHaptic: MutableState<Boolean>,
) {
    val songList = remember { mutableStateMapOf<Int, Array<String>>() }
    var showLoadingProgressBar by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val musicInfo = remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    var showSongInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (songList.isEmpty()) {  // TODO 刷新操作
            coroutineScope.launch(Dispatchers.IO) {
                showLoadingProgressBar = true
                tagPage.getMusicList(songList)
                showLoadingProgressBar = false
            }
        }
    }

    LaunchedEffect(key1 = musicInfo.value) {
        if (musicInfo.value.isNotEmpty()) {
            showSongInfoDialog = true
        }
    }

    if (showSongInfoDialog) {
        YesNoDialog(
            onDismiss = { showSongInfoDialog = false },
            onCancel = { showSongInfoDialog = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    if (tagPage.saveSongInfo(musicInfo.value))
                        showSongInfoDialog = false
                }
            },
            title = stringResource(id = R.string.song_info),
            content = null,
            enableHaptic = enableHaptic.value,
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp / 1.75).dp)
            ) {
                RoundedColumn {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        ItemTitle(
                            text = stringResource(id = R.string.song_name),
                            paddingValues = PaddingValues(
                                start = SaltTheme.dimens.innerHorizontalPadding,
                                end = SaltTheme.dimens.innerHorizontalPadding,
                                top = SaltTheme.dimens.innerVerticalPadding
                            )
                        )
                        ItemEdit(
                            text = musicInfo.value["song"] ?: "",
                            onChange = { musicInfo.value += ("song" to it) },
                            hint = stringResource(id = R.string.text_null),
                            enableHaptic = enableHaptic.value,
                            showClearButton = true,
                            onClear = { musicInfo.value -= "song" },
                            paddingValues = PaddingValues(
                                start = SaltTheme.dimens.innerHorizontalPadding,
                                end = SaltTheme.dimens.innerHorizontalPadding,
                                bottom = SaltTheme.dimens.innerHorizontalPadding,
                                top = 4.dp
                            )
                        )
                        ItemTitle(
                            text = stringResource(id = R.string.atrist),
                            paddingValues = PaddingValues(horizontal = SaltTheme.dimens.innerHorizontalPadding)
                        )
                        ItemEdit(
                            text = musicInfo.value["artist"] ?: "",
                            onChange = { musicInfo.value += ("artist" to it) },
                            hint = stringResource(id = R.string.text_null),
                            enableHaptic = enableHaptic.value,
                            showClearButton = true,
                            onClear = { musicInfo.value -= "artist" },
                            paddingValues = PaddingValues(
                                start = SaltTheme.dimens.innerHorizontalPadding,
                                end = SaltTheme.dimens.innerHorizontalPadding,
                                bottom = SaltTheme.dimens.innerHorizontalPadding,
                                top = 4.dp
                            )
                        )
                        ItemTitle(
                            text = stringResource(id = R.string.album1),
                            paddingValues = PaddingValues(horizontal = SaltTheme.dimens.innerHorizontalPadding)
                        )
                        ItemEdit(
                            text = musicInfo.value["album"] ?: "",
                            onChange = { musicInfo.value += ("album" to it) },
                            hint = stringResource(id = R.string.text_null),
                            enableHaptic = enableHaptic.value,
                            showClearButton = true,
                            onClear = { musicInfo.value -= "album" },
                            paddingValues = PaddingValues(
                                start = SaltTheme.dimens.innerHorizontalPadding,
                                end = SaltTheme.dimens.innerHorizontalPadding,
                                bottom = SaltTheme.dimens.innerHorizontalPadding,
                                top = 4.dp
                            )
                        )
                        ItemTitle(
                            text = stringResource(id = R.string.album_artist_tag_name),
                            paddingValues = PaddingValues(horizontal = SaltTheme.dimens.innerHorizontalPadding)
                        )
                        ItemEdit(
                            text = musicInfo.value["albumArtist"] ?: "",
                            onChange = { musicInfo.value += ("albumArtist" to it) },
                            hint = stringResource(id = R.string.text_null),
                            enableHaptic = enableHaptic.value,
                            showClearButton = true,
                            onClear = { musicInfo.value -= "albumArtist" },
                            paddingValues = PaddingValues(
                                start = SaltTheme.dimens.innerHorizontalPadding,
                                end = SaltTheme.dimens.innerHorizontalPadding,
                                bottom = SaltTheme.dimens.innerHorizontalPadding,
                                top = 4.dp
                            )
                        )
                        ItemTitle(
                            text = stringResource(id = R.string.genre_tag_name),
                            paddingValues = PaddingValues(horizontal = SaltTheme.dimens.innerHorizontalPadding)
                        )
                        ItemEdit(
                            text = musicInfo.value["genre"] ?: "",
                            onChange = { musicInfo.value += ("genre" to it) },
                            hint = stringResource(id = R.string.text_null),
                            enableHaptic = enableHaptic.value,
                            showClearButton = true,
                            onClear = { musicInfo.value -= "genre" },
                            paddingValues = PaddingValues(
                                start = SaltTheme.dimens.innerHorizontalPadding,
                                end = SaltTheme.dimens.innerHorizontalPadding,
                                bottom = SaltTheme.dimens.innerHorizontalPadding,
                                top = 4.dp
                            )
                        )
                        ItemTitle(
                            text = stringResource(id = R.string.track_number_tag_name),
                            paddingValues = PaddingValues(horizontal = SaltTheme.dimens.innerHorizontalPadding)
                        )
                        ItemEdit(
                            text = musicInfo.value["trackNumber"] ?: "",
                            onChange = { musicInfo.value += ("trackNumber" to it) },
                            hint = stringResource(id = R.string.text_null),
                            enableHaptic = enableHaptic.value,
                            showClearButton = true,
                            onClear = { musicInfo.value -= "trackNumber" },
                            paddingValues = PaddingValues(
                                start = SaltTheme.dimens.innerHorizontalPadding,
                                end = SaltTheme.dimens.innerHorizontalPadding,
                                bottom = SaltTheme.dimens.innerHorizontalPadding,
                                top = 4.dp
                            )
                        )
                        ItemTitle(
                            text = stringResource(id = R.string.release_year_tag_name),
                            paddingValues = PaddingValues(horizontal = SaltTheme.dimens.innerHorizontalPadding)
                        )
                        ItemEdit(
                            text = musicInfo.value["releaseYear"] ?: "",
                            onChange = { musicInfo.value += ("releaseYear" to it) },
                            hint = stringResource(id = R.string.text_null),
                            enableHaptic = enableHaptic.value,
                            showClearButton = true,
                            onClear = { musicInfo.value -= "releaseYear" },
                            paddingValues = PaddingValues(
                                start = SaltTheme.dimens.innerHorizontalPadding,
                                end = SaltTheme.dimens.innerHorizontalPadding,
                                bottom = SaltTheme.dimens.innerHorizontalPadding,
                                top = 4.dp
                            )
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {
        TitleBar(
            onBack = {},
            text = stringResource(id = R.string.tag_function_name),
            showBackBtn = false
        )
        Box {
            if (showLoadingProgressBar) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f),
                    color = SaltTheme.colors.highlight,
                    trackColor = SaltTheme.colors.background
                )
            }
            if (songList.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color = SaltTheme.colors.background)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = SaltTheme.colors.subBackground)
                    ) {
                        items(songList.size) {
                            Item(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        musicInfo.value = tagPage.getSongInfo(it)
                                    }
                                },
                                text = songList[it]!![0],
                                sub = "${songList[it]!![1].ifBlank { "?" }} - ${songList[it]!![2].ifBlank { "?" }}",
                            )
                        }
                    }
                }
            } else { // TODO 界面需要优化
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ItemText(
                        text = stringResource(id = R.string.no_music),
                    )
                }
            }
            if (songList.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 26.dp, bottom = 32.dp)
                ) {
                    BasicButton(
                        modifier = Modifier
                            .padding(bottom = 16.dp),
                        onClick = { /*TODO*/ },
                        enableHaptic = enableHaptic.value,
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(all = 2.dp),
                            painter = painterResource(id = R.drawable.search),
                            contentDescription = null,
                            tint = SaltTheme.colors.subBackground
                        )
                    }

                    BasicButton(
                        modifier = Modifier
                            .padding(bottom = 16.dp),
                        onClick = { /*TODO*/ },
                        enableHaptic = enableHaptic.value,
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(all = 3.dp),
                            painter = painterResource(id = R.drawable.plus_no_circle),
                            contentDescription = null,
                            tint = SaltTheme.colors.subBackground
                        )
                    }
                }
            }
        }
    }
}