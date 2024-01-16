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
import com.moriafly.salt.ui.popup.PopupMenuItem
import com.moriafly.salt.ui.popup.rememberPopupState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    enableHaptic: MutableState<Boolean>
) {
    val sourceAppPopupMenuState = rememberPopupState()
    val matchingModePopupMenuState = rememberPopupState()
    var sourceApp by remember { mutableStateOf("") }
    val pages = listOf("0", "1", "2", "3")
    val pageState = rememberPagerState(pageCount = { pages.size })
    var allEnabled by remember { mutableStateOf(false) }
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
                allEnabled = false
                selectedSongIndex = -1
                selectedSearchResult = -1
            } else
                currentPage.intValue--
        } else {
            showConfirmGoBackDialog = true
        }
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
                showSelectedSongInfoDialog = false
                inputSearchWords.value = ""
                convertPage.saveModificationSong(selectedSongIndex, selectedAllIndex)
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
                    "${playlistName[playlistEnabled.indexOfFirst { it == 1 }]}.txt"
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
                            text = stringResource(R.string.match_success),
                            enableHaptic = enableHaptic.value
                        )
                        ItemSwitcher(
                            state = saveCautionSongs,
                            onChange = { saveCautionSongs = it },
                            text = stringResource(R.string.match_caution),
                            enableHaptic = enableHaptic.value
                        )
                        ItemSwitcher(
                            state = saveManualSongs,
                            onChange = { saveManualSongs = it },
                            text = stringResource(R.string.match_manual),
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
                        allEnabled = false
                        selectedSongIndex = -1
                        selectedSearchResult = -1
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
                                ItemPopup( //TODO 为每个子项添加图标
                                    state = sourceAppPopupMenuState,
                                    text = stringResource(R.string.select_source_of_songlist),
                                    selectedItem = sourceApp
                                ) {
                                    PopupMenuItem(
                                        onClick = {
                                            MyVibrationEffect(context, enableHaptic.value).click()
                                            selectedSourceApp.intValue = 1
                                            sourceAppPopupMenuState.dismiss()
                                            databaseFileName.value = ""
                                        },
                                        selected = selectedSourceApp.intValue == 1,
                                        text = stringResource(R.string.source_netease_cloud_music)
                                    )
                                    PopupMenuItem(
                                        onClick = {
                                            MyVibrationEffect(context, enableHaptic.value).click()
                                            selectedSourceApp.intValue = 2
                                            sourceAppPopupMenuState.dismiss()
                                            databaseFileName.value = ""
                                        },
                                        selected = selectedSourceApp.intValue == 2,
                                        text = stringResource(R.string.source_qq_music)
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
//                        iconPainter = painterResource(id = R.drawable.ic_qr_code),
//                        iconColor = SaltTheme.colors.text
                                    )
                                    PopupMenuItem(
                                        onClick = {
                                            MyVibrationEffect(context, enableHaptic.value).click()
                                            selectedSourceApp.intValue = 4
                                            sourceAppPopupMenuState.dismiss()
                                            databaseFileName.value = ""
                                        },
                                        selected = selectedSourceApp.intValue == 4,
                                        text = stringResource(R.string.source_kuwo_music)
                                    )
                                }
                            }

                            sourceApp = when (selectedSourceApp.intValue) {
                                1 -> stringResource(R.string.source_netease_cloud_music)
                                2 -> stringResource(R.string.source_qq_music)
                                3 -> stringResource(R.string.source_kugou_music)
                                4 -> stringResource(R.string.source_kuwo_music)
                                else -> ""
                            }

                            RoundedColumn {
                                ItemTitle(text = stringResource(R.string.import_database))
                                Item(
                                    enabled = selectedSourceApp.intValue != 0,
                                    onClick = { convertPage.selectDatabaseFile() },
                                    text = if (selectedSourceApp.intValue == 0) {
                                        stringResource(R.string.please_select_source_app)
                                    } else {
                                        stringResource(R.string.select_database_file_match_to_source_1) + sourceApp + stringResource(
                                            R.string.select_database_file_match_to_source_2
                                        )
                                    },
                                )
                                AnimatedVisibility(
                                    visible = databaseFileName.value != ""
                                ) {
                                    ItemValue(
                                        text = stringResource(R.string.you_have_selected),
                                        rightSub = databaseFileName.value
                                    )
                                }

                            }

                            RoundedColumn {
                                ItemTitle(text = stringResource(R.string.import_result_file))
                                ItemSwitcher(
                                    state = useCustomResultFile.value,
                                    onChange = {
                                        useCustomResultFile.value = it
                                    },
                                    text = stringResource(R.string.use_custom_result_file),
                                    sub = stringResource(R.string.use_other_result_file),
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
                                            text = stringResource(R.string.select_result_file_item_title),
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

//                RoundedColumn {
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
                                        onClick = { convertPage.requestPermission() },
                                        text = stringResource(R.string.next_step_text),
                                        enabled = !it,
                                        enableHaptic = enableHaptic.value
                                    )
                                }
                            }
//                }
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
                                                .heightIn(
                                                    min = 20.dp,
                                                    max = (LocalConfiguration.current.screenHeightDp / 1.6).dp
                                                )
                                                .background(color = SaltTheme.colors.subBackground)
                                        ) {
                                            ItemCheck(
                                                state = allEnabled,
                                                onChange = {
                                                    if (allEnabled) {
                                                        allEnabled = false
                                                        playlistEnabled.replaceAll { 0 }
                                                    } else {
                                                        allEnabled = true
                                                        playlistEnabled.replaceAll { 1 }
                                                    }
                                                },
                                                text = stringResource(R.string.select_all),
                                                enableHaptic = enableHaptic.value
                                            )
                                            LazyColumn(
                                                modifier = Modifier.weight(1f)
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
                                                        sub = "${stringResource(R.string.total)}${playlistSum[index]}${
                                                            stringResource(R.string.songs)
                                                        }",
                                                        enableHaptic = enableHaptic.value
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            ItemValue(
                                                text = "${stringResource(R.string.user_selected)}${playlistEnabled.count { it != 0 }}${
                                                    stringResource(
                                                        R.string.ge
                                                    )
                                                }",
                                                rightSub = "${stringResource(R.string.in_total)}${playlistEnabled.size}${
                                                    stringResource(
                                                        R.string.ge
                                                    )
                                                }"
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
                                                rightSub = "${stringResource(R.string.current_no)}${
                                                    if (playlistEnabled.count { it == 2 } == -1)
                                                        0
                                                    else {
                                                        playlistEnabled.count { it == 2 } + 1
                                                    }
                                                }${stringResource(R.string.ge)} - ${
                                                    stringResource(R.string.in_total)
                                                }${playlistEnabled.count { it != 0 }}${
                                                    stringResource(R.string.ge)
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
                                            ItemPopup( //TODO 为每个子项添加图标
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
                                                    text = stringResource(R.string.split_matching)
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
                                                    text = stringResource(R.string.overall_matching)
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
                                            ItemPopup( //TODO 为每个子项添加图标
                                                state = filterPopupMenuState,
                                                text = stringResource(id = R.string.convert_status),
                                                selectedItem = when (selectedFilterIndex) {
                                                    0 -> "${stringResource(id = R.string.all)} - ${convertResult.size}"
                                                    1 -> "${stringResource(id = R.string.match_success)} - ${
                                                        convertResult.count {
                                                            it.value[0] == stringResource(
                                                                R.string.match_success
                                                            )
                                                        }
                                                    }"

                                                    2 -> "${stringResource(id = R.string.match_caution)} - ${
                                                        convertResult.count {
                                                            it.value[0] == stringResource(
                                                                R.string.match_caution
                                                            )
                                                        }
                                                    }"

                                                    3 -> "${stringResource(id = R.string.match_manual)} - ${
                                                        convertResult.count {
                                                            it.value[0] == stringResource(
                                                                R.string.match_manual
                                                            )
                                                        }
                                                    }"

                                                    else -> ""
                                                }
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
                                                    selected = selectedFilterIndex == 0
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
                                                            it.value[0] == stringResource(
                                                                R.string.match_success
                                                            )
                                                        }
                                                    }",
                                                    selected = selectedFilterIndex == 1
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
                                                            it.value[0] == stringResource(
                                                                R.string.match_caution
                                                            )
                                                        }
                                                    }",
                                                    selected = selectedFilterIndex == 2
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
                                                            it.value[0] == stringResource(
                                                                R.string.match_manual
                                                            )
                                                        }
                                                    }",
                                                    selected = selectedFilterIndex == 3
                                                )
                                            }
                                        }

                                        RoundedColumn {
                                            ItemTitle(text = stringResource(R.string.convert_result))
                                            ItemContainer {
                                                Column(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .heightIn(max = (LocalConfiguration.current.screenHeightDp / 1.8).dp)
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
                                                                            text = convertResult[index]!![1],
                                                                            sub = "${
                                                                                stringResource(
                                                                                    R.string.singer
                                                                                )
                                                                            }${convertResult[index]!![3]}\n${
                                                                                stringResource(R.string.album)
                                                                            }${convertResult[index]!![5]}",
                                                                            rightSub = convertResult[index]!![0],
                                                                            rightSubColor = when (convertResult[index]!![0]) {
                                                                                stringResource(R.string.match_success) -> colorResource(
                                                                                    id = R.color.matched
                                                                                )

                                                                                stringResource(R.string.match_caution) -> colorResource(
                                                                                    id = R.color.unmatched
                                                                                )

                                                                                else -> colorResource(
                                                                                    R.color.manual
                                                                                )
                                                                            },
                                                                        )
                                                                }

                                                                1 -> items(convertResult.size) { index ->
                                                                    if (convertResult[index]!![0] == stringResource(
                                                                            R.string.match_success
                                                                        ) && convertResult[index] != null
                                                                    )
                                                                        Item(
                                                                            onClick = {
                                                                                selectedSongIndex =
                                                                                    index
                                                                                showSelectedSongInfoDialog =
                                                                                    true
                                                                            },
                                                                            text = convertResult[index]!![1],
                                                                            sub = "${
                                                                                stringResource(
                                                                                    R.string.singer
                                                                                )
                                                                            }${convertResult[index]!![3]}\n${
                                                                                stringResource(R.string.album)
                                                                            }${convertResult[index]!![5]}",
                                                                            rightSub = convertResult[index]!![0],
                                                                            rightSubColor = when (convertResult[index]!![0]) {
                                                                                stringResource(R.string.match_success) -> colorResource(
                                                                                    id = R.color.matched
                                                                                )

                                                                                stringResource(R.string.match_caution) -> colorResource(
                                                                                    id = R.color.unmatched
                                                                                )

                                                                                else -> colorResource(
                                                                                    R.color.manual
                                                                                )
                                                                            },
                                                                        )
                                                                }

                                                                2 -> items(convertResult.size) { index ->
                                                                    if (convertResult[index]!![0] == stringResource(
                                                                            R.string.match_caution
                                                                        ) && convertResult[index] != null
                                                                    )
                                                                        Item(
                                                                            onClick = {
                                                                                selectedSongIndex =
                                                                                    index
                                                                                showSelectedSongInfoDialog =
                                                                                    true
                                                                            },
                                                                            text = convertResult[index]!![1],
                                                                            sub = "${
                                                                                stringResource(
                                                                                    R.string.singer
                                                                                )
                                                                            }${convertResult[index]!![3]}\n${
                                                                                stringResource(R.string.album)
                                                                            }${convertResult[index]!![5]}",
                                                                            rightSub = convertResult[index]!![0],
                                                                            rightSubColor = when (convertResult[index]!![0]) {
                                                                                stringResource(R.string.match_success) -> colorResource(
                                                                                    id = R.color.matched
                                                                                )

                                                                                stringResource(R.string.match_caution) -> colorResource(
                                                                                    id = R.color.unmatched
                                                                                )

                                                                                else -> colorResource(
                                                                                    R.color.manual
                                                                                )
                                                                            },
                                                                        )
                                                                }

                                                                3 -> items(convertResult.size) { index ->
                                                                    if (convertResult[index]!![0] == stringResource(
                                                                            R.string.match_manual
                                                                        ) && convertResult[index] != null
                                                                    )
                                                                        Item(
                                                                            onClick = {
                                                                                selectedSongIndex =
                                                                                    index
                                                                                showSelectedSongInfoDialog =
                                                                                    true
                                                                            },
                                                                            text = convertResult[index]!![1],
                                                                            sub = "${
                                                                                stringResource(
                                                                                    R.string.singer
                                                                                )
                                                                            }${convertResult[index]!![3]}\n${
                                                                                stringResource(R.string.album)
                                                                            }${convertResult[index]!![5]}",
                                                                            rightSub = convertResult[index]!![0],
                                                                            rightSubColor = when (convertResult[index]!![0]) {
                                                                                stringResource(R.string.match_success) -> colorResource(
                                                                                    id = R.color.matched
                                                                                )

                                                                                stringResource(R.string.match_caution) -> colorResource(
                                                                                    id = R.color.unmatched
                                                                                )

                                                                                else -> colorResource(
                                                                                    R.color.manual
                                                                                )
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
                                                    convertResult.clear()
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
                                            }/MusicHelper/${playlistName[index]}.txt",
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
                                        onClick = { convertPage.launchSaltPlayer() },
                                        modifier = Modifier.weight(1f),
                                        text = stringResource(id = R.string.open_salt_player),
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