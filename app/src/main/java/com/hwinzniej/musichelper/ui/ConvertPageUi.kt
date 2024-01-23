package com.hwinzniej.musichelper.ui

import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.activity.ConvertPage
import com.hwinzniej.musichelper.utils.MyVibrationEffect
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.popup.rememberPopupState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(UnstableSaltApi::class, ExperimentalFoundationApi::class)
@Composable
fun ConvertPageUi(
    convertPage: ConvertPage,
    selectedSourceApp: MutableIntState,
    databaseFileName: MutableState<String>,
    useCustomResultFile: MutableState<Boolean>,
    customResultFileName: MutableState<String>,
    showLoadingProgressBar: MutableState<Boolean>,
    showErrorDialog: MutableState<Boolean>,
    errorDialogTitle: MutableState<String>,
    errorDialogContent: MutableState<String>,
    playlistName: MutableList<String>,
    playlistEnabled: MutableList<Int>,
    playlistSum: MutableList<Int>,
    currentPage: MutableIntState,
    selectedMatchingMode: MutableIntState,
    enableBracketRemoval: MutableState<Boolean>,
    enableArtistNameMatch: MutableState<Boolean>,
    enableAlbumNameMatch: MutableState<Boolean>,
    similarity: MutableFloatState,
    convertResult: MutableMap<Int, Array<String>>,
    inputSearchWords: MutableState<String>,
    searchResult: MutableList<Array<String>>,
    showDialogProgressBar: MutableState<Boolean>,
    showSaveDialog: MutableState<Boolean>,
    mainActivityPageState: PagerState,
    enableHaptic: MutableState<Boolean>,
    useRootAccess: MutableState<Boolean>,
    sourceApp: MutableState<String>,
    databaseFilePath: MutableState<String>,
    showSelectSourceDialog: MutableState<Boolean>,
    multiSource: MutableList<Array<String>>,
) {
    val sourceAppPopupMenuState = rememberPopupState()
    val matchingModePopupMenuState = rememberPopupState()
    val pages = listOf("0", "1", "2", "3")
    val pageState = rememberPagerState(pageCount = { pages.size })
    var showSetSimilarityDialog by remember { mutableStateOf(false) }
    val resultPages = listOf("0", "1")
    val resultPageState = rememberPagerState(pageCount = { resultPages.size })
    var showSelectedSongInfoDialog by remember { mutableStateOf(false) }
    var selectedSongIndex by remember { mutableIntStateOf(-1) }
    val coroutine = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var selectedSearchResult by remember { mutableIntStateOf(-1) }
    val filterPopupMenuState = rememberPopupState()
    var showConfirmGoBackDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var showOriginalSonglist by remember { mutableIntStateOf(1) }
    val originalSonglistPopupMenuState = rememberPopupState()
    var selectedTargetApp by remember { mutableIntStateOf(0) }
    val targetAppPopupMenuState = rememberPopupState()
    var selectedMultiSourceApp by remember { mutableIntStateOf(-1) }

    BackHandler(enabled = (currentPage.intValue != 0 && mainActivityPageState.currentPage == 1)) {
        if (convertResult.isEmpty()) {
            if (currentPage.intValue == 3) {
                currentPage.intValue = 0
                selectedSourceApp.intValue = 0
                databaseFileName.value = ""
                useCustomResultFile.value = false
                customResultFileName.value = ""
                playlistName.clear()
                playlistEnabled.clear()
                playlistSum.clear()
                selectedMatchingMode.intValue = 1
                enableBracketRemoval.value = false
                enableArtistNameMatch.value = true
                enableAlbumNameMatch.value = true
                similarity.floatValue = 85f
                convertResult.clear()
                inputSearchWords.value = ""
                searchResult.clear()
                selectedSongIndex = -1
                selectedSearchResult = -1
                showOriginalSonglist = 1
                selectedTargetApp = 0
                selectedMultiSourceApp = -1
            } else
                currentPage.intValue--
        } else {
            showConfirmGoBackDialog = true
        }
    }

    LaunchedEffect(key1 = useRootAccess.value) {
        databaseFileName.value = ""
        databaseFilePath.value = ""
    }

    if (showConfirmGoBackDialog) {
        YesNoDialog(
            onDismiss = { showConfirmGoBackDialog = false },
            onCancel = { showConfirmGoBackDialog = false },
            onConfirm = {
                showConfirmGoBackDialog = false
                convertResult.clear()
            },
            title = stringResource(id = R.string.go_back_confirm_dialog_title),
            content = stringResource(id = R.string.go_back_confirm_dialog_content),
            enableHaptic = enableHaptic.value
        )
    }

    if (showErrorDialog.value) {
        YesDialog(
            onDismissRequest = { showErrorDialog.value = false },
            title = errorDialogTitle.value,
            content = errorDialogContent.value,
            enableHaptic = enableHaptic.value
        )
    }

    if (showSelectSourceDialog.value) {
        YesNoDialog(
            onDismiss = {
                convertPage.haveError = true
                convertPage.loadingProgressSema.release()
                showSelectSourceDialog.value = false
            },
            onCancel = {
                convertPage.haveError = true
                convertPage.loadingProgressSema.release()
                showSelectSourceDialog.value = false
            },
            onConfirm = {
                if (selectedMultiSourceApp != -1) {
                    showDialogProgressBar.value = true
                    convertPage.getSelectedMultiSource(selectedMultiSourceApp)
                }
            },
            title = stringResource(R.string.select_source_app),
            content = "",
            cancelText = stringResource(R.string.cancel_button_text),
            confirmText = stringResource(R.string.ok_button_text),
            onlyComposeView = true,
            enableHaptic = enableHaptic.value,
            customContent = {
                Box {
                    if (showDialogProgressBar.value) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(1f),
                            color = SaltTheme.colors.highlight,
                            trackColor = SaltTheme.colors.background
                        )
                    }
                    LazyColumn {
                        items(multiSource.size) { index ->
                            ItemCheck(
                                state = selectedMultiSourceApp == index,
                                onChange = {
                                    selectedMultiSourceApp = if (it) index else -1
                                },
                                text = multiSource[index][1],
                                sub = multiSource[index][0],
                                iconAtLeft = false,
                                enableHaptic = enableHaptic.value
                            )
                        }
                    }
                }
            }
        )
    }

    LaunchedEffect(key1 = showSetSimilarityDialog) {
        if (showSetSimilarityDialog) {
            MyVibrationEffect(context, enableHaptic.value).dialog()
        }
    }

    if (showSetSimilarityDialog) {
        var slideSimilarity by remember { mutableFloatStateOf(similarity.floatValue) }
        LaunchedEffect(key1 = slideSimilarity) {
            MyVibrationEffect(context, enableHaptic.value).dragMove()
        }
        YesNoDialog(
            onDismiss = { showSetSimilarityDialog = false },
            onCancel = { showSetSimilarityDialog = false },
            onConfirm = {
                similarity.floatValue = slideSimilarity
                showSetSimilarityDialog = false
            },
            title = stringResource(R.string.set_similarity_dialog_title),
            content = "",
            cancelText = stringResource(R.string.cancel_button_text),
            confirmText = stringResource(R.string.ok_button_text),
            onlyComposeView = true,
            enableHaptic = enableHaptic.value,
            customContent = {
                Column {
                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { slideSimilarity-- },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = SaltTheme.colors.text
                            ),
                        ) {
                            Text("-", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${slideSimilarity.roundToInt()}%",
                            fontSize = 18.sp,
                            color = SaltTheme.colors.text
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { slideSimilarity++ },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = SaltTheme.colors.text
                            ),
                        ) {
                            Text(text = "+", fontSize = 22.sp)
                        }
                    }

                    Slider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        value = slideSimilarity,
                        onValueChange = { slideSimilarity = it.roundToInt().toFloat() },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = SaltTheme.colors.highlight,
                            activeTrackColor = SaltTheme.colors.highlight,
                            inactiveTrackColor = SaltTheme.colors.subBackground
                        )
                    )
                }
            }
        )
    }

    LaunchedEffect(key1 = showSelectedSongInfoDialog) {
        if (showSelectedSongInfoDialog) {
            MyVibrationEffect(context, enableHaptic.value).dialog()
        }
    }

    if (showSelectedSongInfoDialog) {
        var selectedAllIndex by remember { mutableIntStateOf(-1) }
        YesNoDialog(
            onDismiss = {
                showSelectedSongInfoDialog = false
                inputSearchWords.value = ""
            },
            onCancel = {
                showSelectedSongInfoDialog = false
                inputSearchWords.value = ""
            },
            onConfirm = {
                if (selectedSearchResult != -1) {
                    showSelectedSongInfoDialog = false
                    inputSearchWords.value = ""
                    convertPage.saveModificationSong(selectedSongIndex, selectedAllIndex)
                }
            },
            title = stringResource(id = R.string.modify_conversion_results),
            content = "",
            cancelText = stringResource(R.string.cancel_button_text),
            confirmText = stringResource(R.string.ok_button_text),
            onlyComposeView = true,
            enableHaptic = enableHaptic.value,
            customContent = {
                val focus = LocalFocusManager.current
                Box {
                    if (showDialogProgressBar.value) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(1f),
                            color = SaltTheme.colors.highlight,
                            trackColor = SaltTheme.colors.background
                        )
                    }
                    Column {
                        Row {
                            Column(modifier = Modifier.weight(1f)) {
                                ItemEdit(
                                    text = inputSearchWords.value,
                                    hint = stringResource(R.string.search_song_library),
                                    onChange = {
                                        inputSearchWords.value = it
                                    },
                                    showClearButton = inputSearchWords.value != "",
                                    onClear = {
                                        inputSearchWords.value = ""
                                    },
                                    enableHaptic = enableHaptic.value
                                )
                            }
                            BasicButton(
                                modifier = Modifier
                                    .padding(top = 12.dp, bottom = 12.dp, end = 16.dp),
                                enabled = true,
                                onClick = { showDeleteDialog = true },
                                backgroundColor = colorResource(id = R.color.unmatched),
                                enableHaptic = enableHaptic.value
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .size(20.dp),
                                    painter = painterResource(id = R.drawable.ic_delete),
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                        AnimatedVisibility(visible = searchResult.isNotEmpty()) {  //TODO 仅第一次新增搜索结果时有动画，变化与删除时无动画
                            LazyColumn(
                                modifier = Modifier.weight(1f)
                            ) {
                                items(searchResult.size) { index ->
                                    ItemCheck(
                                        state = selectedSearchResult == index,
                                        onChange = {
                                            focus.clearFocus()
                                            if (!it) selectedSearchResult = -1
                                            else {
                                                selectedSearchResult = index
                                                selectedAllIndex = searchResult[index][3].toInt()
                                            }
                                        },
                                        text = searchResult[index][0],
                                        sub = if (searchResult[index].size == 1) null else "${searchResult[index][1]} - ${searchResult[index][2]}",
                                        iconAtLeft = false,
                                        hideIcon = searchResult[index].size == 1,
                                        enabled = searchResult[index].size != 1,
                                        enableHaptic = enableHaptic.value
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ItemTitle(text = stringResource(R.string.songlist_song_info))
                        ItemValue(
                            text = stringResource(id = R.string.song_name),
                            rightSub = convertResult[selectedSongIndex]!![2],
                            clickable = true,
                            onClick = {
                                inputSearchWords.value =
                                    "${inputSearchWords.value}${convertResult[selectedSongIndex]!![2]}"
                            }
                        )
                        ItemValue(
                            text = stringResource(id = R.string.singer).replace(
                                "(：)|(: )".toRegex(),
                                ""
                            ),
                            rightSub = convertResult[selectedSongIndex]!![4],
                            clickable = true,
                            onClick = {
                                inputSearchWords.value =
                                    "${inputSearchWords.value}${convertResult[selectedSongIndex]!![4]}"
                            }
                        )
                        ItemValue(
                            text = stringResource(id = R.string.album).replace(
                                "(：)|(: )".toRegex(),
                                ""
                            ),
                            rightSub = convertResult[selectedSongIndex]!![6],
                            clickable = true,
                            onClick = {
                                inputSearchWords.value =
                                    "${inputSearchWords.value}${convertResult[selectedSongIndex]!![6]}"
                            }
                        )
                    }
                }
            }
        )
    }

    LaunchedEffect(key1 = showSaveDialog.value) {
        if (showSaveDialog.value) {
            MyVibrationEffect(context, enableHaptic.value).dialog()
        }
    }

    if (showSaveDialog.value) {
        var saveSuccessSongs by remember { mutableStateOf(true) }
        var saveCautionSongs by remember { mutableStateOf(true) }
        var saveManualSongs by remember { mutableStateOf(true) }
        YesNoDialog(
            onDismiss = { showSaveDialog.value = false },
            onCancel = { showSaveDialog.value = false },
            onConfirm = {
                convertPage.saveCurrentConvertResult(
                    saveSuccessSongs,
                    saveCautionSongs,
                    saveManualSongs,
                    "${playlistName[playlistEnabled.indexOfFirst { it == 1 }]}${
                        when (selectedTargetApp) {
                            0 -> ".txt"
                            1 -> ".m3u"
                            2 -> ".m3u8"
                            else -> ""
                        }
                    }"
                )
            },
            title = stringResource(id = R.string.save_conversion_results),
            content = "",
            cancelText = stringResource(R.string.cancel_button_text),
            confirmText = stringResource(R.string.ok_button_text),
            onlyComposeView = true,
            enableHaptic = enableHaptic.value,
            customContent = {
                Box {
                    if (showDialogProgressBar.value) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(1f),
                            color = SaltTheme.colors.highlight,
                            trackColor = SaltTheme.colors.background
                        )
                    }
                    Column {
                        ItemTitle(text = stringResource(R.string.state_of_the_song_to_be_saved))
//                        ItemText(text = stringResource(R.string.state_of_the_song_to_be_saved))
                        ItemSwitcher(
                            state = saveSuccessSongs,
                            onChange = { saveSuccessSongs = it },
                            text = "${stringResource(R.string.match_success)} - ${
                                convertResult.count {
                                    it.value[0] == "0"
                                }
                            }",
                            enableHaptic = enableHaptic.value
                        )
                        ItemSwitcher(
                            state = saveCautionSongs,
                            onChange = { saveCautionSongs = it },
                            text = "${stringResource(R.string.match_caution)} - ${
                                convertResult.count {
                                    it.value[0] == "1"
                                }
                            }",
                            enableHaptic = enableHaptic.value
                        )
                        ItemSwitcher(
                            state = saveManualSongs,
                            onChange = { saveManualSongs = it },
                            text = "${stringResource(R.string.match_manual)} - ${
                                convertResult.count {
                                    it.value[0] == "2"
                                }
                            }",
                            enableHaptic = enableHaptic.value
                        )
//                        ItemText(
//                            text = "${stringResource(R.string.result_file_save_location)}\n${
//                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//                            }/MusicHelper/${playlistName[playlistEnabled.indexOfFirst { it == 1 }]}.txt"
//                        )
                    }
                }
            }
        )
    }

    if (showDeleteDialog) {
        YesNoDialog(
            onDismiss = { showDeleteDialog = false },
            onCancel = { showDeleteDialog = false },
            onConfirm = {
                inputSearchWords.value = ""
                showDeleteDialog = false
                showSelectedSongInfoDialog = false
                convertResult.remove(selectedSongIndex)
                Toast.makeText(
                    context,
                    context.getString(R.string.delete_success),
                    Toast.LENGTH_SHORT
                ).show()
            },
            title = stringResource(id = R.string.delete_dialog_title),
            content = "${stringResource(id = R.string.delete_dialog_content)}\n${
                convertResult[selectedSongIndex]!![2]
            } - ${convertResult[selectedSongIndex]!![4]} - ${convertResult[selectedSongIndex]!![6]}",
            confirmButtonColor = colorResource(id = R.color.unmatched),
            enableHaptic = enableHaptic.value
        )
    }

    LaunchedEffect(key1 = currentPage.intValue) {
        pageState.animateScrollToPage(currentPage.intValue, animationSpec = spring(2f))
    }
    LaunchedEffect(key1 = convertResult.isEmpty()) {
        if (convertResult.isEmpty()) {
            resultPageState.animateScrollToPage(0, animationSpec = spring(2f))
        } else {
            resultPageState.animateScrollToPage(1, animationSpec = spring(2f))
        }
    }
    LaunchedEffect(key1 = inputSearchWords.value) {
        job?.cancel()
        job = coroutine.launch {
            delay(500)
            showDialogProgressBar.value = true
            delay(500)
            convertPage.searchSong()
            selectedSearchResult = -1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {
        TitleBar(
            onBack = {
                if (convertResult.isEmpty()) {
                    MyVibrationEffect(context, enableHaptic.value).click()
                    if (currentPage.intValue == 3) {
                        currentPage.intValue = 0
                        selectedSourceApp.intValue = 0
                        databaseFileName.value = ""
                        useCustomResultFile.value = false
                        customResultFileName.value = ""
                        playlistName.clear()
                        playlistEnabled.clear()
                        playlistSum.clear()
                        selectedMatchingMode.intValue = 1
                        enableBracketRemoval.value = false
                        enableArtistNameMatch.value = true
                        enableAlbumNameMatch.value = true
                        similarity.floatValue = 85f
                        convertResult.clear()
                        inputSearchWords.value = ""
                        searchResult.clear()
                        selectedSongIndex = -1
                        selectedSearchResult = -1
                        showOriginalSonglist = 1
                        selectedTargetApp = 0
                        selectedMultiSourceApp = -1
                    } else
                        currentPage.intValue--
                } else {
                    showConfirmGoBackDialog = true
                }
            },
            text = stringResource(R.string.convert_function_name),
            showBackBtn = pageState.currentPage != 0
        )
        Box {
            if (showLoadingProgressBar.value) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f),
                    color = SaltTheme.colors.highlight,
                    trackColor = SaltTheme.colors.background
                )
            }

            HorizontalPager(
                state = pageState,
                modifier = Modifier
                    .fillMaxSize(),
                userScrollEnabled = false,
                beyondBoundsPageCount = 1
            ) { page ->
                when (page) {
                    0 -> {
                        Column(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .fillMaxSize()
                                .background(color = SaltTheme.colors.background)
                                .verticalScroll(rememberScrollState())
                        ) {
                            RoundedColumn {
                                ItemTitle(text = stringResource(R.string.source_of_songlist_app))
                                ItemPopup(
                                    state = sourceAppPopupMenuState,
                                    text = stringResource(R.string.select_source_of_songlist),
                                    selectedItem = when (selectedSourceApp.intValue) {
                                        1 -> stringResource(R.string.source_netease_cloud_music)
                                        2 -> stringResource(R.string.source_qq_music)
                                        3 -> stringResource(R.string.source_kugou_music)
                                        4 -> stringResource(R.string.source_kuwo_music)
                                        else -> ""
                                    },
                                    popupWidth = 180,
                                    sub = if (useRootAccess.value) stringResource(
                                        R.string.with_root_access
                                    ) else null
                                ) {
                                    PopupMenuItem(
                                        onClick = {
                                            MyVibrationEffect(context, enableHaptic.value).click()
                                            selectedSourceApp.intValue = 1
                                            sourceAppPopupMenuState.dismiss()
                                            databaseFileName.value = ""
                                        },
                                        selected = selectedSourceApp.intValue == 1,
                                        text = stringResource(R.string.source_netease_cloud_music),
                                        iconPainter = painterResource(id = R.drawable.cloudmusic),
                                        iconColor = SaltTheme.colors.text
                                    )
                                    PopupMenuItem(
                                        onClick = {
                                            MyVibrationEffect(context, enableHaptic.value).click()
                                            selectedSourceApp.intValue = 2
                                            sourceAppPopupMenuState.dismiss()
                                            databaseFileName.value = ""
                                        },
                                        selected = selectedSourceApp.intValue == 2,
                                        text = stringResource(R.string.source_qq_music),
                                        iconPainter = painterResource(id = R.drawable.qqmusic),
                                        iconColor = SaltTheme.colors.text
                                    )

                                    PopupMenuItem(
                                        onClick = {
                                            MyVibrationEffect(context, enableHaptic.value).click()
                                            selectedSourceApp.intValue = 3
                                            sourceAppPopupMenuState.dismiss()
                                            databaseFileName.value = ""
                                        },
                                        selected = selectedSourceApp.intValue == 3,
                                        text = stringResource(R.string.source_kugou_music),
                                        iconPainter = painterResource(id = R.drawable.kugou),
                                        iconColor = SaltTheme.colors.text,
                                        iconPaddingValues = PaddingValues(
                                            start = 1.5.dp,
                                            end = 1.5.dp,
                                            top = 1.5.dp,
                                            bottom = 1.5.dp
                                        )
                                    )
                                    PopupMenuItem(
                                        onClick = {
                                            MyVibrationEffect(context, enableHaptic.value).click()
                                            selectedSourceApp.intValue = 4
                                            sourceAppPopupMenuState.dismiss()
                                            databaseFileName.value = ""
                                        },
                                        selected = selectedSourceApp.intValue == 4,
                                        text = stringResource(R.string.source_kuwo_music),
                                        iconPainter = painterResource(id = R.drawable.kuwo),
                                        iconColor = SaltTheme.colors.text,
                                        iconPaddingValues = PaddingValues(
                                            start = 1.5.dp,
                                            end = 1.5.dp,
                                            top = 1.5.dp,
                                            bottom = 1.5.dp
                                        )
                                    )
                                }
                                AnimatedVisibility(
                                    visible = (selectedSourceApp.intValue != 0) && !useRootAccess.value
                                ) {
                                    Item(
                                        onClick = { convertPage.selectDatabaseFile() },
                                        text = stringResource(R.string.select_database_file_match_to_source).replace(
                                            "#",
                                            when (selectedSourceApp.intValue) {
                                                1 -> stringResource(R.string.source_netease_cloud_music)
                                                2 -> stringResource(R.string.source_qq_music)
                                                3 -> stringResource(R.string.source_kugou_music)
                                                4 -> stringResource(R.string.source_kuwo_music)
                                                else -> ""
                                            }
                                        )
//                                        if (selectedSourceApp.intValue == 0) {
//                                            stringResource(R.string.please_select_source_app)
//                                        } else {
//                                            if (useRootAccess.value)
//                                                stringResource(R.string.get_file_with_root).replace(
//                                                    "#",
//                                                    sourceApp.value
//                                                )
//                                            else
//                                                stringResource(R.string.select_database_file_match_to_source).replace(
//                                                    "#",
//                                                    sourceApp.value
//                                                )
//                                        },
//                                        sub = if (useRootAccess.value && (selectedSourceApp.intValue != 0)) stringResource(
//                                            R.string.with_root_access
//                                        ) else null,
                                    )
                                }
                                AnimatedVisibility(
                                    visible = (databaseFileName.value != "") && !useRootAccess.value
                                ) {
                                    ItemValue(
                                        text = stringResource(R.string.you_have_selected),
                                        rightSub = databaseFileName.value
                                    )
                                }
                            }

                            sourceApp.value = when (selectedSourceApp.intValue) {
                                1 -> stringResource(R.string.source_netease_cloud_music)
                                2 -> stringResource(R.string.source_qq_music)
                                3 -> stringResource(R.string.source_kugou_music)
                                4 -> stringResource(R.string.source_kuwo_music)
                                else -> ""
                            }

                            RoundedColumn {
                                ItemTitle(text = stringResource(R.string.import_result_file))
                                ItemSwitcher(
                                    state = useCustomResultFile.value,
                                    onChange = {
                                        useCustomResultFile.value = it
                                    },
                                    text = stringResource(R.string.use_custom_result_file),
                                    sub = stringResource(R.string.use_other_result_file).replace(
                                        "#",
                                        stringResource(id = R.string.app_name)
                                    ),
                                    enableHaptic = enableHaptic.value
                                )
                                AnimatedVisibility(
                                    visible = useCustomResultFile.value
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize()
                                            .background(color = SaltTheme.colors.subBackground)
                                    ) {
                                        Item(
                                            onClick = { convertPage.selectResultFile() },
                                            text = stringResource(R.string.select_result_file_item_title).replace(
                                                "#",
                                                stringResource(id = R.string.app_name)
                                            ),
                                        )
                                        AnimatedVisibility(
                                            visible = customResultFileName.value != ""
                                        ) {
                                            ItemValue(
                                                text = stringResource(R.string.you_have_selected),
                                                rightSub = customResultFileName.value
                                            )
                                        }
                                    }
                                }
                            }

                            RoundedColumn {
                                ItemTitle(text = stringResource(R.string.target_formats))
                                ItemPopup(
                                    state = targetAppPopupMenuState,
                                    text = stringResource(R.string.using_player),
                                    selectedItem = when (selectedTargetApp) {
                                        0 -> "Salt Player"
                                        1 -> "APlayer"
                                        2 -> "Poweramp"
                                        else -> ""
                                    },
                                    popupWidth = 160
                                ) {
                                    PopupMenuItem(
                                        onClick = {
                                            MyVibrationEffect(context, enableHaptic.value).click()
                                            selectedTargetApp = 0
                                            targetAppPopupMenuState.dismiss()
                                        },
                                        selected = selectedTargetApp == 0,
                                        text = "Salt Player",
                                        iconPainter = painterResource(id = R.drawable.saltplayer),
                                        iconColor = SaltTheme.colors.text,
//                                        iconPaddingValues = PaddingValues(
//                                            start = 1.5.dp,
//                                            end = 1.5.dp,
//                                            top = 1.5.dp,
//                                            bottom = 1.5.dp
//                                        )
                                    )
                                    PopupMenuItem(
                                        onClick = {
                                            MyVibrationEffect(context, enableHaptic.value).click()
                                            selectedTargetApp = 1
                                            targetAppPopupMenuState.dismiss()
                                        },
                                        selected = selectedTargetApp == 1,
                                        text = "APlayer",
                                        iconPainter = painterResource(id = R.drawable.aplayer),
                                        iconColor = SaltTheme.colors.text,
                                        iconPaddingValues = PaddingValues(
                                            start = 1.dp,
                                            end = 1.dp,
                                            top = 1.dp,
                                            bottom = 1.dp
                                        )
                                    )
                                    PopupMenuItem(
                                        onClick = {
                                            MyVibrationEffect(context, enableHaptic.value).click()
                                            selectedTargetApp = 2
                                            targetAppPopupMenuState.dismiss()
                                        },
                                        selected = selectedTargetApp == 2,
                                        text = "Poweramp",
                                        iconPainter = painterResource(id = R.drawable.poweramp),
                                        iconColor = SaltTheme.colors.text,
                                        iconPaddingValues = PaddingValues(
                                            start = 2.dp,
                                            end = 2.dp,
                                            top = 2.dp,
                                            bottom = 2.dp
                                        )
                                    )
                                }
                            }

                            AnimatedContent(
                                targetState = showLoadingProgressBar.value,
                                label = "",
                                transitionSpec = {
                                    if (targetState != initialState) {
                                        fadeIn() togetherWith fadeOut()
                                    } else {
                                        fadeIn() togetherWith fadeOut()
                                    }
                                }) {
                                ItemContainer {
                                    TextButton(
                                        onClick = {
                                            selectedMultiSourceApp = -1
                                            convertPage.requestPermission()
                                        },
                                        text = stringResource(R.string.next_step_text),
                                        enabled = !it,
                                        enableHaptic = enableHaptic.value
                                    )
                                }
                            }
                        }

                    }

                    1 -> {
                        Column(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .fillMaxSize()
                                .background(color = SaltTheme.colors.background)
                                .verticalScroll(rememberScrollState())
                        ) {
                            RoundedColumn {
                                ItemTitle(text = stringResource(R.string.select_needed_musiclist))
                                AnimatedVisibility(
                                    visible = playlistEnabled.isNotEmpty()
                                ) {
                                    ItemContainer {
                                        Column(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(color = SaltTheme.colors.subBackground)
                                        ) {
                                            ItemCheck(
                                                state = playlistEnabled.all { it == 1 },
                                                onChange = {
                                                    if (it) {
                                                        playlistEnabled.replaceAll { 1 }
                                                    } else {
                                                        playlistEnabled.replaceAll { 0 }
                                                    }
                                                },
                                                text = stringResource(R.string.select_all),
                                                enableHaptic = enableHaptic.value
                                            )
                                            LazyColumn(
                                                modifier = Modifier.heightIn(
                                                    max = (LocalConfiguration.current.screenHeightDp / 1.9).dp
                                                )
                                            ) {
                                                items(playlistEnabled.size) { index ->
                                                    ItemCheck(
                                                        state = playlistEnabled[index] != 0,
                                                        onChange = {
                                                            if (it)
                                                                playlistEnabled[index] = 1
                                                            else
                                                                playlistEnabled[index] = 0
                                                        },
                                                        text = playlistName[index],
                                                        sub = stringResource(R.string.total).replace(
                                                            "#",
                                                            playlistSum[index].toString()
                                                        ),
                                                        enableHaptic = enableHaptic.value
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            ItemValue(
                                                text = stringResource(R.string.user_selected).replace(
                                                    "#",
                                                    playlistEnabled.count { it != 0 }.toString()
                                                ),
                                                rightSub = stringResource(R.string.in_total).replace(
                                                    "#",
                                                    playlistEnabled.size.toString()
                                                )
                                            )
                                        }
                                    }
                                }
                            }
//                RoundedColumn {
                            ItemContainer {
                                TextButton(
                                    onClick = { convertPage.checkSongListSelection() },
                                    text = stringResource(R.string.next_step_text),
                                    enableHaptic = enableHaptic.value
                                )
                            }
//                }
                        }
                    }

                    2 -> {
                        VerticalPager(
                            state = resultPageState,
                            modifier = Modifier
                                .fillMaxSize(),
                            userScrollEnabled = false,
                            beyondBoundsPageCount = 1
                        ) { resultPage ->
                            when (resultPage) {
                                0 -> {
                                    Column(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .fillMaxSize()
                                            .background(color = SaltTheme.colors.background)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        RoundedColumn {
                                            ItemTitle(text = stringResource(R.string.current_songlist_info))
                                            ItemValue(
                                                text = stringResource(R.string.songlist_sequence),
                                                rightSub = "${
                                                    stringResource(R.string.current_no).replace("#",
                                                        if (playlistEnabled.count { it == 2 } == -1)
                                                            "0"
                                                        else {
                                                            (playlistEnabled.count { it == 2 } + 1).toString()
                                                        })
                                                } - ${
                                                    stringResource(R.string.in_total).replace(
                                                        "#",
                                                        playlistEnabled.count { it != 0 }.toString()
                                                    )
                                                }"
                                            )
                                            ItemValue(
                                                text = stringResource(R.string.songlist_name),
                                                rightSub = if (playlistEnabled.indexOfFirst { it == 1 } == -1) "" else playlistName[playlistEnabled.indexOfFirst { it == 1 }]
                                            )
                                            ItemValue(
                                                text = stringResource(R.string.songlist_sum),
                                                rightSub = "${if (playlistEnabled.indexOfFirst { it == 1 } == -1) "" else playlistSum[playlistEnabled.indexOfFirst { it == 1 }]}"
                                            )
                                        }
                                        RoundedColumn {
                                            ItemTitle(text = stringResource(R.string.convert_options))
                                            Item(
                                                onClick = { showSetSimilarityDialog = true },
                                                text = stringResource(R.string.set_similarity_item),
                                                rightSub = "${similarity.floatValue.roundToInt()}%"
                                            )
                                            ItemPopup(
                                                state = matchingModePopupMenuState,
                                                text = stringResource(R.string.matching_mode),
                                                selectedItem = when (selectedMatchingMode.intValue) {
                                                    1 -> stringResource(R.string.split_matching)
                                                    2 -> stringResource(R.string.overall_matching)
                                                    else -> ""
                                                }
                                            ) {
                                                PopupMenuItem(
                                                    onClick = {
                                                        MyVibrationEffect(
                                                            context,
                                                            enableHaptic.value
                                                        ).click()
                                                        selectedMatchingMode.intValue = 1
                                                        matchingModePopupMenuState.dismiss()
                                                    },
                                                    selected = selectedMatchingMode.intValue == 1,
                                                    text = stringResource(R.string.split_matching),
                                                    iconPainter = painterResource(id = R.drawable.split),
                                                    iconColor = SaltTheme.colors.text,
                                                    iconPaddingValues = PaddingValues(
                                                        start = 3.dp,
                                                        end = 2.dp,
                                                        top = 2.dp,
                                                        bottom = 2.dp
                                                    )
                                                )
                                                PopupMenuItem(
                                                    onClick = {
                                                        MyVibrationEffect(
                                                            context,
                                                            enableHaptic.value
                                                        ).click()
                                                        selectedMatchingMode.intValue = 2
                                                        matchingModePopupMenuState.dismiss()
                                                    },
                                                    selected = selectedMatchingMode.intValue == 2,
                                                    text = stringResource(R.string.overall_matching),
                                                    iconPainter = painterResource(id = R.drawable.overall),
                                                    iconColor = SaltTheme.colors.text,
                                                    iconPaddingValues = PaddingValues(
                                                        start = 3.dp,
                                                        end = 2.dp,
                                                        top = 2.dp,
                                                        bottom = 2.dp
                                                    )
                                                )
                                            }

                                            ItemSwitcher(
                                                state = enableBracketRemoval.value,
                                                onChange = { enableBracketRemoval.value = it },
                                                text = stringResource(R.string.enable_bracket_removal),
                                                enableHaptic = enableHaptic.value
                                            )
                                            ItemSwitcher(
                                                state = enableArtistNameMatch.value,
                                                onChange = { enableArtistNameMatch.value = it },
                                                text = stringResource(R.string.enable_artist_name_match),
                                                enableHaptic = enableHaptic.value
                                            )
                                            ItemSwitcher(
                                                state = enableAlbumNameMatch.value,
                                                onChange = { enableAlbumNameMatch.value = it },
                                                text = stringResource(R.string.enable_album_name_match),
                                                enableHaptic = enableHaptic.value
                                            )
                                        }
                                        AnimatedContent(
                                            targetState = showLoadingProgressBar.value,
                                            label = "",
                                            transitionSpec = {
                                                if (targetState != initialState) {
                                                    fadeIn() togetherWith fadeOut()
                                                } else {
                                                    fadeIn() togetherWith fadeOut()
                                                }
                                            }) {
                                            ItemContainer {
                                                TextButton(
                                                    onClick = {
                                                        selectedFilterIndex = 0
                                                        convertPage.previewResult()
                                                    },
                                                    text = stringResource(R.string.preview_convert_result),
                                                    enabled = !it,
                                                    enableHaptic = enableHaptic.value
                                                )
                                            }
                                        }
                                    }
                                }

                                1 -> {
                                    Column(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .fillMaxSize()
                                            .background(color = SaltTheme.colors.background)
                                    ) {
                                        RoundedColumn {
                                            ItemTitle(text = stringResource(R.string.filter))
                                            ItemPopup(
                                                state = originalSonglistPopupMenuState,
                                                text = stringResource(id = R.string.type_of_view),
                                                selectedItem = when (showOriginalSonglist) {
                                                    0 -> stringResource(id = R.string.original_songlist).replace(
                                                        "#",
                                                        when (selectedSourceApp.intValue) {
                                                            1 -> stringResource(R.string.source_netease_cloud_music)
                                                            2 -> stringResource(R.string.source_qq_music)
                                                            3 -> stringResource(R.string.source_kugou_music)
                                                            4 -> stringResource(R.string.source_kuwo_music)
                                                            else -> ""
                                                        }
                                                    )

                                                    1 -> stringResource(R.string.convert_result)
                                                    else -> ""
                                                }
                                            ) {
                                                PopupMenuItem(
                                                    onClick = {
                                                        MyVibrationEffect(
                                                            context,
                                                            enableHaptic.value
                                                        ).click()
                                                        showOriginalSonglist = 0
                                                        originalSonglistPopupMenuState.dismiss()
                                                    },
                                                    text = stringResource(id = R.string.original_songlist).replace(
                                                        "#",
                                                        "${
                                                            when (selectedSourceApp.intValue) {
                                                                1 -> stringResource(R.string.source_netease_cloud_music)
                                                                2 -> stringResource(R.string.source_qq_music)
                                                                3 -> stringResource(R.string.source_kugou_music)
                                                                4 -> stringResource(R.string.source_kuwo_music)
                                                                else -> ""
                                                            }
                                                        }\n"
                                                    ),
                                                    selected = showOriginalSonglist == 0,
                                                    iconPainter = when (selectedSourceApp.intValue) {
                                                        1 -> painterResource(id = R.drawable.cloudmusic)
                                                        2 -> painterResource(id = R.drawable.qqmusic)
                                                        3 -> painterResource(id = R.drawable.kugou)
                                                        4 -> painterResource(id = R.drawable.kuwo)
                                                        else -> painterResource(id = R.drawable.android)
                                                    },
                                                    iconColor = SaltTheme.colors.text,
                                                    iconPaddingValues = PaddingValues(
                                                        start = 2.dp,
                                                        end = 2.dp,
                                                        top = 2.dp,
                                                        bottom = 2.dp
                                                    )
                                                )
                                                PopupMenuItem(
                                                    onClick = {
                                                        MyVibrationEffect(
                                                            context,
                                                            enableHaptic.value
                                                        ).click()
                                                        showOriginalSonglist = 1
                                                        originalSonglistPopupMenuState.dismiss()
                                                    },
                                                    text = stringResource(R.string.convert_result),
                                                    selected = showOriginalSonglist == 1,
                                                    iconPainter = painterResource(id = R.drawable.result),
                                                    iconColor = SaltTheme.colors.text,
                                                    iconPaddingValues = PaddingValues(
                                                        start = 1.5.dp,
                                                        end = 1.5.dp,
                                                        top = 1.5.dp,
                                                        bottom = 1.5.dp
                                                    )
                                                )
                                            }
                                            ItemPopup(
                                                state = filterPopupMenuState,
                                                text = stringResource(id = R.string.convert_status),
                                                selectedItem = when (selectedFilterIndex) {
                                                    0 -> "${stringResource(id = R.string.all)} - ${convertResult.size}"
                                                    1 -> "${stringResource(id = R.string.match_success)} - ${
                                                        convertResult.count {
                                                            it.value[0] == "0"
                                                        }
                                                    }"

                                                    2 -> "${stringResource(id = R.string.match_caution)} - ${
                                                        convertResult.count {
                                                            it.value[0] == "1"
                                                        }
                                                    }"

                                                    3 -> "${stringResource(id = R.string.match_manual)} - ${
                                                        convertResult.count {
                                                            it.value[0] == "2"
                                                        }
                                                    }"

                                                    else -> ""
                                                },
                                                popupWidth = 180
                                            ) {
                                                PopupMenuItem(
                                                    onClick = {
                                                        MyVibrationEffect(
                                                            context,
                                                            enableHaptic.value
                                                        ).click()
                                                        selectedFilterIndex = 0
                                                        filterPopupMenuState.dismiss()
                                                    },
                                                    text = "${stringResource(id = R.string.all)} - ${convertResult.size}",
                                                    selected = selectedFilterIndex == 0,
                                                    iconPainter = painterResource(id = R.drawable.all),
                                                    iconColor = SaltTheme.colors.text,
                                                    iconPaddingValues = PaddingValues(
                                                        start = 2.dp,
                                                        end = 2.dp,
                                                        top = 2.dp,
                                                        bottom = 2.dp
                                                    )
                                                )
                                                PopupMenuItem(
                                                    onClick = {
                                                        MyVibrationEffect(
                                                            context,
                                                            enableHaptic.value
                                                        ).click()
                                                        selectedFilterIndex = 1
                                                        filterPopupMenuState.dismiss()
                                                    },
                                                    text = "${stringResource(id = R.string.match_success)} - ${
                                                        convertResult.count {
                                                            it.value[0] == "0"
                                                        }
                                                    }",
                                                    selected = selectedFilterIndex == 1,
                                                    iconPainter = painterResource(id = R.drawable.success),
                                                    iconColor = SaltTheme.colors.text,
                                                    iconPaddingValues = PaddingValues(
                                                        start = 2.dp,
                                                        end = 2.dp,
                                                        top = 2.dp,
                                                        bottom = 2.dp
                                                    )
                                                )
                                                PopupMenuItem(
                                                    onClick = {
                                                        MyVibrationEffect(
                                                            context,
                                                            enableHaptic.value
                                                        ).click()
                                                        selectedFilterIndex = 2
                                                        filterPopupMenuState.dismiss()
                                                    },
                                                    text = "${stringResource(id = R.string.match_caution)} - ${
                                                        convertResult.count {
                                                            it.value[0] == "1"
                                                        }
                                                    }",
                                                    selected = selectedFilterIndex == 2,
                                                    iconPainter = painterResource(id = R.drawable.caution),
                                                    iconColor = SaltTheme.colors.text,
                                                )
                                                PopupMenuItem(
                                                    onClick = {
                                                        MyVibrationEffect(
                                                            context,
                                                            enableHaptic.value
                                                        ).click()
                                                        selectedFilterIndex = 3
                                                        filterPopupMenuState.dismiss()
                                                    },
                                                    text = "${stringResource(id = R.string.match_manual)} - ${
                                                        convertResult.count {
                                                            it.value[0] == "2"
                                                        }
                                                    }",
                                                    selected = selectedFilterIndex == 3,
                                                    iconPainter = painterResource(id = R.drawable.manual),
                                                    iconColor = SaltTheme.colors.text
                                                )
                                            }
                                        }

                                        RoundedColumn {
                                            ItemTitle(
                                                text = when (showOriginalSonglist) {
                                                    0 -> stringResource(id = R.string.original_songlist).replace(
                                                        "#",
                                                        when (selectedSourceApp.intValue) {
                                                            1 -> stringResource(R.string.source_netease_cloud_music)
                                                            2 -> stringResource(R.string.source_qq_music)
                                                            3 -> stringResource(R.string.source_kugou_music)
                                                            4 -> stringResource(R.string.source_kuwo_music)
                                                            else -> ""
                                                        }
                                                    )

                                                    1 -> stringResource(R.string.convert_result)
                                                    else -> ""
                                                }
                                            )
                                            ItemContainer {
                                                Column(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .heightIn(max = (LocalConfiguration.current.screenHeightDp / 2.05).dp)
                                                        .background(color = SaltTheme.colors.subBackground)
                                                ) {
                                                    AnimatedContent(
                                                        targetState = selectedFilterIndex,
                                                        label = "",
                                                        transitionSpec = {
                                                            if (targetState != initialState) {
                                                                fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
                                                            } else {
                                                                fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
                                                            }
                                                        }) {
                                                        LazyColumn(
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            when (it) {
                                                                0 -> items(convertResult.size) { index ->
                                                                    if (convertResult[index] != null)
                                                                        Item(
                                                                            onClick = {
                                                                                selectedSongIndex =
                                                                                    index
                                                                                showSelectedSongInfoDialog =
                                                                                    true
                                                                            },
                                                                            text = convertResult[index]!![2 - showOriginalSonglist],
                                                                            sub = "${
                                                                                stringResource(
                                                                                    R.string.singer
                                                                                )
                                                                            }${convertResult[index]!![4 - showOriginalSonglist]}\n${
                                                                                stringResource(R.string.album)
                                                                            }${convertResult[index]!![6 - showOriginalSonglist]}",
                                                                            rightSub = when (convertResult[index]!![0]) {
                                                                                "0" -> stringResource(
                                                                                    R.string.match_success
                                                                                )

                                                                                "1" -> stringResource(
                                                                                    R.string.match_caution
                                                                                )

                                                                                "2" -> stringResource(
                                                                                    R.string.match_manual
                                                                                )

                                                                                else -> ""
                                                                            },
                                                                            rightSubColor = when (convertResult[index]!![0]) {
                                                                                "0" -> colorResource(
                                                                                    id = R.color.matched
                                                                                )

                                                                                "1" -> colorResource(
                                                                                    id = R.color.unmatched
                                                                                )

                                                                                "2" -> colorResource(
                                                                                    R.color.manual
                                                                                )

                                                                                else -> SaltTheme.colors.text
                                                                            },
                                                                        )
                                                                }

                                                                1 -> items(convertResult.size) { index ->
                                                                    if (convertResult[index]!![0] == "0" && convertResult[index] != null
                                                                    )
                                                                        Item(
                                                                            onClick = {
                                                                                selectedSongIndex =
                                                                                    index
                                                                                showSelectedSongInfoDialog =
                                                                                    true
                                                                            },
                                                                            text = convertResult[index]!![2 - showOriginalSonglist],
                                                                            sub = "${
                                                                                stringResource(
                                                                                    R.string.singer
                                                                                )
                                                                            }${convertResult[index]!![4 - showOriginalSonglist]}\n${
                                                                                stringResource(R.string.album)
                                                                            }${convertResult[index]!![6 - showOriginalSonglist]}",
                                                                            rightSub = when (convertResult[index]!![0]) {
                                                                                "0" -> stringResource(
                                                                                    R.string.match_success
                                                                                )

                                                                                "1" -> stringResource(
                                                                                    R.string.match_caution
                                                                                )

                                                                                "2" -> stringResource(
                                                                                    R.string.match_manual
                                                                                )

                                                                                else -> ""
                                                                            },
                                                                            rightSubColor = when (convertResult[index]!![0]) {
                                                                                "0" -> colorResource(
                                                                                    id = R.color.matched
                                                                                )

                                                                                "1" -> colorResource(
                                                                                    id = R.color.unmatched
                                                                                )

                                                                                "2" -> colorResource(
                                                                                    R.color.manual
                                                                                )

                                                                                else -> SaltTheme.colors.text
                                                                            },
                                                                        )
                                                                }

                                                                2 -> items(convertResult.size) { index ->
                                                                    if (convertResult[index]!![0] == "1" && convertResult[index] != null
                                                                    )
                                                                        Item(
                                                                            onClick = {
                                                                                selectedSongIndex =
                                                                                    index
                                                                                showSelectedSongInfoDialog =
                                                                                    true
                                                                            },
                                                                            text = convertResult[index]!![2 - showOriginalSonglist],
                                                                            sub = "${
                                                                                stringResource(
                                                                                    R.string.singer
                                                                                )
                                                                            }${convertResult[index]!![4 - showOriginalSonglist]}\n${
                                                                                stringResource(R.string.album)
                                                                            }${convertResult[index]!![6 - showOriginalSonglist]}",
                                                                            rightSub = when (convertResult[index]!![0]) {
                                                                                "0" -> stringResource(
                                                                                    R.string.match_success
                                                                                )

                                                                                "1" -> stringResource(
                                                                                    R.string.match_caution
                                                                                )

                                                                                "2" -> stringResource(
                                                                                    R.string.match_manual
                                                                                )

                                                                                else -> ""
                                                                            },
                                                                            rightSubColor = when (convertResult[index]!![0]) {
                                                                                "0" -> colorResource(
                                                                                    id = R.color.matched
                                                                                )

                                                                                "1" -> colorResource(
                                                                                    id = R.color.unmatched
                                                                                )

                                                                                "2" -> colorResource(
                                                                                    R.color.manual
                                                                                )

                                                                                else -> SaltTheme.colors.text
                                                                            },
                                                                        )
                                                                }

                                                                3 -> items(convertResult.size) { index ->
                                                                    if (convertResult[index]!![0] == "2" && convertResult[index] != null
                                                                    )
                                                                        Item(
                                                                            onClick = {
                                                                                selectedSongIndex =
                                                                                    index
                                                                                showSelectedSongInfoDialog =
                                                                                    true
                                                                            },
                                                                            text = convertResult[index]!![2 - showOriginalSonglist],
                                                                            sub = "${
                                                                                stringResource(
                                                                                    R.string.singer
                                                                                )
                                                                            }${convertResult[index]!![4 - showOriginalSonglist]}\n${
                                                                                stringResource(R.string.album)
                                                                            }${convertResult[index]!![6 - showOriginalSonglist]}",
                                                                            rightSub = when (convertResult[index]!![0]) {
                                                                                "0" -> stringResource(
                                                                                    R.string.match_success
                                                                                )

                                                                                "1" -> stringResource(
                                                                                    R.string.match_caution
                                                                                )

                                                                                "2" -> stringResource(
                                                                                    R.string.match_manual
                                                                                )

                                                                                else -> ""
                                                                            },
                                                                            rightSubColor = when (convertResult[index]!![0]) {
                                                                                "0" -> colorResource(
                                                                                    id = R.color.matched
                                                                                )

                                                                                "1" -> colorResource(
                                                                                    id = R.color.unmatched
                                                                                )

                                                                                "2" -> colorResource(
                                                                                    R.color.manual
                                                                                )

                                                                                else -> SaltTheme.colors.text
                                                                            },
                                                                        )
                                                                }
                                                            }
                                                        }
                                                    }
//                                        Spacer(modifier = Modifier.height(5.dp))
//                                        ItemValue(
//                                            text = "${stringResource(R.string.in_total)}${playlistEnabled.size}",
//                                            sub = "${stringResource(R.string.selected)}${playlistEnabled.count { it }}"
//                                        )
                                                }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.padding(horizontal = SaltTheme.dimens.outerHorizontalPadding)
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    showConfirmGoBackDialog = true
//                                                    convertResult.clear()
                                                },
                                                modifier = Modifier.weight(1f),
                                                text = stringResource(id = R.string.re_modify_params),
                                                textColor = SaltTheme.colors.subText,
                                                backgroundColor = SaltTheme.colors.subBackground,
                                                enableHaptic = enableHaptic.value
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            TextButton(
                                                onClick = {
                                                    showSaveDialog.value = true
                                                },
                                                modifier = Modifier.weight(1f),
                                                text = stringResource(id = R.string.save_conversion_results),
                                                enableHaptic = enableHaptic.value
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        Column(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .fillMaxSize()
                                .background(color = SaltTheme.colors.background)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            RoundedColumn {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Icon(
                                        modifier = Modifier.size(50.dp),
                                        painter = painterResource(id = R.drawable.ic_check),
                                        contentDescription = null,
                                        tint = colorResource(id = R.color.matched)
                                    )
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.Start,
                                    ) {
//                    Text(text = stringResource(id = R.string.all_done))
                                        ItemText(
                                            text = stringResource(id = R.string.all_done),
                                            fontSize = 24.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            RoundedColumn {
                                ItemTitle(text = stringResource(id = R.string.details_of_results))
                                val condition: (Int) -> Boolean = { it1 -> it1 == 2 }
                                val indices =
                                    playlistEnabled.withIndex().filter { condition(it.value) }
                                        .map { it.index }
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .heightIn(max = (LocalConfiguration.current.screenHeightDp / 2).dp)
                                        .clip(RoundedCornerShape(10.dp))
                                ) {
                                    items(indices) { index ->
                                        ItemText(
                                            text = "${
                                                Environment.getExternalStoragePublicDirectory(
                                                    Environment.DIRECTORY_DOWNLOADS
                                                )
                                            }/${context.getString(R.string.app_name)}/${
                                                LocalDate.now()
                                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                            }/${playlistName[index]}${
                                                when (selectedTargetApp) {
                                                    0 -> ".txt"
                                                    1 -> ".m3u"
                                                    2 -> ".m3u8"
                                                    else -> ""
                                                }
                                            }",
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            ItemContainer {
                                Row {
                                    TextButton(
                                        onClick = { convertPage.copyFolderPathToClipboard() },
                                        modifier = Modifier.weight(1f),
                                        text = stringResource(id = R.string.copy_folder_path),
                                        textColor = SaltTheme.colors.subText,
                                        backgroundColor = SaltTheme.colors.subBackground,
                                        enableHaptic = enableHaptic.value
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    TextButton(
                                        onClick = { convertPage.launchLocalPlayer(selectedTargetApp) },
                                        modifier = Modifier.weight(1f),
                                        text = stringResource(id = R.string.open_other_app).replace(
                                            "#", when (selectedTargetApp) {
                                                0 -> "Salt Player"
                                                1 -> "APlayer"
                                                2 -> "Poweramp"
                                                else -> ""
                                            }
                                        ),
                                        enableHaptic = enableHaptic.value
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