package com.hwinzniej.musichelper.pages

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.alibaba.fastjson2.JSON
import com.hwinzniej.musichelper.Item
import com.hwinzniej.musichelper.ItemCheck
import com.hwinzniej.musichelper.ItemPopup
import com.hwinzniej.musichelper.ItemText
import com.hwinzniej.musichelper.ItemValue
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.TextButton
import com.hwinzniej.musichelper.YesDialog
import com.hwinzniej.musichelper.YesNoDialog
import com.hwinzniej.musichelper.data.SourceApp
import com.hwinzniej.musichelper.data.database.MusicDatabase
import com.hwinzniej.musichelper.utils.Tools
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.ItemEdit
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.popup.PopupMenuItem
import com.moriafly.salt.ui.popup.rememberPopupState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
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
    var resultFilePath = ""  //TODO 直接换用SQL文件？
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

                    val songConvertResult = music3InfoList[songNameMaxKey.toInt()]
                    convertResultMap[num++] =
                        arrayOf(
                            if (autoSuccess) context.getString(R.string.match_success)
                            else context.getString(R.string.match_caution),  //是否自动匹配成功
                            songConvertResult.song,  //本地音乐歌曲名
                            songName,  //云音乐歌曲名
                            songConvertResult.artist,  //本地音乐歌手名
                            songArtist,  //云音乐歌手名
                            songConvertResult.album,  //本地音乐专辑名
                            songAlbum,  //云音乐专辑名
                            songConvertResult.absolutePath,  //本地音乐绝对路径
                        )

                } else if (selectedMatchingMode.intValue == 2) {
                    val similarityArray = mutableMapOf<String, Double>()
                    var songInfo = songName
                    if (enableArtistNameMatch.value)
                        songInfo = "$songInfo$songArtist"
                    if (enableAlbumNameMatch.value)
                        songInfo = "$songInfo$songAlbum"

                    if (enableBracketRemoval.value)
                        for (k in music3InfoList.indices) {
                            similarityArray[k.toString()] = Tools().similarityRatio(
                                songInfo.replace(
                                    "(?i) ?\\((?!inst|[^()]* ver)[^)]*\\) ?".toRegex(),
                                    ""
                                ).lowercase(),
                                "${music3InfoList[k].song}${music3InfoList[k].artist}${music3InfoList[k].album}"
                                    .replace(
                                        "(?i) ?\\((?!inst|[^()]* ver)[^)]*\\) ?".toRegex(),
                                        ""
                                    ).lowercase()
                            )
                        }
                    else
                        for (k in music3InfoList.indices) {
                            similarityArray[k.toString()] = Tools().similarityRatio(
                                songInfo.lowercase(),
                                "${music3InfoList[k].song}${music3InfoList[k].artist}${music3InfoList[k].album}".lowercase()
                            )
                        }
                    val maxSimilarity = Tools().getMaxValue(similarityArray)
                    val songMaxSimilarity = maxSimilarity?.value!!
                    val songMaxKey = maxSimilarity.key

                    val autoSuccess = songMaxSimilarity >= similarity.floatValue / 100

                    val songConvertResult = music3InfoList[songMaxKey.toInt()]

                    convertResultMap[num++] =
                        arrayOf(
                            if (autoSuccess) context.getString(R.string.match_success)
                            else context.getString(R.string.match_caution),  //是否自动匹配成功
                            songConvertResult.song,  //本地音乐歌曲名
                            songName,  //云音乐歌曲名
                            songConvertResult.artist,  //本地音乐歌手名
                            songArtist,  //云音乐歌手名
                            songConvertResult.album,  //本地音乐专辑名
                            songAlbum,  //云音乐专辑名
                            songConvertResult.absolutePath,  //本地音乐绝对路径
                        )
                }
                songInfoCursor.close()
            }
            cursor.close()
            convertResult.putAll(convertResultMap)
            showLoadingProgressBar.value = false
        }
    }

    var inputSearchWords = mutableStateOf("")
    var searchResult = mutableStateListOf<Array<String>>()
    var showDialogProgressBar = mutableStateOf(false)
    fun searchSong() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            showDialogProgressBar.value = true
            searchResult.clear()
            if (inputSearchWords.value == "") {
                showDialogProgressBar.value = false
                return@launch
            }
            val searchResultList = db.musicDao().searchMusic("%${inputSearchWords.value}%")
            val searchResultMap = mutableListOf<Array<String>>()
            for (i in searchResultList.indices) {
                searchResultMap.add(
                    arrayOf(
                        searchResultList[i].song,
                        searchResultList[i].artist,
                        searchResultList[i].album,
                        searchResultList[i].id.toString()
                    )
                )
            }
            if (searchResultMap.isEmpty()) {
                searchResult.add(arrayOf(context.getString(R.string.no_search_result)))
            } else {
                searchResult.addAll(searchResultMap)
            }
            showDialogProgressBar.value = false
        }
    }

    fun saveModificationSong(songPosition: Int, songId: Int) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val songInfo = db.musicDao().getMusicById(songId)
            convertResult[songPosition]?.set(0, context.getString(R.string.match_manual))
            convertResult[songPosition]?.set(1, songInfo.song)
            convertResult[songPosition]?.set(3, songInfo.artist)
            convertResult[songPosition]?.set(5, songInfo.album)
            convertResult[songPosition]?.set(7, songInfo.absolutePath)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.modification_success),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    var showSaveDialog = mutableStateOf(false)
    fun saveCurrentConvertResult(
        saveSuccessSongs: Boolean,
        saveCautionSongs: Boolean,
        saveManualSongs: Boolean,
        fileName: String
    ) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            if (!saveSuccessSongs && !saveCautionSongs && !saveManualSongs) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.no_song_to_save),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }
            showDialogProgressBar.value = true
            val firstIndex1 = playlistEnabled.indexOfFirst { it == 1 }
            val file = File(
                "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/MusicHelper",
                fileName
            )
            if (file.exists())
                file.delete()
            if (file.parentFile?.exists() == false)
                file.parentFile?.mkdirs()
            val fileWriter = FileWriter(file, true)

            for (it in 0 until convertResult.size) {
                if (convertResult[it]!![0] == context.getString(R.string.match_success) && saveSuccessSongs) {
                    fileWriter.write("${convertResult[it]!![7]}\n")
                }
                if (convertResult[it]!![0] == context.getString(R.string.match_caution) && saveCautionSongs) {
                    fileWriter.write("${convertResult[it]!![7]}\n")
                }
                if (convertResult[it]!![0] == context.getString(R.string.match_manual) && saveManualSongs) {
                    fileWriter.write("${convertResult[it]!![7]}\n")
                }
            }
            fileWriter.close()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.save_success),
                    Toast.LENGTH_SHORT
                ).show()
            }
            showDialogProgressBar.value = false
            showSaveDialog.value = false
            convertResult.clear()
            playlistEnabled[firstIndex1] = 2
        }
    }
}

@OptIn(UnstableSaltApi::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
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
) {
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
    var showSelectedSongInfoDialog by remember { mutableStateOf(false) }
    var selectedSongIndex by remember { mutableIntStateOf(-1) }
    val coroutine = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var selectedSearchResult by remember { mutableIntStateOf(-1) }
    val filterPopupMenuState = rememberPopupState()

    BackHandler(enabled = currentPage.intValue != 0) { // TODO 在其他页也会被拦截；当在处理转换结果时尝试返回，弹出确认对话框
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
                        Text(
                            text = "${slideSimilarity.roundToInt()}%",
                            fontSize = 18.sp,
                            color = SaltTheme.colors.text
                        )
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
                convertPage.saveModificationSong(selectedSongIndex, selectedAllIndex)
                inputSearchWords.value = ""
                showSelectedSongInfoDialog = false
            },
            title = stringResource(id = R.string.modify_conversion_results),
            content = "",
            cancelText = stringResource(R.string.cancel_button_text),
            confirmText = stringResource(R.string.ok_button_text),
            onlyComposeView = true,
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
                        ItemEdit(
                            text = inputSearchWords.value,
                            hint = stringResource(R.string.search_song_library),
                            onChange = {
                                inputSearchWords.value = it
                            }
                        )
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
                                        enabled = searchResult[index].size != 1
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ItemTitle(text = stringResource(R.string.songlist_song_info))
                        ItemValue(
                            text = stringResource(id = R.string.song_name),
                            sub = convertResult[selectedSongIndex]!![2],
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
                            sub = convertResult[selectedSongIndex]!![4],
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
                            sub = convertResult[selectedSongIndex]!![6],
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
                        ItemTitle(text = stringResource(R.string.save_options))
                        ItemText(text = stringResource(R.string.state_of_the_song_to_be_saved))
                        ItemSwitcher(
                            state = saveSuccessSongs,
                            onChange = { saveSuccessSongs = it },
                            text = stringResource(R.string.match_success)
                        )
                        ItemSwitcher(
                            state = saveCautionSongs,
                            onChange = { saveCautionSongs = it },
                            text = stringResource(R.string.match_caution)
                        )
                        ItemSwitcher(
                            state = saveManualSongs,
                            onChange = { saveManualSongs = it },
                            text = stringResource(R.string.match_manual)
                        )
                        ItemText(text = "${stringResource(R.string.result_file_save_location)}\n/Download/MusicHelper/${playlistName[playlistEnabled.indexOfFirst { it == 1 }]}.txt")
                    }
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
                    currentPage.intValue--
                } else {
                    convertResult.clear()
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
                                    selectedItem = sourceApp
                                ) {
                                    PopupMenuItem(
                                        onClick = {
                                            selectedSourceApp.intValue = 1
                                            sourceAppPopupMenuState.dismiss()
                                            databaseFileName.value = ""
                                        },
                                        selected = selectedSourceApp.intValue == 1,
                                        text = stringResource(R.string.source_netease_cloud_music)
                                    )
                                    PopupMenuItem(
                                        onClick = {
                                            selectedSourceApp.intValue = 2
                                            sourceAppPopupMenuState.dismiss()
                                            databaseFileName.value = ""
                                        },
                                        selected = selectedSourceApp.intValue == 2,
                                        text = stringResource(R.string.source_qq_music)
                                    )

                                    PopupMenuItem(
                                        onClick = {
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
                                        sub = databaseFileName.value
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
                                    sub = stringResource(R.string.use_other_result_file)
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
                                        text = stringResource(R.string.next_step_text),
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
                                ItemTitle(text = stringResource(R.string.select_needed_musiclist))
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
                                                text = stringResource(R.string.select_all),
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
                                                        sub = "${stringResource(R.string.total)}${playlistSum[index]}${
                                                            stringResource(R.string.songs)
                                                        }"
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            ItemValue(
                                                text = "${stringResource(R.string.selected)}${playlistEnabled.count { it != 0 }}${
                                                    stringResource(
                                                        R.string.ge
                                                    )
                                                }",
                                                sub = "${stringResource(R.string.in_total)}${playlistEnabled.size}${
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
                                    text = stringResource(R.string.next_step_text)
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
                            var selectedFilterIndex by remember { mutableIntStateOf(0) }
                            var selectedFilter by remember { mutableStateOf("") }
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
                                                text = stringResource(R.string.songlist_sequence),  //TODO 判断是否是最后一个歌单
                                                sub = "${stringResource(R.string.current_no)}${
                                                    if (playlistEnabled.count { it == 2 } == -1) 0 else {
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
                                                sub = if (playlistEnabled.indexOfFirst { it == 1 } == -1) "" else playlistName[playlistEnabled.indexOfFirst { it == 1 }]
                                            )
                                            ItemValue(
                                                text = stringResource(R.string.songlist_sum),
                                                sub = "${if (playlistEnabled.indexOfFirst { it == 1 } == -1) "" else playlistSum[playlistEnabled.indexOfFirst { it == 1 }]}"
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
                                                selectedItem = matchingMode
                                            ) {
                                                PopupMenuItem(
                                                    onClick = {
                                                        selectedMatchingMode.intValue = 1
                                                        matchingModePopupMenuState.dismiss()
                                                    },
                                                    selected = selectedMatchingMode.intValue == 1,
                                                    text = stringResource(R.string.split_matching)
                                                )
                                                PopupMenuItem(
                                                    onClick = {
                                                        selectedMatchingMode.intValue = 2
                                                        matchingModePopupMenuState.dismiss()
                                                    },
                                                    selected = selectedMatchingMode.intValue == 2,
                                                    text = stringResource(R.string.overall_matching)
                                                )
                                            }

                                            matchingMode = when (selectedMatchingMode.intValue) {
                                                1 -> stringResource(R.string.split_matching)
                                                2 -> stringResource(R.string.overall_matching)
                                                else -> ""
                                            }

                                            ItemSwitcher(
                                                state = enableBracketRemoval.value,
                                                onChange = { enableBracketRemoval.value = it },
                                                text = stringResource(R.string.enable_bracket_removal)
                                            )
                                            ItemSwitcher(
                                                state = enableArtistNameMatch.value,
                                                onChange = { enableArtistNameMatch.value = it },
                                                text = stringResource(R.string.enable_artist_name_match)
                                            )
                                            ItemSwitcher(
                                                state = enableAlbumNameMatch.value,
                                                onChange = { enableAlbumNameMatch.value = it },
                                                text = stringResource(R.string.enable_album_name_match)
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
                                                        convertPage.previewResult()
                                                        selectedFilterIndex = 0
                                                        selectedFilter = ""
                                                    },
                                                    text = stringResource(R.string.preview_convert_result),
                                                    enabled = !it
                                                )
                                            }
                                        }


//                RoundedColumn {
                                        ItemContainer {
                                            TextButton(
                                                onClick = { convertPage.checkSongListSelection() },
                                                text = stringResource(R.string.next_step_text)
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
                                            ItemTitle(text = stringResource(R.string.filter))
                                            ItemPopup(
                                                state = filterPopupMenuState,
                                                text = stringResource(id = R.string.convert_status),
                                                selectedItem = selectedFilter
                                            ) {
                                                PopupMenuItem(
                                                    onClick = {
                                                        selectedFilterIndex = 0
                                                        filterPopupMenuState.dismiss()
                                                    },
                                                    text = "${stringResource(id = R.string.all)} - ${convertResult.size}",
                                                    selected = selectedFilterIndex == 0
                                                )
                                                PopupMenuItem(
                                                    onClick = {
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

                                        selectedFilter = when (selectedFilterIndex) {
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
                                                                    Item(
                                                                        onClick = {
                                                                            selectedSongIndex =
                                                                                index
                                                                            showSelectedSongInfoDialog =
                                                                                true
                                                                        },
                                                                        text = convertResult[index]!![1],
                                                                        sub = "${stringResource(R.string.singer)}${convertResult[index]!![3]}\n${
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

                                                                            else -> colorResource(R.color.manual)
                                                                        },
                                                                    )
                                                                }

                                                                1 -> items(convertResult.size) { index ->
                                                                    if (convertResult[index]!![0] == stringResource(
                                                                            R.string.match_success
                                                                        )
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
                                                                        )
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
                                                                        )
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
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            TextButton(
                                                onClick = {
                                                    showSaveDialog.value = true
                                                },
                                                modifier = Modifier.weight(1f),
                                                text = stringResource(id = R.string.save_conversion_results)
                                            )
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