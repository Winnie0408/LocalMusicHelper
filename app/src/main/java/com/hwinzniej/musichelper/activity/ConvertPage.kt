package com.hwinzniej.musichelper.activity

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData.newPlainText
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.hwinzniej.musichelper.MainActivity
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.data.SourceApp
import com.hwinzniej.musichelper.data.database.MusicDatabase
import com.hwinzniej.musichelper.data.model.MusicInfo
import com.hwinzniej.musichelper.utils.MyVibrationEffect
import com.hwinzniej.musichelper.utils.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ConvertPage(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val openMusicPlatformSqlFileLauncher: ActivityResultLauncher<Array<String>>,
    val openResultSqlFileLauncher: ActivityResultLauncher<Array<String>>,
    val db: MusicDatabase,
    componentActivity: ComponentActivity
) : PermissionResultHandler {
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
    var useRootAccess = mutableStateOf(false)
    var sourceAppText = mutableStateOf("")
    var playlistId = mutableStateListOf<String>()
    var playlistName = mutableStateListOf<String>()
    var playlistEnabled = mutableStateListOf<Int>()
    var playlistSum = mutableStateListOf<Int>()

    /**
     * 请求存储权限
     */

//==================== Android 11+ 使用====================

    @RequiresApi(Build.VERSION_CODES.R)
    private val requestPermissionLauncher = componentActivity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Environment.isExternalStorageManager()) {
            checkSelectedFiles(250L)
        } else {
            Toast.makeText(context, R.string.permission_not_granted_toast, Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { //Android 11+
            if (Environment.isExternalStorageManager()) {
                checkSelectedFiles()
            } else {
                Toast.makeText(context, R.string.request_permission_toast, Toast.LENGTH_SHORT)
                    .show()
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                requestPermissionLauncher.launch(intent)
//                startActivity(context, intent, null)
            }
        } else { //Android 10-
            if (allPermissionsGranted()) {
                checkSelectedFiles()
            } else {
                Toast.makeText(context, R.string.request_permission_toast, Toast.LENGTH_SHORT)
                    .show()
                ActivityCompat.requestPermissions(
                    context as Activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        }
    }

//==================== Android 11+ 使用====================

//==================== Android 10- 使用====================

    private val REQUEST_CODE_PERMISSIONS = 1002
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    context, permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    override fun onPermissionResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                checkSelectedFiles(250L)
            } else {
                Toast.makeText(context, R.string.permission_not_granted_toast, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

//==================== Android 10- 使用====================

    fun selectDatabaseFile() {
        openMusicPlatformSqlFileLauncher.launch(arrayOf("*/*"))
    }

    fun selectResultFile() {
        openResultSqlFileLauncher.launch(arrayOf("*/*"))
    }

    fun handleUri(uri: Uri?, code: Int) {
        if (uri == null) {
            return
        }
        selectedFileName.value = uri.pathSegments[uri.pathSegments.size - 1]
        selectedFileName.value =
            selectedFileName.value.substring(selectedFileName.value.lastIndexOf("/") + 1)
        if (code == 0) {
            databaseFilePath = Tools().uriToAbsolutePath(uri)
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                delay(200L) //播放动画
                databaseFileName.value = selectedFileName.value
            }
        } else if (code == 1) {
            resultFilePath = Tools().uriToAbsolutePath(uri)
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                delay(200L)  //播放动画
                customResultFileName.value = selectedFileName.value
            }
        }
    }

    var haveError = false
    fun checkSelectedFiles(delay: Long = 0L) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            haveError = false
            showLoadingProgressBar.value = true
            playlistEnabled.clear()
            playlistId.clear()
            playlistName.clear()
            playlistSum.clear()
            errorDialogContent.value = context.getString(R.string.error_details)
            loadingProgressSema.acquire()
            loadingProgressSema.acquire()
            checkDatabaseFile()
            checkResultFile()
            delay(delay)
            loadingProgressSema.acquire()
            loadingProgressSema.acquire()
            MyVibrationEffect(
                context,
                (context as MainActivity).enableHaptic.value
            ).done()
            if (haveError) {
                showLoadingProgressBar.value = false
            } else {
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
            if (useCustomResultFile.value) {
                val db = SQLiteDatabase.openOrCreateDatabase(File(resultFilePath), null)
                try {
                    val cursor = db.rawQuery(
                        "SELECT id, song, artist, album, absolutePath FROM music LIMIT 1",
                        null
                    )
                    cursor.close()
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
                    db.close()
                    File("${resultFilePath}-journal").delete()
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

    fun checkAppStatusWithRoot() {
        val appExists =
            Tools().execShellCmdWithRoot("pm list packages | grep '${sourceApp.pakageName}'")
        if (appExists.contains(sourceApp.pakageName)) {
            val dirPath = context.getExternalFilesDir(null)?.absolutePath + "/userDatabase"
            val dir = File(dirPath)
            if (!dir.exists())
                dir.mkdirs()

            val copy = Tools().execShellCmdWithRoot(
                "cp -f /data/data/${sourceApp.pakageName}/databases/${
                    sourceApp.databaseName
                } ${dir.absolutePath}/${sourceApp.sourceEng}_temp.db"
            )

            if (copy == "") {
                databaseFilePath = "${dir.absolutePath}/${sourceApp.sourceEng}_temp.db"
                loadingProgressSema.release()
            } else {
                showErrorDialog.value = true
                errorDialogTitle.value =
                    context.getString(R.string.error_while_getting_data_dialog_title)
                errorDialogContent.value =
                    "${errorDialogContent.value}\n- ${context.getString(R.string.database_file)} ${
                        context.getString(
                            R.string.read_failed
                        )
                    }: $copy"
                haveError = true
                loadingProgressSema.release()
                return
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
                    context.getString(R.string.app_not_installed).replace("#", sourceAppText.value)
                }"
            haveError = true
            loadingProgressSema.release()
        }
    }

    fun checkDatabaseFile() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
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
                if (useRootAccess.value)
                    checkAppStatusWithRoot()
                else {
                    val db = SQLiteDatabase.openOrCreateDatabase(File(databaseFilePath), null)
                    try {
                        val cursor =
                            db.rawQuery(
                                "SELECT ${sourceApp.songListId}, ${sourceApp.songListName} FROM ${sourceApp.songListTableName} LIMIT 1",
                                null
                            )
                        cursor.close()
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
                    } finally {
                        loadingProgressSema.release()
                        db.close()
                    }
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

    fun databaseSummary() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            showLoadingProgressBar.value = true
            val innerPlaylistId = MutableList(0) { "" }
            val innerPlaylistName = MutableList(0) { "" }
            val innerPlaylistEnabled = MutableList(0) { 0 }
            val innerPlaylistSum = MutableList(0) { 0 }

            val db = SQLiteDatabase.openOrCreateDatabase(File(databaseFilePath), null)

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
            db.close()
            playlistId.addAll(innerPlaylistId)
            playlistName.addAll(innerPlaylistName)
            playlistEnabled.addAll(innerPlaylistEnabled)
            playlistSum.addAll(innerPlaylistSum)
            showLoadingProgressBar.value = false
            MyVibrationEffect(
                context,
                (context as MainActivity).enableHaptic.value
            ).done()
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

    var convertResult = mutableStateMapOf<Int, Array<String>>()  //TODO 切换语言后，匹配结果（成功、注意、手动）不会刷新
    fun previewResult() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            convertResult.clear()
            val convertResultMap = mutableMapOf<Int, Array<String>>()
            val firstIndex1 = playlistEnabled.indexOfFirst { it == 1 }

            showLoadingProgressBar.value = true
            val music3InfoList = if (useCustomResultFile.value) {
                val db = SQLiteDatabase.openOrCreateDatabase(File(resultFilePath), null)
                val musicInfoList = mutableListOf<MusicInfo>()
                db.rawQuery("SELECT song, artist, album, absolutePath, id FROM music", null)
                    .use { cursor ->
                        while (cursor.moveToNext()) {
                            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                            val song = cursor.getString(cursor.getColumnIndexOrThrow("song"))
                            val artist = cursor.getString(cursor.getColumnIndexOrThrow("artist"))
                            val album = cursor.getString(cursor.getColumnIndexOrThrow("album"))
                            val absolutePath =
                                cursor.getString(cursor.getColumnIndexOrThrow("absolutePath"))
                            val musicInfo = MusicInfo(
                                id,
                                song,
                                artist,
                                album,
                                null,
                                null,
                                null,
                                null,
                                absolutePath
                            )
                            musicInfoList.add(musicInfo)
                        }
                    }
                db.close()
                musicInfoList
            } else {
                db.musicDao().getMusic3Info()
            }

            var songName: String
            var songArtist: String
            var songAlbum: String
            var num = 0

            val db = SQLiteDatabase.openOrCreateDatabase(File(databaseFilePath), null)
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
                if (sourceApp.sourceEng == "CloudMusic") {
                    var tempArtist = ""
                    val jsonResult = JSON.parseArray(songArtist)
                    jsonResult.forEachIndexed { index, it ->
                        tempArtist = "${tempArtist}${(it as JSONObject).getString("name")}"
                        if (index != jsonResult.size - 1) {
                            tempArtist = "$tempArtist/"
                        }
                    }
                    songArtist = tempArtist
                }
//                        JSON.parseObject(songArtist.substring(1, songArtist.length - 1))
//                            .getString("name")
                songArtist = songArtist.replace(" ?& ?".toRegex(), "/").replace("、", "/")
                songAlbum =
                    songInfoCursor.getString(songInfoCursor.getColumnIndexOrThrow(sourceApp.songInfoSongAlbum))

                if (selectedMatchingMode.intValue == 1) {
                    val songSimilarityArray = mutableMapOf<String, Double>()
                    val artistSimilarityArray = mutableMapOf<String, Double>()
                    val albumSimilarityArray = mutableMapOf<String, Double>()

                    var songArtistMaxSimilarity = 0.0
                    var songAlbumMaxSimilarity = 0.0

                    val songThread = async {
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
                    }

                    val artistThread = async {
                        //歌手名相似度列表
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
                            songArtistMaxSimilarity =
                                Tools().getMaxValue(artistSimilarityArray)?.value!! //获取相似度的最大值
//                            val songArtistMaxKey =
//                                Tools().getMaxValue(artistSimilarityArray)?.key //获取相似度的最大值对应的歌手名
                        } else {
                            songArtistMaxSimilarity = 1.0
                        }
                    }

                    val albumThread = async {
                        //专辑名相似度列表
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
                            songAlbumMaxSimilarity =
                                Tools().getMaxValue(albumSimilarityArray)?.value!! //获取相似度的最大值
//                            val songAlbumMaxKey =
//                                Tools().getMaxValue(albumSimilarityArray)?.key //获取相似度的最大值对应的专辑名
                        } else {
                            songAlbumMaxSimilarity = 1.0
                        }
                    }
                    songThread.await()
                    artistThread.await()
                    albumThread.await()

                    val songNameMaxSimilarity = Tools().getMaxValue(songSimilarityArray)?.value!!
                    val songNameMaxKey = Tools().getMaxValue(songSimilarityArray)?.key

                    val autoSuccess =
                        (songNameMaxSimilarity >= similarity.floatValue / 100
                                && songArtistMaxSimilarity >= similarity.floatValue / 100
                                && songAlbumMaxSimilarity >= similarity.floatValue / 100)

                    val songConvertResult = music3InfoList[songNameMaxKey?.toInt()!!]
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
            db.close()
            convertResult.putAll(convertResultMap)
            showLoadingProgressBar.value = false
            MyVibrationEffect(
                context,
                (context as MainActivity).enableHaptic.value
            ).done()
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
            val searchResultList = if (useCustomResultFile.value) {
                val db = SQLiteDatabase.openOrCreateDatabase(File(resultFilePath), null)
                val musicInfoList = mutableListOf<MusicInfo>()
                db.rawQuery(
                    "SELECT song, artist, album, absolutePath, id FROM music WHERE song LIKE ? OR artist LIKE ? OR album LIKE ? LIMIT 3",
                    arrayOf(inputSearchWords.value, inputSearchWords.value, inputSearchWords.value)
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                        val song = cursor.getString(cursor.getColumnIndexOrThrow("song"))
                        val artist = cursor.getString(cursor.getColumnIndexOrThrow("artist"))
                        val album = cursor.getString(cursor.getColumnIndexOrThrow("album"))
                        val absolutePath =
                            cursor.getString(cursor.getColumnIndexOrThrow("absolutePath"))
                        val musicInfo = MusicInfo(
                            id,
                            song,
                            artist,
                            album,
                            null,
                            null,
                            null,
                            null,
                            absolutePath
                        )
                        musicInfoList.add(musicInfo)
                    }
                }
                db.close()
                musicInfoList
            } else
                db.musicDao().searchMusic("%${inputSearchWords.value}%")
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
            MyVibrationEffect(
                context,
                (context as MainActivity).enableHaptic.value
            ).done()
        }
    }

    fun saveModificationSong(songPosition: Int, songId: Int) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val songInfo = if (useCustomResultFile.value) {
                val db = SQLiteDatabase.openOrCreateDatabase(File(resultFilePath), null)
                val cursor = db.rawQuery(
                    "SELECT song, artist, album, absolutePath FROM music WHERE id = ?",
                    arrayOf(songId.toString())
                )
                cursor.moveToFirst()
                val song = cursor.getString(cursor.getColumnIndexOrThrow("song"))
                val artist = cursor.getString(cursor.getColumnIndexOrThrow("artist"))
                val album = cursor.getString(cursor.getColumnIndexOrThrow("album"))
                val absolutePath =
                    cursor.getString(cursor.getColumnIndexOrThrow("absolutePath"))
                cursor.close()
                db.close()
                MusicInfo(songId, song, artist, album, null, null, null, null, absolutePath)
            } else
                db.musicDao().getMusicById(songId)
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
                "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${
                    context.getString(
                        R.string.app_name
                    )
                }/${
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                }",
                fileName
            )
            if (file.exists())
                file.delete()
            if (file.parentFile?.exists() == false)
                file.parentFile?.mkdirs()
            val fileWriter = FileWriter(file, true)

            for (i in 0 until convertResult.size) {
                if (convertResult[i] == null)
                    continue
                if (convertResult[i]!![0] == context.getString(R.string.match_success) && saveSuccessSongs) {
                    fileWriter.write("${convertResult[i]!![7]}\n")
                }
                if (convertResult[i]!![0] == context.getString(R.string.match_caution) && saveCautionSongs) {
                    fileWriter.write("${convertResult[i]!![7]}\n")
                }
                if (convertResult[i]!![0] == context.getString(R.string.match_manual) && saveManualSongs) {
                    fileWriter.write("${convertResult[i]!![7]}\n")
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
            MyVibrationEffect(
                context,
                (context as MainActivity).enableHaptic.value
            ).done()
            showSaveDialog.value = false
            convertResult.clear()
            playlistEnabled[firstIndex1] = 2
            if (playlistEnabled.count { it == 1 } == 0) {
                currentPage.intValue = 3
                File("${resultFilePath}-journal").delete()
            }
        }
    }

    fun launchSaltPlayer() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName("com.salt.music", "com.salt.music.ui.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.salt_player_not_installed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun copyFolderPathToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = newPlainText(
            "SaltPlayerFolder",
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath}/${
                context.getString(
                    R.string.app_name
                )
            }"
        )
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
    }
}

