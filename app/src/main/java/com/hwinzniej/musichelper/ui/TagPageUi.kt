package com.hwinzniej.musichelper.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.activity.TagPage
import com.hwinzniej.musichelper.utils.MyVibrationEffect
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.popup.rememberPopupState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableSaltApi::class, ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun TagPageUi(
    tagPage: TagPage,
    enableHaptic: MutableState<Boolean>,
    hapticStrength: MutableIntState
) {
    val context = LocalContext.current
    val songList = remember { mutableStateMapOf<Int, Array<String>>() }
    var showLoadingProgressBar by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val musicInfo = remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    var showSongInfoDialog by remember { mutableStateOf(false) }
    val coverImage = remember { mutableStateOf<ByteArray?>(null) }
    var showSearchInput by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    var job by remember { mutableStateOf<Job?>(null) }
    var showConfirmGoBackDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteCoverDialog by remember { mutableStateOf(false) }
    val searchResult = remember { mutableStateMapOf<Int, Array<String>>() }
    var searching by remember { mutableStateOf(false) }
    val showFab = remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showDialogProgressBar by remember { mutableStateOf(false) }
    val completeResult =
        remember { mutableStateListOf<Map<String, Int>>() } // Int: 0: 失败 1: 成功 2: 提示
    var refreshComplete by remember { mutableStateOf(true) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = !refreshComplete,
        onRefresh = {
            coroutineScope.launch(Dispatchers.IO) {
                refreshComplete = false
                tagPage.getMusicList(songList)
                MyVibrationEffect(
                    context,
                    enableHaptic.value,
                    hapticStrength.intValue
                ).done()
                withContext(Dispatchers.Main) {
                    Toast
                        .makeText(
                            context,
                            context.getString(R.string.refresh_success),
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
                refreshComplete = true
            }
        }
    )

    BackHandler(enabled = showSearchInput) {
        showSearchInput = false
        searchInput = ""
    }

    LaunchedEffect(Unit) {
        if (songList.isEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                showLoadingProgressBar = true
                tagPage.getMusicList(songList)
                showLoadingProgressBar = false
            }
        }
    }

    LaunchedEffect(key1 = tagPage.coverImage.value) {
        if (tagPage.coverImage.value != null) {
            delay(150L)
            coverImage.value = tagPage.coverImage.value
            tagPage.coverImage.value = null
        }
    }

    LaunchedEffect(key1 = musicInfo.value) {
        if (musicInfo.value.isNotEmpty()) {
            showSongInfoDialog = true
        }
    }

    LaunchedEffect(key1 = searchInput) {
        job?.cancel()
        job = coroutineScope.launch(Dispatchers.IO) {
            if (searchInput.isBlank()) {
                searchResult.clear()
                return@launch
            }
            searching = true
            delay(500L)
            showLoadingProgressBar = true
            delay(500L)
            tagPage.searchSong(searchInput, searchResult)
            showLoadingProgressBar = false
            searching = false
        }
    }

    if (showConfirmDeleteCoverDialog) {
        YesNoDialog(
            onDismiss = { showConfirmDeleteCoverDialog = false },
            onCancel = { showConfirmDeleteCoverDialog = false },
            onConfirm = {
                showConfirmDeleteCoverDialog = false
                coverImage.value = null
            },
            title = stringResource(id = R.string.delete_cover_image),
            content = null,
            confirmButtonColor = colorResource(id = R.color.unmatched),
            enableHaptic = enableHaptic.value,
            hapticStrength = hapticStrength.intValue
        )
    }

    if (showSongInfoDialog) {
        YesNoDialog(
            onDismiss = {
                showConfirmGoBackDialog = true
//                showSongInfoDialog = false
//                musicInfo.value = emptyMap()
            },
            onCancel = {
                showSongInfoDialog = false
                musicInfo.value = emptyMap()
            },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    if (tagPage.saveSongInfo(musicInfo.value, coverImage)) {
                        tagPage.searchSong(searchInput, searchResult)
                        showSongInfoDialog = false
                        musicInfo.value = emptyMap()
                        showLoadingProgressBar = true
                        tagPage.getMusicList(songList)
                        showLoadingProgressBar = false
                    }
                }
            },
            title = stringResource(id = R.string.song_info),
            content = null,
            enableHaptic = enableHaptic.value,
            hapticStrength = hapticStrength.intValue
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp / 1.7).dp)
            ) {
                RoundedColumn {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        ItemTitle(
                            text = stringResource(id = R.string.cover_pic),
                            paddingValues = PaddingValues(
                                start = SaltTheme.dimens.innerHorizontalPadding,
                                end = SaltTheme.dimens.innerHorizontalPadding,
                                top = SaltTheme.dimens.innerVerticalPadding,
                                bottom = 4.dp
                            )
                        )
                        AnimatedContent(
                            targetState = coverImage.value,
                            label = "",
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            }
                        ) {
                            if (it != null) {
                                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            top = 4.dp,
                                            bottom = 8.dp
                                        ),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(
                                        modifier = Modifier
                                            .padding(horizontal = SaltTheme.dimens.innerHorizontalPadding)
                                            .clip(RoundedCornerShape(12.dp))
                                            .combinedClickable(
                                                onClick = { tagPage.selectCoverImage() },
                                                onLongClick = {
                                                    showConfirmDeleteCoverDialog = true
                                                }
                                            ),
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = stringResource(id = R.string.cover_pic),
                                    )
                                }
                            } else {
                                BasicButton(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = SaltTheme.dimens.innerHorizontalPadding,
                                            end = SaltTheme.dimens.innerHorizontalPadding,
                                            bottom = 8.dp
                                        ),
                                    onClick = { tagPage.selectCoverImage() },
                                    backgroundColor = SaltTheme.colors.subText.copy(alpha = 0.1f),
                                    hapticStrength = hapticStrength.intValue
                                ) {
                                    Icon(
                                        modifier = Modifier
                                            .size(17.5.dp)
                                            .align(Alignment.Center),
                                        painter = painterResource(id = R.drawable.plus_no_circle),
                                        contentDescription = null,
                                        tint = SaltTheme.colors.text
                                    )
                                }
                            }
                        }
                        SongInfoItem(
                            title = stringResource(id = R.string.song_name),
                            editText = musicInfo.value["song"],
                            onChange = { musicInfo.value += ("song" to it) },
                            onClear = { musicInfo.value -= "song" },
                            enableHaptic = enableHaptic.value,
                            hapticStrength = hapticStrength.intValue
                        )
                        SongInfoItem(
                            title = stringResource(id = R.string.atrist),
                            editText = musicInfo.value["artist"],
                            onChange = { musicInfo.value += ("artist" to it) },
                            onClear = { musicInfo.value -= "artist" },
                            enableHaptic = enableHaptic.value,
                            hapticStrength = hapticStrength.intValue
                        )
                        SongInfoItem(
                            title = stringResource(id = R.string.album1),
                            editText = musicInfo.value["album"],
                            onChange = { musicInfo.value += ("album" to it) },
                            onClear = { musicInfo.value -= "album" },
                            enableHaptic = enableHaptic.value,
                            hapticStrength = hapticStrength.intValue
                        )
                        SongInfoItem(
                            title = stringResource(id = R.string.album_artist_tag_name),
                            editText = musicInfo.value["albumArtist"],
                            onChange = { musicInfo.value += ("albumArtist" to it) },
                            onClear = { musicInfo.value -= "albumArtist" },
                            enableHaptic = enableHaptic.value,
                            hapticStrength = hapticStrength.intValue
                        )
                        SongInfoItem(
                            title = stringResource(id = R.string.genre_tag_name),
                            editText = musicInfo.value["genre"],
                            onChange = { musicInfo.value += ("genre" to it) },
                            onClear = { musicInfo.value -= "genre" },
                            enableHaptic = enableHaptic.value,
                            hapticStrength = hapticStrength.intValue
                        )
                        SongInfoItem(
                            title = stringResource(id = R.string.track_number_tag_name),
                            editText = musicInfo.value["trackNumber"],
                            onChange = { musicInfo.value += ("trackNumber" to it) },
                            onClear = { musicInfo.value -= "trackNumber" },
                            enableHaptic = enableHaptic.value,
                            hapticStrength = hapticStrength.intValue
                        )
                        SongInfoItem(
                            title = stringResource(id = R.string.disc_number),
                            editText = musicInfo.value["discNumber"],
                            onChange = { musicInfo.value += ("discNumber" to it) },
                            onClear = { musicInfo.value -= "discNumber" },
                            enableHaptic = enableHaptic.value,
                            hapticStrength = hapticStrength.intValue
                        )
                        SongInfoItem(
                            title = stringResource(id = R.string.release_year_tag_name),
                            editText = musicInfo.value["releaseYear"],
                            onChange = { musicInfo.value += ("releaseYear" to it) },
                            onClear = { musicInfo.value -= "releaseYear" },
                            enableHaptic = enableHaptic.value,
                            hapticStrength = hapticStrength.intValue
                        )
                        SongInfoItem(
                            title = stringResource(id = R.string.composer),
                            editText = musicInfo.value["composer"],
                            onChange = { musicInfo.value += ("composer" to it) },
                            onClear = { musicInfo.value -= "composer" },
                            enableHaptic = enableHaptic.value,
                            hapticStrength = hapticStrength.intValue
                        )
                        SongInfoItem(
                            title = stringResource(id = R.string.lyricist),
                            editText = musicInfo.value["lyricist"],
                            onChange = { musicInfo.value += ("lyricist" to it) },
                            onClear = { musicInfo.value -= "lyricist" },
                            enableHaptic = enableHaptic.value,
                            hapticStrength = hapticStrength.intValue
                        )
                        SongInfoItem(
                            title = stringResource(id = R.string.lyrics),
                            editText = musicInfo.value["lyrics"],
                            onChange = { musicInfo.value += ("lyrics" to it) },
                            onClear = { musicInfo.value -= "lyrics" },
                            enableHaptic = enableHaptic.value,
                            hapticStrength = hapticStrength.intValue,
                            textStyle = TextStyle(fontSize = 15.sp)
                        )
                    }
                }
            }
        }
    }

    if (showConfirmGoBackDialog) {
        YesNoDialog(
            onDismiss = { showConfirmGoBackDialog = false },
            onCancel = { showConfirmGoBackDialog = false },
            onConfirm = {
                showConfirmGoBackDialog = false
                showSongInfoDialog = false
                musicInfo.value = emptyMap()
            },
            title = stringResource(id = R.string.go_back_confirm_dialog_title),
            content = stringResource(id = R.string.go_back_confirm_dialog_content),
            enableHaptic = enableHaptic.value,
            hapticStrength = hapticStrength.intValue
        )
    }

    if (showCompleteDialog) {
        val popupState = rememberPopupState()
        var selectedItem by remember { mutableIntStateOf(-1) }
        var switchState by remember { mutableStateOf(false) }
        YesNoDialog(
            onDismiss = {
                showCompleteDialog = false
                completeResult.clear()
            },
            onCancel = {
                showCompleteDialog = false
                completeResult.clear()
            },
            onConfirm = {
                if (completeResult.size == 0 ||
                    completeResult.first().keys.first()
                        .contains(context.getString(R.string.all_done)) ||
                    (completeResult.first().keys.first()
                        .contains(context.getString(R.string.no_album_artist_null_count_in_song_list)) && !switchState)
                ) {
                    showCompleteDialog = false
                    completeResult.clear()
                    return@YesNoDialog
                } else {
                    when (selectedItem) {
                        0 -> {
                            coroutineScope.launch(Dispatchers.IO) {
                                showDialogProgressBar = true
                                tagPage.handleDuplicateAlbum(
                                    overwrite = switchState,
                                    completeResult = completeResult
                                )
                                tagPage.getMusicList(songList)
                                showDialogProgressBar = false
                            }
                        }
                    }
                }
            },
            title = stringResource(id = R.string.completion),
            content = null,
            enableHaptic = enableHaptic.value,
            enableConfirmButton = !showDialogProgressBar,
            hapticStrength = hapticStrength.intValue
        ) {
            Box {
                if (showDialogProgressBar) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(1f),
                        color = SaltTheme.colors.highlight,
                        trackColor = SaltTheme.colors.background
                    )
                }
                Column(
                    modifier = Modifier
                        .heightIn(max = (LocalConfiguration.current.screenHeightDp / 2).dp)
                ) {
                    RoundedColumn {
                        ItemTitle(text = stringResource(id = R.string.completion_options))
                        ItemPopup(
                            state = popupState,
                            text = stringResource(id = R.string.choose_need_complete_type),
                            selectedItem = when (selectedItem) {
                                0 -> stringResource(id = R.string.album_artist_tag_name)
                                else -> ""
                            },
                        ) {
                            PopupMenuItem(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        selectedItem = 0
                                        popupState.dismiss()
                                        showDialogProgressBar = true
                                        delay(300L)
                                        tagPage.searchDuplicateAlbum(completeResult)
                                        showDialogProgressBar = false
                                    }
                                },
                                text = stringResource(id = R.string.album_artist_tag_name),
                                selected = selectedItem == 0
                            )
                        }
                        AnimatedVisibility(visible = selectedItem == 0) {
                            AnimatedContent(
                                targetState = switchState,
                                label = "",
                                transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                }) {
                                ItemSwitcher(
                                    state = switchState,
                                    onChange = { it1 -> switchState = it1 },
                                    text = stringResource(id = R.string.file_conflict_dialog_yes_text),
                                    sub = if (it)
                                        stringResource(id = R.string.overwrite_original_album_artist_tag_sub_on)
                                    else
                                        stringResource(id = R.string.overwrite_original_album_artist_tag_sub_off),
                                    enableHaptic = enableHaptic.value,
                                    hapticStrength = hapticStrength.intValue
                                )
                            }
                        }
                    }
                    AnimatedVisibility(visible = completeResult.size != 0) {
                        RoundedColumn {
                            ItemTitle(text = stringResource(id = R.string.completion_log))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                items(completeResult.size) { index ->
                                    Text(
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 4.dp
                                        ),
                                        text = completeResult[index].keys.first(),
                                        fontSize = 14.sp,
                                        color = if (completeResult[index].values.first() == 1)
                                            SaltTheme.colors.subText
                                        else if (completeResult[index].values.first() == 2)
                                            colorResource(id = R.color.manual)
                                        else
                                            colorResource(id = R.color.unmatched),
                                    )
                                }
                            }
                        }
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
        Box {
            Column {
                AnimatedContent(targetState = showSearchInput, label = "") {
                    if (it) {
                        Row(
                            modifier = Modifier
                                .height(56.dp)
                                .align(Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val focusRequester = remember { FocusRequester() }
                            ItemEdit(
                                modifier = Modifier
                                    .focusRequester(focusRequester)
                                    .weight(1f),
                                text = searchInput,
                                onChange = { it1 -> searchInput = it1 },
                                paddingValues = PaddingValues(
                                    start = 16.dp,
                                    end = 4.dp,
                                    top = 6.dp,
                                    bottom = 6.dp
                                ),
                                showClearButton = true,
                                onClear = {
                                    searchInput = ""
                                },
                                hint = stringResource(id = R.string.search_song_library),
                                iconPainter = painterResource(id = R.drawable.search),
                                iconColor = SaltTheme.colors.subText,
                                iconPaddingValues = PaddingValues(all = 3.5.dp),
                                singleLine = true,
                                hapticStrength = hapticStrength.intValue
                            )
                            LaunchedEffect(Unit) {
                                delay(300L)
                                focusRequester.requestFocus()
                            }

                            androidx.compose.material3.TextButton(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(72.dp),
                                onClick = {
                                    MyVibrationEffect(
                                        context,
                                        enableHaptic.value,
                                        hapticStrength.intValue
                                    ).click()
                                    showSearchInput = false
                                    searchInput = ""
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = SaltTheme.colors.highlight
                                ),
                            ) {
                                Text(
                                    stringResource(id = R.string.cancel_button_text),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    } else {
                        TitleBar(
                            onBack = {},
                            text = stringResource(id = R.string.tag_function_name),
                            showBackBtn = false
                        )
                    }
                }
            }
        }
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
            AnimatedContent(
                targetState = when {
                    searchResult.isNotEmpty() -> "searchResult"
                    songList.isNotEmpty() && searchResult.isEmpty() && searchInput.isBlank() -> "songList"
                    songList.isEmpty() -> "emptyList"
                    searchResult.isEmpty() && searchInput.isNotBlank() && !searching -> "emptyResult"
                    else -> ""
                },
                label = "",
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }
            ) { targetState ->
                when (targetState) {
                    "searchResult" -> {
                        Column(
                            modifier = Modifier
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 16.dp,
                                    top = 12.dp
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(color = SaltTheme.colors.background)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .background(color = SaltTheme.colors.subBackground)
                            ) {
                                items(searchResult.size) {
                                    AnimatedContent(
                                        targetState = searchResult[it],
                                        label = "",
                                    ) { it1 ->
                                        Item(
                                            onClick = {
                                                if (showFab.value)
                                                    showFab.value = false
                                                else
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        musicInfo.value =
                                                            tagPage.getSongInfo(
                                                                it1!![3].toInt(),
                                                                coverImage
                                                            )
                                                    }
                                            },
                                            text = it1!![0],
                                            sub = "${it1[1].ifBlank { "?" }} - ${it1[2].ifBlank { "?" }}",
                                            indication = if (showFab.value) null else rememberRipple()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "songList" -> {
                        Box(
                            modifier = Modifier.pullRefresh(pullRefreshState)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        bottom = 16.dp,
                                        top = 12.dp
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color = SaltTheme.colors.background)
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .background(color = SaltTheme.colors.subBackground)
                                ) {
                                    items(songList.size) {
                                        if (songList[it] != null)
                                            Item(
                                                onClick = {
                                                    if (showFab.value)
                                                        showFab.value = false
                                                    else
                                                        coroutineScope.launch(Dispatchers.IO) {
                                                            musicInfo.value =
                                                                tagPage.getSongInfo(
                                                                    songList[it]!![3].toInt(),
                                                                    coverImage
                                                                )
                                                        }
                                                },
                                                text = songList[it]!![0],
                                                sub = "${songList[it]!![1].ifBlank { "?" }} - ${songList[it]!![2].ifBlank { "?" }}",
                                                indication = if (showFab.value) null else rememberRipple()
                                            )
                                    }
                                }
                            }
                        }
                    }

                    "emptyList" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 16.dp,
                                    top = 12.dp
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(color = SaltTheme.colors.background)
                                .pullRefresh(pullRefreshState)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                modifier = Modifier.size(96.dp),
                                painter = painterResource(id = R.drawable.no_items),
                                contentDescription = stringResource(id = R.string.no_music)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = stringResource(id = R.string.no_music),
                                fontSize = 16.sp, color = SaltTheme.colors.subText
                            )
                        }
                    }

                    "emptyResult" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 16.dp,
                                    top = 12.dp
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(color = SaltTheme.colors.background),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                modifier = Modifier.size(96.dp),
                                painter = painterResource(id = R.drawable.no_items),
                                contentDescription = stringResource(id = R.string.no_music)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = stringResource(id = R.string.no_search_result),
                                fontSize = 16.sp, color = SaltTheme.colors.subText
                            )
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 16.dp,
                                    top = 12.dp
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(color = SaltTheme.colors.background),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {

                        }
                    }
                }
            }

            FloatingActionButton(
                expanded = showFab,
                heightExpand = 100.dp,
                widthExpand = 175.dp,
                enableHaptic = enableHaptic.value,
                hapticStrength = hapticStrength.intValue
            ) {
                PopupMenuItem(
                    onClick = {
                        MyVibrationEffect(
                            context,
                            enableHaptic.value,
                            hapticStrength.intValue
                        ).click()
                        showFab.value = false
                        showSearchInput = !showSearchInput
                    },
                    text = stringResource(id = R.string.search),
                    iconPainter = painterResource(id = R.drawable.search),
                    iconColor = SaltTheme.colors.text,
                    iconPaddingValues = PaddingValues(all = 2.5.dp)
                )
                PopupMenuItem(
                    onClick = {
                        MyVibrationEffect(
                            context,
                            enableHaptic.value,
                            hapticStrength.intValue
                        ).click()
                        showFab.value = false
                        showCompleteDialog = true
                    },
                    text = stringResource(id = R.string.completion),
                    iconPainter = painterResource(id = R.drawable.complete),
                    iconColor = SaltTheme.colors.text,
                    iconPaddingValues = PaddingValues(all = 3.dp)
                )
            }
            PullRefreshIndicator(
                modifier = Modifier.align(Alignment.TopCenter),
                refreshing = !refreshComplete,
                state = pullRefreshState
            )
        }
    }
}

@Composable
fun SongInfoItem(
    title: String,
    editText: String?,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    enableHaptic: Boolean,
    hapticStrength: Int,
    textStyle: TextStyle = SaltTheme.textStyles.main
) {
    ItemTitle(
        text = title,
        paddingValues = PaddingValues(
            start = SaltTheme.dimens.innerHorizontalPadding,
            end = SaltTheme.dimens.innerHorizontalPadding,
            top = SaltTheme.dimens.innerVerticalPadding
        )
    )
    ItemEdit(
        text = editText ?: "",
        onChange = onChange,
        hint = stringResource(id = R.string.text_null),
        enableHaptic = enableHaptic,
        showClearButton = true,
        onClear = onClear,
        paddingValues = PaddingValues(
            start = SaltTheme.dimens.innerHorizontalPadding,
            end = SaltTheme.dimens.innerHorizontalPadding,
            bottom = 8.dp,
            top = 4.dp
        ),
        hapticStrength = hapticStrength,
        textStyle = textStyle
    )
}