package com.hwinzniej.musichelper.pages

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.alibaba.fastjson2.JSON
import com.hwinzniej.musichelper.Item
import com.hwinzniej.musichelper.ItemCheck
import com.hwinzniej.musichelper.ItemPopup
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.TextButton
import com.hwinzniej.musichelper.YesDialog
import com.hwinzniej.musichelper.YesNoDialog
import com.hwinzniej.musichelper.data.SourceApp
import com.hwinzniej.musichelper.data.database.MusicDatabase
import com.hwinzniej.musichelper.utils.Tools
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.ItemValue
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.popup.PopupMenuItem
import com.moriafly.salt.ui.popup.rememberPopupState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import java.io.File
import kotlin.math.roundToInt

class ConvertPage(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val openFileLauncher: ActivityResultLauncher<Array<String>>,
    val db: MusicDatabase,
) {
    var databaseFileName = mutableStateOf("")
    var selectedSourceApp = mutableIntStateOf(0)
    var useCustomResultFile = mutableStateOf(false)
    var customResultFileName = mutableStateOf("")
    var selectedFileName = mutableStateOf("")
    var showLoadingProgressBar = mutableStateOf(false)
    var showErrorDialog = mutableStateOf(false)
    var errorDialogTitle = mutableStateOf("")
    var errorDialogContent = mutableStateOf("")
    var databaseFilePath = ""
    var resultFilePath = ""
    var sourceApp = SourceApp()
    val loadingProgressSema = Semaphore(2)
    var currentPage = mutableIntStateOf(0)
    var selectedMatchingMode = mutableIntStateOf(1)
    var enableBracketRemoval = mutableStateOf(false)
    var enableArtistNameMatch = mutableStateOf(true)
    var enableAlbumNameMatch = mutableStateOf(true)
    var similarity = mutableFloatStateOf(85f)

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
        errorDialogContent.value = context.getString(R.string.error_details)
        checkDatabaseFile()
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
            if (useCustomResultFile.value) {
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
                        "${errorDialogContent.value}\n- ${context.getString(R.string.result_file)} ${
                            context.getString(
                                R.string.read_failed
                            )
                        }: ${e.message}"
                    haveError = true
                } finally {
                    loadingProgressSema.release()
                }
            } else {
                try {
                    val musicCount = db.musicDao().getMusicCount()
                    if (musicCount == 0) {
                        showErrorDialog.value = true
                        errorDialogTitle.value =
                            context.getString(R.string.error_while_getting_data_dialog_title)
                        errorDialogContent.value =
                            "${errorDialogContent.value}\n- ${context.getString(R.string.result_file)} ${
                                context.getString(
                                    R.string.read_failed
                                )
                            }: ${context.getString(R.string.use_scan_fun_first)}"
                        haveError = true
                    }
                } catch (e: Exception) {
                    showErrorDialog.value = true
                    errorDialogTitle.value =
                        context.getString(R.string.error_while_getting_data_dialog_title)
                    errorDialogContent.value =
                        "${errorDialogContent.value}\n- ${context.getString(R.string.result_file)} ${
                            context.getString(
                                R.string.read_failed
                            )
                        }: ${context.getString(R.string.use_scan_fun_first)}"
                    haveError = true
                } finally {
                    loadingProgressSema.release()
                }
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
                        "${errorDialogContent.value}\n- ${context.getString(R.string.database_file)} ${
                            context.getString(
                                R.string.read_failed
                            )
                        }: ${
                            context.getString(
                                R.string.please_select_source_app
                            )
                        }"
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
                    loadingProgressSema.release()
                } catch (e: Exception) {
                    showErrorDialog.value = true
                    errorDialogTitle.value =
                        context.getString(R.string.error_while_getting_data_dialog_title)
                    errorDialogContent.value =
                        "${errorDialogContent.value}\n- ${context.getString(R.string.database_file)} ${
                            context.getString(
                                R.string.read_failed
                            )
                        }: ${e.message}"
                    haveError = true
                    loadingProgressSema.release()
                }
            } else {
                showErrorDialog.value = true
                errorDialogTitle.value =
                    context.getString(R.string.error_while_getting_data_dialog_title)
                errorDialogContent.value =
                    "${errorDialogContent.value}\n- ${context.getString(R.string.database_file)} ${
                        context.getString(
                            R.string.read_failed
                        )
                    }: ${
                        context.getString(
                            R.string.please_select_source_app
                        )
                    }"
                haveError = true
                loadingProgressSema.release()
            }
        }
    }

    var playlistId = mutableStateListOf<String>()
    var playlistName = mutableStateListOf<String>()
    var playlistEnabled = mutableStateListOf<Int>()
    var playlistSum = mutableStateListOf<Int>()
    fun databaseSummary() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            showLoadingProgressBar.value = true
            playlistId.clear()
            playlistName.clear()
            playlistEnabled.clear()
            playlistSum.clear()
            val innerPlaylistId = MutableList(0) { "" }
            val innerPlaylistName = MutableList(0) { "" }
            val innerPlaylistEnabled = MutableList(0) { 0 }
            val innerPlaylistSum = MutableList(0) { 0 }

            val file = File(databaseFilePath)
            val db = SQLiteDatabase.openOrCreateDatabase(file, null)

            val cursor = if (sourceApp.sourceEng == "KuWoMusic")
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
                val songListId =
                    cursor.getString(cursor.getColumnIndexOrThrow(sourceApp.songListId))
                val songListName =
                    cursor.getString(cursor.getColumnIndexOrThrow(sourceApp.songListName))

                val currentSonglistSumCursor = db.rawQuery(
                    "SELECT COUNT(*) FROM ${sourceApp.songListSongInfoTableName} WHERE ${sourceApp.songListSongInfoPlaylistId} = ?",
                    arrayOf(songListId)
                )
                currentSonglistSumCursor.moveToFirst()
                if (currentSonglistSumCursor.getInt(0) == 0) {
                    currentSonglistSumCursor.close()
                    continue
                } else {
                    innerPlaylistSum.add(currentSonglistSumCursor.getInt(0))
                    currentSonglistSumCursor.close()
                }
                innerPlaylistId.add(songListId)
                innerPlaylistName.add(songListName)
                innerPlaylistEnabled.add(0)
            }
            cursor.close()
            playlistId.addAll(innerPlaylistId)
            playlistName.addAll(innerPlaylistName)
            playlistEnabled.addAll(innerPlaylistEnabled)
            playlistSum.addAll(innerPlaylistSum)
            showLoadingProgressBar.value = false
        }
    }

    fun checkSongListSelection() {
        if (playlistEnabled.all { it == 0 }) {
            showErrorDialog.value = true
            errorDialogTitle.value =
                context.getString(R.string.error)
            errorDialogContent.value =
                context.getString(R.string.please_select_at_least_one_playlist)
            return
        }
        for (i in playlistEnabled.indices) {
            if (playlistEnabled[i] == 2) {
                playlistEnabled[i] = 1
            }
        }
        currentPage.intValue = 2
    }

    var convertResult = mutableStateMapOf<Int, Array<String>>()
    fun previewResult() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            convertResult.clear()
            val convertResultMap = mutableMapOf<Int, Array<String>>()
            val firstIndex1 = playlistEnabled.indexOfFirst { it == 1 }

            showLoadingProgressBar.value = true
            val music3InfoList = db.musicDao().getMusic3Info()
            var songName: String
            var songArtist: String
            var songAlbum: String
            var num = 0

            val file = File(databaseFilePath)
            val db = SQLiteDatabase.openOrCreateDatabase(file, null)
            val cursor = db.rawQuery(
                "SELECT ${sourceApp.songListSongInfoSongId} FROM ${sourceApp.songListSongInfoTableName} WHERE ${sourceApp.songListSongInfoPlaylistId} = '${playlistId[firstIndex1]}' ORDER BY ${sourceApp.sortField}",
                null
            )
            while (cursor.moveToNext()) {
                val trackId =
                    cursor.getString(cursor.getColumnIndexOrThrow(sourceApp.songListSongInfoSongId))
                val songInfoCursor = db.rawQuery(
                    "SELECT ${sourceApp.songInfoSongName} , ${sourceApp.songInfoSongArtist} , ${sourceApp.songInfoSongAlbum} FROM ${sourceApp.songInfoTableName} WHERE ${sourceApp.songInfoSongId} = $trackId",
                    null
                )
                songInfoCursor.moveToFirst()
                songName =
                    songInfoCursor.getString(songInfoCursor.getColumnIndexOrThrow(sourceApp.songInfoSongName))
                songArtist =
                    songInfoCursor.getString(songInfoCursor.getColumnIndexOrThrow(sourceApp.songInfoSongArtist))
                if (sourceApp.sourceEng == "CloudMusic")
                    songArtist =
                        JSON.parseObject(songArtist.substring(1, songArtist.length - 1))
                            .getString("name")
                songArtist = songArtist.replace(" ?& ?".toRegex(), "/").replace("、", "/")
                songAlbum =
                    songInfoCursor.getString(songInfoCursor.getColumnIndexOrThrow(sourceApp.songInfoSongAlbum))

                if (selectedMatchingMode.intValue == 1) {
                    val songSimilarityArray = mutableMapOf<String, Double>()
                    val artistSimilarityArray = mutableMapOf<String, Double>()
                    val albumSimilarityArray = mutableMapOf<String, Double>()

                    //歌曲名相似度列表
                    if (enableBracketRemoval.value) for (k in music3InfoList.indices) {
                        songSimilarityArray[k.toString()] = Tools().similarityRatio(
                            songName.replace(
                                "(?i) ?\\((?!inst|[^()]* ver)[^)]*\\) ?".toRegex(),
                                ""
                            ).lowercase(),
                            music3InfoList[k].song
                                .replace(
                                    "(?i) ?\\((?!inst|[^()]* ver)[^)]*\\) ?".toRegex(),
                                    ""
                                ).lowercase()
                        )
                    } else for (k in music3InfoList.indices) {
                        songSimilarityArray[k.toString()] = Tools().similarityRatio(
                            songName.lowercase(), music3InfoList[k].song.lowercase()
                        )
                    }

                    var maxSimilarity = Tools().getMaxValue(songSimilarityArray)
                    val songNameMaxSimilarity = maxSimilarity?.value!!
                    val songNameMaxKey = maxSimilarity.key

                    //歌手名相似度列表
                    var songArtistMaxSimilarity: Double
                    if (enableArtistNameMatch.value) {
                        if (enableBracketRemoval.value) for (k in music3InfoList.indices) {
                            artistSimilarityArray[k.toString()] =
                                Tools().similarityRatio(
                                    songArtist.replace(
                                        "(?i) ?\\((?!inst|[^()]* ver)[^)]*\\) ?".toRegex(),
                                        ""
                                    ).lowercase(),
                                    music3InfoList[k].artist
                                        .replace(
                                            "(?i) ?\\((?!inst|[^()]* ver)[^)]*\\) ?".toRegex(),
                                            ""
                                        ).lowercase()
                                )
                        } else for (k in music3InfoList.indices) {
                            artistSimilarityArray[k.toString()] =
                                Tools().similarityRatio(
                                    songArtist.lowercase(),
                                    music3InfoList[k].artist.lowercase()
                                )
                        }
                        maxSimilarity =
                            Tools().getMaxValue(artistSimilarityArray) //获取键值对表中相似度的最大值所在的键值对
                        songArtistMaxSimilarity = maxSimilarity?.value!! //获取相似度的最大值
                        val songArtistMaxKey = maxSimilarity?.key //获取相似度的最大值对应的歌手名
                    } else {
                        songArtistMaxSimilarity = 1.0
                    }

                    //专辑名相似度列表
                    var songAlbumMaxSimilarity: Double
                    if (enableAlbumNameMatch.value) {
                        if (enableBracketRemoval.value) for (k in music3InfoList.indices) {
                            albumSimilarityArray[k.toString()] =
                                Tools().similarityRatio(
                                    songAlbum.replace(
                                        "(?i) ?\\((?!inst|[^()]* ver)[^)]*\\) ?".toRegex(),
                                        ""
                                    ).lowercase(),
                                    music3InfoList[k].album
                                        .replace(
                                            "(?i) ?\\((?!inst|[^()]* ver)[^)]*\\) ?".toRegex(),
                                            ""
                                        )
                                        .lowercase()
                                )
                        } else for (k in music3InfoList.indices) {
                            albumSimilarityArray[k.toString()] =
                                Tools().similarityRatio(
                                    songAlbum.lowercase(),
                                    music3InfoList[k].album.lowercase()
                                )
                        }
                        maxSimilarity =
                            Tools().getMaxValue(albumSimilarityArray) //获取键值对表中相似度的最大值所在的键值对
                        songAlbumMaxSimilarity = maxSimilarity?.value!! //获取相似度的最大值
                        val songAlbumMaxKey = maxSimilarity?.key //获取相似度的最大值对应的专辑名
                    } else {
                        songAlbumMaxSimilarity = 1.0
                    }

                    val autoSuccess =
                        (songNameMaxSimilarity >= similarity.floatValue / 100
                                && songArtistMaxSimilarity >= similarity.floatValue / 100
                                && songAlbumMaxSimilarity >= similarity.floatValue / 100)

                    val SongConvertResult = music3InfoList[songNameMaxKey.toInt()]
                    convertResultMap[num++] =
                        arrayOf(
                            autoSuccess.toString(),  //是否自动匹配成功
                            SongConvertResult.song,  //本地音乐歌曲名
                            songName,  //云音乐歌曲名
                            SongConvertResult.artist,  //本地音乐歌手名
                            songArtist,  //云音乐歌手名
                            SongConvertResult.album,  //本地音乐专辑名
                            songArtist,  //云音乐专辑名
                        )

                } else if (selectedMatchingMode.intValue == 2) {

                }
                songInfoCursor.close()
            }
            cursor.close()
            playlistEnabled[firstIndex1] = 2
            convertResult.putAll(convertResultMap)
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
    playlistEnabled: MutableList<Int>,
    playlistSum: MutableList<Int>,
    currentPage: MutableIntState,
    selectedMatchingMode: MutableIntState,
    enableBracketRemoval: MutableState<Boolean>,
    enableArtistNameMatch: MutableState<Boolean>,
    enableAlbumNameMatch: MutableState<Boolean>,
    similarity: MutableFloatState,
    convertResult: MutableMap<Int, Array<String>>,
) {
    val context = LocalContext.current
    val sourceAppPopupMenuState = rememberPopupState()
    val matchingModePopupMenuState = rememberPopupState()
    var sourceApp by remember { mutableStateOf("") }
    var matchingMode by remember { mutableStateOf("") }
    val pages = listOf("0", "1", "2", "3")
    val pageState = rememberPagerState(pageCount = { pages.size })
    var allEnabled by remember { mutableStateOf(false) }
    var showSetSimilarityDialog by remember { mutableStateOf(false) }
    val resultPages = listOf("0", "1")
    val resultPageState = rememberPagerState(pageCount = { resultPages.size })

    BackHandler(enabled = currentPage.intValue != 0) {
        if (convertResult.isEmpty()) {
            currentPage.intValue--
        } else {
            convertResult.clear()
        }
    }

    if (showErrorDialog.value) {
        YesDialog(
            onDismissRequest = { showErrorDialog.value = false },
            title = errorDialogTitle.value,
            content = errorDialogContent.value
        )
    }

    if (showSetSimilarityDialog) {
        var slideSimilarity by remember { mutableFloatStateOf(similarity.floatValue) }
        YesNoDialog(
            onDismiss = { showSetSimilarityDialog = false },
            onNegative = { showSetSimilarityDialog = false },
            onPositive = {
                similarity.floatValue = slideSimilarity
                showSetSimilarityDialog = false
            },
            title = context.getString(R.string.set_similarity_dialog_title),
            content = "",
            noText = context.getString(R.string.cancel_button_text),
            yesText = context.getString(R.string.ok_button_text),
            onlyComposeView = true,
            customContent = {
                Column {
                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = { slideSimilarity-- },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = SaltTheme.colors.text
                            ),
                        ) {
                            Text("-", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "${slideSimilarity.roundToInt()}%", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.TextButton(
                            onClick = { slideSimilarity++ },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
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
    LaunchedEffect(key1 = currentPage.intValue) {
        pageState.animateScrollToPage(currentPage.intValue)
    }
    LaunchedEffect(key1 = convertResult.isEmpty()) {
        if (convertResult.isEmpty()) {
            resultPageState.animateScrollToPage(0)
        } else {
            resultPageState.animateScrollToPage(1)
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
                    currentPage.intValue--
                } else {
                    convertResult.clear()
                }
            },
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
                                    state = sourceAppPopupMenuState,
                                    text = context.getString(R.string.select_source_of_songlist),
                                    selectedItem = sourceApp
                                ) {
                                    PopupMenuItem(
                                        onClick = {
                                            selectedSourceApp.intValue = 1
                                            sourceAppPopupMenuState.dismiss()
                                            databaseFileName.value = ""
                                        },
                                        selected = selectedSourceApp.intValue == 1,
                                        text = context.getString(R.string.source_netease_cloud_music)
                                    )
                                    PopupMenuItem(
                                        onClick = {
                                            selectedSourceApp.intValue = 2
                                            sourceAppPopupMenuState.dismiss()
                                            databaseFileName.value = ""
                                        },
                                        selected = selectedSourceApp.intValue == 2,
                                        text = context.getString(R.string.source_qq_music)
                                    )

                                    PopupMenuItem(
                                        onClick = {
                                            selectedSourceApp.intValue = 3
                                            sourceAppPopupMenuState.dismiss()
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
                                            sourceAppPopupMenuState.dismiss()
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
                                        onClick = { convertPage.checkSelectedFiles() },
                                        text = context.getString(R.string.next_step_text),
                                        enabled = !it
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
                                ItemTitle(text = context.getString(R.string.select_needed_musiclist))
                                AnimatedVisibility(
                                    visible = playlistEnabled.isNotEmpty()
                                ) {
                                    ItemContainer {
                                        Column(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .heightIn(max = (LocalConfiguration.current.screenHeightDp / 1.6).dp)
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
                                                text = context.getString(R.string.select_all),
                                            )
                                            LazyColumn(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                items(playlistEnabled.size) { index ->
                                                    ItemCheck(
                                                        state = playlistEnabled[index] != 0,
                                                        onChange = {
                                                            if (playlistEnabled[index] == 0)
                                                                playlistEnabled[index] = 1
                                                            else
                                                                playlistEnabled[index] = 0
                                                        },
                                                        text = playlistName[index],
                                                        sub = "${context.getString(R.string.total)}${playlistSum[index]}${
                                                            context.getString(R.string.songs)
                                                        }"
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            ItemValue(
                                                text = "${context.getString(R.string.selected)}${playlistEnabled.count { it != 0 }}${
                                                    context.getString(
                                                        R.string.ge
                                                    )
                                                }",
                                                sub = "${context.getString(R.string.in_total)}${playlistEnabled.size}${
                                                    context.getString(
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
                                    text = context.getString(R.string.next_step_text)
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
                                            ItemTitle(text = context.getString(R.string.current_songlist_info))
                                            ItemValue(
                                                text = context.getString(R.string.songlist_sequence),
                                                sub = "${context.getString(R.string.current_no)}${
                                                    if (playlistEnabled.count { it == 2 } == -1) 0 else {
                                                        playlistEnabled.count { it == 2 } + 1
                                                    }
                                                }${context.getString(R.string.ge)} - ${
                                                    context.getString(R.string.in_total)
                                                }${playlistEnabled.count { it != 0 }}${
                                                    context.getString(R.string.ge)
                                                }"
                                            )
                                            ItemValue(
                                                text = context.getString(R.string.songlist_name),
                                                sub = if (playlistEnabled.indexOfFirst { it == 1 } == -1) "" else playlistName[playlistEnabled.indexOfFirst { it == 1 }]
                                            )
                                            ItemValue(
                                                text = context.getString(R.string.songlist_sum),
                                                sub = "${if (playlistEnabled.indexOfFirst { it == 1 } == -1) "" else playlistSum[playlistEnabled.indexOfFirst { it == 1 }]}"
                                            )
                                        }
                                        RoundedColumn {
                                            ItemTitle(text = context.getString(R.string.convert_options))
                                            Item(
                                                onClick = { showSetSimilarityDialog = true },
                                                text = context.getString(R.string.set_similarity_item),
                                                rightSub = "${similarity.floatValue.roundToInt()}%"
                                            )
                                            ItemPopup(
                                                state = matchingModePopupMenuState,
                                                text = context.getString(R.string.matching_mode),
                                                selectedItem = matchingMode
                                            ) {
                                                PopupMenuItem(
                                                    onClick = {
                                                        selectedMatchingMode.intValue = 1
                                                        matchingModePopupMenuState.dismiss()
                                                    },
                                                    selected = selectedMatchingMode.intValue == 1,
                                                    text = context.getString(R.string.split_matching)
                                                )
                                                PopupMenuItem(
                                                    onClick = {
                                                        selectedMatchingMode.intValue = 2
                                                        matchingModePopupMenuState.dismiss()
                                                    },
                                                    selected = selectedMatchingMode.intValue == 2,
                                                    text = context.getString(R.string.overall_matching)
                                                )
                                            }

                                            matchingMode = when (selectedMatchingMode.intValue) {
                                                1 -> context.getString(R.string.split_matching)
                                                2 -> context.getString(R.string.overall_matching)
                                                else -> ""
                                            }

                                            ItemSwitcher(
                                                state = enableBracketRemoval.value,
                                                onChange = { enableBracketRemoval.value = it },
                                                text = context.getString(R.string.enable_bracket_removal)
                                            )
                                            ItemSwitcher(
                                                state = enableArtistNameMatch.value,
                                                onChange = { enableArtistNameMatch.value = it },
                                                text = context.getString(R.string.enable_artist_name_match)
                                            )
                                            ItemSwitcher(
                                                state = enableAlbumNameMatch.value,
                                                onChange = { enableAlbumNameMatch.value = it },
                                                text = context.getString(R.string.enable_album_name_match)
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
                                                    onClick = { convertPage.previewResult() },
                                                    text = context.getString(R.string.preview_convert_result),
                                                    enabled = !it
                                                )
                                            }
                                        }


//                RoundedColumn {
                                        ItemContainer {
                                            TextButton(
                                                onClick = { convertPage.checkSongListSelection() },
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
                                    ) {
//                                        AnimatedVisibility(
//                                            visible = convertResult.isNotEmpty()
//                                        ) {
                                        RoundedColumn {
                                            ItemTitle(text = context.getString(R.string.convert_result))
                                            ItemContainer {
                                                Column(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .heightIn(max = (LocalConfiguration.current.screenHeightDp / 1.4).dp)
                                                        .background(color = SaltTheme.colors.subBackground)
                                                ) {
                                                    LazyColumn(
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        items(convertResult.size) { index ->
                                                            Item(
                                                                onClick = { /*TODO*/ },
                                                                text = convertResult[index]!![1],
                                                                sub = "${context.getString(R.string.singer)}${convertResult[index]!![3]}\n${
                                                                    context.getString(
                                                                        R.string.album
                                                                    )
                                                                }${convertResult[index]!![5]}",
                                                                rightSub = convertResult[index]!![0],
                                                                rightSubColor = if (convertResult[index]!![0] == "true") Color(
                                                                    ContextCompat.getColor(
                                                                        context,
                                                                        R.color.matched
                                                                    )
                                                                ) else Color(
                                                                    ContextCompat.getColor(
                                                                        context,
                                                                        R.color.unmatched
                                                                    )
                                                                ),
                                                            )
                                                        }
                                                    }
//                                        Spacer(modifier = Modifier.height(5.dp))
//                                        ItemValue(
//                                            text = "${context.getString(R.string.in_total)}${playlistEnabled.size}",
//                                            sub = "${context.getString(R.string.selected)}${playlistEnabled.count { it }}"
//                                        )
                                                }
                                            }
                                        }
//                                        }
                                    }
                                }
                            }
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