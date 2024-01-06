package com.hwinzniej.musichelper.pages

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.hwinzniej.musichelper.ItemCheck
import com.hwinzniej.musichelper.ItemPopup
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.YesDialog
import com.hwinzniej.musichelper.data.SourceApp
import com.hwinzniej.musichelper.utils.Tools
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.ItemValue
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TextButton
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.popup.PopupMenuItem
import com.moriafly.salt.ui.popup.rememberPopupState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import java.io.File

class ConvertPage(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val openFileLauncher: ActivityResultLauncher<Array<String>>,
) {
    var databaseFileName = mutableStateOf("")
    var selectedSourceApp = mutableIntStateOf(0)
    var useCustomResultFile = mutableStateOf(false)
    var customResultFileName = mutableStateOf("")
    var selectedFileName = mutableStateOf("")
    var showLoadingProgressBar = mutableStateOf(false)
    var showErrorDialog = mutableStateOf(false)
    var errorDialogTitle = mutableStateOf("")
    var errorDialogContent =
        mutableStateOf(context.getString(R.string.error_while_getting_data_dialog_content))
    var databaseFilePath = ""
    var resultFilePath = ""
    var sourceApp = SourceApp()
    val loadingProgressSema = Semaphore(2)
    var currentPage = mutableIntStateOf(0)

    fun selectDatabaseFile() {
        openFileLauncher.launch(arrayOf("*/*"))
    }

    fun selectResultFile() {
        openFileLauncher.launch(arrayOf("text/plain"))
    }

    fun handleUri(uri: Uri?) {
        // 这是用户选择的目录的Uri
        // 你可以在这里处理用户选择的目录
        if (uri == null) {
            return
        }
        selectedFileName.value = uri.pathSegments[uri.pathSegments.size - 1]
        selectedFileName.value =
            selectedFileName.value.substring(selectedFileName.value.lastIndexOf("/") + 1)
        if (selectedFileName.value.endsWith(".txt")) {
            resultFilePath = Tools().uriToAbsolutePath(uri)
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                delay(300L)
                customResultFileName.value = selectedFileName.value
            }
        } else {
            databaseFilePath = Tools().uriToAbsolutePath(uri)
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                delay(300L)
                databaseFileName.value = selectedFileName.value
            }
        }
    }

    var haveError = false
    fun checkSelectedFiles() {
        haveError = false
        showLoadingProgressBar.value = true
        errorDialogContent.value =
            context.getString(R.string.error_while_getting_data_dialog_content)
        checkDatabaseFile()
        if (useCustomResultFile.value)
            checkResultFile()
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            delay(100L)
            loadingProgressSema.acquire()
            loadingProgressSema.acquire()
            showLoadingProgressBar.value = false
            if (!haveError) {
                showLoadingProgressBar.value = true
                currentPage.intValue = 1
                delay(500L)
                databaseSummary()
            }
            loadingProgressSema.release()
            loadingProgressSema.release()
        }
    }

    fun checkResultFile() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            loadingProgressSema.acquire()
            try {
                val file = File(resultFilePath)
                val localMusicFile =
                    file.readText().split("\n").dropLast(1)
                val localMusic = Array(localMusicFile.size) {
                    arrayOfNulls<String>(
                        5
                    )
                }
                for ((a, i) in localMusicFile.withIndex()) {
                    val parts = i.split("#\\*#".toRegex())
                    localMusic[a][0] = parts[0]
                    localMusic[a][1] = parts[1]
                    localMusic[a][2] = parts[2]
                    localMusic[a][3] = parts[3]
                    localMusic[a][4] = parts[4]
                }
            } catch (e: Exception) {
                showErrorDialog.value = true
                errorDialogTitle.value =
                    context.getString(R.string.error_while_getting_data_dialog_title)
                errorDialogContent.value =
                    "${errorDialogContent.value} \n${context.getString(R.string.result_file)}"
                haveError = true
            } finally {
                loadingProgressSema.release()
            }
        }
    }

    fun checkDatabaseFile() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            loadingProgressSema.acquire()
            when (selectedSourceApp.intValue) {
                0 -> {
                    showErrorDialog.value = true
                    errorDialogTitle.value =
                        context.getString(R.string.error_while_getting_data_dialog_title)
                    errorDialogContent.value =
                        "${errorDialogContent.value}\n${context.getString(R.string.database_file)}"
                    loadingProgressSema.release()
                    haveError = true
                    return@launch
                }

                1 -> sourceApp.init("CloudMusic")
                2 -> sourceApp.init("QQMusic")
                3 -> sourceApp.init("KugouMusic")
                4 -> sourceApp.init("KuwoMusic")
            }
            if (sourceApp.sourceEng != "") {
                val file = File(databaseFilePath)
                val db = SQLiteDatabase.openOrCreateDatabase(file, null)
                try {
                    val cursor =
                        db.rawQuery("SELECT * FROM  ${sourceApp.songListTableName} LIMIT 1", null)
                    cursor.close()
                    db.close()
                    loadingProgressSema.release()
                } catch (e: Exception) {
                    showErrorDialog.value = true
                    errorDialogTitle.value =
                        context.getString(R.string.error_while_getting_data_dialog_title)
                    errorDialogContent.value =
                        "${errorDialogContent.value}\n${context.getString(R.string.database_file)}"
                    haveError = true
                    loadingProgressSema.release()
                }
            } else {
                showErrorDialog.value = true
                errorDialogTitle.value =
                    context.getString(R.string.error_while_getting_data_dialog_title)
                errorDialogContent.value =
                    "${errorDialogContent.value}\n${context.getString(R.string.database_file)}"
                haveError = true
                loadingProgressSema.release()
            }
        }
    }

    var playlistId = mutableStateListOf<String>()
    var playlistName = mutableStateListOf<String>()
    var playlistEnabled = mutableStateListOf<Boolean>()
    var playlistSum = mutableStateListOf<Int>()
    fun databaseSummary() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            showLoadingProgressBar.value = true
            playlistId.clear()
            playlistName.clear()
            playlistEnabled.clear()
            playlistSum.clear()
            val file = File(databaseFilePath)
            val db = SQLiteDatabase.openOrCreateDatabase(file, null)

            val cursor = if (sourceApp.sourceEng.equals("KuWoMusic"))
                db.rawQuery(
                    "SELECT ${sourceApp.songListId}, ${sourceApp.songListName} FROM ${sourceApp.songListTableName}  WHERE uid NOT NULL",
                    null
                )
            else
                db.rawQuery(
                    "SELECT ${sourceApp.songListId}, ${sourceApp.songListName} FROM ${sourceApp.songListTableName}",
                    null
                )
            while (cursor.moveToNext()) {
                val songListId = cursor.getString(cursor.getColumnIndex(sourceApp.songListId))
                val songListName =
                    cursor.getString(cursor.getColumnIndex(sourceApp.songListName))

                val currentSonglistSumCursor = db.rawQuery(
                    "SELECT COUNT(*) FROM ${sourceApp.songListSongInfoTableName} WHERE ${sourceApp.songListSongInfoPlaylistId} = ?",
                    arrayOf(songListId)
                )
                currentSonglistSumCursor.moveToFirst()
                if (currentSonglistSumCursor.getInt(0) == 0) {
                    currentSonglistSumCursor.close()
                    continue
                } else {
                    playlistSum.add(currentSonglistSumCursor.getInt(0))
                    currentSonglistSumCursor.close()
                }
                playlistId.add(songListId)
                playlistName.add(songListName)
                playlistEnabled.add(false)
            }
            cursor.close()
            db.close()
            showLoadingProgressBar.value = false
        }
    }
}

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
    playlistEnabled: MutableList<Boolean>,
    playlistSum: MutableList<Int>,
    currentPage: MutableIntState
) {
    val context = LocalContext.current
    val popupMenuState = rememberPopupState()
    var sourceApp by remember { mutableStateOf("") }
    val pages = listOf("0", "1", "2", "3")
    val pageState = rememberPagerState(pageCount = { pages.size })

    if (showErrorDialog.value) {
        YesDialog(
            onDismissRequest = { showErrorDialog.value = false },
            title = errorDialogTitle.value,
            content = errorDialogContent.value
        )
    }
    LaunchedEffect(key1 = currentPage.intValue) {
        pageState.animateScrollToPage(currentPage.intValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {
        TitleBar(
            onBack = { currentPage.intValue-- },
            text = context.getString(R.string.convert_function_name),
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
                                ItemTitle(text = context.getString(R.string.source_of_songlist_app))
                                ItemPopup(
                                    state = popupMenuState,
                                    text = context.getString(R.string.select_source_of_songlist),
                                    selectedItem = sourceApp
                                ) {
                                    PopupMenuItem(
                                        onClick = {
                                            selectedSourceApp.intValue = 1
                                            popupMenuState.dismiss()
                                            databaseFileName.value = ""
                                        },
                                        selected = selectedSourceApp.intValue == 1,
                                        text = context.getString(R.string.source_netease_cloud_music)
                                    )
                                    PopupMenuItem(
                                        onClick = {
                                            selectedSourceApp.intValue = 2
                                            popupMenuState.dismiss()
                                            databaseFileName.value = ""
                                        },
                                        selected = selectedSourceApp.intValue == 2,
                                        text = context.getString(R.string.source_qq_music)
                                    )

                                    PopupMenuItem(
                                        onClick = {
                                            selectedSourceApp.intValue = 3
                                            popupMenuState.dismiss()
                                            databaseFileName.value = ""
                                        },
                                        selected = selectedSourceApp.intValue == 3,
                                        text = context.getString(R.string.source_kugou_music),
//                        iconPainter = painterResource(id = R.drawable.ic_qr_code),
//                        iconColor = SaltTheme.colors.text
                                    )
                                    PopupMenuItem(
                                        onClick = {
                                            selectedSourceApp.intValue = 4
                                            popupMenuState.dismiss()
                                            databaseFileName.value = ""
                                        },
                                        selected = selectedSourceApp.intValue == 4,
                                        text = context.getString(R.string.source_kuwo_music)
                                    )
                                }
                            }

                            sourceApp = when (selectedSourceApp.intValue) {
                                1 -> context.getString(R.string.source_netease_cloud_music)
                                2 -> context.getString(R.string.source_qq_music)
                                3 -> context.getString(R.string.source_kugou_music)
                                4 -> context.getString(R.string.source_kuwo_music)
                                else -> ""
                            }

                            RoundedColumn {
                                ItemTitle(text = context.getString(R.string.import_database))
                                Item(
                                    enabled = selectedSourceApp.intValue != 0,
                                    onClick = { convertPage.selectDatabaseFile() },
                                    text = if (selectedSourceApp.intValue == 0) {
                                        context.getString(R.string.please_select_source_app)
                                    } else {
                                        context.getString(R.string.select_database_file_match_to_source_1) + sourceApp + context.getString(
                                            R.string.select_database_file_match_to_source_2
                                        )
                                    },
                                )
                                AnimatedVisibility(
                                    visible = databaseFileName.value != ""
                                ) {
                                    ItemValue(
                                        text = context.getString(R.string.you_have_selected),
                                        sub = databaseFileName.value
                                    )
                                }

                            }

                            RoundedColumn {
                                ItemTitle(text = context.getString(R.string.import_result_file))
                                ItemSwitcher(
                                    state = useCustomResultFile.value,
                                    onChange = {
                                        useCustomResultFile.value = it
                                    },
                                    text = context.getString(R.string.use_custom_result_file),
                                    sub = context.getString(R.string.use_other_result_file)
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
                                            text = context.getString(R.string.select_result_file_item_title),
                                        )
                                        AnimatedVisibility(
                                            visible = customResultFileName.value != ""
                                        ) {
                                            ItemValue(
                                                text = context.getString(R.string.you_have_selected),
                                                sub = customResultFileName.value
                                            )
                                        }
                                    }
                                }
                            }

//                RoundedColumn {
                            ItemContainer {
                                TextButton(
                                    onClick = { convertPage.checkSelectedFiles() },
                                    text = context.getString(R.string.next_step_text)
                                )
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
                                ItemTitle(text = context.getString(R.string.select_needed_musiclist))
                                AnimatedVisibility(
                                    visible = playlistEnabled.isNotEmpty()
                                ) {
                                    ItemContainer {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .heightIn(max = (LocalConfiguration.current.screenHeightDp / 1.75).dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(color = SaltTheme.colors.subBackground)
                                        ) {
                                            item {
                                                playlistEnabled.forEachIndexed { index, state ->
                                                    ItemCheck(
                                                        state = state,
                                                        onChange = {
                                                            playlistEnabled[index] = !state
                                                        },
                                                        text = playlistName[index],
                                                        sub = "${context.getString(R.string.total)}${playlistSum[index]}${
                                                            context.getString(R.string.songs)
                                                        }"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

//                RoundedColumn {
                            ItemContainer {
                                TextButton(
                                    onClick = { convertPage.databaseSummary() },
                                    text = context.getString(R.string.next_step_text)
                                )
                            }
//                }
                        }


                    }
                }
            }
        }
    }

}

//@Preview(showBackground = true)
//@Composable
//fun Preview() {
//    val convertPage = ConvertPage()
//    SaltTheme(
//        colors = lightSaltColors()
//    ) {
//        ConvertPageUi(
//            convertPage = convertPage,
//            selectedSourceApp = mutableIntStateOf(0),
//            databaseFileName = mutableStateOf("111.db")
//        )
//    }
//}