package com.hwinzniej.musichelper.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.activity.ConvertPage
import com.hwinzniej.musichelper.data.DataStoreConstants
import com.hwinzniej.musichelper.utils.MyVibrationEffect
import com.hwinzniej.musichelper.utils.Tools
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.popup.rememberPopupState
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    databaseFilePath: MutableState<String>,
    showSelectSourceDialog: MutableState<Boolean>,
    multiSource: MutableList<Array<String>>,
    showNumberProgressBar: MutableState<Boolean>,
    selectedMethod: MutableIntState,
    selectedLoginMethod: MutableIntState,
    showLoginDialog: MutableState<Boolean>,
    dataStore: DataStore<Preferences>,
    showSongNumMismatchDialog: MutableState<Boolean>,
    hapticStrength: MutableIntState
) {
    val sourceAppPopupMenuState = rememberPopupState()
    val methodPopupMenuState = rememberPopupState()
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
    var selectedSourceLocalApp by remember { mutableIntStateOf(2) }
    val targetAppPopupMenuState = rememberPopupState()
    val sourceLocalAppPopupMenuState = rememberPopupState()
    var selectedMultiSourceApp by remember { mutableIntStateOf(-1) }
    var userLoggedIn by remember { mutableStateOf(false) }
    var kugouDeviceId by remember { mutableStateOf(false) }
    val qrCodeDeviceRelated = remember { mutableStateMapOf<String, String>() }
    var loginQrCode by remember { mutableStateOf<Bitmap?>(null) }
    var kugouCurrentIp by remember { mutableStateOf("") }
    val kugouUserRelated = remember { mutableStateMapOf<String, String>() }
    val convertModePopupMenuState = rememberPopupState()
    val selectedConvertMode = remember { mutableIntStateOf(1) }

    fun init(delay: Long = 0L) {
        coroutine.launch {
            currentPage.intValue = 0
            delay(delay)
            databaseFileName.value = ""
            useCustomResultFile.value = false
            customResultFileName.value = ""
            playlistName.clear()
            playlistEnabled.clear()
            playlistSum.clear()
            convertPage.playlistShow.clear()
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
            selectedSourceLocalApp = 2
            convertPage.sourcePlaylistFileName.value = ""
        }
    }

    BackHandler(enabled = (currentPage.intValue != 0 && mainActivityPageState.currentPage == 1)) {
        if (showLoadingProgressBar.value || showNumberProgressBar.value || showDialogProgressBar.value) {
            Toast.makeText(
                context,
                context.getString(R.string.wait_operate_end),
                Toast.LENGTH_SHORT
            ).show()
            return@BackHandler
        }
        if (convertResult.isEmpty()) {
            if (currentPage.intValue == 3) {
                init(500L)
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
            enableHaptic = enableHaptic.value,
            hapticStrength = hapticStrength.intValue
        )
    }

    if (showErrorDialog.value) {
        YesDialog(
            onDismissRequest = {
                showErrorDialog.value = false
                convertPage.errorDialogCustomAction.value()
                convertPage.errorDialogCustomAction.value = {}
            },
            title = errorDialogTitle.value,
            content = null,
            enableHaptic = enableHaptic.value,
            hapticStrength = hapticStrength.intValue
        ) {
            RoundedColumn {
                ItemTitle(text = stringResource(id = R.string.error_details))
                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .heightIn(
                            min = 20.dp,
                            max = (LocalConfiguration.current.screenHeightDp / 2.5).dp
                        )
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    item {
                        MarkdownText(
                            modifier = Modifier.padding(bottom = 8.dp),
                            markdown = errorDialogContent.value,
                            style = TextStyle(
                                color = SaltTheme.colors.text,
                                fontSize = 14.sp
                            ),
                            isTextSelectable = true,
                            disableLinkMovementMethod = true
                        )
                    }
                }
            }
        }
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
            content = null,
            cancelText = stringResource(R.string.cancel_button_text),
            confirmText = stringResource(R.string.ok_button_text),
            enableHaptic = enableHaptic.value,
            hapticStrength = hapticStrength.intValue
        ) {
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
                            enableHaptic = enableHaptic.value,
                            hapticStrength = hapticStrength.intValue
                        )
                    }
                }
            }
        }
    }

    if (showSetSimilarityDialog) { //TODO 相似度分成3段，每段的颜色不同，失败、可能成功、成功？
        var slideSimilarity by remember { mutableFloatStateOf(similarity.floatValue) }
        LaunchedEffect(key1 = slideSimilarity) {
            MyVibrationEffect(context, enableHaptic.value, hapticStrength.intValue).dragMove()
        }
        YesNoDialog(
            onDismiss = { showSetSimilarityDialog = false },
            onCancel = { showSetSimilarityDialog = false },
            onConfirm = {
                similarity.floatValue = slideSimilarity
                showSetSimilarityDialog = false
            },
            title = stringResource(R.string.set_similarity_dialog_title),
            content = null,
            cancelText = stringResource(R.string.cancel_button_text),
            confirmText = stringResource(R.string.ok_button_text),
            enableHaptic = enableHaptic.value,
            hapticStrength = hapticStrength.intValue
        ) {
            Column {
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            if (slideSimilarity > 0)
                                slideSimilarity--
                        },
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
                    androidx.compose.material3.TextButton(
                        onClick = {
                            if (slideSimilarity < 100)
                                slideSimilarity++
                        },
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
            content = null,
            cancelText = stringResource(R.string.cancel_button_text),
            confirmText = stringResource(R.string.ok_button_text),
            enableHaptic = enableHaptic.value,
            hapticStrength = hapticStrength.intValue
        ) {
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
                                enableHaptic = enableHaptic.value,
                                hapticStrength = hapticStrength.intValue
                            )
                        }
                        BasicButton(
                            modifier = Modifier
                                .padding(top = 12.dp, bottom = 12.dp, end = 16.dp),
                            enabled = true,
                            onClick = { showDeleteDialog = true },
                            backgroundColor = colorResource(id = R.color.unmatched),
                            enableHaptic = enableHaptic.value,
                            hapticStrength = hapticStrength.intValue
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
                        RoundedColumn {
                            ItemTitle(text = stringResource(R.string.search_results))
                            LazyColumn {
                                items(searchResult.size) { index ->
                                    ItemCheck(
                                        state = selectedSearchResult == index,
                                        onChange = {
                                            focus.clearFocus()
                                            if (!it) selectedSearchResult = -1
                                            else {
                                                selectedSearchResult = index
                                                selectedAllIndex =
                                                    searchResult[index][3].toInt()
                                            }
                                        },
                                        text = searchResult[index][0],
                                        sub = if (searchResult[index].size == 1) null else "${searchResult[index][1]} - ${searchResult[index][2]}",
                                        iconAtLeft = false,
                                        hideIcon = searchResult[index].size == 1,
                                        enabled = searchResult[index].size != 1,
                                        enableHaptic = enableHaptic.value,
                                        hapticStrength = hapticStrength.intValue
                                    )
                                }
                            }
                        }
                    }
                    RoundedColumn {
                        ItemTitle(text = stringResource(R.string.songlist_song_info))
                        ItemValue(
                            text = stringResource(id = R.string.song_name),
                            rightSub = convertResult[selectedSongIndex]!![2],
                            clickable = true,
                            onClick = {
                                inputSearchWords.value =
                                    "${inputSearchWords.value}${convertResult[selectedSongIndex]!![2]}"
                            },
                            textWeight = 1f,
                            rightSubWeight = 2.6f
                        )
                        ItemValue(
                            text = stringResource(id = R.string.singer).replace(
                                "(：)|(:\\s)".toRegex(),
                                ""
                            ),
                            rightSub = convertResult[selectedSongIndex]!![4],
                            clickable = true,
                            onClick = {
                                inputSearchWords.value =
                                    "${inputSearchWords.value}${convertResult[selectedSongIndex]!![4]}"
                            },
                            textWeight = 1f,
                            rightSubWeight = 2.6f
                        )
                        ItemValue(
                            text = stringResource(id = R.string.album).replace(
                                "(：)|(:\\s)".toRegex(),
                                ""
                            ),
                            rightSub = convertResult[selectedSongIndex]!![6],
                            clickable = true,
                            onClick = {
                                inputSearchWords.value =
                                    "${inputSearchWords.value}${convertResult[selectedSongIndex]!![6]}"
                            },
                            textWeight = 1f,
                            rightSubWeight = 2.6f
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(key1 = kugouDeviceId) {
        if (kugouDeviceId) {
            coroutine.launch(Dispatchers.IO) {
                qrCodeDeviceRelated.putAll(
                    convertPage.kugouGetLoginQrCodeUrl(
                        type = selectedLoginMethod.intValue,
                        currentIp = kugouCurrentIp
                    )
                )
                loginQrCode =
                    qrCodeDeviceRelated["qrCodeUrl"]?.let { Tools().generateQRCode(content = it) }
                qrCodeDeviceRelated["ticket"]?.let {
                    kugouUserRelated.putAll(
                        convertPage.kugouGetLoginStatus(
                            type = selectedLoginMethod.intValue,
                            ticket = it,
                            currentIp = kugouCurrentIp
                        )
                    )
                    userLoggedIn = kugouUserRelated.size != 0
                    if (userLoggedIn) {
                        dataStore.edit { settings ->
                            settings[DataStoreConstants.KUGOU_TOKEN] =
                                kugouUserRelated["token"] ?: ""
                            settings[DataStoreConstants.KUGOU_USER_ID] =
                                kugouUserRelated["userId"] ?: ""
                        }
                    }
                }
            }
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
            content = null,
            cancelText = stringResource(R.string.cancel_button_text),
            confirmText = stringResource(R.string.ok_button_text),
            enableHaptic = enableHaptic.value,
            hapticStrength = hapticStrength.intValue
        ) {
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
                RoundedColumn {
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
                        enableHaptic = enableHaptic.value,
                        hapticStrength = hapticStrength.intValue
                    )
                    ItemSwitcher(
                        state = saveCautionSongs,
                        onChange = { saveCautionSongs = it },
                        text = "${stringResource(R.string.match_caution)} - ${
                            convertResult.count {
                                it.value[0] == "1"
                            }
                        }",
                        enableHaptic = enableHaptic.value,
                        hapticStrength = hapticStrength.intValue
                    )
                    ItemSwitcher(
                        state = saveManualSongs,
                        onChange = { saveManualSongs = it },
                        text = "${stringResource(R.string.match_manual)} - ${
                            convertResult.count {
                                it.value[0] == "2"
                            }
                        }",
                        enableHaptic = enableHaptic.value,
                        hapticStrength = hapticStrength.intValue
                    )
//                        ItemText(
//                            text = "${stringResource(R.string.result_file_save_location)}\n${
//                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//                            }/MusicHelper/${playlistName[playlistEnabled.indexOfFirst { it == 1 }]}.txt"
//                        )
                }
            }
        }
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
            content = null,
            confirmButtonColor = colorResource(id = R.color.unmatched),
            enableHaptic = enableHaptic.value,
            hapticStrength = hapticStrength.intValue
        ) {
            RoundedColumn {
                ItemContainer {
                    MarkdownText(
                        modifier = Modifier.padding(vertical = 8.dp),
                        markdown = "${stringResource(id = R.string.delete_dialog_content)}\n- ${
                            convertResult[selectedSongIndex]!![2]
                        }\n  - ${
                            stringResource(R.string.singer)
                        }${convertResult[selectedSongIndex]!![4]}\n  - ${
                            stringResource(R.string.album)
                        }${convertResult[selectedSongIndex]!![6]}",
                        style = TextStyle(
                            color = SaltTheme.colors.text,
                            fontSize = 14.sp
                        ),
                        isTextSelectable = true,
                        disableLinkMovementMethod = true
                    )
                }
            }
        }
    }

    var needNcmUserId by remember { mutableStateOf(false) }
    var userInput by remember { mutableStateOf("") }

    LaunchedEffect(key1 = showLoginDialog.value) {
        if (showLoginDialog.value) {
            userInput = ""
            if (selectedSourceApp.intValue == 3)
                coroutine.launch(Dispatchers.IO) {
                    kugouCurrentIp = Tools().getCurrentIp()
                }
            delay(300L)
            userInput = convertPage.autoFillCookie()
            if (userInput != "") {
                if (userInput == "KUGOU") {
                    kugouUserRelated["token"] =
                        dataStore.data.first()[DataStoreConstants.KUGOU_TOKEN] ?: ""
                    kugouUserRelated["userId"] =
                        dataStore.data.first()[DataStoreConstants.KUGOU_USER_ID] ?: ""
                }
                userLoggedIn = true
            }
        }
    }

    if (showLoginDialog.value) {
        val loginMethodPopupState = rememberPopupState()
        val webViewState = remember { mutableStateOf<WebView?>(null) }

        fun destoryWebview() {
            showDialogProgressBar.value = false
            webViewState.value?.loadUrl("about:blank")
            webViewState.value?.clearHistory()
            webViewState.value?.clearCache(true)
            webViewState.value?.clearFormData()
            webViewState.value?.removeAllViews()
            webViewState.value?.pauseTimers()
            webViewState.value?.onPause()
            webViewState.value?.removeAllViews()
            webViewState.value?.destroy()
            webViewState.value?.removeAllViews()
        }

        YesNoDialog(
            onDismiss = {
                destoryWebview()
                showLoginDialog.value = false
                convertPage.stopGetLoginStatus()
                userLoggedIn = false
            },
            onCancel = {
                destoryWebview()
                showLoginDialog.value = false
                convertPage.stopGetLoginStatus()
                userLoggedIn = false
            },
            onConfirm = {
                if (selectedLoginMethod.intValue == 2) {
                    if (selectedSourceApp.intValue == 3 && userLoggedIn) {
                        destoryWebview()
                        showLoginDialog.value = false
                        convertPage.getOnlinePlaylist(
                            kugouUserRelated = kugouUserRelated,
                            kugouCurrentIp = kugouCurrentIp
                        )
                    }
                    return@YesNoDialog
                }
                if (selectedLoginMethod.intValue == 0) {
                    convertPage.cookie.value = when (selectedSourceApp.intValue) {
                        1 -> CookieManager.getInstance().getCookie("music.163.com")
                        2 -> CookieManager.getInstance().getCookie("y.qq.com")
                        3 -> ""
                        4 -> CookieManager.getInstance().getCookie("kuwo.cn")
                        5 -> ""
                        else -> ""
                    }

                    val cookieValid = when (selectedSourceApp.intValue) {
                        1 -> {
                            convertPage.cookie.value.contains("\\bMUSIC_U=\\w+".toRegex()) &&
                                    convertPage.cookie.value.contains("\\b__csrf=\\w+".toRegex())
                        }

                        2 -> {
                            (convertPage.cookie.value.contains("\\buin=\\d+".toRegex())
                                    ||
                                    convertPage.cookie.value.contains("\\bwxuin=\\d+".toRegex())
                                    ) &&
                                    convertPage.cookie.value.contains("\\bqm_keyst=\\w+".toRegex())
                        }

                        3 -> userLoggedIn
                        4 -> convertPage.cookie.value.contains("\\bHm_Iuvt.*=\\w+".toRegex())
                        5 -> userLoggedIn
                        else -> false
                    }

                    if (cookieValid) {
                        if (selectedSourceApp.intValue == 1 && convertPage.loginUserId.value == "") {
                            needNcmUserId = true
                            showDialogProgressBar.value = true
                            webViewState.value?.onResume()
                            webViewState.value?.resumeTimers()
                            webViewState.value?.loadUrl("https://music.163.com/#/user/update")
                            return@YesNoDialog
                        } else {
                            destoryWebview()
                            showLoginDialog.value = false
                            convertPage.getOnlinePlaylist(
                                kugouUserRelated = kugouUserRelated,
                                kugouCurrentIp = kugouCurrentIp
                            )
                        }
                    } else {
                        convertPage.cookie.value = ""
                        Toast.makeText(
                            context,
                            context.getString(R.string.no_valid_login_params_found),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@YesNoDialog
                    }
                } else {
                    val inputValid = when (selectedSourceApp.intValue) {
                        1 -> {
                            userInput.contains("\\bMUSIC_U=\\w+".toRegex()) &&
                                    userInput.contains("\\b__csrf=\\w+".toRegex()) &&
                                    userInput.contains("\\buid=\\d+".toRegex())
                        }

                        2 -> {
                            (userInput.contains("\\buin=\\d+".toRegex())
                                    ||
                                    userInput.contains("\\bwxuin=\\d+".toRegex()))
                                    &&
                                    userInput.contains("\\bqm_keyst=\\w+".toRegex())
                        }

                        3 -> userLoggedIn
                        4 -> userInput.contains("\\bHm_Iuvt.*=\\w+".toRegex())
                        5 -> {
                            if (userInput.isBlank())
                                true
                            else
                                userInput.contains("\\bsessionid(_ss)?=\\w+".toRegex())
                        }

                        else -> false
                    }

                    if (inputValid) {
                        convertPage.cookie.value = userInput
                        destoryWebview()
                        showLoginDialog.value = false
                        convertPage.getOnlinePlaylist(
                            kugouUserRelated = kugouUserRelated,
                            kugouCurrentIp = kugouCurrentIp
                        )
                    } else {
                        convertPage.cookie.value = ""
                        Toast.makeText(
                            context,
                            context.getString(R.string.no_valid_login_params_found),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@YesNoDialog
                    }
                }
                destoryWebview()
            },
            title = stringResource(id = R.string.please_login).replace(
                "#", when (selectedSourceApp.intValue) {
                    1 -> stringResource(R.string.source_netease_cloud_music)
                    2 -> stringResource(R.string.source_qq_music)
                    3 -> stringResource(R.string.source_kugou_music)
                    4 -> stringResource(R.string.source_kuwo_music)
                    5 -> stringResource(R.string.source_luna_music)
                    else -> ""
                }
            ),
            content = null,
            enableHaptic = enableHaptic.value,
            enableConfirmButton = !showDialogProgressBar.value,
            hapticStrength = hapticStrength.intValue
        ) {
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
                    RoundedColumn {
                        ItemPopup(
                            state = loginMethodPopupState,
                            text = stringResource(R.string.login_method),
                            selectedItem = when (selectedLoginMethod.intValue) {
                                0 -> when (selectedSourceApp.intValue) {
                                    3 -> stringResource(R.string.kugou_app_qr_code)
                                    5 -> stringResource(R.string.luna_app_qr_code)
                                    else -> stringResource(R.string.web_login)
                                }

                                1 -> if (selectedSourceApp.intValue != 3)
                                    stringResource(R.string.cookie_login)
                                else
                                    stringResource(R.string.kugou_wechat_qr_code)

                                else -> ""
                            },
                            popupWidth = 150
                        ) {
                            PopupMenuItem(
                                onClick = {
                                    MyVibrationEffect(
                                        context,
                                        enableHaptic.value,
                                        hapticStrength.intValue
                                    ).click()
                                    webViewState.value?.onResume()
                                    webViewState.value?.resumeTimers()
                                    userLoggedIn = false
                                    convertPage.cookie.value = ""
                                    selectedLoginMethod.intValue = 0
                                    loginMethodPopupState.dismiss()
                                    showDialogProgressBar.value = true
                                    when (selectedSourceApp.intValue) {
                                        3 -> {
                                            coroutine.launch(Dispatchers.IO) {
                                                loginQrCode = null
                                                convertPage.stopGetLoginStatus()
                                                kugouDeviceId = false
                                                qrCodeDeviceRelated.clear()
                                                kugouUserRelated.clear()
                                                kugouDeviceId =
                                                    convertPage.kugouActiveDevice(currentIp = kugouCurrentIp)
                                            }
                                        }

                                        4 -> {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.kuwo_no_login_click_ok_button),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                        5 -> {
                                            coroutine.launch(Dispatchers.IO) {
                                                loginQrCode = null
                                                convertPage.stopGetLoginStatus()
                                                qrCodeDeviceRelated.clear()
                                                qrCodeDeviceRelated.putAll(
                                                    convertPage.lunaGetLoginQrCodeUrl()
                                                )
                                                loginQrCode =
                                                    qrCodeDeviceRelated["qrCodeUrl"]?.let {
                                                        Tools().generateQRCode(
                                                            content = it
                                                        )
                                                    }
                                                convertPage.lunaVerifyRelated.clear()
                                                qrCodeDeviceRelated["token"]?.let {
                                                    userLoggedIn =
                                                        convertPage.lunaGetLoginStatus(token = it)
                                                    if (userLoggedIn)
                                                        convertPage.lunaVerifyRelated.clear()
                                                }
                                                showDialogProgressBar.value = false
                                            }
                                        }
                                    }
                                },
                                selected = selectedLoginMethod.intValue == 0,
                                text = when (selectedSourceApp.intValue) {
                                    3 -> stringResource(R.string.kugou_app_qr_code)
                                    5 -> stringResource(R.string.luna_app_qr_code)
                                    else -> stringResource(R.string.web_login)
                                },
                                iconPainter =
                                when (selectedSourceApp.intValue) {
                                    3 -> painterResource(id = R.drawable.kugou)
                                    5 -> painterResource(id = R.drawable.luna_music)
                                    else -> painterResource(id = R.drawable.web_page)
                                },
                                iconColor = SaltTheme.colors.text,
                                iconPaddingValues = PaddingValues(all = 1.dp)
                            )
                            PopupMenuItem(
                                onClick = {
                                    MyVibrationEffect(
                                        context,
                                        enableHaptic.value,
                                        hapticStrength.intValue
                                    ).click()
                                    userLoggedIn = false
                                    convertPage.cookie.value = ""
                                    selectedLoginMethod.intValue = 1
                                    loginMethodPopupState.dismiss()
                                    if (selectedSourceApp.intValue == 3) {
                                        coroutine.launch(Dispatchers.IO) {
                                            loginQrCode = null
                                            convertPage.stopGetLoginStatus()
                                            kugouDeviceId = false
                                            qrCodeDeviceRelated.clear()
                                            kugouUserRelated.clear()
                                            kugouDeviceId =
                                                convertPage.kugouActiveDevice(currentIp = kugouCurrentIp)
                                        }
                                    }
                                },
                                selected = selectedLoginMethod.intValue == 1,
                                text = if (selectedSourceApp.intValue != 3)
                                    stringResource(R.string.cookie_login)
                                else
                                    stringResource(R.string.kugou_wechat_qr_code),
                                iconPainter = if (selectedSourceApp.intValue != 3)
                                    painterResource(id = R.drawable.cookie)
                                else
                                    painterResource(id = R.drawable.wechat_app),
                                iconColor = SaltTheme.colors.text
                            )
                        }
                        AnimatedVisibility(visible = selectedLoginMethod.intValue == 0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                AnimatedVisibility(visible = userLoggedIn) {
                                    ItemContainer {
                                        MarkdownText(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clip(
                                                    RoundedCornerShape(
                                                        0.dp,
                                                        0.dp,
                                                        10.dp,
                                                        10.dp
                                                    )
                                                ),
                                            markdown = stringResource(id = R.string.already_logged_in_click_ok).replace(
                                                "#n",
                                                "\n"
                                            ),
                                            style = TextStyle(
                                                color = SaltTheme.colors.text,
                                                fontSize = 16.sp,
                                                lineHeight = 1.5.em
                                            ),
                                            isTextSelectable = true,
                                            disableLinkMovementMethod = true
                                        )
                                    }
                                }
                                AnimatedVisibility(visible = !userLoggedIn && selectedSourceApp.intValue != 3 && selectedSourceApp.intValue != 5) {
                                    AndroidView(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height((LocalConfiguration.current.screenHeightDp / 3).dp)
                                            .clip(RoundedCornerShape(0.dp, 0.dp, 10.dp, 10.dp)),
                                        factory = { context ->
                                            WebView(context).also { webView ->
                                                webViewState.value = webView
                                            }
                                        },
                                    ) { webView ->
                                        webView.webViewClient = object : WebViewClient() {
                                            override fun onPageFinished(
                                                view: WebView,
                                                url: String
                                            ) {
                                                coroutine.launch(Dispatchers.Main) {
                                                    delay(1000L)
                                                    when (selectedSourceApp.intValue) {
                                                        1 -> {  // TODO 自动判断是否登录成功，并显示登录成功的信息
                                                            view.evaluateJavascript(
                                                                """
                                                        var xpath = '/html/body/div[1]/div[1]/div/div[1]/a';
                                                        var result = document.evaluate(xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
                                                        var element = result.singleNodeValue;
                                                        if (element) {
                                                            if (element.innerText.includes('登录')) {
                                                                element.click();
                                                                true;
                                                            } else {
                                                                var xpath1 = '/html/body/div[1]/div[1]/div/div[1]/div[1]/a';
                                                                var result1 = document.evaluate(xpath1, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
                                                                var element1 = result1.singleNodeValue;
                                                                element1.href;
                                                            }
                                                        } else {
                                                            false;
                                                        }
                                                        """.trimIndent()
                                                            ) { result ->
                                                                if (result.contains("/user/home\\?id=\\d+".toRegex())) {
                                                                    convertPage.loginUserId.value =
                                                                        result.substring(
                                                                            result.indexOf("/user/home?id=") + 14,
                                                                            result.length - 1
                                                                        )
                                                                    userLoggedIn = true
                                                                    convertPage.lastLoginTimestamp.longValue =
                                                                        System.currentTimeMillis()
                                                                    coroutine.launch {
                                                                        dataStore.edit { settings ->
                                                                            settings[DataStoreConstants.LAST_LOGIN_TIMESTAMP] =
                                                                                System.currentTimeMillis()
                                                                        }
                                                                    }
                                                                    if (needNcmUserId) {
                                                                        destoryWebview()
                                                                        showDialogProgressBar.value =
                                                                            false
                                                                        showLoginDialog.value =
                                                                            false
                                                                        needNcmUserId = false
                                                                        convertPage.getOnlinePlaylist()
                                                                    } else {
                                                                        view.pauseTimers()
                                                                        view.onPause()
                                                                    }
                                                                } else if (result == "false") {
                                                                    showDialogProgressBar.value =
                                                                        false
                                                                    view.loadUrl("https://music.163.com/#/user/update")
                                                                }
                                                            }
                                                        }

                                                        2 -> {
                                                            view.evaluateJavascript(
                                                                """
                                                        var xpath = '/html/body/div/div[1]/div/div[2]/span/a';
                                                        var result = document.evaluate(xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
                                                        var element =  result.singleNodeValue;
                                                        if (element) {
                                                            if (element.innerText.includes('登录')) {
                                                                element.click();
                                                                true;
                                                            } else {
                                                                'logged';
                                                            }
                                                        } else {
                                                            false;
                                                        }
                                                        """.trimIndent()
                                                            ) { result ->
                                                                if (result == "\"logged\"") {
                                                                    view.pauseTimers()
                                                                    view.onPause()
                                                                    userLoggedIn = true
                                                                    convertPage.lastLoginTimestamp.longValue =
                                                                        System.currentTimeMillis()
                                                                    coroutine.launch {
                                                                        dataStore.edit { settings ->
                                                                            settings[DataStoreConstants.LAST_LOGIN_TIMESTAMP] =
                                                                                System.currentTimeMillis()
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        3 -> {}

                                                        4 -> {}

                                                        5 -> {}
                                                    }
//                                                view.scrollTo(800, 950)
                                                    showDialogProgressBar.value = false
//                                            MyVibrationEffect(context, enableHaptic.value).done()
                                                }
                                            }
                                        }

                                        webView.setBackgroundColor(0)
                                        webView.settings.javaScriptEnabled = true
                                        webView.settings.userAgentString =
                                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                        webView.settings.builtInZoomControls = true
                                        webView.settings.useWideViewPort = true
                                        webView.settings.loadWithOverviewMode = true
                                        webView.settings.domStorageEnabled = true
                                        webView.settings.displayZoomControls = false
//                                        webView.setInitialScale(250)
                                        when (selectedSourceApp.intValue) {
                                            1 -> webView.loadUrl("https://music.163.com/#/user/update")
                                            2 -> webView.loadUrl("https://y.qq.com/n/ryqq/profile")
                                            3 -> webView.loadUrl("about:blank")
                                            4 -> webView.loadUrl("https://kuwo.cn/")
                                            5 -> webView.loadUrl("about:blank")
                                        }
                                    }
                                }
                                AnimatedVisibility(visible = !userLoggedIn && (selectedSourceApp.intValue == 3 || selectedSourceApp.intValue == 5)) {
                                    AnimatedVisibility(visible = loginQrCode == null) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = stringResource(id = R.string.getting_login_QR_code),
                                                color = SaltTheme.colors.subText
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                    AnimatedVisibility(visible = loginQrCode != null) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            loginQrCode?.let {
                                                Image(
                                                    bitmap = it.asImageBitmap(),
                                                    contentDescription = "Kugou Login QR Code",
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        AnimatedVisibility(visible = selectedLoginMethod.intValue == 1) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = (LocalConfiguration.current.screenHeightDp / 4).dp)
                            ) {
                                AnimatedVisibility(visible = userLoggedIn && selectedSourceApp.intValue == 3) {
                                    ItemContainer {
                                        MarkdownText(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clip(
                                                    RoundedCornerShape(
                                                        0.dp,
                                                        0.dp,
                                                        10.dp,
                                                        10.dp
                                                    )
                                                ),
                                            markdown = stringResource(id = R.string.already_logged_in_click_ok).replace(
                                                "#n",
                                                "\n"
                                            ),
                                            style = TextStyle(
                                                color = SaltTheme.colors.text,
                                                fontSize = 16.sp,
                                                lineHeight = 1.5.em
                                            ),
                                            isTextSelectable = true,
                                            disableLinkMovementMethod = true
                                        )
                                    }
                                }
                                AnimatedVisibility(visible = selectedSourceApp.intValue != 3) {
                                    val focusRequester = remember { FocusRequester() }
                                    ItemEdit(
                                        modifier = Modifier.focusRequester(focusRequester),
                                        text = userInput,
                                        hint = when (selectedSourceApp.intValue) {
                                            1 -> "MUSIC_U=xxx; __csrf=xxx; uid=xxx"
                                            2 -> "(wx)uin=xxx; qm_keyst=xxx"
                                            3 -> ""
                                            4 -> "Hm_Iuvtxxx=xxx"
                                            5 -> "sessionid(_ss)=xxx"
                                            else -> ""
                                        },
                                        onChange = {
                                            userInput = it
                                        },
                                        enableHaptic = enableHaptic.value,
                                        showClearButton = userInput != "",
                                        onClear = {
                                            userInput = ""
                                        },
                                        hapticStrength = hapticStrength.intValue
                                    )
                                    LaunchedEffect(Unit) {
                                        focusRequester.requestFocus()
                                    }
                                }
                                AnimatedVisibility(visible = !userLoggedIn && selectedSourceApp.intValue == 3) {
                                    AnimatedVisibility(visible = loginQrCode == null) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = stringResource(id = R.string.getting_login_QR_code),
                                                color = SaltTheme.colors.subText
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                    AnimatedVisibility(visible = loginQrCode != null) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            loginQrCode?.let {
                                                Image(
                                                    bitmap = it.asImageBitmap(),
                                                    contentDescription = "Login QR Code",
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        AnimatedVisibility(visible = selectedLoginMethod.intValue == 2) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = (LocalConfiguration.current.screenHeightDp / 4).dp)
                            ) {
                                AnimatedVisibility(visible = userLoggedIn && selectedSourceApp.intValue == 3) {
                                    ItemContainer {
                                        MarkdownText(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clip(
                                                    RoundedCornerShape(
                                                        0.dp,
                                                        0.dp,
                                                        10.dp,
                                                        10.dp
                                                    )
                                                ),
                                            markdown = stringResource(id = R.string.already_logged_in_click_ok).replace(
                                                "#n",
                                                "\n"
                                            ),
                                            style = TextStyle(
                                                color = SaltTheme.colors.text,
                                                fontSize = 16.sp,
                                                lineHeight = 1.5.em
                                            ),
                                            isTextSelectable = true,
                                            disableLinkMovementMethod = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                    AnimatedVisibility(visible = userLoggedIn && (selectedLoginMethod.intValue == 0 || selectedSourceApp.intValue == 3)) {
                        ItemContainer {
                            TextButton(
                                onClick = {
                                    if (selectedLoginMethod.intValue != 2)
                                        showDialogProgressBar.value = true
                                    convertPage.loginUserId.value = ""
                                    coroutine.launch(Dispatchers.IO) {
                                        if (selectedSourceApp.intValue == 5)
                                            convertPage.lunaLogout()
                                        dataStore.edit { settings ->
                                            settings[DataStoreConstants.NETEASE_USER_ID] = ""
                                            settings[DataStoreConstants.LAST_LOGIN_TIMESTAMP] =
                                                0L
                                            settings[DataStoreConstants.KUGOU_TOKEN] = ""
                                            settings[DataStoreConstants.KUGOU_USER_ID] = ""
                                        }
                                    }
                                    CookieManager.getInstance().removeAllCookies(null)
                                    CookieManager.getInstance().flush()
                                    webViewState.value?.onResume()
                                    webViewState.value?.resumeTimers()
                                    when (selectedSourceApp.intValue) {
                                        1 -> {
                                            webViewState.value?.loadUrl("https://music.163.com/#/user/update")
                                        }

                                        2 -> {
                                            webViewState.value?.loadUrl("https://y.qq.com/n/ryqq/profile")
                                        }

                                        3 -> {
                                            loginQrCode = null
                                            convertPage.stopGetLoginStatus()
                                            kugouDeviceId = false
                                            qrCodeDeviceRelated.clear()
                                            kugouUserRelated.clear()
                                            coroutine.launch(Dispatchers.IO) {
                                                kugouCurrentIp = Tools().getCurrentIp()
                                            }
                                        }

                                        4 -> {
                                            webViewState.value?.loadUrl("https://kuwo.cn/")
                                        }

                                        5 -> {
                                            loginQrCode = null
                                            convertPage.stopGetLoginStatus()
                                            qrCodeDeviceRelated.clear()
                                            convertPage.lunaVerifyRelated.clear()
                                        }
                                    }
                                    userLoggedIn = false
                                },
                                text = stringResource(id = R.string.switch_account_logout),
                                enableHaptic = enableHaptic.value,
                                hapticStrength = hapticStrength.intValue
                            )
                        }
                    }
                }
            }
        }
    }

    if (convertPage.showLunaVerifyDialog.value) {
        YesNoDialog(
            onDismiss = { convertPage.showLunaVerifyDialog.value = false },
            onCancel = { convertPage.showLunaVerifyDialog.value = false },
            onConfirm = {
                coroutine.launch(Dispatchers.IO) {
                    showDialogProgressBar.value = true
                    qrCodeDeviceRelated["token"]?.let {
                        if (convertPage.lunaGetVerifyStatus()) {
                            convertPage.showLunaVerifyDialog.value = false
                            userLoggedIn = convertPage.lunaGetLoginStatus(token = it, now = true)
                            convertPage.lunaVerifyRelated.clear()
                        }
                    }
                    showDialogProgressBar.value = false
                }
            },
            title = stringResource(id = R.string.luna_music_sec_verify),
            content = null,
            enableHaptic = enableHaptic.value,
            enableConfirmButton = !showDialogProgressBar.value,
            hapticStrength = hapticStrength.intValue
        ) {
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
                    RoundedColumn {
                        ItemTitle(text = stringResource(id = R.string.error_details))
                        ItemContainer {
                            MarkdownText(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                markdown = stringResource(id = R.string.luna_music_verify_content)
                                    .replace("#n", "\n")
                                    .replace("#phone", convertPage.lunaVerifyRelated["phone"] ?: "")
                                    .replace(
                                        "#content",
                                        convertPage.lunaVerifyRelated["content"] ?: ""
                                    )
                                    .replace(
                                        "#target",
                                        convertPage.lunaVerifyRelated["target"] ?: ""
                                    ),
                                style = TextStyle(
                                    color = SaltTheme.colors.text,
                                    fontSize = 16.sp,
                                    lineHeight = 1.5.em
                                ),
                                isTextSelectable = true,
                                disableLinkMovementMethod = true
                            )
                        }
                        ItemContainer {
                            TextButton(
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("sms:${convertPage.lunaVerifyRelated["target"]}")
                                    )
                                    intent.putExtra(
                                        "sms_body",
                                        convertPage.lunaVerifyRelated["content"]
                                    )
                                    context.startActivity(intent)
                                },
                                text = stringResource(id = R.string.send_sms),
                                hapticStrength = hapticStrength.intValue
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSongNumMismatchDialog.value) {
        YesNoDialog(
            onDismiss = { showSongNumMismatchDialog.value = false },
            onCancel = {
                showSongNumMismatchDialog.value = false
                convertPage.saveCurrentConvertResult(
                    saveSuccessSongs = false,
                    saveCautionSongs = false,
                    saveManualSongs = true,
                    fileName = ""
                )
                Toast.makeText(
                    context,
                    context.getString(R.string.skipped),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onConfirm = {
                showSongNumMismatchDialog.value = false
                convertPage.previewResult(directlyStart = true)
            },
            title = stringResource(id = R.string.error_while_getting_data_dialog_title),
            content = null,
            enableHaptic = enableHaptic.value,
            confirmText = stringResource(id = R.string.continue_button_name),
            cancelText = stringResource(id = R.string.skip_button_name),
            hapticStrength = hapticStrength.intValue
        ) {
            RoundedColumn {
                ItemTitle(text = stringResource(id = R.string.error_details))
                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .heightIn(
                            min = 20.dp,
                            max = (LocalConfiguration.current.screenHeightDp / 2.5).dp
                        )
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    item {
                        MarkdownText(
                            modifier = Modifier.padding(bottom = 8.dp),
                            markdown = errorDialogContent.value,
                            style = TextStyle(
                                color = SaltTheme.colors.text,
                                fontSize = 14.sp
                            ),
                            isTextSelectable = true,
                            disableLinkMovementMethod = true
                        )
                    }
                }
            }
        }
    }

    val lazyListState1 = rememberLazyListState()

    if (convertPage.showCustomPlaylistDialog.value) {
        YesNoDialog(
            onCancel = {
                convertPage.showCustomPlaylistDialog.value = false
                convertPage.customPlaylistInput.value = ""
            },
            onDismiss = {
                convertPage.showCustomPlaylistDialog.value = false
                convertPage.customPlaylistInput.value = ""
            },
            onConfirm = {
                coroutine.launch(Dispatchers.IO) {
                    convertPage.getCustomPlaylist()
                    if (playlistEnabled.size > 1) {
                        withContext(Dispatchers.Main) {
                            lazyListState1.animateScrollToItem(0)
                        }
                    }
                }
            },
            title = stringResource(id = R.string.add_extra_playlist),
            enableHaptic = enableHaptic.value,
            enableConfirmButton = !showDialogProgressBar.value,
            content = null,
            hapticStrength = hapticStrength.intValue
        ) {
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
                val focusRequester = remember { FocusRequester() }
                ItemEdit(
                    text = convertPage.customPlaylistInput.value,
                    onChange = {
                        convertPage.customPlaylistInput.value = it
                    },
                    hint = when (selectedSourceApp.intValue) {
                        3 -> stringResource(id = R.string.hint_add_extra_playlist2)
                        else -> stringResource(id = R.string.hint_add_extra_playlist1)
                    },
                    modifier = Modifier
                        .focusRequester(focusRequester),
                    paddingValues = PaddingValues(
                        start = SaltTheme.dimens.outerHorizontalPadding,
                        end = SaltTheme.dimens.outerHorizontalPadding,
                        top = 14.dp,
                        bottom = 8.dp
                    ),
                    showClearButton = true,
                    onClear = { convertPage.customPlaylistInput.value = "" },
                    hapticStrength = hapticStrength.intValue
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        }
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
            delay(500L)
            showDialogProgressBar.value = true
            delay(500L)
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
                if (showLoadingProgressBar.value || showNumberProgressBar.value || showDialogProgressBar.value) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.wait_operate_end),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@TitleBar
                }
                if (convertResult.isEmpty()) {
                    MyVibrationEffect(context, enableHaptic.value, hapticStrength.intValue).click()
                    if (currentPage.intValue == 3) {
                        init(500L)
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
            if (showNumberProgressBar.value) {
                val progressAnimation by animateFloatAsState(
                    targetValue = convertPage.numberProgress.floatValue,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = ""
                )
                LinearProgressIndicator(
                    progress = { progressAnimation },
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(2f),
                    color = SaltTheme.colors.highlight,
                    trackColor = SaltTheme.colors.background,
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
                                ItemTitle(text = stringResource(R.string.convert_mode))
                                ItemPopup(
                                    state = convertModePopupMenuState,
                                    text = stringResource(R.string.choose_convert_mode),
                                    sub = null,
                                    selectedItem = when (selectedConvertMode.intValue) {
                                        1 -> stringResource(R.string.online_to_local)
                                        2 -> stringResource(R.string.local_to_local)
                                        else -> ""
                                    },
                                    popupWidth = 170,
//                                    rightSubWeight = 1f
                                ) {
                                    PopupMenuItem(
                                        onClick = {
                                            MyVibrationEffect(
                                                context,
                                                enableHaptic.value,
                                                hapticStrength.intValue
                                            ).click()
                                            selectedConvertMode.intValue = 1
                                            convertModePopupMenuState.dismiss()
                                            init()
                                        },
                                        selected = selectedConvertMode.intValue == 1,
                                        text = stringResource(R.string.online_to_local),
                                    )
                                    PopupMenuItem(
                                        onClick = {
                                            MyVibrationEffect(
                                                context,
                                                enableHaptic.value,
                                                hapticStrength.intValue
                                            ).click()
                                            selectedConvertMode.intValue = 2
                                            convertModePopupMenuState.dismiss()
                                            init()
                                        },
                                        selected = selectedConvertMode.intValue == 2,
                                        text = stringResource(R.string.local_to_local),
                                    )
                                }
                            }
                            AnimatedContent(
                                targetState = selectedConvertMode.intValue,
                                label = "",
                                transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                }) { it1 ->
                                if (it1 == 1) {
                                    Column {
                                        RoundedColumn {
                                            ItemTitle(text = stringResource(R.string.source_of_songlist_app))
                                            AnimatedContent(
                                                targetState = useRootAccess.value && selectedMethod.intValue == 0,
                                                label = "",
                                                transitionSpec = {
                                                    fadeIn() togetherWith fadeOut()
                                                }) {
                                                ItemPopup(
                                                    state = sourceAppPopupMenuState,
                                                    text = stringResource(R.string.select_source_of_songlist),
                                                    sub = if (it)
                                                        stringResource(R.string.with_root_access)
                                                    else null,
                                                    selectedItem = when (selectedSourceApp.intValue) {
                                                        1 -> stringResource(R.string.source_netease_cloud_music)
                                                        2 -> stringResource(R.string.source_qq_music)
                                                        3 -> stringResource(R.string.source_kugou_music)
                                                        4 -> stringResource(R.string.source_kuwo_music)
                                                        5 -> stringResource(R.string.source_luna_music)
                                                        else -> ""
                                                    },
                                                    popupWidth = 180
                                                ) {
                                                    PopupMenuItem(
                                                        onClick = {
                                                            MyVibrationEffect(
                                                                context,
                                                                enableHaptic.value,
                                                                hapticStrength.intValue
                                                            ).click()
                                                            selectedSourceApp.intValue = 1
                                                            coroutine.launch {
                                                                dataStore.edit { settings ->
                                                                    settings[DataStoreConstants.PLAYLIST_SOURCE_PLATFORM] =
                                                                        1
                                                                }
                                                            }
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
                                                            MyVibrationEffect(
                                                                context,
                                                                enableHaptic.value,
                                                                hapticStrength.intValue
                                                            ).click()
                                                            selectedSourceApp.intValue = 2
                                                            coroutine.launch {
                                                                dataStore.edit { settings ->
                                                                    settings[DataStoreConstants.PLAYLIST_SOURCE_PLATFORM] =
                                                                        2
                                                                }
                                                            }
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
                                                            MyVibrationEffect(
                                                                context,
                                                                enableHaptic.value,
                                                                hapticStrength.intValue
                                                            ).click()
                                                            selectedSourceApp.intValue = 3
                                                            coroutine.launch {
                                                                dataStore.edit { settings ->
                                                                    settings[DataStoreConstants.PLAYLIST_SOURCE_PLATFORM] =
                                                                        3
                                                                }
                                                            }
                                                            sourceAppPopupMenuState.dismiss()
                                                            databaseFileName.value = ""
                                                        },
                                                        selected = selectedSourceApp.intValue == 3,
                                                        text = stringResource(R.string.source_kugou_music),
                                                        iconPainter = painterResource(id = R.drawable.kugou),
                                                        iconColor = SaltTheme.colors.text,
                                                        iconPaddingValues = PaddingValues(all = 1.5.dp)
                                                    )
                                                    PopupMenuItem(
                                                        onClick = {
                                                            MyVibrationEffect(
                                                                context,
                                                                enableHaptic.value,
                                                                hapticStrength.intValue
                                                            ).click()
                                                            selectedSourceApp.intValue = 4
                                                            coroutine.launch {
                                                                dataStore.edit { settings ->
                                                                    settings[DataStoreConstants.PLAYLIST_SOURCE_PLATFORM] =
                                                                        4
                                                                }
                                                            }
                                                            sourceAppPopupMenuState.dismiss()
                                                            databaseFileName.value = ""
                                                        },
                                                        selected = selectedSourceApp.intValue == 4,
                                                        text = stringResource(R.string.source_kuwo_music),
                                                        iconPainter = painterResource(id = R.drawable.kuwo),
                                                        iconColor = SaltTheme.colors.text,
                                                        iconPaddingValues = PaddingValues(all = 1.5.dp)
                                                    )
                                                    PopupMenuItem(
                                                        onClick = {
                                                            MyVibrationEffect(
                                                                context,
                                                                enableHaptic.value,
                                                                hapticStrength.intValue
                                                            ).click()
                                                            selectedSourceApp.intValue = 5
                                                            coroutine.launch {
                                                                dataStore.edit { settings ->
                                                                    settings[DataStoreConstants.PLAYLIST_SOURCE_PLATFORM] =
                                                                        5
                                                                }
                                                            }
                                                            sourceAppPopupMenuState.dismiss()
                                                            databaseFileName.value = ""
                                                        },
                                                        selected = selectedSourceApp.intValue == 5,
                                                        text = stringResource(R.string.source_luna_music),
                                                        iconPainter = painterResource(id = R.drawable.luna_music),
                                                        iconColor = SaltTheme.colors.text,
                                                        iconPaddingValues = PaddingValues(all = 1.5.dp)
                                                    )
                                                }
                                            }
                                            ItemPopup(
                                                state = methodPopupMenuState,
                                                text = stringResource(R.string.way_to_get_song_list),
                                                selectedItem = when (selectedMethod.intValue) {
                                                    0 -> if (selectedSourceApp.intValue == 5)
                                                        stringResource(R.string.json_file)
                                                    else
                                                        stringResource(R.string.database)

                                                    1 -> stringResource(R.string.online)
                                                    else -> ""
                                                },
                                                popupWidth = 165,
                                            ) {
                                                PopupMenuItem(
                                                    onClick = {
                                                        MyVibrationEffect(
                                                            context,
                                                            enableHaptic.value,
                                                            hapticStrength.intValue
                                                        ).click()
                                                        selectedMethod.intValue = 0
                                                        coroutine.launch {
                                                            dataStore.edit { settings ->
                                                                settings[DataStoreConstants.GET_PLAYLIST_METHOD] =
                                                                    0
                                                            }
                                                        }
                                                        methodPopupMenuState.dismiss()
                                                    },
                                                    selected = selectedMethod.intValue == 0,
                                                    text = if (selectedSourceApp.intValue == 5)
                                                        stringResource(R.string.json_file)
                                                    else stringResource(R.string.database),
                                                    iconPainter = if (selectedSourceApp.intValue == 5)
                                                        painterResource(id = R.drawable.json_file)
                                                    else painterResource(id = R.drawable.database),
                                                    iconColor = SaltTheme.colors.text
                                                )
                                                PopupMenuItem(
                                                    onClick = {
                                                        MyVibrationEffect(
                                                            context,
                                                            enableHaptic.value,
                                                            hapticStrength.intValue
                                                        ).click()
                                                        selectedMethod.intValue = 1
                                                        coroutine.launch {
                                                            dataStore.edit { settings ->
                                                                settings[DataStoreConstants.GET_PLAYLIST_METHOD] =
                                                                    1
                                                            }
                                                        }
                                                        methodPopupMenuState.dismiss()
                                                    },
                                                    selected = selectedMethod.intValue == 1,
                                                    text = stringResource(R.string.online),
                                                    iconPainter = painterResource(id = R.drawable.network),
                                                    iconColor = SaltTheme.colors.text
                                                )
                                            }
                                            AnimatedVisibility(
                                                visible = (selectedSourceApp.intValue != 0) && !useRootAccess.value && (selectedMethod.intValue == 0)
                                            ) {
                                                Column {
                                                    Item(
                                                        onClick = {
                                                            if (selectedSourceApp.intValue == 5)
                                                                convertPage.selectJsonFileDir()
                                                            else
                                                                convertPage.selectDatabaseFile()
                                                        },
                                                        text =
                                                        if (selectedSourceApp.intValue == 5)
                                                            stringResource(R.string.select_json_file_dir_match_to_source).replace(
                                                                "#",
                                                                stringResource(R.string.source_luna_music)
                                                            )
                                                        else
                                                            stringResource(R.string.select_database_file_match_to_source).replace(
                                                                "#",
                                                                when (selectedSourceApp.intValue) {
                                                                    1 -> stringResource(R.string.source_netease_cloud_music)
                                                                    2 -> stringResource(R.string.source_qq_music)
                                                                    3 -> stringResource(R.string.source_kugou_music)
                                                                    4 -> stringResource(R.string.source_kuwo_music)
                                                                    else -> ""
                                                                }
                                                            )
                                                    )
                                                    AnimatedVisibility(
                                                        visible = (databaseFileName.value != "") && !useRootAccess.value
                                                    ) {
                                                        ItemValue(
                                                            text = stringResource(R.string.you_have_selected),
                                                            rightSub = databaseFileName.value
                                                        )
                                                    }
                                                }
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
                                                sub = stringResource(R.string.use_other_result_file).replace(
                                                    "#",
                                                    stringResource(id = R.string.app_name)
                                                ),
                                                enableHaptic = enableHaptic.value,
                                                hapticStrength = hapticStrength.intValue
                                            )
                                            AnimatedVisibility(
                                                visible = useCustomResultFile.value
                                            ) {
                                                Column {
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
                                    }
                                } else {
                                    RoundedColumn {
                                        ItemTitle(text = stringResource(R.string.source_format))
                                        ItemPopup(
                                            state = sourceLocalAppPopupMenuState,
                                            text = stringResource(R.string.source_music_player),
                                            selectedItem = when (selectedSourceLocalApp) {
                                                0 -> "Salt Player"
                                                1 -> "APlayer"
                                                2 -> "Poweramp"
                                                else -> ""
                                            },
                                            popupWidth = 160
                                        ) {
                                            PopupMenuItem(
                                                onClick = {
                                                    MyVibrationEffect(
                                                        context,
                                                        enableHaptic.value,
                                                        hapticStrength.intValue
                                                    ).click()
                                                    selectedSourceLocalApp = 0
                                                    sourceLocalAppPopupMenuState.dismiss()
                                                },
                                                selected = selectedSourceLocalApp == 0,
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
                                                    MyVibrationEffect(
                                                        context,
                                                        enableHaptic.value,
                                                        hapticStrength.intValue
                                                    ).click()
                                                    selectedSourceLocalApp = 1
                                                    sourceLocalAppPopupMenuState.dismiss()
                                                },
                                                selected = selectedSourceLocalApp == 1,
                                                text = "APlayer",
                                                iconPainter = painterResource(id = R.drawable.aplayer),
                                                iconColor = SaltTheme.colors.text,
                                                iconPaddingValues = PaddingValues(all = 1.dp)
                                            )
                                            PopupMenuItem(
                                                onClick = {
                                                    MyVibrationEffect(
                                                        context,
                                                        enableHaptic.value,
                                                        hapticStrength.intValue
                                                    ).click()
                                                    selectedSourceLocalApp = 2
                                                    sourceLocalAppPopupMenuState.dismiss()
                                                },
                                                selected = selectedSourceLocalApp == 2,
                                                text = "Poweramp",
                                                iconPainter = painterResource(id = R.drawable.poweramp),
                                                iconColor = SaltTheme.colors.text,
                                                iconPaddingValues = PaddingValues(all = 1.dp)
                                            )
                                        }
                                        Item(
                                            onClick = {
                                                convertPage.selectPlaylistFile(
                                                    selectedSourceLocalApp
                                                )
                                            },
                                            text = stringResource(R.string.select_playlist_file_match_to_source).replace(
                                                "#",
                                                when (selectedSourceLocalApp) {
                                                    0 -> "Salt Player"
                                                    1 -> "APlayer"
                                                    2 -> "Poweramp"
                                                    else -> ""
                                                }
                                            )
                                        )
                                        AnimatedVisibility(
                                            visible = convertPage.sourcePlaylistFileName.value.isNotBlank()
                                        ) {
                                            ItemValue(
                                                text = stringResource(R.string.you_have_selected),
                                                rightSub = convertPage.sourcePlaylistFileName.value
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
                                            MyVibrationEffect(
                                                context,
                                                enableHaptic.value,
                                                hapticStrength.intValue
                                            ).click()
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
                                            MyVibrationEffect(
                                                context,
                                                enableHaptic.value,
                                                hapticStrength.intValue
                                            ).click()
                                            selectedTargetApp = 1
                                            targetAppPopupMenuState.dismiss()
                                        },
                                        selected = selectedTargetApp == 1,
                                        text = "APlayer",
                                        iconPainter = painterResource(id = R.drawable.aplayer),
                                        iconColor = SaltTheme.colors.text,
                                        iconPaddingValues = PaddingValues(all = 1.dp)
                                    )
                                    PopupMenuItem(
                                        onClick = {
                                            MyVibrationEffect(
                                                context,
                                                enableHaptic.value,
                                                hapticStrength.intValue
                                            ).click()
                                            selectedTargetApp = 2
                                            targetAppPopupMenuState.dismiss()
                                        },
                                        selected = selectedTargetApp == 2,
                                        text = "Poweramp",
                                        iconPainter = painterResource(id = R.drawable.poweramp),
                                        iconColor = SaltTheme.colors.text,
                                        iconPaddingValues = PaddingValues(all = 1.dp)
                                    )
                                }
                            }

                            AnimatedContent(
                                targetState = showLoadingProgressBar.value,
                                label = "",
                                transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                }) {
                                ItemContainer {
                                    TextButton(
                                        onClick = {
                                            if (selectedConvertMode.intValue == 1) {
                                                selectedMultiSourceApp = -1
                                                convertPage.requestPermission()
                                            } else {
                                                if (selectedSourceLocalApp == selectedTargetApp) {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.source_target_same),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    MyVibrationEffect(
                                                        context,
                                                        enableHaptic.value,
                                                        hapticStrength.intValue
                                                    ).done()
                                                } else {
                                                    coroutine.launch(Dispatchers.IO) {
                                                        showDialogProgressBar.value = true
                                                        val convertSuccess =
                                                            convertPage.convertLocalPlaylist(
                                                                sourceApp = selectedSourceLocalApp,
                                                                targetApp = selectedTargetApp
                                                            )
                                                        if (convertSuccess.isBlank()) {
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(
                                                                    context,
                                                                    context.getString(R.string.convert_failed),
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        } else {
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(
                                                                    context,
                                                                    "${context.getString(R.string.convert_success)}${convertSuccess}",
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                            }
                                                        }
                                                        MyVibrationEffect(
                                                            context,
                                                            enableHaptic.value,
                                                            hapticStrength.intValue
                                                        ).done()
                                                        showDialogProgressBar.value = true
                                                    }
                                                }
                                            }
                                        },
                                        text = if (selectedConvertMode.intValue == 1) stringResource(
                                            R.string.next_step_text
                                        ) else stringResource(R.string.start_text),
                                        enabled = !it,
                                        enableHaptic = enableHaptic.value,
                                        hapticStrength = hapticStrength.intValue
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
                                    visible = playlistEnabled.isEmpty()
                                ) {
                                    ItemContainer {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            AnimatedContent(
                                                targetState = showLoadingProgressBar.value,
                                                label = "",
                                                transitionSpec = {
                                                    fadeIn() togetherWith fadeOut()
                                                }) {
                                                Text(
                                                    text = if (it)
                                                        stringResource(id = R.string.loading)
                                                    else
                                                        stringResource(id = R.string.no_playlist_found),
                                                    fontSize = 16.sp,
                                                    color = SaltTheme.colors.subText
                                                )
                                            }
                                        }
                                    }
                                }
                                AnimatedVisibility(
                                    visible = playlistEnabled.isNotEmpty()
                                ) {
                                    ItemContainer {
                                        Column(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
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
                                                enableHaptic = enableHaptic.value,
                                                hapticStrength = hapticStrength.intValue
                                            )
                                            LazyColumn(
                                                state = lazyListState1,
                                                modifier = Modifier.heightIn(
                                                    max = (LocalConfiguration.current.screenHeightDp / 1.9).dp
                                                )
                                            ) {
                                                items(playlistEnabled.size) { index ->
                                                    AnimatedVisibility(
                                                        visible = convertPage.playlistShow[index],
                                                        exit = ExitTransition.None
                                                    ) {
                                                        ItemCheck(
                                                            state = playlistEnabled[index] != 0,
                                                            onChange = {
                                                                if (it)
                                                                    playlistEnabled[index] = 1
                                                                else
                                                                    playlistEnabled[index] = 0
                                                            },
                                                            text = if (convertPage.playlistShow[index]) playlistName[index] else "",
                                                            sub = if (convertPage.playlistShow[index])
                                                                stringResource(R.string.total).replace(
                                                                    "#",
                                                                    playlistSum[index].toString()
                                                                ) else "",
                                                            enableHaptic = enableHaptic.value,
                                                            minHeightIn = if (convertPage.playlistShow[index])
                                                                50.dp else 0.dp,
                                                            hapticStrength = hapticStrength.intValue
                                                        )
                                                    }
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
                            AnimatedContent(
                                targetState = showLoadingProgressBar.value,
                                label = "",
                                transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                }) {
                                ItemContainer {
                                    Row {
                                        if (selectedMethod.intValue == 1) {
                                            BasicButton(
                                                modifier = Modifier
                                                    .padding(end = 16.dp),
                                                enabled = !it,
                                                onClick = {
                                                    convertPage.getOnlinePlaylist(
                                                        kugouUserRelated = kugouUserRelated,
                                                        kugouCurrentIp = kugouCurrentIp
                                                    )
                                                },
                                                backgroundColor = SaltTheme.colors.subBackground,
                                                enableHaptic = enableHaptic.value,
                                                hapticStrength = hapticStrength.intValue
                                            ) {
                                                Icon(
                                                    modifier = Modifier
                                                        .size(22.dp),
                                                    painter = painterResource(id = R.drawable.refresh),
                                                    contentDescription = null,
                                                    tint = if (it) Color.Gray else SaltTheme.colors.text
                                                )
                                            }
                                        }
                                        TextButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = { convertPage.checkSongListSelection() },
                                            text = stringResource(R.string.next_step_text),
                                            enabled = !it,
                                            enableHaptic = enableHaptic.value,
                                            hapticStrength = hapticStrength.intValue
                                        )
                                        if (selectedMethod.intValue == 1) {
                                            BasicButton(
                                                modifier = Modifier
                                                    .padding(start = 16.dp),
                                                enabled = !it,
                                                onClick = {
                                                    convertPage.showCustomPlaylistDialog.value =
                                                        true
                                                },
                                                backgroundColor = SaltTheme.colors.subBackground,
                                                enableHaptic = enableHaptic.value,
                                                hapticStrength = hapticStrength.intValue
                                            ) {
                                                Icon(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .padding(all = 1.dp),
                                                    painter = painterResource(id = R.drawable.plus),
                                                    contentDescription = null,
                                                    tint = if (it) Color.Gray else SaltTheme.colors.text
                                                )
                                            }
                                        }
                                    }
                                }
                            }
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
                                                        if (playlistEnabled.count { it == 2 || it == 3 } == -1)
                                                            "0"
                                                        else {
                                                            (playlistEnabled.count { it == 2 || it == 3 } + 1).toString()
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
                                                textWeight = 1f,
                                                rightSubWeight = 2f,
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
                                                            enableHaptic.value,
                                                            hapticStrength.intValue
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
                                                            enableHaptic.value,
                                                            hapticStrength.intValue
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
                                                enableHaptic = enableHaptic.value,
                                                hapticStrength = hapticStrength.intValue
                                            )
                                            ItemSwitcher(
                                                state = enableArtistNameMatch.value,
                                                onChange = { enableArtistNameMatch.value = it },
                                                text = stringResource(R.string.enable_artist_name_match),
                                                enableHaptic = enableHaptic.value,
                                                hapticStrength = hapticStrength.intValue
                                            )
                                            ItemSwitcher(
                                                state = enableAlbumNameMatch.value,
                                                onChange = { enableAlbumNameMatch.value = it },
                                                text = stringResource(R.string.enable_album_name_match),
                                                enableHaptic = enableHaptic.value,
                                                hapticStrength = hapticStrength.intValue
                                            )
                                        }
                                        AnimatedContent(
                                            targetState = showNumberProgressBar.value || showLoadingProgressBar.value,
                                            label = "",
                                            transitionSpec = {
                                                fadeIn() togetherWith fadeOut()
                                            }) {
                                            ItemContainer {
                                                TextButton(
                                                    onClick = {
                                                        selectedFilterIndex = 0
                                                        convertPage.previewResult()
                                                    },
                                                    text = stringResource(R.string.preview_convert_result),
                                                    enabled = !it,
                                                    enableHaptic = enableHaptic.value,
                                                    hapticStrength = hapticStrength.intValue
                                                )
                                            }
                                        }
                                    }
                                }

                                1 -> {
                                    val lazyListState = rememberLazyListState()
                                    Column(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .fillMaxSize()
                                            .background(color = SaltTheme.colors.background)
                                            .verticalScroll(rememberScrollState())
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
                                                            5 -> stringResource(R.string.source_luna_music)
                                                            else -> ""
                                                        }
                                                    )

                                                    1 -> stringResource(R.string.convert_result)
                                                    else -> ""
                                                },
                                                popupWidth = 180,
                                                rightSubWeight = 2f
                                            ) {
                                                PopupMenuItem(
                                                    onClick = {
                                                        MyVibrationEffect(
                                                            context,
                                                            enableHaptic.value,
                                                            hapticStrength.intValue
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
                                                                5 -> stringResource(R.string.source_luna_music)
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
                                                        5 -> painterResource(id = R.drawable.luna_music)
                                                        else -> painterResource(id = R.drawable.android)
                                                    },
                                                    iconColor = SaltTheme.colors.text,
                                                    iconPaddingValues = PaddingValues(all = 2.dp)
                                                )
                                                PopupMenuItem(
                                                    onClick = {
                                                        MyVibrationEffect(
                                                            context,
                                                            enableHaptic.value,
                                                            hapticStrength.intValue
                                                        ).click()
                                                        showOriginalSonglist = 1
                                                        originalSonglistPopupMenuState.dismiss()
                                                    },
                                                    text = stringResource(R.string.convert_result),
                                                    selected = showOriginalSonglist == 1,
                                                    iconPainter = painterResource(id = R.drawable.result),
                                                    iconColor = SaltTheme.colors.text,
                                                    iconPaddingValues = PaddingValues(all = 1.5.dp)
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
                                                            enableHaptic.value,
                                                            hapticStrength.intValue
                                                        ).click()
                                                        selectedFilterIndex = 0
                                                        filterPopupMenuState.dismiss()
                                                    },
                                                    text = "${stringResource(id = R.string.all)} - ${convertResult.size}",
                                                    selected = selectedFilterIndex == 0,
                                                    iconPainter = painterResource(id = R.drawable.all),
                                                    iconColor = SaltTheme.colors.text,
                                                    iconPaddingValues = PaddingValues(all = 2.dp)
                                                )
                                                PopupMenuItem(
                                                    onClick = {
                                                        MyVibrationEffect(
                                                            context,
                                                            enableHaptic.value,
                                                            hapticStrength.intValue
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
                                                    iconPaddingValues = PaddingValues(all = 2.dp)
                                                )
                                                PopupMenuItem(
                                                    onClick = {
                                                        MyVibrationEffect(
                                                            context,
                                                            enableHaptic.value,
                                                            hapticStrength.intValue
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
                                                            enableHaptic.value,
                                                            hapticStrength.intValue
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

                                        AnimatedContent(
                                            targetState = showOriginalSonglist,
                                            label = "",
                                            transitionSpec = {
                                                fadeIn() togetherWith fadeOut()

                                            }) { it1 ->
                                            RoundedColumn {
                                                ItemTitle(
                                                    text = when (it1) {
                                                        0 -> stringResource(id = R.string.original_songlist).replace(
                                                            "#",
                                                            when (selectedSourceApp.intValue) {
                                                                1 -> stringResource(R.string.source_netease_cloud_music)
                                                                2 -> stringResource(R.string.source_qq_music)
                                                                3 -> stringResource(R.string.source_kugou_music)
                                                                4 -> stringResource(R.string.source_kuwo_music)
                                                                5 -> stringResource(R.string.source_luna_music)
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
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .heightIn(max = (LocalConfiguration.current.screenHeightDp / 2.1).dp)
                                                            .background(color = SaltTheme.colors.subBackground)
                                                    ) {
                                                        AnimatedContent(
                                                            targetState = selectedFilterIndex,
                                                            label = "",
                                                            transitionSpec = {
                                                                fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
                                                            }) {
                                                            LazyColumn(
                                                                state = lazyListState,
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
                                                                                text = convertResult[index]!![2 - it1],
                                                                                sub = "${
                                                                                    stringResource(
                                                                                        R.string.singer
                                                                                    )
                                                                                }${convertResult[index]!![4 - it1]}\n${
                                                                                    stringResource(R.string.album)
                                                                                }${convertResult[index]!![6 - it1]}",
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
                                                                        if (convertResult[index] != null && convertResult[index]!![0] == "0"
                                                                        )
                                                                            Item(
                                                                                onClick = {
                                                                                    selectedSongIndex =
                                                                                        index
                                                                                    showSelectedSongInfoDialog =
                                                                                        true
                                                                                },
                                                                                text = convertResult[index]!![2 - it1],
                                                                                sub = "${
                                                                                    stringResource(
                                                                                        R.string.singer
                                                                                    )
                                                                                }${convertResult[index]!![4 - it1]}\n${
                                                                                    stringResource(R.string.album)
                                                                                }${convertResult[index]!![6 - it1]}",
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
                                                                        if (convertResult[index] != null && convertResult[index]!![0] == "1"
                                                                        )
                                                                            Item(
                                                                                onClick = {
                                                                                    selectedSongIndex =
                                                                                        index
                                                                                    showSelectedSongInfoDialog =
                                                                                        true
                                                                                },
                                                                                text = convertResult[index]!![2 - it1],
                                                                                sub = "${
                                                                                    stringResource(
                                                                                        R.string.singer
                                                                                    )
                                                                                }${convertResult[index]!![4 - it1]}\n${
                                                                                    stringResource(R.string.album)
                                                                                }${convertResult[index]!![6 - it1]}",
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
                                                                        if (convertResult[index] != null && convertResult[index]!![0] == "2"
                                                                        )
                                                                            Item(
                                                                                onClick = {
                                                                                    selectedSongIndex =
                                                                                        index
                                                                                    showSelectedSongInfoDialog =
                                                                                        true
                                                                                },
                                                                                text = convertResult[index]!![2 - it1],
                                                                                sub = "${
                                                                                    stringResource(
                                                                                        R.string.singer
                                                                                    )
                                                                                }${convertResult[index]!![4 - it1]}\n${
                                                                                    stringResource(R.string.album)
                                                                                }${convertResult[index]!![6 - it1]}",
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
                                        }
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = SaltTheme.dimens.outerHorizontalPadding,
                                                vertical = 8.dp
                                            ),
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
                                                enableHaptic = enableHaptic.value,
                                                hapticStrength = hapticStrength.intValue
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            TextButton(
                                                onClick = {
                                                    showSaveDialog.value = true
                                                },
                                                modifier = Modifier.weight(1f),
                                                text = stringResource(id = R.string.save_conversion_results),
                                                enableHaptic = enableHaptic.value,
                                                hapticStrength = hapticStrength.intValue
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
                                ItemContainer {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .heightIn(max = (LocalConfiguration.current.screenHeightDp / 2.2).dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(color = SaltTheme.colors.background)
                                    ) {
                                        items(convertPage.resultFileLocation.size) { index ->
                                            ItemText(
                                                text = convertPage.resultFileLocation[index],
                                                fontSize = 15.sp,
                                                verticalPadding = 4.dp
                                            )
                                        }
                                    }
                                }
                            }
                            AnimatedVisibility(visible = userLoggedIn) {
                                RoundedColumn {
                                    ItemTitle(
                                        text = stringResource(R.string.account).replace(
                                            "#",
                                            when (selectedSourceApp.intValue) {
                                                1 -> stringResource(id = R.string.source_netease_cloud_music)
                                                2 -> stringResource(id = R.string.source_qq_music)
                                                3 -> stringResource(id = R.string.source_kugou_music)
                                                4 -> stringResource(id = R.string.source_kuwo_music)
                                                5 -> stringResource(R.string.source_luna_music)
                                                else -> ""
                                            }
                                        )
                                    )
                                    ItemContainer {
                                        TextButton(
                                            onClick = {
                                                CookieManager.getInstance().removeAllCookies(null)
                                                CookieManager.getInstance().flush()
                                                coroutine.launch(Dispatchers.IO) {
                                                    if (selectedSourceApp.intValue == 5)
                                                        convertPage.lunaLogout()
                                                    dataStore.edit { settings ->
                                                        settings[DataStoreConstants.NETEASE_USER_ID] =
                                                            ""
                                                        settings[DataStoreConstants.LAST_LOGIN_TIMESTAMP] =
                                                            0L
                                                        settings[DataStoreConstants.KUGOU_TOKEN] =
                                                            ""
                                                        settings[DataStoreConstants.KUGOU_USER_ID] =
                                                            ""
                                                    }
                                                }
                                                loginQrCode = null
                                                convertPage.stopGetLoginStatus()
                                                kugouDeviceId = false
                                                qrCodeDeviceRelated.clear()
                                                kugouUserRelated.clear()
                                                kugouCurrentIp = ""
                                                convertPage.lunaVerifyRelated.clear()
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.logged_out),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                userLoggedIn = false
                                            },
                                            text = stringResource(id = R.string.logout),
                                            enableHaptic = enableHaptic.value,
                                            hapticStrength = hapticStrength.intValue
                                        )
                                    }
                                }
                            }

                            ItemContainer {
                                Row {
                                    TextButton(
                                        onClick = { convertPage.copyFolderPathToClipboard() },
                                        modifier = Modifier.weight(1f),
                                        text = stringResource(id = R.string.copy_folder_path),
                                        textColor = SaltTheme.colors.subText,
                                        backgroundColor = SaltTheme.colors.subBackground,
                                        enableHaptic = enableHaptic.value,
                                        hapticStrength = hapticStrength.intValue
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
                                        enableHaptic = enableHaptic.value,
                                        hapticStrength = hapticStrength.intValue
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