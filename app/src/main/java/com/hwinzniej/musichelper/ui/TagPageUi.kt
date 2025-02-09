package com.hwinzniej.musichelper.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ripple
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.activity.ScanPage
import com.hwinzniej.musichelper.activity.TagPage
import com.hwinzniej.musichelper.data.DataStoreConstants
import com.hwinzniej.musichelper.utils.MyVibrationEffect
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.popup.rememberPopupState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterialApi::class
)
@Composable
fun TagPageUi(
    tagPage: TagPage,
    enableHaptic: MutableState<Boolean>,
    hapticStrength: MutableIntState,
    scanPage: ScanPage,
    pageState: PagerState,
    slow: MutableState<Boolean>,
    overwrite: MutableState<Boolean>,
    lyricist: MutableState<Boolean>,
    composer: MutableState<Boolean>,
    arranger: MutableState<Boolean>,
    sortMethod: MutableIntState,
    dataStore: DataStore<Preferences>,
) {
    val context = LocalContext.current
    val songList = remember { mutableStateMapOf<Int, Array<String>>() }
    var showLoadingProgressBar by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val musicInfo = remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    val musicInfoOriginal = remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    val coverImage = remember { mutableStateOf<ByteArray?>(null) }
    val coverImageModified = remember { mutableStateOf(false) }
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
    val lazyColumnState = rememberLazyListState()
    var multiSelect by remember { mutableStateOf(false) }
    val selectedSongList = remember { mutableStateListOf<Int>() }
    var intervalSelectionStart by remember { mutableIntStateOf(-1) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = !refreshComplete,
        onRefresh = {
            coroutineScope.launch(Dispatchers.IO) {
                refreshComplete = false
                if (multiSelect) {
                    multiSelect = false
                    delay(842L)
                }
                tagPage.getMusicList(songList, sortMethod.intValue, selectedSongList)
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
    val pagesInner = listOf("0", "1")
    val pageStateInner = rememberPagerState(pageCount = { pagesInner.size })

    BackHandler(enabled = showSearchInput) {
        showSearchInput = false
        searchInput = ""
    }

    BackHandler(enabled = multiSelect && !showSearchInput) {
        multiSelect = false
        coroutineScope.launch(Dispatchers.Default) {
            delay(300L)
            intervalSelectionStart = -1
            selectedSongList.replaceAll { 0 }
        }
    }

    BackHandler(enabled = pageStateInner.currentPage == 1) {
        if (musicInfo.value != musicInfoOriginal.value || coverImageModified.value) {
            showConfirmGoBackDialog = true
        } else {
            coroutineScope.launch {
                pageStateInner.animateScrollToPage(0, animationSpec = spring(2f))
                musicInfo.value = emptyMap()
                musicInfoOriginal.value = emptyMap()
                coverImage.value = null
                coverImageModified.value = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (songList.isEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                showLoadingProgressBar = true
                delay(1000L)
                tagPage.getMusicList(songList, sortMethod.intValue, selectedSongList)
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
            pageStateInner.animateScrollToPage(1, animationSpec = spring(2f))
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

    LaunchedEffect(key1 = lazyColumnState.isScrollInProgress) {
        showFab.value = false
    }

    LaunchedEffect(key1 = pageState.currentPage) {
        if (pageState.currentPage == 2) {
            showSearchInput = false
            searchInput = ""
            showFab.value = false
        }
    }

    LaunchedEffect(key1 = scanPage.scanComplete.value) {
        if (scanPage.scanComplete.value) {
            scanPage.scanComplete.value = false
            coroutineScope.launch(Dispatchers.IO) {
                tagPage.getMusicList(songList, sortMethod.intValue, selectedSongList)
            }
        }
    }

    if (showConfirmDeleteCoverDialog) {
        YesNoDialog(
            onDismiss = { showConfirmDeleteCoverDialog = false },
            onCancel = { showConfirmDeleteCoverDialog = false },
            onConfirm = {
                showConfirmDeleteCoverDialog = false
                coverImage.value = null
                coverImageModified.value = true
            },
            title = stringResource(id = R.string.delete_cover_image),
            content = null,
            confirmButtonColor = colorResource(id = R.color.unmatched),
            enableHaptic = enableHaptic.value,
            hapticStrength = hapticStrength.intValue
        )
    }

    if (showConfirmGoBackDialog) {
        YesNoDialog(
            onDismiss = { showConfirmGoBackDialog = false },
            onCancel = { showConfirmGoBackDialog = false },
            onConfirm = {
                showConfirmGoBackDialog = false
                coroutineScope.launch {
                    pageStateInner.animateScrollToPage(0, animationSpec = spring(2f))
                }
                musicInfo.value = emptyMap()
                musicInfoOriginal.value = emptyMap()
                coverImage.value = null
                coverImageModified.value = false
            },
            title = stringResource(id = R.string.go_back_confirm_dialog_title),
            content = stringResource(id = R.string.go_back_confirm_dialog_content),
            enableHaptic = enableHaptic.value,
            hapticStrength = hapticStrength.intValue
        )
    }

    if (showCompleteDialog) {
        val popupState1 = rememberPopupState()
        val popupState2 = rememberPopupState()
        var selectedItem by remember { mutableIntStateOf(-1) }
        val subtype = remember {
            mutableStateListOf(
                if (lyricist.value) context.getString(R.string.lyricist) else "",
                if (composer.value) " ${context.getString(R.string.composer)}" else "",
                if (arranger.value) " ${context.getString(R.string.arranger)}" else ""
            )
        }
        var readyForComplete by remember { mutableStateOf(false) }
        var completeDone by remember { mutableStateOf(false) }

        YesNoDialog(
            onDismiss = {
                if (showDialogProgressBar) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.wait_operate_end),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    if (completeDone) {
                        coroutineScope.launch(Dispatchers.IO) {
                            tagPage.getMusicList(songList, sortMethod.intValue, selectedSongList)
                        }
                        completeDone = false
                    }
                    showCompleteDialog = false
                    completeResult.clear()
                }
            },
            onCancel = {
                if (showDialogProgressBar) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.wait_operate_end),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    if (completeDone) {
                        coroutineScope.launch(Dispatchers.IO) {
                            tagPage.getMusicList(songList, sortMethod.intValue, selectedSongList)
                        }
                        completeDone = false
                    }
                    showCompleteDialog = false
                    completeResult.clear()
                }
            },
            onConfirm = {
                if (!readyForComplete) {
                    if (completeDone) {
                        coroutineScope.launch(Dispatchers.IO) {
                            tagPage.getMusicList(songList, sortMethod.intValue, selectedSongList)
                        }
                        completeDone = false
                    }
                    showCompleteDialog = false
                    completeResult.clear()
                    return@YesNoDialog
                } else {
                    when (selectedItem) {
                        0 -> {
                            coroutineScope.launch(Dispatchers.IO) {
                                showDialogProgressBar = true
                                tagPage.handleDuplicateAlbum(
                                    overwrite = overwrite.value,
                                    completeResult = completeResult,
                                    slow = slow.value,
                                    selectedSongList = selectedSongList
                                )
                                readyForComplete = false
                                showDialogProgressBar = false
                                completeDone = true
                                MyVibrationEffect(
                                    context,
                                    enableHaptic.value,
                                    hapticStrength.intValue
                                ).done()
                            }
                        }

                        1 -> {
                            coroutineScope.launch(Dispatchers.IO) {
                                if (!lyricist.value && !composer.value && !arranger.value) {
                                    withContext(Dispatchers.Main) {
                                        Toast
                                            .makeText(
                                                context,
                                                context.getString(R.string.no_tag_to_complete),
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                    return@launch
                                }
                                showDialogProgressBar = true
                                tagPage.handleBlankLyricistComposerArranger(
                                    overwrite = overwrite.value,
                                    lyricist = lyricist.value,
                                    composer = composer.value,
                                    arranger = arranger.value,
                                    completeResult = completeResult,
                                    slow = slow.value,
                                    selectedSongList = selectedSongList
                                )
                                readyForComplete = false
                                showDialogProgressBar = false
                                completeDone = true
                                MyVibrationEffect(
                                    context,
                                    enableHaptic.value,
                                    hapticStrength.intValue
                                ).done()
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
                        trackColor = SaltTheme.colors.background,
                        strokeCap = StrokeCap.Square
                    )
                }
                Column {
                    RoundedColumn {
                        ItemTitle(text = stringResource(id = R.string.completion_options))
                        ItemPopup(
                            state = popupState1,
                            text = stringResource(id = R.string.choose_need_complete_type),
                            selectedItem = when (selectedItem) {
                                0 -> stringResource(id = R.string.album_artist_tag_name)
                                1 -> stringResource(id = R.string.lyric_and_songs)

                                else -> ""
                            },
                        ) {
                            PopupMenuItem(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        selectedItem = 0
                                        popupState1.dismiss()
                                        showDialogProgressBar = true
                                        completeResult.clear()
                                        delay(300L)
                                        readyForComplete =
                                            tagPage.searchDuplicateAlbum(
                                                completeResult = completeResult,
                                                multiSelect = multiSelect,
                                                selectedSongList = selectedSongList
                                            )
                                        if (overwrite.value)
                                            readyForComplete = true
                                        showDialogProgressBar = false
                                    }
                                },
                                text = stringResource(id = R.string.album_artist_tag_name),
                                selected = selectedItem == 0
                            )
                            PopupMenuItem(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        selectedItem = 1
                                        popupState1.dismiss()
                                        showDialogProgressBar = true
                                        completeResult.clear()
                                        delay(300L)
                                        readyForComplete =
                                            tagPage.searchBlankLyricistComposerArranger(
                                                completeResult = completeResult,
                                                multiSelect = multiSelect,
                                                selectedSongList = selectedSongList
                                            )
                                        if (overwrite.value)
                                            readyForComplete = true
                                        showDialogProgressBar = false
                                    }
                                },
                                text = stringResource(id = R.string.lyric_and_songs),
                                selected = selectedItem == 1
                            )
                        }
                        AnimatedVisibility(visible = selectedItem == 0) {
                            Column {
                                ItemSwitcher(
                                    state = overwrite.value,
                                    onChange = { it1 ->
                                        coroutineScope.launch {
                                            dataStore.edit { settings ->
                                                settings[DataStoreConstants.TAG_OVERWRITE] = it1
                                            }
                                        }
                                        readyForComplete = it1
                                    },
                                    text = stringResource(id = R.string.file_conflict_dialog_yes_text),
                                    sub =
                                    if (selectedSongList.all { it == 0 })
                                        stringResource(id = R.string.overwrite_original_album_artist_tag_sub_on)
                                    else
                                        stringResource(id = R.string.overwrite_original_album_artist_tag_sub_on2),
                                    subOff = stringResource(id = R.string.overwrite_original_album_artist_tag_sub_off)
                                        .replace(
                                            "#",
                                            stringResource(id = R.string.album_artist_tag_name)
                                        ),
                                    enableHaptic = enableHaptic.value,
                                    hapticStrength = hapticStrength.intValue
                                )
                                ItemSwitcher(
                                    state = slow.value,
                                    onChange = { it1 ->
                                        coroutineScope.launch {
                                            dataStore.edit { settings ->
                                                settings[DataStoreConstants.SLOW_MODE] = it1
                                            }
                                        }
                                    },
                                    text = stringResource(id = R.string.keep_original_file_sort),
                                    sub = stringResource(id = R.string.keep_original_file_sort_sub),
                                    enableHaptic = enableHaptic.value,
                                    hapticStrength = hapticStrength.intValue
                                )
                            }
                        }
                        AnimatedVisibility(visible = selectedItem == 1) {
                            Column {
                                ItemSwitcher(
                                    state = overwrite.value,
                                    onChange = { it1 ->
                                        coroutineScope.launch {
                                            dataStore.edit { settings ->
                                                settings[DataStoreConstants.TAG_OVERWRITE] = it1
                                            }
                                        }
                                        readyForComplete = it1
                                    },
                                    text = stringResource(id = R.string.file_conflict_dialog_yes_text),
                                    sub =
                                    if (selectedSongList.all { it == 0 })
                                        stringResource(id = R.string.overwrite_original_album_artist_tag_sub_on)
                                    else
                                        stringResource(id = R.string.overwrite_original_album_artist_tag_sub_on2),
                                    subOff = stringResource(id = R.string.overwrite_original_album_artist_tag_sub_off)
                                        .replace(
                                            "#",
                                            stringResource(id = R.string.choose_need_complete_subtype)
                                        ),
                                    enableHaptic = enableHaptic.value,
                                    hapticStrength = hapticStrength.intValue
                                )
                                ItemSwitcher(
                                    state = slow.value,
                                    onChange = { it1 ->
                                        coroutineScope.launch {
                                            dataStore.edit { settings ->
                                                settings[DataStoreConstants.SLOW_MODE] = it1
                                            }
                                        }
                                    },
                                    text = stringResource(id = R.string.keep_original_file_sort),
                                    sub = stringResource(id = R.string.keep_original_file_sort_sub),
                                    enableHaptic = enableHaptic.value,
                                    hapticStrength = hapticStrength.intValue
                                )
                                ItemPopup(
                                    state = popupState2,
                                    text = stringResource(id = R.string.choose_need_complete_subtype),
                                    selectedItem = subtype.joinToString(separator = ""),
                                    popupWidth = 135,
                                    rightSubWeight = 1f
                                ) {
                                    PopupMenuItem(
                                        onClick = {
                                            coroutineScope.launch {
                                                dataStore.edit { settings ->
                                                    settings[DataStoreConstants.TAG_LYRICIST] =
                                                        !lyricist.value
                                                }
                                            }
                                            if (!lyricist.value)
                                                subtype[0] =
                                                    context.getString(R.string.lyricist)
                                            else
                                                subtype[0] = ""
                                        },
                                        text = stringResource(id = R.string.lyricist),
                                        selected = lyricist.value
                                    )
                                    PopupMenuItem(
                                        onClick = {
                                            coroutineScope.launch {
                                                dataStore.edit { settings ->
                                                    settings[DataStoreConstants.TAG_COMPOSER] =
                                                        !composer.value
                                                }
                                            }
                                            if (!composer.value)
                                                subtype[1] =
                                                    " ${context.getString(R.string.composer)}"
                                            else
                                                subtype[1] = ""
                                        },
                                        text = stringResource(id = R.string.composer),
                                        selected = composer.value
                                    )
                                    PopupMenuItem(
                                        onClick = {
                                            coroutineScope.launch {
                                                dataStore.edit { settings ->
                                                    settings[DataStoreConstants.TAG_ARRANGER] =
                                                        !arranger.value
                                                }
                                            }
                                            if (!arranger.value)
                                                subtype[2] =
                                                    " ${context.getString(R.string.arranger)}"
                                            else
                                                subtype[2] = ""
                                        },
                                        text = stringResource(id = R.string.arranger),
                                        selected = arranger.value
                                    )
                                }
                            }
                        }
                    }
                    AnimatedVisibility(visible = completeResult.isNotEmpty()) {
                        RoundedColumn {
                            ItemTitle(text = stringResource(id = R.string.completion_log))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = (LocalConfiguration.current.screenHeightDp / 1.6).dp)
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
                AnimatedContent(
                    targetState = showSearchInput,
                    label = ""
                ) {
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
                        AnimatedContent(
                            targetState = multiSelect || pageStateInner.currentPage == 1,
                            label = "",
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            }) { animate ->
                            TitleBar(
                                onBack = {
                                    MyVibrationEffect(
                                        context,
                                        enableHaptic.value,
                                        hapticStrength.intValue
                                    ).click()
                                    if (multiSelect)
                                        multiSelect = false
                                    if (pageStateInner.currentPage == 1) {
                                        if (musicInfo.value != musicInfoOriginal.value || coverImageModified.value) {
                                            showConfirmGoBackDialog = true
                                        } else {
                                            coroutineScope.launch {
                                                pageStateInner.animateScrollToPage(
                                                    0,
                                                    animationSpec = spring(2f)
                                                )
                                                musicInfo.value = emptyMap()
                                                musicInfoOriginal.value = emptyMap()
                                                coverImage.value = null
                                                coverImageModified.value = false
                                            }
                                        }
                                    }
                                    coroutineScope.launch(Dispatchers.Default) {
                                        delay(300L)
                                        intervalSelectionStart = -1
                                        selectedSongList.replaceAll { 0 }
                                    }
                                },
                                text = if (animate && multiSelect)
                                    "${stringResource(id = R.string.tag_function_name)} (${selectedSongList.count { it1 -> it1 != 0 }})"
                                else if (animate && pageStateInner.currentPage == 1)
                                    stringResource(id = R.string.song_info)
                                else stringResource(id = R.string.tag_function_name),
                                showBackBtn = animate,
                                showRightBtn = animate && pageStateInner.currentPage == 1,
                                rightBtnIcon = painterResource(id = R.drawable.check),
                                rightBtnContentDescription = stringResource(id = R.string.ok_button_text),
                                onRightBtn = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        if (pageStateInner.currentPage == 1 && tagPage.saveSongInfo(
                                                musicInfo.value,
                                                coverImage
                                            )
                                        ) {
                                            pageStateInner.animateScrollToPage(
                                                0,
                                                animationSpec = spring(2f)
                                            )
                                            if (searchInput.isNotBlank())
                                                tagPage.searchSong(searchInput, searchResult)
                                            musicInfo.value = emptyMap()
                                            musicInfoOriginal.value = emptyMap()
                                            coverImage.value = null
                                            showLoadingProgressBar = true
                                            tagPage.getMusicList(
                                                songList,
                                                sortMethod.intValue,
                                                selectedSongList
                                            )
                                            showLoadingProgressBar = false
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        HorizontalPager(
            state = pageStateInner,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false,
            beyondViewportPageCount = 0
        ) { page ->
            when (page) {
                0 -> {
                    Box {
                        if (showLoadingProgressBar) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zIndex(1f),
                                color = SaltTheme.colors.highlight,
                                trackColor = SaltTheme.colors.background,
                                strokeCap = StrokeCap.Square
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
                                                    targetState = multiSelect,
                                                    label = "",
                                                    transitionSpec = {
                                                        fadeIn() togetherWith fadeOut()
                                                    }) { animate ->
                                                    if (!animate)
                                                        Item(
                                                            onClick = {
                                                                if (showFab.value)
                                                                    showFab.value = false
                                                                else
                                                                    coroutineScope.launch(
                                                                        Dispatchers.IO
                                                                    ) {
                                                                        musicInfo.value =
                                                                            tagPage.getSongInfo(
                                                                                searchResult[it]!![3].toInt(),
                                                                                coverImage
                                                                            )
                                                                        musicInfoOriginal.value =
                                                                            musicInfo.value
                                                                    }
                                                            },
                                                            onLongClick = {
                                                                coroutineScope.launch(Dispatchers.Default) {
                                                                    delay(248L)
                                                                    if (showFab.value)
                                                                        showFab.value = false
                                                                    multiSelect = true
                                                                    MyVibrationEffect(
                                                                        context,
                                                                        enableHaptic.value,
                                                                        hapticStrength.intValue
                                                                    ).click()
                                                                    selectedSongList[searchResult[it]!![3].toInt()] =
                                                                        1
                                                                }
                                                            },
                                                            text = searchResult[it]!![0],
                                                            sub = "${searchResult[it]!![1].ifBlank { "?" }} - ${searchResult[it]!![2].ifBlank { "?" }}",
                                                            indication = if (showFab.value) null else ripple()
                                                        )
                                                    else
                                                        ItemCheck(
                                                            state = selectedSongList[searchResult[it]!![3].toInt()] == 1,
                                                            onChange = { it1 ->
                                                                if (showFab.value)
                                                                    showFab.value = false
                                                                else {
                                                                    selectedSongList[searchResult[it]!![3].toInt()] =
                                                                        if (it1) 1 else 0
                                                                    MyVibrationEffect(
                                                                        context,
                                                                        enableHaptic.value,
                                                                        hapticStrength.intValue
                                                                    ).click()
                                                                }
                                                            },
                                                            onLongChange = { it1 ->
                                                                if (showFab.value)
                                                                    showFab.value = false
                                                                else {
                                                                    MyVibrationEffect(
                                                                        context,
                                                                        enableHaptic.value,
                                                                        hapticStrength.intValue
                                                                    ).click()
                                                                    if (intervalSelectionStart == -1) {
                                                                        intervalSelectionStart = it
                                                                    } else {
                                                                        val start =
                                                                            intervalSelectionStart
                                                                        val end = it
                                                                        if (start < end) {
                                                                            for (i in start..end) {
                                                                                selectedSongList[searchResult[i]!![3].toInt()] =
                                                                                    if (it1) 1 else 0
                                                                            }
                                                                        } else {
                                                                            for (i in end..start) {
                                                                                selectedSongList[searchResult[i]!![3].toInt()] =
                                                                                    if (it1) 1 else 0
                                                                            }
                                                                        }
                                                                        intervalSelectionStart = -1
                                                                    }
                                                                }
                                                            },
                                                            highlight = intervalSelectionStart == it,
                                                            indication = if (showFab.value) null else ripple(),
                                                            iconAtLeft = false,
                                                            text = searchResult[it]!![0],
                                                            sub = "${searchResult[it]!![1].ifBlank { "?" }} - ${searchResult[it]!![2].ifBlank { "?" }}",
                                                            enableHaptic = false,
                                                            hapticStrength = hapticStrength.intValue
                                                        )
                                                }
                                            }
                                        }
                                    }
                                }

                                "songList" -> {
//                          SideBar(
//                              onSelect = {
//                                  value ->
//                                  songList[2].sortBy { it[2] as String }
//                              val index = dataList.indexOfFirst { it.py[0].uppercase() == value }
//                              if (index >= 0) {
//                                  scope.launch { listState.scrollToItem(index) }
//                              }
//                          }) {
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
                                                state = lazyColumnState,
                                                modifier = Modifier
                                                    .background(color = SaltTheme.colors.subBackground)
                                            ) {
                                                items(songList.size) {
                                                    AnimatedContent(
                                                        targetState = multiSelect,
                                                        label = "",
                                                        transitionSpec = {
                                                            fadeIn() togetherWith fadeOut()
                                                        }) { animate ->
                                                        if (songList[it] != null && !animate)
                                                            Item(
                                                                onClick = {
                                                                    if (showFab.value)
                                                                        showFab.value = false
                                                                    else
                                                                        coroutineScope.launch(
                                                                            Dispatchers.IO
                                                                        ) {
                                                                            musicInfo.value =
                                                                                tagPage.getSongInfo(
                                                                                    songList[it]!![3].toInt(),
                                                                                    coverImage
                                                                                )
                                                                            musicInfoOriginal.value =
                                                                                musicInfo.value
                                                                        }
                                                                },
                                                                onLongClick = {
                                                                    coroutineScope.launch(
                                                                        Dispatchers.Default
                                                                    ) {
                                                                        delay(248L)
                                                                        if (showFab.value)
                                                                            showFab.value = false
                                                                        else {
                                                                            multiSelect = true
                                                                            MyVibrationEffect(
                                                                                context,
                                                                                enableHaptic.value,
                                                                                hapticStrength.intValue
                                                                            ).click()
                                                                            selectedSongList[songList[it]!![3].toInt()] =
                                                                                1
                                                                        }
                                                                    }
                                                                },
                                                                text = songList[it]!![0],
                                                                sub = "${songList[it]!![1].ifBlank { "?" }} - ${songList[it]!![2].ifBlank { "?" }}",
                                                                indication = if (showFab.value) null else ripple()
                                                            )
                                                        else if (songList[it] != null && animate)
                                                            ItemCheck(
                                                                state = selectedSongList[songList[it]!![3].toInt()] == 1,
                                                                onChange = { it1 ->
                                                                    if (showFab.value)
                                                                        showFab.value = false
                                                                    else {
                                                                        selectedSongList[songList[it]!![3].toInt()] =
                                                                            if (it1) 1 else 0
                                                                        MyVibrationEffect(
                                                                            context,
                                                                            enableHaptic.value,
                                                                            hapticStrength.intValue
                                                                        ).click()
                                                                    }
                                                                },
                                                                onLongChange = { it1 ->
                                                                    if (showFab.value)
                                                                        showFab.value = false
                                                                    else {
                                                                        MyVibrationEffect(
                                                                            context,
                                                                            enableHaptic.value,
                                                                            hapticStrength.intValue
                                                                        ).click()
                                                                        if (intervalSelectionStart == -1) {
                                                                            intervalSelectionStart =
                                                                                it
                                                                        } else {
                                                                            val start =
                                                                                intervalSelectionStart
                                                                            val end = it
                                                                            if (start < end) {
                                                                                for (i in start..end) {
                                                                                    selectedSongList[songList[i]!![3].toInt()] =
                                                                                        if (it1) 1 else 0
                                                                                }
                                                                            } else {
                                                                                for (i in end..start) {
                                                                                    selectedSongList[songList[i]!![3].toInt()] =
                                                                                        if (it1) 1 else 0
                                                                                }
                                                                            }
                                                                            intervalSelectionStart =
                                                                                -1
                                                                        }
                                                                    }
                                                                },
                                                                highlight = intervalSelectionStart == it,
                                                                indication = if (showFab.value) null else ripple(),
                                                                iconAtLeft = false,
                                                                text = songList[it]!![0],
                                                                sub = "${songList[it]!![1].ifBlank { "?" }} - ${songList[it]!![2].ifBlank { "?" }}",
                                                                enableHaptic = false,
                                                                hapticStrength = hapticStrength.intValue
                                                            )
                                                    }
                                                }
                                            }
                                        }
                                    }
//                                    }
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

                        if (songList.isNotEmpty() || !refreshComplete || showFab.value) {
                            FloatingActionButton(
                                expanded = showFab,
                                heightExpand = 150.dp,
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
                                        showLoadingProgressBar = true
                                        if (sortMethod.intValue == 4)
                                            coroutineScope.launch(Dispatchers.IO) {
                                                dataStore.edit { settings ->
                                                    settings[DataStoreConstants.SORT_METHOD] = 0
                                                    tagPage.getMusicList(
                                                        songList,
                                                        0,
                                                        selectedSongList
                                                    )
                                                }
                                            }
                                        else
                                            coroutineScope.launch(Dispatchers.IO) {
                                                dataStore.edit { settings ->
                                                    settings[DataStoreConstants.SORT_METHOD] =
                                                        ++sortMethod.intValue
                                                    tagPage.getMusicList(
                                                        songList,
                                                        sortMethod.intValue,
                                                        selectedSongList
                                                    )
                                                }
                                            }
                                        showLoadingProgressBar = false
                                    },
                                    text = when (sortMethod.intValue) {
                                        0 -> stringResource(id = R.string.default_sort)
                                        1 -> stringResource(id = R.string.title_asc)
                                        2 -> stringResource(id = R.string.title_desc)
                                        3 -> stringResource(id = R.string.modify_date_asc)
                                        4 -> stringResource(id = R.string.modify_date_desc)
                                        else -> stringResource(id = R.string.default_sort)
                                    },
                                    iconPainter = painterResource(id = R.drawable.sort),
                                    iconColor = SaltTheme.colors.text,
                                    iconPaddingValues = PaddingValues(all = 0.5.dp)
                                )
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
                        }
                        PullRefreshIndicator(
                            modifier = Modifier.align(Alignment.TopCenter),
                            refreshing = !refreshComplete,
                            state = pullRefreshState,
                            backgroundColor = SaltTheme.colors.background,
                            contentColor = SaltTheme.colors.highlight
                        )
                    }
                }

                1 -> {
                    Box {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            RoundedColumn {
                                val infoPageScrollState = rememberScrollState()
                                LaunchedEffect(Unit) {
                                    showSearchInput = false
                                    infoPageScrollState.scrollTo(0)
//                                    infoPageScrollState.animateScrollTo(
//                                        value = 0,
//                                        animationSpec = spring(2f)
//                                    )
                                }
                                Column(modifier = Modifier.verticalScroll(infoPageScrollState)) {
                                    ItemTitle(
                                        text = stringResource(id = R.string.cover_pic),
                                        paddingValues = PaddingValues(
                                            start = SaltTheme.dimens.padding,
                                            end = SaltTheme.dimens.padding,
                                            top = SaltTheme.dimens.subPadding,
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
                                            val bitmap =
                                                BitmapFactory.decodeByteArray(it, 0, it.size)
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        top = 4.dp,
                                                        bottom = 8.dp,
                                                    ),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Image(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = SaltTheme.dimens.padding)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .combinedClickable(
                                                            onClick = {
                                                                coverImageModified.value = true
                                                                tagPage.selectCoverImage()
                                                            },
                                                            onLongClick = {
                                                                showConfirmDeleteCoverDialog = true
                                                            }
                                                        ),
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = stringResource(id = R.string.cover_pic),
                                                    contentScale = ContentScale.FillWidth
                                                )
                                            }
                                        } else {
                                            BasicButton(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        start = SaltTheme.dimens.padding,
                                                        end = SaltTheme.dimens.padding,
                                                        bottom = 8.dp
                                                    ),
                                                onClick = {
                                                    coverImageModified.value = true
                                                    tagPage.selectCoverImage()
                                                },
                                                backgroundColor = SaltTheme.colors.subText.copy(
                                                    alpha = 0.1f
                                                ),
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
                                        title = stringResource(id = R.string.lyricist),
                                        editText = musicInfo.value["lyricist"],
                                        onChange = { musicInfo.value += ("lyricist" to it) },
                                        onClear = { musicInfo.value -= "lyricist" },
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
                                        title = stringResource(id = R.string.arranger),
                                        editText = musicInfo.value["arranger"],
                                        onChange = { musicInfo.value += ("arranger" to it) },
                                        onClear = { musicInfo.value -= "arranger" },
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
                                        textStyle = TextStyle(
                                            fontSize = 15.sp,
                                            color = SaltTheme.colors.text
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
            start = SaltTheme.dimens.padding,
            end = SaltTheme.dimens.padding,
            top = SaltTheme.dimens.subPadding
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
            start = SaltTheme.dimens.padding,
            end = SaltTheme.dimens.padding,
            bottom = 8.dp,
            top = 4.dp
        ),
        hapticStrength = hapticStrength,
        textStyle = textStyle
    )
}