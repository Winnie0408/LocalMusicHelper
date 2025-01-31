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
import android.webkit.CookieManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.TextNode
import com.hwinzniej.musichelper.MainActivity
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.data.DataStoreConstants
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
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.net.SocketTimeoutException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class ConvertPage(
    val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val openMusicPlatformSqlFileLauncher: ActivityResultLauncher<Array<String>>,
    private val openResultSqlFileLauncher: ActivityResultLauncher<Array<String>>,
    private val openPlaylistFileLauncher: ActivityResultLauncher<Array<String>>,
    private val openCsvFileLauncher: ActivityResultLauncher<Array<String>>,
    private val openLocalFileLauncher: ActivityResultLauncher<Uri?>,
    private val openLunaJSONDirLauncher: ActivityResultLauncher<Uri?>,
    val db: MusicDatabase,
    componentActivity: ComponentActivity,
    val encryptServer: MutableState<String>,
    val dataStore: DataStore<Preferences>,
) : PermissionResultHandler {
    var databaseFileName = mutableStateOf("")
    var selectedSourceApp = mutableIntStateOf(0)
    var useCustomResultFile = mutableStateOf(false)
    var customResultFileName = mutableStateOf("")
    var sourcePlaylistFileName = mutableStateOf("")
    private var selectedFileName = mutableStateOf("")
    var showLoadingProgressBar = mutableStateOf(false)
    var showErrorDialog = mutableStateOf(false)
    var errorDialogTitle = mutableStateOf("")
    var errorDialogContent = mutableStateOf("")
    var databaseFilePath = mutableStateOf("")
    private var resultFilePath = ""
    private var sourcePlaylistFilePath = ""
    private var sourceApp = SourceApp()
    val loadingProgressSema = Semaphore(2)
    var currentPage = mutableIntStateOf(0)
    var selectedMatchingMode = mutableIntStateOf(2)
    var enableBracketRemoval = mutableStateOf(false)
    var enableArtistNameMatch = mutableStateOf(true)
    var enableAlbumNameMatch = mutableStateOf(true)
    var similarity = mutableFloatStateOf(85f)
    var useRootAccess = mutableStateOf(false)
    private var playlistId = mutableStateListOf<String>()
    var playlistName = mutableStateListOf<String>()
    var playlistEnabled = mutableStateListOf<Int>()
    var playlistSum = mutableStateListOf<Int>()
    var playlistShow = mutableStateListOf<Boolean>()
    var showSelectSourceDialog = mutableStateOf(false)
    var multiSource = mutableStateListOf<Array<String>>()
    var showNumberProgressBar = mutableStateOf(false)
    var selectedMethod = mutableIntStateOf(0)
    var selectedLoginMethod = mutableIntStateOf(2)
    var cookie = mutableStateOf("")
    var showLoginDialog = mutableStateOf(false)
    var neteaseUserId = mutableStateOf("")
    var errorDialogCustomAction = mutableStateOf({})
    var lastLoginTimestamp = mutableLongStateOf(0L)
    var showSongNumMismatchDialog = mutableStateOf(false)
    var showCustomPlaylistDialog = mutableStateOf(false)
    var customPlaylistInput = mutableStateOf("")
    var lunaDeviceId = mutableStateOf("")
    var lunaInstallId = mutableStateOf("")
    var lunaCookie = mutableStateOf("")
    var showLunaVerifyDialog = mutableStateOf(false)
    var lunaVerifyRelated = mutableStateMapOf<String, String>()
    val convertMode = mutableIntStateOf(1)
    val selectedSourceLocalApp = mutableIntStateOf(2)
    val selectedTargetApp = mutableIntStateOf(0)
    val spotifyUserId = mutableStateOf("")
    private var csvFilePath = ""
    var localMusicPath = mutableStateOf("")
    var musicDirName = mutableStateOf("Music")
    var winPath =
        mutableStateOf("C:\\Users\\{YourUserName}\\${musicDirName.value}") //用于Zune相关内容的Windows音乐目录路径
    var isAutoMatched = mutableIntStateOf(0) //用于分辨路径为用户选择/自动匹配
    var itemCount = mutableIntStateOf(0) //用于记录从歌单文件自动匹配到的歌曲数目
    var isCorrectPlaylist = mutableStateOf(false)
    var showAdvancedOptions = mutableStateOf(false)
    var webdavUsername =
        mutableStateOf("A Example Username")
    var webdavPath =
        mutableStateOf("https://www.example.com/dav/${musicDirName.value}") //用于Qinalt相关内容的Webdav音乐目录路径

    /**
     * 请求存储权限
     */

//==================== Android 11+ 使用====================

    @RequiresApi(Build.VERSION_CODES.R)
    private val requestPermissionLauncher = componentActivity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Environment.isExternalStorageManager()) {
            if (convertMode.intValue == 1) {
                checkSelectedFiles(250L)
            }
        } else {
            Toast.makeText(context, R.string.permission_not_granted_toast, Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun requestPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { //Android 11+
            if (Environment.isExternalStorageManager()) {
                if (convertMode.intValue == 1) {
                    checkSelectedFiles()
                }
                return true
            } else {
                Toast.makeText(context, R.string.request_permission_toast, Toast.LENGTH_SHORT)
                    .show()
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                requestPermissionLauncher.launch(intent)
//                startActivity(context, intent, null)
                return false
            }
        } else { //Android 10-
            if (allPermissionsGranted()) {
                if (convertMode.intValue == 1) {
                    checkSelectedFiles()
                }
                return true
            } else {
                Toast.makeText(context, R.string.request_permission_toast, Toast.LENGTH_SHORT)
                    .show()
                ActivityCompat.requestPermissions(
                    context as Activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
                return false
            }
        }
    }

//==================== Android 11+ 使用====================

//==================== Android 10- 使用====================

    private val REQUEST_CODE_PERMISSIONS = 1002
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private fun allPermissionsGranted(): Boolean {
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
                if (convertMode.intValue == 1) {
                    checkSelectedFiles(250L)
                }
            } else {
                Toast.makeText(context, R.string.permission_not_granted_toast, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

//==================== Android 10- 使用====================

    fun selectDatabaseFile() {
        try {
            openMusicPlatformSqlFileLauncher.launch(arrayOf("*/*"))
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.unable_start_documentsui),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun selectResultFile() {
        try {
            openResultSqlFileLauncher.launch(arrayOf("*/*"))
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.unable_start_documentsui),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun selectPlaylistFile() {
        try {
            openPlaylistFileLauncher.launch(
                arrayOf(
                    when (selectedSourceLocalApp.intValue) {
                        0 -> "text/plain"
                        1 -> "audio/x-mpegurl"
                        2 -> "audio/x-mpegurl"
                        3 -> "*/*"
                        else -> "*/*"
                    }
                )
            )
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.unable_start_documentsui),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun selectJsonFileDir() {
        try {
            openLunaJSONDirLauncher.launch(null)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.unable_start_documentsui),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun selectCsvFile() {
        try {
            openCsvFileLauncher.launch(arrayOf("text/*"))
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.unable_start_documentsui),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun selectLocalDir() {
        try {
            openLocalFileLauncher.launch(null)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.unable_start_documentsui),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun handleLunaDirUri(uri: Uri?) {
        if (uri == null) {
            return
        }
        try {
            lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val temp = Tools().copyFilesToExternalFilesDir(uri, context, "lunaJsonDir", true)
                delay(200L) //播放动画
                databaseFileName.value = temp
            }
        } catch (e: Exception) {
            Toast.makeText(context, R.string.failed_to_get_file_from_dir, Toast.LENGTH_SHORT).show()
            return
        }
    }

    fun handleLocalFileDirUri(uri: Uri?) {
        val chooseLocalMusicPath = mutableStateOf("")
        if (uri == null) {
            return
        }
        try {
            chooseLocalMusicPath.value = Tools().uriToAbsolutePath(uri)
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                delay(200L) //播放动画
            }
        } catch (e: Exception) {
            Toast.makeText(context, R.string.failed_to_get_file_from_dir, Toast.LENGTH_SHORT).show()
            return
        }
        isAutoMatched.intValue = 0
        localMusicPath.value = chooseLocalMusicPath.value
    }

    fun handleUri(uri: Uri?, code: Int) {
        if (uri == null) {
            return
        }
        selectedFileName.value = Tools().getFileNameFromUri(context, uri)
        if (selectedFileName.value.isBlank()) {
            Toast.makeText(context, R.string.cannot_get_file_name, Toast.LENGTH_SHORT).show()
            return
        }
        when (code) {
            0 -> {
                databaseFilePath.value =
                    Tools().fromUriCopyFileToExternalFilesDir(context, uri, selectedFileName.value)
                lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                    delay(200L) //播放动画
                    databaseFileName.value = selectedFileName.value
                }
            }

            1 -> {
                resultFilePath =
                    Tools().fromUriCopyFileToExternalFilesDir(context, uri, selectedFileName.value)
                lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                    delay(200L)  //播放动画
                    customResultFileName.value = selectedFileName.value
                }
            }

            2 -> {
                isCorrectPlaylist.value = false
                sourcePlaylistFilePath =
                    Tools().fromUriCopyFileToExternalFilesDir(context, uri, selectedFileName.value)
                lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                    delay(200L)  //播放动画
                    sourcePlaylistFileName.value = selectedFileName.value
                    when (selectedSourceLocalApp.intValue) {
                        3 -> {
                            val sourceFile = File(sourcePlaylistFilePath)
                            val zunePlaylist = sourceFile.readText()
                            isCorrectPlaylist.value =
                                zunePlaylist.startsWith("""<?zpl version="2.0"?>""")
                                        && sourcePlaylistFilePath.endsWith(""".zpl""")
                            if (!isCorrectPlaylist.value) {
                                return@launch
                            }
                            val doc: Document = Ksoup.parse(zunePlaylist)
                            val playlistItems = doc.select("smil seq media")
                            playlistItems.forEach {
                                winPath.value = it.select("media").attr("src").replace(
                                    "${musicDirName.value}.*".toRegex(),
                                    musicDirName.value
                                )
                            }
                            isAutoMatched.intValue = 2
                            itemCount.intValue = playlistItems.size
                            isCorrectPlaylist.value = true
                        }

                        else -> when (selectedTargetApp.intValue) {
                            3, 4 -> {
                                val sourceFile = File(sourcePlaylistFilePath)
                                val readPlayList = sourceFile.readLines()
                                isAutoMatched.intValue = 1
                                localMusicPath.value = readPlayList[0].replace(
                                    "${musicDirName.value}.+".toRegex(),
                                    musicDirName.value
                                )
                                itemCount.intValue = readPlayList.size
                                isCorrectPlaylist.value = true
                            }

                            else -> {
                                itemCount.intValue = File(sourcePlaylistFilePath).readLines().size
                                isCorrectPlaylist.value = true
                            }
                        }
                    }
                }
            }

            3 -> {
                csvFilePath =
                    Tools().fromUriCopyFileToExternalFilesDir(context, uri, selectedFileName.value)
                lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                    delay(200L)  //播放动画
                    databaseFileName.value = selectedFileName.value
                }
            }
        }
    }

    var haveError = false
    private fun checkSelectedFiles(delay: Long = 0L) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            haveError = false
            showLoadingProgressBar.value = true
            playlistEnabled.clear()
            playlistId.clear()
            playlistName.clear()
            playlistSum.clear()
            playlistShow.clear()
            errorDialogTitle.value = ""
            errorDialogContent.value = ""
            loadingProgressSema.acquire()
            loadingProgressSema.acquire()
            checkDatabaseFile()
            checkResultFile()
            delay(delay)
            loadingProgressSema.acquire()
            loadingProgressSema.acquire()
            if (selectedMethod.intValue == 0) {
//                MyVibrationEffect(
//                    context,
//                    (context as MainActivity).enableHaptic.value,
//                    context.hapticStrength.intValue
//                ).done()
                if (haveError) {
                    showLoadingProgressBar.value = false
                } else {
                    showLoadingProgressBar.value = true
                    delay(500L)
                    databaseSummary()
                }
            } else {
                showLoadingProgressBar.value = false
                if (!haveError)
                    showLoginDialog.value = true
            }
            loadingProgressSema.release()
            loadingProgressSema.release()
        }
    }

    private fun checkResultFile() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            if (useCustomResultFile.value) {
                if (resultFilePath.isEmpty()) {
                    errorDialogTitle.value =
                        context.getString(R.string.error_while_getting_data_dialog_title)
                    errorDialogContent.value +=
                        "- ${context.getString(R.string.result_file)} ${
                            context.getString(
                                R.string.read_failed
                            )
                        }:\n  - ${
                            context.getString(
                                R.string.please_select_result_file
                            )
                        }\n"
                    errorDialogCustomAction.value = {}
                    showErrorDialog.value = true
                    currentPage.intValue = 0
                    loadingProgressSema.release()
                    haveError = true
                    return@launch
                }
                var db: SQLiteDatabase? = null
                try {
                    db = SQLiteDatabase.openDatabase(
                        resultFilePath,
                        null,
                        SQLiteDatabase.OPEN_READONLY
                    )
                    db.rawQuery(
                        "SELECT id, song, artist, album, absolutePath FROM music LIMIT 1",
                        null
                    ).close()
                } catch (e: Exception) {
                    errorDialogTitle.value =
                        context.getString(R.string.error_while_getting_data_dialog_title)
                    errorDialogContent.value +=
                        "- ${context.getString(R.string.result_file)} ${
                            context.getString(
                                R.string.read_failed
                            )
                        }:\n  - ${e}\n"
                    errorDialogCustomAction.value = {}
                    showErrorDialog.value = true
                    haveError = true
                    currentPage.intValue = 0
                } finally {
                    db?.close()
                    loadingProgressSema.release()
                    File("${resultFilePath}-journal").delete()
                }
            } else {
                try {
                    val musicCount = db.musicDao().getMusicCount()
                    if (musicCount == 0) {
                        errorDialogTitle.value =
                            context.getString(R.string.error_while_getting_data_dialog_title)
                        errorDialogContent.value +=
                            "- ${context.getString(R.string.result_file)} ${
                                context.getString(
                                    R.string.read_failed
                                )
                            }:\n  - ${context.getString(R.string.use_scan_fun_first)}\n"
                        errorDialogCustomAction.value = {}
                        showErrorDialog.value = true
                        haveError = true
                        currentPage.intValue = 0
                    }
                } catch (e: Exception) {
                    errorDialogTitle.value =
                        context.getString(R.string.error_while_getting_data_dialog_title)
                    errorDialogContent.value +=
                        "- ${context.getString(R.string.result_file)} ${
                            context.getString(
                                R.string.read_failed
                            )
                        }:\n  - ${context.getString(R.string.use_scan_fun_first)}\n"
                    errorDialogCustomAction.value = {}
                    showErrorDialog.value = true
                    haveError = true
                    currentPage.intValue = 0
                } finally {
                    loadingProgressSema.release()
                }
            }
        }
    }

    fun clearCache() {
        isAutoMatched.intValue = 0
        itemCount.intValue = 0
        sourcePlaylistFileName.value = ""
        resultFilePath = ""
    }

    private fun getSongTag(absolutePath: String): Map<String, String?> {
        val audioFile: AudioFile = AudioFileIO.read(File(absolutePath))
        return mapOf(
            "song" to audioFile.tag.getFirst(FieldKey.TITLE),
            "artist" to audioFile.tag.getFirst(FieldKey.ARTIST),
            "album" to audioFile.tag.getFirst(FieldKey.ALBUM),
            "albumArtist" to audioFile.tag.getFirst(FieldKey.ALBUM_ARTIST),
            "genre" to audioFile.tag.getFirst(FieldKey.GENRE),
            "trackNumber" to audioFile.tag.getFirst(FieldKey.TRACK),
            "discNumber" to audioFile.tag.getFirst(FieldKey.DISC_NO),
            "releaseYear" to audioFile.tag.getFirst(FieldKey.YEAR),
            "composer" to audioFile.tag.getFirst(FieldKey.COMPOSER),
            "arranger" to audioFile.tag.getFirst(FieldKey.ARRANGER),
            "lyricist" to audioFile.tag.getFirst(FieldKey.LYRICIST),
            "lyrics" to audioFile.tag.getFirst(FieldKey.LYRICS),
        )
    }

    private fun localToZune(targetFile: File, sourceFile: File): Int {
        var fileName = ""
        val guid = UUID.randomUUID().toString()
        val creatorId = UUID.randomUUID().toString()
        val songCount = itemCount.intValue
        val dot = selectedFileName.value.lastIndexOf('.')
        val fileWriter = FileWriter(targetFile, true)
        if ((dot > -1) && (dot < (selectedFileName.value.length))) {
            fileName = selectedFileName.value.substring(0, dot)
        }
        var fileData = """<?zpl version="2.0"?>
        <smil>
          <head>
            <guid>{""" + guid + """}</guid>
            <meta name="generator" content="Zune -- 4.8.2345.0" />
            <meta name="itemCount" content="""" + songCount + """" />
            <meta name="totalDuration" content="19854" />
            <meta name="averageRating" content="0" />
            <meta name="creatorId" content="{""" + creatorId + """}" />
            <meta name="autoRefresh" content="FALSE" />
            <meta name="autoRefreshInterval" content="5" />
            <title>""" + fileName + """</title>
          </head>
          <body>
            <seq>
        """.trimIndent()
        var errorCount = 0
        val errorSongPath = mutableListOf("")
        val inputFile = sourceFile.readLines()
        inputFile.forEach {
            try {
                fileData = "$fileData      <media src=\"" +
                        TextNode(
                            it.replace(localMusicPath.value, winPath.value).replace("/", "\\")
                                .replace(""""""", "&quot;") +
                                    """" albumTitle="""" + getSongTag(it)["album"]!!.replace(
                                """"""",
                                "&quot;"
                            ) +
                                    """" albumArtist="""" + getSongTag(it)["albumArtist"]!!.replace(
                                """"""",
                                "&quot;"
                            ) +
                                    """" trackTitle="""" + getSongTag(it)["song"]!!.replace(
                                """"""",
                                "&quot;"
                            ) +
                                    """" trackArtist="""" + getSongTag(it)["artist"]!!.replace(
                                """"""",
                                "&quot;"
                            )
                        ).toString() +
                        """" duration="""" + "114514" + """" />""" + "\r\n"
            } catch (e: Exception) {
                errorCount += 1
                errorSongPath.add(it)
            }
        }
        fileData += """    </seq>
          </body>
        </smil>""".trimIndent()
        fileWriter.write(fileData)
        fileWriter.close()
        when (errorCount) {
            0 -> TODO()
            else -> {
                var errorSongPathShow = ""
                errorSongPath.forEach {
                    errorSongPathShow += "  - ${it.replace("\t\n", "")}\n"
                }
                errorDialogContent.value =
                    "- ${context.getString(R.string.playlist_song_num_not_match)}\n  - ${
                        context.getString(
                            R.string.playlist_song_num_not_match_detail
                        ).replace("#1", songCount.toString())
                            .replace("#2", (songCount - errorCount).toString()).replace("#n", "\n")
                            .replace("#b", " ")
                    }\n- ${context.getString(R.string.please_check_song_file)} ${errorSongPathShow}"
                showDialogProgressBar.value = false
                errorDialogTitle.value = context.getString(R.string.read_failed)
                errorDialogCustomAction.value = {}
                showErrorDialog.value = true
            }
        }
        return errorCount
    }

    private fun onlineToZune(
        convertResult: SnapshotStateMap<Int, Array<String>>,
        correctFileName: String,
        saveSuccessSongs: Boolean,
        saveCautionSongs: Boolean,
        saveManualSongs: Boolean
    ): String {
        var fileName = ""
        val guid = UUID.randomUUID().toString()
        val creatorId = UUID.randomUUID().toString()
        val songCount = convertResult.size

        val dot = correctFileName.lastIndexOf('.')
        if ((dot > -1) && (dot < (correctFileName.length))) {
            fileName = correctFileName.substring(0, dot)
        }

        var fileData = """<?zpl version="2.0"?>
        <smil>
          <head>
            <guid>{""" + guid + """}</guid>
            <meta name="generator" content="Zune -- 4.8.2345.0" />
            <meta name="itemCount" content="""" + songCount + """" />
            <meta name="totalDuration" content="19854" />
            <meta name="averageRating" content="0" />
            <meta name="creatorId" content="{""" + creatorId + """}" />
            <meta name="autoRefresh" content="FALSE" />
            <meta name="autoRefreshInterval" content="5" />
            <title>""" + fileName + """</title>
          </head>
          <body>
            <seq>
        """.trimIndent()
        for (i in 0 until convertResult.size) {
            if (convertResult[i] == null)
                continue
            if (convertResult[i]!![0] == "0" && saveSuccessSongs) {
                fileData = "$fileData      <media src=\"" +
                        TextNode(
                            convertResult[i]!![7].replace(localMusicPath.value, winPath.value)
                                .replace("/", "\\") +
                                    """" albumTitle="""" + convertResult[i]!![5].replace(
                                """"""",
                                "&quot;"
                            ) +
                                    """" albumArtist="""" + convertResult[i]!![8].replace(
                                """"""",
                                "&quot;"
                            ) +
                                    """" trackTitle="""" + convertResult[i]!![1].replace(
                                """"""",
                                "&quot;"
                            ) +
                                    """" trackArtist="""" + convertResult[i]!![3].replace(
                                """"""",
                                "&quot;"
                            )
                        ).toString() +
                        """" duration="""" + "114514" + """" />""" + "\r\n"
                continue
            }
            if (convertResult[i]!![0] == "1" && saveCautionSongs) {
                fileData = "$fileData      <media src=\"" +
                        TextNode(
                            convertResult[i]!![7].replace(localMusicPath.value, winPath.value)
                                .replace("/", "\\") +
                                    """" albumTitle="""" + convertResult[i]!![5].replace(
                                """"""",
                                "&quot;"
                            ) +
                                    """" albumArtist="""" + convertResult[i]!![8].replace(
                                """"""",
                                "&quot;"
                            ) +
                                    """" trackTitle="""" + convertResult[i]!![1].replace(
                                """"""",
                                "&quot;"
                            ) +
                                    """" trackArtist="""" + convertResult[i]!![3].replace(
                                """"""",
                                "&quot;"
                            )
                        ).toString() +
                        """" duration="""" + "114514" + """" />""" + "\r\n"
                continue
            }
            if (convertResult[i]!![0] == "2" && saveManualSongs) {
                fileData = "$fileData      <media src=\"" +
                        TextNode(
                            convertResult[i]!![7].replace(localMusicPath.value, winPath.value)
                                .replace("/", "\\") +
                                    """" albumTitle="""" + convertResult[i]!![5].replace(
                                """"""",
                                "&quot;"
                            ) +
                                    """" albumArtist="""" + convertResult[i]!![8].replace(
                                """"""",
                                "&quot;"
                            ) +
                                    """" trackTitle="""" + convertResult[i]!![1].replace(
                                """"""",
                                "&quot;"
                            ) +
                                    """" trackArtist="""" + convertResult[i]!![3].replace(
                                """"""",
                                "&quot;"
                            )
                        ).toString() +
                        """" duration="""" + "114514" + """" />""" + "\r\n"
                continue
            }
        }
        fileData += """    </seq>
  </body>
</smil>"""
        return fileData
    }

    fun getSelectedMultiSource(selected: Int) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val dirPath = context.getExternalFilesDir(null)?.absolutePath + "/userDatabase"
            val dir = File(dirPath)
            if (!dir.exists())
                dir.mkdirs()
            val copyResult = Tools().execShellCmd(
                "cp -f '/data/data/${multiSource[selected][0]}/databases/${
                    sourceApp.databaseName
                }' '${dir.absolutePath}/${sourceApp.sourceEng}_temp.db'"
            )
            if (copyResult == "") {
                databaseFilePath.value = "${dir.absolutePath}/${sourceApp.sourceEng}_temp.db"
                showDialogProgressBar.value = false
                showSelectSourceDialog.value = false
                loadingProgressSema.release()
            } else {
                showDialogProgressBar.value = false
                showSelectSourceDialog.value = false
                errorDialogTitle.value =
                    context.getString(R.string.error_while_getting_data_dialog_title)
                errorDialogContent.value +=
                    "- ${context.getString(R.string.database_file)} ${
                        context.getString(
                            R.string.read_failed
                        )
                    }:\n  - $copyResult\n"
                errorDialogCustomAction.value = {}
                showErrorDialog.value = true
                haveError = true
                currentPage.intValue = 0
                loadingProgressSema.release()
                return@launch
            }
        }
    }

    private fun checkAppStatusWithRoot() {
        val appExists =
            Tools().execShellCmd("pm list packages | grep -E '${sourceApp.pakageName}'")
                .replace("\n", "")
        if (appExists.isNotEmpty()) {
            if (appExists.indexOf("package:") == appExists.lastIndexOf("package:")) {
                val dirPath = context.getExternalFilesDir(null)?.absolutePath + "/userDatabase"
                val dir = File(dirPath)
                if (!dir.exists())
                    dir.mkdirs()

                val copyResult = if (selectedSourceApp.intValue == 5) {
                    val tempDir =
                        File(context.getExternalFilesDir(null)?.absolutePath + "/lunaJsonDir")
                    if (!tempDir.exists()) {
                        tempDir.mkdirs()
                    }
                    val tempPath = context.getExternalFilesDir(null)?.absolutePath
                    Tools().execShellCmd(
                        "find '/data/data/${appExists.split(":")[1]}/cache/NetCacheLoader' -type f -exec cp {} '${
                            tempPath
                        }/lunaJsonDir' \\; && chmod -R +r '${tempPath}/lunaJsonDir'"
                    )
                } else {
                    Tools().execShellCmd(
                        "cp -f '/data/data/${appExists.split(":")[1]}/databases/${
                            sourceApp.databaseName
                        }' '${dir.absolutePath}/${sourceApp.sourceEng}_temp.db' && chmod +r '${dir.absolutePath}/${sourceApp.sourceEng}_temp.db'"
                    )
                }

                if (copyResult == "") {
                    databaseFilePath.value = "${dir.absolutePath}/${sourceApp.sourceEng}_temp.db"
                    if (selectedSourceApp.intValue == 5) {
                        lunaJsonToDatabase()
                    }
                    loadingProgressSema.release()
                } else {
                    errorDialogTitle.value =
                        context.getString(R.string.error_while_getting_data_dialog_title)
                    errorDialogContent.value +=
                        "- ${context.getString(R.string.database_file)} ${
                            context.getString(
                                R.string.read_failed
                            )
                        }:\n  - $copyResult\n"
                    errorDialogCustomAction.value = {}
                    showErrorDialog.value = true
                    haveError = true
                    loadingProgressSema.release()
                    currentPage.intValue = 0
                    return
                }
            } else {
                multiSource.clear()
                val pm = context.packageManager
                val sourceAppsPackageName = appExists.split("package:").filter { it.isNotEmpty() }
                sourceAppsPackageName.forEach {
                    multiSource.add(
                        arrayOf(
                            it,
                            pm.getPackageInfo(
                                it,
                                PackageManager.GET_META_DATA
                            ).applicationInfo?.loadLabel(pm).toString()
                        )
                    )
                }
                showSelectSourceDialog.value = true
            }
        } else {
            errorDialogTitle.value =
                context.getString(R.string.error_while_getting_data_dialog_title)
            errorDialogContent.value +=
                "- ${context.getString(R.string.database_file)} ${
                    context.getString(
                        R.string.read_failed
                    )
                }:\n  - ${
                    context.getString(R.string.app_not_installed).replace(
                        "#", when (selectedSourceApp.intValue) {
                            1 -> context.getString(R.string.source_netease_cloud_music)
                            2 -> context.getString(R.string.source_qq_music)
                            3 -> context.getString(R.string.source_kugou_music)
                            4 -> context.getString(R.string.source_kuwo_music)
                            5 -> context.getString(R.string.source_luna_music)
                            6 -> context.getString(R.string.source_spotify)
                            7 -> context.getString(R.string.tune_my_music)
                            else -> ""
                        }
                    )
                }\n"
            errorDialogCustomAction.value = {}
            showErrorDialog.value = true
            haveError = true
            currentPage.intValue = 0
            loadingProgressSema.release()
        }
    }

    private fun checkDatabaseFile() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            when (selectedSourceApp.intValue) {
                0 -> {
                    errorDialogTitle.value =
                        context.getString(R.string.error_while_getting_data_dialog_title)
                    errorDialogContent.value +=
                        "- ${context.getString(R.string.database_file)} ${
                            context.getString(
                                R.string.read_failed
                            )
                        }:\n  - ${
                            context.getString(
                                R.string.please_select_source_app
                            )
                        }\n"
                    errorDialogCustomAction.value = {}
                    showErrorDialog.value = true
                    loadingProgressSema.release()
                    haveError = true
                    currentPage.intValue = 0
                    return@launch
                }

                1 -> sourceApp.init("CloudMusic")
                2 -> sourceApp.init("QQMusic")
                3 -> sourceApp.init("KugouMusic")
                4 -> sourceApp.init("KuwoMusic")
                5 -> sourceApp.init("LunaMusic")
                6 -> sourceApp.init("Spotify")
                7 -> sourceApp.init("TuneMyMusic")
            }
            if (selectedMethod.intValue == 0) {
                if (sourceApp.sourceEng != "") {
                    currentPage.intValue = 1
                    if (useRootAccess.value && selectedSourceApp.intValue != 7)
                        checkAppStatusWithRoot()
                    else {
                        var db: SQLiteDatabase? = null
                        try {
                            try {
                                if (selectedSourceApp.intValue == 5) {
                                    if (!lunaJsonToDatabase())
                                        throw Exception()
                                }
                                if (selectedSourceApp.intValue == 7) {
                                    if (!tuneMyMusicCsvToDatabase())
                                        throw Exception()
                                }
                                if (databaseFilePath.value.isBlank() || databaseFileName.value.isBlank()) {
                                    throw Exception()
                                }
                            } catch (e: Exception) {
                                errorDialogTitle.value =
                                    context.getString(R.string.error_while_getting_data_dialog_title)
                                errorDialogContent.value +=
                                    "- ${
                                        when (selectedSourceApp.intValue) {
                                            5 -> context.getString(R.string.json_file_dir)
                                            7 -> context.getString(R.string.csv_file)
                                            else -> context.getString(R.string.database_file)
                                        }
                                    } ${
                                        context.getString(
                                            R.string.read_failed
                                        )
                                    }:\n  - ${
                                        when (selectedSourceApp.intValue) {
                                            5 -> {
                                                context.getString(
                                                    R.string.not_select_json_dir_or_select_wrong
                                                )
                                            }

                                            7 -> {
                                                context.getString(
                                                    R.string.not_select_csv_or_select_wrong
                                                )
                                            }

                                            else -> {
                                                context.getString(
                                                    R.string.please_select_database_file
                                                )
                                            }
                                        }
                                    }\n"
                                errorDialogCustomAction.value = {}
                                showErrorDialog.value = true
                                currentPage.intValue = 0
                                haveError = true
                                return@launch
                            }
                            db = SQLiteDatabase.openDatabase(
                                databaseFilePath.value,
                                null,
                                SQLiteDatabase.OPEN_READONLY
                            )
                            db.rawQuery(
                                "SELECT ${sourceApp.songListId}, ${sourceApp.songListName} FROM ${sourceApp.songListTableName} LIMIT 1",
                                null
                            ).close()
                            val playlistNum = db.rawQuery(
                                "SELECT COUNT(*) FROM ${sourceApp.songListTableName}",
                                null
                            )
                            playlistNum.moveToFirst()
                            if (playlistNum.getInt(0) == 0) {
                                throw Exception(
                                    context.getString(R.string.no_info_in_playlist)
                                        .replace(
                                            "#", when (selectedSourceApp.intValue) {
                                                1 -> context.getString(R.string.source_netease_cloud_music)
                                                2 -> context.getString(R.string.source_qq_music)
                                                3 -> context.getString(R.string.source_kugou_music)
                                                4 -> context.getString(R.string.source_kuwo_music)
                                                5 -> context.getString(R.string.source_luna_music)
                                                6 -> context.getString(R.string.source_spotify)
                                                7 -> context.getString(R.string.tune_my_music)
                                                else -> ""
                                            }
                                        )
                                )
                            }
                            playlistNum.close()
                        } catch (e: Exception) {
                            errorDialogTitle.value =
                                context.getString(R.string.error_while_getting_data_dialog_title)
                            errorDialogContent.value +=
                                "- ${context.getString(R.string.database_file)} ${
                                    context.getString(
                                        R.string.read_failed
                                    )
                                }:\n  - ${e}\n"
                            errorDialogCustomAction.value = {}
                            showErrorDialog.value = true
                            haveError = true
                            currentPage.intValue = 0
                        } finally {
                            db?.close()
                            loadingProgressSema.release()
                            File("${databaseFilePath.value}-journal").delete()
                        }
                    }
                } else {
                    errorDialogTitle.value =
                        context.getString(R.string.error_while_getting_data_dialog_title)
                    errorDialogContent.value +=
                        "- ${context.getString(R.string.database_file)} ${
                            context.getString(
                                R.string.read_failed
                            )
                        }:\n  - ${
                            context.getString(
                                R.string.please_select_source_app
                            )
                        }\n"
                    errorDialogCustomAction.value = {}
                    showErrorDialog.value = true
                    haveError = true
                    currentPage.intValue = 0
                    loadingProgressSema.release()
                }
            } else {
//                if (selectedSourceApp.intValue == 4) {
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(
//                            context,
//                            context.getString(R.string.developing),
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                    haveError = true
//                    loadingProgressSema.release()
//                    return@launch
//                }
                selectedLoginMethod.intValue = 2
                loadingProgressSema.release()
            }
        }
    }

    fun getOnlinePlaylist(
        kugouUserRelated: SnapshotStateMap<String, String> = mutableStateMapOf(),
        kugouCurrentIp: String = ""
    ) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            currentPage.intValue = 1
            if (selectedSourceApp.intValue == 4) {
                delay(300L)
                showLoadingProgressBar.value = false
                errorDialogTitle.value =
                    context.getString(R.string.tips)
                errorDialogContent.value =
                    "- ${context.getString(R.string.kuwo_cannot_read_online_playlist)}"
                errorDialogCustomAction.value = {}
                showErrorDialog.value = true
                return@launch
            }
            if (playlistId.size != 0) {
                playlistId.clear()
                playlistName.clear()
                playlistEnabled.clear()
                playlistSum.clear()
                playlistShow.clear()
            }
            showLoadingProgressBar.value = true

            try {
                val url = when (selectedSourceApp.intValue) {
                    1 -> "https://music.163.com/weapi/user/playlist"
                    2 -> "https://c.y.qq.com/rsc/fcgi-bin/fcg_user_created_diss"
                    3 -> "https://thirdsso.kugou.com/v2/favorite/selfv2/list"
                    4 -> ""
                    5 -> "https://api.qishui.com/luna/pc/me/playlist"
                    6 -> "https://api.spotify.com/v1/users"
                    else -> ""
                }
                val client = OkHttpClient()
                var request = Request.Builder()
                when (selectedSourceApp.intValue) {
                    1 -> {
                        if (cookie.value.contains("\\buid=\\d+".toRegex())) {
                            neteaseUserId.value =
                                "\\buid=\\d+".toRegex().find(cookie.value)?.value?.substring(4)!!
                            cookie.value = "uid=\\d+;?\\s?".toRegex().replace(cookie.value, "")
                        }
                        dataStore.edit { settings ->
                            settings[DataStoreConstants.NETEASE_USER_ID] = neteaseUserId.value
                        }
                        val encrypted = Tools().encryptString(
                            """{"limit":100,"offset":0,"uid":${neteaseUserId.value}}""",
                            "netease",
                            encryptServer.value
                        )
                        val formBody = encrypted?.let {
                            FormBody.Builder()
                                .add("params", it.getString("encText"))
                                .add("encSecKey", it.getString("encSecKey"))
                                .build()
                        }
                        formBody?.let {
                            request = request.url(
                                "${url}?csrf_token=${
                                    "__csrf=\\w+".toRegex().find(cookie.value)?.value?.substring(7)
                                }"
                            ).addHeader("Cookie", cookie.value)
                                .post(it)
                        }
                    }

                    2 -> {
                        var uin = "(uin=\\d+)|(wxuin=\\d+)".toRegex().find(cookie.value)?.value
                        uin = uin?.substring(uin.indexOf("=") + 1)
                        request =
                            request.url("${url}?uin=${uin}&hostuin=${uin}&ct=24&format=json&inCharset=utf-8&outCharset=utf-8&sin=0&size=100")
                                .addHeader("Cookie", cookie.value)
                                .addHeader("Referer", "https://y.qq.com/")
                                .get()
                    }

                    3 -> {
                        val nonce =
                            Tools().generateRandomString(
                                length = 32,
                                includeLowerCase = true,
                                includeDigits = true
                            )
                        val time = System.currentTimeMillis() / 1000
                        val deviceId = Tools().md5(
                            input = Settings.System.getString(
                                context.contentResolver, Settings.Secure.ANDROID_ID
                            )
                        )
                        val json =
                            """{"size":30,"device_id":"${deviceId}","client_ver":"133-62b8ece-20240223165034","pid":"203051","client_ip":"${kugouCurrentIp}","page":1,"apk_ver":"9845","nonce":"${nonce}","sp":"KG","userid":"${kugouUserRelated["userId"]}","timestamp":${time},"token":"${kugouUserRelated["token"]}"}"""
                        val requestBody =
                            json.toRequestBody("application/json; charset=utf-8".toMediaType())
                        request = request
                            .url(url)
                            .addHeader(
                                "User-Agent",
                                "Android13-androidCar-133-62b8ece-203051-0-UltimateSdk-wifi"
                            )
                            .addHeader("Content-Type", "application/json; charset=UTF-8")
                            .addHeader("Host", "thirdsso.kugou.com")
                            .addHeader("Connection", "Keep-Alive")
                            .addHeader("Accept-Encoding", "gzip")
                            .addHeader(
                                "signature",
                                Tools().md5(input = json + "9046ad4ecae74a70aa750c1bb2307ae6")
                            )
                            .post(requestBody)
                    }

                    4 -> {}

                    5 -> {
                        if (lunaDeviceId.value.isBlank() && lunaInstallId.value.isBlank()) {
                            val temp =
                                Tools().generateRandomString(length = 13, includeDigits = true)
                            dataStore.edit {
                                it[DataStoreConstants.LUNA_DEVICE_ID] =
                                    temp + Tools().generateRandomString(
                                        length = 3,
                                        includeDigits = true
                                    )
                                it[DataStoreConstants.LUNA_INSTALL_ID] =
                                    temp + Tools().generateRandomString(
                                        length = 3,
                                        includeDigits = true
                                    )
                            }
                        }
                        request = request
                            .url("${url}?aid=386088&app_name=luna_pc&region=cn&geo_region=cn&os_region=cn&device_id=${lunaDeviceId.value}&iid=${lunaInstallId.value}&version_name=1.6.3&version_code=10060300&channel=master&build_mode=master&ac=wifi&tz_name=Asia%2FShanghai&device_platform=windows&device_type=Windows&os_version=Windows+10+Pro&fp=${lunaDeviceId.value}&count=50")
                            .addHeader("Cookie",
                                cookie.value.ifBlank { lunaCookie.value }
                            )
                            .addHeader("Host", "api.qishui.com")
                            .addHeader("User-Agent", "LunaPC/1.6.3(11741945)")
                            .addHeader("Accept", "*/*")
                            .addHeader("Connection", "keep-alive")
                            .get()
                    }

                    6 -> {
                        var spotifyToken = ""
                        if (selectedSourceApp.intValue == 6) {
                            if (encryptServer.value == "cf") {
                                throw Exception(context.getString(R.string.spotify_token_cf))
                            }
                            try {
                                spotifyToken = Tools().encryptString(
                                    "0248",
                                    "spotify",
                                    encryptServer.value
                                )?.getString("token").toString()
                            } catch (e: Exception) {
                                showLoadingProgressBar.value = false
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.spotify_token_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                return@launch
                            }
                        }
                        request = request
                            .url("${url}/${spotifyUserId.value}/playlists?offset=0&limit=100")
                            .addHeader("Authorization", "Bearer ${spotifyToken}")
                            .addHeader("Accept", "application/json")
                            .addHeader("Connection", "keep-alive")
                            .get()
                    }
                }

                var response: JSONObject
                client.newCall(request.build()).execute().use { responses ->
                    response = JSON.parseObject(responses.body?.string())
                }

                val innerPlaylistId = MutableList(0) { "" }
                val innerPlaylistName = MutableList(0) { "" }
                val innerPlaylistEnabled = MutableList(0) { 0 }
                val innerPlaylistSum = MutableList(0) { 0 }
                val innerPlaylistShow = MutableList(0) { true }
                var emptyPlaylistNum = 0

                when (selectedSourceApp.intValue) {
                    1 -> {
                        Tools().copyAssetFileToExternalFilesDir(
                            context,
                            "cloudmusic.db"
                        )
                        val databaseFile =
                            File(context.getExternalFilesDir(null), "cloudmusic.db")
                        val db = SQLiteDatabase.openDatabase(
                            databaseFile.absolutePath,
                            null,
                            SQLiteDatabase.OPEN_READWRITE
                        )
                        databaseFilePath.value = databaseFile.absolutePath

                        val playlistInfo = response.getJSONArray("playlist")
                        playlistInfo.forEach {
                            val playlist = it as JSONObject
                            if (playlist.getInteger("trackCount") == 0) {
                                emptyPlaylistNum++
                                return@forEach
                            }
                            innerPlaylistId.add(playlist.getString("id"))
                            innerPlaylistName.add(playlist.getString("name"))
                            innerPlaylistSum.add(playlist.getInteger("trackCount"))
                            innerPlaylistEnabled.add(0)
                            innerPlaylistShow.add(true)
                            db.execSQL(
                                "INSERT INTO ${sourceApp.songListTableName} (${sourceApp.songListId}, ${sourceApp.songListName}, ${sourceApp.musicNum}) VALUES (?, ?, ?)",
                                arrayOf(
                                    playlist.getString("id"),
                                    playlist.getString("name"),
                                    playlist.getInteger("trackCount")
                                )
                            )
                        }
                        db.close()
                        if (response.getString("version") == "0") {
                            errorDialogTitle.value = context.getString(R.string.tips)
                            errorDialogContent.value =
                                "- ${context.getString(R.string.login_info_maybe_expired)}\n"
                            errorDialogCustomAction.value = {}
                            showErrorDialog.value = true
                        }
                    }

                    2 -> {
                        Tools().copyAssetFileToExternalFilesDir(
                            context,
                            "QQMusic"
                        )
                        val databaseFile =
                            File(context.getExternalFilesDir(null), "QQMusic")
                        val db = SQLiteDatabase.openDatabase(
                            databaseFile.absolutePath,
                            null,
                            SQLiteDatabase.OPEN_READWRITE
                        )
                        databaseFilePath.value = databaseFile.absolutePath

                        var loginInfoExpired = true
                        val playlistInfo =
                            response.getJSONObject("data").getJSONArray("disslist")
                        playlistInfo.forEach {
                            val playlist = it as JSONObject
                            if (playlist.getString("diss_cover") == "?n=1") {
                                loginInfoExpired = false
                            }
                            if (playlist.getInteger("song_cnt") == 0) {
                                emptyPlaylistNum++
                                return@forEach
                            }
                            innerPlaylistId.add(playlist.getString("tid"))
                            innerPlaylistName.add(playlist.getString("diss_name"))
                            innerPlaylistSum.add(playlist.getInteger("song_cnt"))
                            innerPlaylistEnabled.add(0)
                            innerPlaylistShow.add(true)
                            db.execSQL(
                                "INSERT INTO ${sourceApp.songListTableName} (${sourceApp.songListId}, ${sourceApp.songListName}, ${sourceApp.musicNum}) VALUES (?, ?, ?)",
                                arrayOf(
                                    playlist.getString("tid"),
                                    playlist.getString("diss_name"),
                                    playlist.getInteger("song_cnt")
                                )
                            )
                        }

                        var uin = "(uin=\\d+)|(wxuin=\\d+)".toRegex().find(cookie.value)?.value
                        uin = uin?.substring(uin.indexOf("=") + 1)
                        val requestQQLikePlaylist = Request.Builder()
                            .addHeader("Cookie", cookie.value)
                            .url("https://c.y.qq.com/fav/fcgi-bin/fcg_get_profile_order_asset.fcg?uin=${uin}&userid=${uin}&ct=24&format=json&inCharset=utf-8&outCharset=utf-8&sin=0&ein=99&reqtype=3&cid=205360956")
                            .addHeader("Referer", "https://y.qq.com/")
                            .get()

                        val responseQQLikePlaylist: JSONObject
                        client.newCall(requestQQLikePlaylist.build()).execute().use { responses ->
                            responseQQLikePlaylist = JSON.parseObject(responses.body?.string())
                        }
                        val qqLikePlaylistInfo =
                            responseQQLikePlaylist.getJSONObject("data").getJSONArray("cdlist")
                        qqLikePlaylistInfo.forEach {
                            val playlist = it as JSONObject
                            if (playlist.getInteger("songnum") == 0) {
                                emptyPlaylistNum++
                                return@forEach
                            }
                            innerPlaylistId.add(playlist.getString("dissid"))
                            innerPlaylistName.add(playlist.getString("dissname"))
                            innerPlaylistSum.add(playlist.getInteger("songnum"))
                            innerPlaylistEnabled.add(0)
                            innerPlaylistShow.add(true)
                            db.execSQL(
                                "INSERT INTO ${sourceApp.songListTableName} (${sourceApp.songListId}, ${sourceApp.songListName}, ${sourceApp.musicNum}) VALUES (?, ?, ?)",
                                arrayOf(
                                    playlist.getString("dissid"),
                                    playlist.getString("dissname"),
                                    playlist.getInteger("songnum")
                                )
                            )
                        }
                        db.close()
                        if (loginInfoExpired) {
                            errorDialogTitle.value = context.getString(R.string.tips)
                            errorDialogContent.value =
                                "- ${context.getString(R.string.login_info_maybe_expired)}\n"
                            errorDialogCustomAction.value = {}
                            showErrorDialog.value = true
                        }
                    }

                    3 -> {
                        Tools().copyAssetFileToExternalFilesDir(
                            context,
                            "kugou_music_phone_v7.db"
                        )
                        val databaseFile =
                            File(context.getExternalFilesDir(null), "kugou_music_phone_v7.db")
                        val db = SQLiteDatabase.openDatabase(
                            databaseFile.absolutePath,
                            null,
                            SQLiteDatabase.OPEN_READWRITE
                        )
                        databaseFilePath.value = databaseFile.absolutePath
                        val deviceId = Tools().md5(
                            input = Settings.System.getString(
                                context.contentResolver, Settings.Secure.ANDROID_ID
                            )
                        )
                        var times = 1

                        while (true) {
                            val playlistInfo =
                                response.getJSONObject("data").getJSONArray("playlists")
                            if (playlistInfo.size == 0) {
                                db.close()
                                break
                            }
                            playlistInfo.forEach {
                                val playlist = it as JSONObject
                                if (playlist.getInteger("total") == 0) {
                                    emptyPlaylistNum++
                                    return@forEach
                                }
                                innerPlaylistId.add(playlist.getString("playlist_id"))
                                innerPlaylistName.add(playlist.getString("playlist_name"))
                                innerPlaylistSum.add(playlist.getInteger("total"))
                                innerPlaylistEnabled.add(0)
                                innerPlaylistShow.add(true)
                                db.execSQL(
                                    "INSERT INTO ${sourceApp.songListTableName} (${sourceApp.songListId}, ${sourceApp.songListName}, ${sourceApp.musicNum}) VALUES (?, ?, ?)",
                                    arrayOf(
                                        playlist.getString("playlist_id"),
                                        playlist.getString("playlist_name"),
                                        playlist.getInteger("total")
                                    )
                                )
                            }
                            val cursor = db.rawQuery(
                                "SELECT COUNT(*) FROM ${sourceApp.songListTableName}",
                                null
                            )
                            cursor.moveToFirst()
                            val dbPlaylistNum = cursor.getInt(0)
                            if ((dbPlaylistNum + emptyPlaylistNum) ==
                                response.getJSONObject("data").getInteger("total")
                            ) {
                                cursor.close()
                                db.close()
                                break
                            } else {
                                cursor.close()
                                val nonce =
                                    Tools().generateRandomString(
                                        length = 32,
                                        includeLowerCase = true,
                                        includeDigits = true
                                    )
                                val time = System.currentTimeMillis() / 1000
                                val json =
                                    """{"size":30,"device_id":"${deviceId}","client_ver":"133-62b8ece-20240223165034","pid":"203051","client_ip":"${kugouCurrentIp}","page":${++times},"apk_ver":"9845","nonce":"${nonce}","sp":"KG","userid":"${kugouUserRelated["userId"]}","timestamp":${time},"token":"${kugouUserRelated["token"]}"}"""
                                val requestBody =
                                    json.toRequestBody("application/json; charset=utf-8".toMediaType())
                                request = Request.Builder()
                                request = request
                                    .url(url)
                                    .addHeader(
                                        "User-Agent",
                                        "Android13-androidCar-133-62b8ece-203051-0-UltimateSdk-wifi"
                                    )
                                    .addHeader(
                                        "Content-Type",
                                        "application/json; charset=UTF-8"
                                    )
                                    .addHeader("Host", "thirdsso.kugou.com")
                                    .addHeader("Connection", "Keep-Alive")
                                    .addHeader("Accept-Encoding", "gzip")
                                    .addHeader(
                                        "signature",
                                        Tools().md5(input = json + "9046ad4ecae74a70aa750c1bb2307ae6")
                                    )
                                    .post(requestBody)
                                client.newCall(request.build()).execute().use { responses ->
                                    response = JSON.parseObject(responses.body?.string())
                                }
                            }
                        }
                    }

                    4 -> {}

                    5 -> {
                        Tools().copyAssetFileToExternalFilesDir(
                            context,
                            "QQMusic"
                        )
                        val databaseFile =
                            File(context.getExternalFilesDir(null), "QQMusic")
                        val db = SQLiteDatabase.openDatabase(
                            databaseFile.absolutePath,
                            null,
                            SQLiteDatabase.OPEN_READWRITE
                        )
                        databaseFilePath.value = databaseFile.absolutePath
                        val playlistInfo =
                            response.getJSONArray("playlists")
                        playlistInfo.forEach {
                            val playlist = it as JSONObject
                            if (!playlist.containsKey("count_tracks") || playlist.getInteger("count_tracks") == 0) {
                                emptyPlaylistNum++
                                return@forEach
                            }
                            innerPlaylistId.add(playlist.getString("id"))
                            innerPlaylistName.add(playlist.getString("title"))
                            innerPlaylistSum.add(playlist.getInteger("count_tracks"))
                            innerPlaylistEnabled.add(0)
                            innerPlaylistShow.add(true)
                            db.execSQL(
                                "INSERT INTO ${sourceApp.songListTableName} (${sourceApp.songListId}, ${sourceApp.songListName}, ${sourceApp.musicNum}) VALUES (?, ?, ?)",
                                arrayOf(
                                    playlist.getString("id"),
                                    playlist.getString("title"),
                                    playlist.getInteger("count_tracks")
                                )
                            )
                        }
                        db.close()
                    }

                    6 -> {
                        Tools().copyAssetFileToExternalFilesDir(
                            context,
                            "QQMusic"
                        )
                        val databaseFile =
                            File(context.getExternalFilesDir(null), "QQMusic")
                        val db = SQLiteDatabase.openDatabase(
                            databaseFile.absolutePath,
                            null,
                            SQLiteDatabase.OPEN_READWRITE
                        )
                        databaseFilePath.value = databaseFile.absolutePath
                        val playlistInfo =
                            response.getJSONArray("items")
                        playlistInfo.forEach {
                            val playlist = it as JSONObject
                            if (!playlist.containsKey("tracks") || playlist.getJSONObject("tracks")
                                    .getInteger("total") == 0
                            ) {
                                emptyPlaylistNum++
                                return@forEach
                            }
                            innerPlaylistId.add(playlist.getString("id"))
                            innerPlaylistName.add(playlist.getString("name"))
                            innerPlaylistSum.add(
                                playlist.getJSONObject("tracks").getInteger("total")
                            )
                            innerPlaylistEnabled.add(0)
                            innerPlaylistShow.add(true)
                            db.execSQL(
                                "INSERT INTO ${sourceApp.songListTableName} (${sourceApp.songListId}, ${sourceApp.songListName}, ${sourceApp.musicNum}) VALUES (?, ?, ?)",
                                arrayOf(
                                    playlist.getString("id"),
                                    playlist.getString("name"),
                                    playlist.getJSONObject("tracks").getInteger("total")
                                )
                            )
                        }
                        db.close()
                    }
                }
                if (innerPlaylistId.size == 0) {
                    throw Exception(
                        context.getString(R.string.online_server_response_null)
                            .replace(
                                "#", when (selectedSourceApp.intValue) {
                                    1 -> context.getString(R.string.source_netease_cloud_music)
                                    2 -> context.getString(R.string.source_qq_music)
                                    3 -> context.getString(R.string.source_kugou_music)
                                    4 -> context.getString(R.string.source_kuwo_music)
                                    5 -> context.getString(R.string.source_luna_music)
                                    6 -> context.getString(R.string.source_spotify)
                                    7 -> context.getString(R.string.tune_my_music)
                                    else -> ""
                                }
                            )
                    )
                }
                playlistId.addAll(innerPlaylistId)
                playlistName.addAll(innerPlaylistName)
                playlistEnabled.addAll(innerPlaylistEnabled)
                playlistSum.addAll(innerPlaylistSum)
                playlistShow.addAll(innerPlaylistShow)
                showLoadingProgressBar.value = false
                MyVibrationEffect(
                    context,
                    (context as MainActivity).enableHaptic.value,
                    context.hapticStrength.intValue
                ).done()
                if (emptyPlaylistNum != 0) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.get_playlist_hidden)
                                .replace("#", emptyPlaylistNum.toString()),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                showLoadingProgressBar.value = false
                currentPage.intValue = 0
                errorDialogTitle.value =
                    context.getString(R.string.error_while_getting_data_dialog_title)
                errorDialogContent.value =
                    "- ${context.getString(R.string.get_playlist_failed)}\n  - $e\n"
                errorDialogCustomAction.value = {}
                showErrorDialog.value = true
            }
        }
    }

    suspend fun getCustomPlaylist() {
        if (customPlaylistInput.value.isBlank()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.please_input_playlist_id),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }
        showDialogProgressBar.value = true
        var customPlaylistId: String?
        customPlaylistId =
            if (customPlaylistInput.value.contains("http"))
                "https://.*\\b".toRegex().find(customPlaylistInput.value)?.value
            else
                customPlaylistInput.value
        if (selectedSourceApp.intValue == 5 && customPlaylistId!!.contains("http"))
            customPlaylistId = Tools().getRealUrl(customPlaylistId)

        if (customPlaylistId != null) {
            val url = when (selectedSourceApp.intValue) {
                1 -> "https://interface.music.163.com/weapi/v6/playlist/detail"
                2 -> "https://u.y.qq.com/cgi-bin/musicu.fcg"
                3 -> ""
                4 -> "https://kuwo.cn/api/www/playlist/playListInfo"
                5 -> "https://api.qishui.com/luna/pc/playlist/detail"
                6 -> "https://api.spotify.com/v1/playlists"
                else -> ""
            }
            val client = OkHttpClient()
            var request = Request.Builder()
            try {
                when (selectedSourceApp.intValue) {
                    1 -> {
                        if (customPlaylistInput.value.contains("http"))
                            customPlaylistId =
                                "\\bid=\\d*".toRegex().find(customPlaylistId)?.value?.substring(
                                    3
                                )
                        val encrypted = Tools().encryptString(
                            """{"id":${customPlaylistId},"n":3,"shareUserId":0,"csrf_token":"${
                                "__csrf=\\w+".toRegex()
                                    .find(cookie.value)?.value?.substring(7)
                            }"}""",
                            "netease",
                            encryptServer.value
                        )
                        val formBody = encrypted?.let {
                            FormBody.Builder()
                                .add("params", it.getString("encText"))
                                .add("encSecKey", it.getString("encSecKey"))
                                .build()
                        }
                        formBody?.let {
                            request = request.url(
                                "${url}?csrf_token=${
                                    "__csrf=\\w+".toRegex()
                                        .find(cookie.value)?.value?.substring(7)
                                }"
                            ).addHeader("Cookie", cookie.value)
                                .post(it)
                        }
                        val response: JSONObject
                        client.newCall(request.build()).execute().use { responses ->
                            response = JSON.parseObject(responses.body?.string())
                        }
                        if (response.getInteger("code") == 200) {
                            if (playlistId.size == 0) {
                                Tools().copyAssetFileToExternalFilesDir(
                                    context,
                                    "cloudmusic.db"
                                )
                            }
                            val databaseFile =
                                File(context.getExternalFilesDir(null), "cloudmusic.db")
                            val db = SQLiteDatabase.openDatabase(
                                databaseFile.absolutePath,
                                null,
                                SQLiteDatabase.OPEN_READWRITE
                            )
                            databaseFilePath.value = databaseFile.absolutePath
                            val playlistInfo = response.getJSONObject("playlist")
                            val cursor = db.rawQuery(
                                "SELECT COUNT(*) FROM ${sourceApp.songListTableName} WHERE ${sourceApp.songListId} = ?",
                                arrayOf(customPlaylistId)
                            )
                            cursor.moveToFirst()
                            if (cursor.getInt(0) != 0) {
                                cursor.close()
                                db.close()
                                throw IllegalStateException(context.getString(R.string.playlist_already_exists))
                            }
                            cursor.close()
                            db.execSQL(
                                "INSERT INTO ${sourceApp.songListTableName} (${sourceApp.songListId}, ${sourceApp.songListName}, ${sourceApp.musicNum}) VALUES (?, ?, ?)",
                                arrayOf(
                                    playlistInfo.getString("id"),
                                    playlistInfo.getString("name"),
                                    playlistInfo.getInteger("trackCount")
                                )
                            )
                            db.close()
                            playlistShow.add(0, false)
                            playlistEnabled.add(0, 0)
                            playlistId.add(0, playlistInfo.getString("id"))
                            playlistName.add(0, playlistInfo.getString("name"))
                            playlistSum.add(0, playlistInfo.getInteger("trackCount"))
                            showCustomPlaylistDialog.value = false
                            showDialogProgressBar.value = false
                            MyVibrationEffect(
                                context,
                                (context as MainActivity).enableHaptic.value,
                                context.hapticStrength.intValue
                            ).done()
                            customPlaylistInput.value = ""
                        } else {
                            throw IllegalStateException(context.getString(R.string.wrong_input_playlist_data))
                        }
                    }

                    2 -> {
                        if (customPlaylistInput.value.contains("http"))
                            customPlaylistId = "(\\bid=|st/)\\d*".toRegex()
                                .find(customPlaylistId)?.value?.substring(3)
                        val json =
                            """{"comm":{"ct":"1","cv":"10080511","v":"10080511"},"GetPlayList":{"module":"music.srfDissInfo.DissInfo","method":"CgiGetDiss","param":{"disstid":${customPlaylistId},"song_num":3}}}"""
                        val requestBody =
                            json.toRequestBody("application/json; charset=utf-8".toMediaType())
                        request = request
                            .url(url)
                            .addHeader("Cookie", cookie.value)
                            .addHeader("Referer", "https://y.qq.com/")
                            .addHeader("Accept", "application/json")
                            .post(requestBody)
                        val response: JSONObject
                        client.newCall(request.build()).execute().use { responses ->
                            response = JSON.parseObject(responses.body?.string())
                        }
                        if (response.getJSONObject("GetPlayList")?.getJSONObject("data")
                                ?.getInteger("code") == 0
                        ) {
                            if (playlistId.size == 0) {
                                Tools().copyAssetFileToExternalFilesDir(
                                    context,
                                    "QQMusic"
                                )
                            }
                            val databaseFile =
                                File(context.getExternalFilesDir(null), "QQMusic")
                            val db = SQLiteDatabase.openDatabase(
                                databaseFile.absolutePath,
                                null,
                                SQLiteDatabase.OPEN_READWRITE
                            )
                            databaseFilePath.value = databaseFile.absolutePath
                            val playlistInfo = response.getJSONObject("GetPlayList")
                                .getJSONObject("data").getJSONObject("dirinfo")
                            val cursor = db.rawQuery(
                                "SELECT COUNT(*) FROM ${sourceApp.songListTableName} WHERE ${sourceApp.songListId} = ?",
                                arrayOf(customPlaylistId)
                            )
                            cursor.moveToFirst()
                            if (cursor.getInt(0) != 0) {
                                cursor.close()
                                db.close()
                                throw IllegalStateException(context.getString(R.string.playlist_already_exists))
                            }
                            cursor.close()
                            db.execSQL(
                                "INSERT INTO ${sourceApp.songListTableName} (${sourceApp.songListId}, ${sourceApp.songListName}, ${sourceApp.musicNum}) VALUES (?, ?, ?)",
                                arrayOf(
                                    playlistInfo.getString("id"),
                                    playlistInfo.getString("title"),
                                    playlistInfo.getInteger("songnum")
                                )
                            )
                            db.close()
                            playlistShow.add(0, false)
                            playlistEnabled.add(0, 0)
                            playlistId.add(0, playlistInfo.getString("id"))
                            playlistName.add(0, playlistInfo.getString("title"))
                            playlistSum.add(0, playlistInfo.getInteger("songnum"))
                            showCustomPlaylistDialog.value = false
                            showDialogProgressBar.value = false
                            MyVibrationEffect(
                                context,
                                (context as MainActivity).enableHaptic.value,
                                context.hapticStrength.intValue
                            ).done()
                            customPlaylistInput.value = ""
                        } else {
                            throw IllegalStateException(context.getString(R.string.wrong_input_playlist_data))
                        }
                    }

                    3 -> {
                        val response: String
                        client.newCall(
                            Request.Builder()
                                .url(customPlaylistId.replace("www.kugou.com", "m.kugou.com"))
                                .addHeader(
                                    "User-Agent",
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0"
                                ).get().build()
                        ).execute().use { responses ->
                            response = responses.body?.string() ?: ""
                        }
                        if (playlistId.size == 0) {
                            Tools().copyAssetFileToExternalFilesDir(
                                context,
                                "kugou_music_phone_v7.db"
                            )
                        }
                        val databaseFile =
                            File(
                                context.getExternalFilesDir(null),
                                "kugou_music_phone_v7.db"
                            )
                        val db = SQLiteDatabase.openDatabase(
                            databaseFile.absolutePath,
                            null,
                            SQLiteDatabase.OPEN_READWRITE
                        )
                        databaseFilePath.value = databaseFile.absolutePath
                        customPlaylistId =
                            "\"global_collection_id\":\"\\w*".toRegex().find(
                                response
                            )?.value?.substring(24)
                        val playlistInfo = JSONObject.parseObject(
                            response.substring(
                                response.indexOf("var nData=") + 10,
                                response.indexOf("}]};") + 3
                            )
                        ).getJSONObject("listinfo")
                        val cursor = db.rawQuery(
                            "SELECT COUNT(*) FROM ${sourceApp.songListTableName} WHERE ${sourceApp.songListId} = ?",
                            arrayOf(customPlaylistId)
                        )
                        cursor.moveToFirst()
                        if (cursor.getInt(0) != 0) {
                            cursor.close()
                            db.close()
                            throw IllegalStateException(context.getString(R.string.playlist_already_exists))
                        }
                        cursor.close()
                        db.execSQL(
                            "INSERT INTO ${sourceApp.songListTableName} (${sourceApp.songListId}, ${sourceApp.songListName}, ${sourceApp.musicNum}) VALUES (?, ?, ?)",
                            arrayOf(
                                customPlaylistId,
                                playlistInfo.getString("name"),
                                playlistInfo.getInteger("count")
                            )
                        )
                        db.close()
                        playlistShow.add(0, false)
                        playlistEnabled.add(0, 0)
                        customPlaylistId?.let { playlistId.add(0, it) }
                        playlistName.add(0, playlistInfo.getString("name"))
                        playlistSum.add(0, playlistInfo.getInteger("count"))
                        showCustomPlaylistDialog.value = false
                        showDialogProgressBar.value = false
                        MyVibrationEffect(
                            context,
                            (context as MainActivity).enableHaptic.value,
                            context.hapticStrength.intValue
                        ).done()
                        customPlaylistInput.value = ""

                    }

                    4 -> {
                        if (customPlaylistInput.value.contains("http"))
                            customPlaylistId = "\\bplaylist_detail/\\d*".toRegex()
                                .find(customPlaylistId)?.value?.substring(16)
                        val getParams =
                            """pid=${customPlaylistId}&pn=1&rn=3&httpsStatus=1&plat=web_www"""
                        val kuwoSecret = Tools().encryptString(
                            cookie.value,
                            "kuwo",
                            encryptServer.value
                        )?.getString("Secret")
                        kuwoSecret?.let {
                            request = request
                                .url("${url}?${getParams}")
                                .addHeader("Cookie", cookie.value)
                                .addHeader(
                                    "Referer",
                                    "https://kuwo.cn/playlist_detail/${customPlaylistId}"
                                )
                                .addHeader("Secret", it)
                                .addHeader(
                                    "User-Agent",
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0"
                                )
                                .addHeader("Connection", "Keep-Alive")
                                .addHeader(
                                    "Accept",
                                    "application/json, text/plain, */*"
                                )
                                .get()
                            val response: JSONObject
                            client.newCall(request.build()).execute().use { responses ->
                                response = JSON.parseObject(responses.body?.string())
                            }
                            if (response.getInteger("code") == 200) {
                                if (playlistId.size == 0) {
                                    Tools().copyAssetFileToExternalFilesDir(
                                        context,
                                        "kwplayer.db"
                                    )
                                }
                                val databaseFile =
                                    File(context.getExternalFilesDir(null), "kwplayer.db")
                                val db = SQLiteDatabase.openDatabase(
                                    databaseFile.absolutePath,
                                    null,
                                    SQLiteDatabase.OPEN_READWRITE
                                )
                                databaseFilePath.value = databaseFile.absolutePath
                                val playlistInfo = response.getJSONObject("data")
                                val cursor = db.rawQuery(
                                    "SELECT COUNT(*) FROM ${sourceApp.songListTableName} WHERE ${sourceApp.songListId} = ?",
                                    arrayOf(customPlaylistId)
                                )
                                cursor.moveToFirst()
                                if (cursor.getInt(0) != 0) {
                                    cursor.close()
                                    db.close()
                                    throw IllegalStateException(context.getString(R.string.playlist_already_exists))
                                }
                                cursor.close()
                                db.execSQL(
                                    "INSERT INTO ${sourceApp.songListTableName} (${sourceApp.songListId}, ${sourceApp.songListName}, ${sourceApp.musicNum}) VALUES (?, ?, ?)",
                                    arrayOf(
                                        playlistInfo.getString("id"),
                                        playlistInfo.getString("name"),
                                        playlistInfo.getInteger("total")
                                    )
                                )
                                db.close()
                                playlistShow.add(0, false)
                                playlistEnabled.add(0, 0)
                                playlistId.add(0, playlistInfo.getString("id"))
                                playlistName.add(0, playlistInfo.getString("name"))
                                playlistSum.add(0, playlistInfo.getInteger("total"))
                                showCustomPlaylistDialog.value = false
                                showDialogProgressBar.value = false
                                MyVibrationEffect(
                                    context,
                                    (context as MainActivity).enableHaptic.value,
                                    context.hapticStrength.intValue
                                ).done()
                                customPlaylistInput.value = ""
                            } else {
                                throw IllegalStateException(context.getString(R.string.wrong_input_playlist_data))
                            }
                        }
                    }

                    5 -> {
                        if (customPlaylistInput.value.contains("http"))
                            customPlaylistId = "(\\bplaylist_id=)\\d*".toRegex()
                                .find(customPlaylistId)?.value?.substring(12)
                        request = request
                            .url("${url}?aid=386088&app_name=luna_pc&region=cn&geo_region=cn&os_region=cn&device_id=${lunaDeviceId.value}&iid=${lunaInstallId.value}&version_name=1.6.3&version_code=10060300&channel=master&build_mode=master&ac=wifi&tz_name=Asia%2FShanghai&device_platform=windows&device_type=Windows&os_version=Windows+10+Pro&fp=${lunaDeviceId.value}&playlist_id=${customPlaylistId}&count=3")
                            .addHeader("Cookie",
                                cookie.value.ifBlank { lunaCookie.value }
                            )
                            .addHeader("Host", "api.qishui.com")
                            .addHeader("User-Agent", "LunaPC/1.6.3(11741945)")
                            .addHeader("Accept", "*/*")
                            .addHeader("Connection", "keep-alive")
                            .get()
                        val response: JSONObject
                        client.newCall(request.build()).execute().use { responses ->
                            response = JSON.parseObject(responses.body?.string())
                        }
                        if (response.getJSONObject("status_info")
                                ?.getString("status_msg") == null
                        ) {
                            if (playlistId.size == 0) {
                                Tools().copyAssetFileToExternalFilesDir(
                                    context,
                                    "QQMusic"
                                )
                            }
                            val databaseFile =
                                File(context.getExternalFilesDir(null), "QQMusic")
                            val db = SQLiteDatabase.openDatabase(
                                databaseFile.absolutePath,
                                null,
                                SQLiteDatabase.OPEN_READWRITE
                            )
                            databaseFilePath.value = databaseFile.absolutePath
                            val playlistInfo = response.getJSONObject("playlist")
                            val cursor = db.rawQuery(
                                "SELECT COUNT(*) FROM ${sourceApp.songListTableName} WHERE ${sourceApp.songListId} = ?",
                                arrayOf(customPlaylistId)
                            )
                            cursor.moveToFirst()
                            if (cursor.getInt(0) != 0) {
                                cursor.close()
                                db.close()
                                throw IllegalStateException(context.getString(R.string.playlist_already_exists))
                            }
                            cursor.close()
                            db.execSQL(
                                "INSERT INTO ${sourceApp.songListTableName} (${sourceApp.songListId}, ${sourceApp.songListName}, ${sourceApp.musicNum}) VALUES (?, ?, ?)",
                                arrayOf(
                                    playlistInfo.getString("id"),
                                    playlistInfo.getString("title"),
                                    playlistInfo.getInteger("count_tracks")
                                )
                            )
                            db.close()
                            playlistShow.add(0, false)
                            playlistEnabled.add(0, 0)
                            playlistId.add(0, playlistInfo.getString("id"))
                            playlistName.add(0, playlistInfo.getString("title"))
                            playlistSum.add(0, playlistInfo.getInteger("count_tracks"))
                            showCustomPlaylistDialog.value = false
                            showDialogProgressBar.value = false
                            MyVibrationEffect(
                                context,
                                (context as MainActivity).enableHaptic.value,
                                context.hapticStrength.intValue
                            ).done()
                            customPlaylistInput.value = ""
                        }
                    }

                    6 -> {
                        if (customPlaylistInput.value.contains("http"))
                            customPlaylistId = "playlist/\\w+".toRegex()
                                .find(customPlaylistId)?.value?.substring(9)
                        var spotifyToken = ""
                        if (selectedSourceApp.intValue == 6) {
                            if (encryptServer.value == "cf") {
                                throw Exception(context.getString(R.string.spotify_token_cf))
                            }
                            try {
                                spotifyToken = Tools().encryptString(
                                    "0248",
                                    "spotify",
                                    encryptServer.value
                                )?.getString("token").toString()
                            } catch (e: Exception) {
                                showDialogProgressBar.value = false
                                MyVibrationEffect(
                                    context,
                                    (context as MainActivity).enableHaptic.value,
                                    context.hapticStrength.intValue
                                ).done()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.spotify_token_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                return
                            }
                        }
                        request = request
                            .url("${url}/${customPlaylistId}")
                            .addHeader("Authorization", "Bearer ${spotifyToken}")
                            .addHeader("Accept", "application/json")
                            .addHeader("Connection", "keep-alive")
                            .get()

                        val response: JSONObject
                        client.newCall(request.build()).execute().use { responses ->
                            response = JSON.parseObject(responses.body?.string())
                        }
                        if (response.containsKey("error")) {
                            throw IllegalStateException(context.getString(R.string.wrong_input_playlist_data))
                        }
                        if (playlistId.size == 0) {
                            Tools().copyAssetFileToExternalFilesDir(
                                context,
                                "QQMusic"
                            )
                        }
                        val databaseFile =
                            File(context.getExternalFilesDir(null), "QQMusic")
                        val db = SQLiteDatabase.openDatabase(
                            databaseFile.absolutePath,
                            null,
                            SQLiteDatabase.OPEN_READWRITE
                        )
                        databaseFilePath.value = databaseFile.absolutePath

                        val cursor = db.rawQuery(
                            "SELECT COUNT(*) FROM ${sourceApp.songListTableName} WHERE ${sourceApp.songListId} = ?",
                            arrayOf(customPlaylistId)
                        )
                        cursor.moveToFirst()
                        if (cursor.getInt(0) != 0) {
                            cursor.close()
                            db.close()
                            throw IllegalStateException(context.getString(R.string.playlist_already_exists))
                        }
                        cursor.close()
                        db.execSQL(
                            "INSERT INTO ${sourceApp.songListTableName} (${sourceApp.songListId}, ${sourceApp.songListName}, ${sourceApp.musicNum}) VALUES (?, ?, ?)",
                            arrayOf(
                                response.getString("id"),
                                response.getString("name"),
                                response.getJSONObject("tracks").getInteger("total")
                            )
                        )
                        db.close()
                        playlistShow.add(0, false)
                        playlistEnabled.add(0, 0)
                        playlistId.add(0, response.getString("id"))
                        playlistName.add(0, response.getString("name"))
                        playlistSum.add(0, response.getJSONObject("tracks").getInteger("total"))
                        showCustomPlaylistDialog.value = false
                        showDialogProgressBar.value = false
                        MyVibrationEffect(
                            context,
                            (context as MainActivity).enableHaptic.value,
                            context.hapticStrength.intValue
                        ).done()
                        customPlaylistInput.value = ""
                    }
                }
                if (playlistShow.size > 1)
                    delay(250L)
                playlistShow[0] = true
            } catch (e: IllegalStateException) {
                showDialogProgressBar.value = false
                MyVibrationEffect(
                    context,
                    (context as MainActivity).enableHaptic.value,
                    context.hapticStrength.intValue
                ).done()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                MyVibrationEffect(
                    context,
                    (context as MainActivity).enableHaptic.value,
                    context.hapticStrength.intValue
                ).done()
                showDialogProgressBar.value = false
                errorDialogTitle.value =
                    context.getString(R.string.error_while_getting_data_dialog_title)
                errorDialogContent.value =
                    "- ${context.getString(R.string.get_playlist_failed)}\n  - $e\n"
                errorDialogCustomAction.value = {}
                showErrorDialog.value = true
            }
        }
    }

    private fun databaseSummary() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            showLoadingProgressBar.value = true
            val innerPlaylistId = MutableList(0) { "" }
            val innerPlaylistName = MutableList(0) { "" }
            val innerPlaylistEnabled = MutableList(0) { 0 }
            val innerPlaylistSum = MutableList(0) { 0 }
            val innerPlaylistShow = MutableList(0) { true }

            val db = SQLiteDatabase.openOrCreateDatabase(File(databaseFilePath.value), null)

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
                    "SELECT ${sourceApp.musicNum} FROM ${sourceApp.songListTableName} WHERE ${sourceApp.songListId} = ?",
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
                innerPlaylistShow.add(true)
            }
            cursor.close()
            db.close()
            playlistId.addAll(innerPlaylistId)
            playlistName.addAll(innerPlaylistName)
            playlistEnabled.addAll(innerPlaylistEnabled)
            playlistSum.addAll(innerPlaylistSum)
            playlistShow.addAll(innerPlaylistShow)
            showLoadingProgressBar.value = false
            MyVibrationEffect(
                context,
                (context as MainActivity).enableHaptic.value,
                context.hapticStrength.intValue
            ).done()
        }
    }

    fun checkSongListSelection() {
        if (playlistEnabled.all { it == 0 }) {
            errorDialogTitle.value =
                context.getString(R.string.error)
            errorDialogContent.value =
                "- ${context.getString(R.string.please_select_at_least_one_playlist)}\n"
            errorDialogCustomAction.value = {}
            showErrorDialog.value = true
            return
        }
        for (i in playlistEnabled.indices) {
            if (playlistEnabled[i] == 2 || playlistEnabled[i] == 3) {
                playlistEnabled[i] = 1
            }
        }
        currentPage.intValue = 2
    }

    var convertResult = mutableStateMapOf<Int, Array<String>>()
    var numberProgress = mutableFloatStateOf(0.0f)
    fun previewResult(
        directlyStart: Boolean = false
    ) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            numberProgress.floatValue = 0.0f
            convertResult.clear()
            val convertResultMap = mutableMapOf<Int, Array<String>>()
            val firstIndex1 = playlistEnabled.indexOfFirst { it == 1 }

            if (!directlyStart) {
                if (selectedMethod.intValue == 1) {
                    var kuwoSecret: String? = null
                    val testDb = SQLiteDatabase.openDatabase(
                        databaseFilePath.value,
                        null,
                        SQLiteDatabase.OPEN_READONLY
                    )
                    val testCursor = testDb.rawQuery(
                        "SELECT COUNT(${sourceApp.songListSongInfoSongId}) FROM ${sourceApp.songListSongInfoTableName} WHERE ${sourceApp.songListSongInfoPlaylistId} = ?",
                        arrayOf(
                            playlistId[firstIndex1]
                        )
                    )
                    testCursor.moveToFirst()
                    if (testCursor.getInt(0) != playlistSum[firstIndex1]) {
                        showLoadingProgressBar.value = true
                        testCursor.close()
                        testDb.close()

                        try {
                            val url = when (selectedSourceApp.intValue) {
                                1 -> "https://interface.music.163.com/weapi/v6/playlist/detail"
                                2 -> "https://u.y.qq.com/cgi-bin/musicu.fcg"
                                3 -> "https://gateway.kugou.com/pubsongs/v4/get_other_list_file"
                                4 -> "https://kuwo.cn/api/www/playlist/playListInfo"
                                5 -> "https://api.qishui.com/luna/pc/playlist/detail"
                                6 -> "https://api.spotify.com/v1/playlists"
                                else -> ""
                            }
                            var spotifyToken = ""
                            if (selectedSourceApp.intValue == 6) {
                                if (encryptServer.value == "cf") {
                                    throw Exception(context.getString(R.string.spotify_token_cf))
                                }
                                try {
                                    spotifyToken = Tools().encryptString(
                                        "0248",
                                        "spotify",
                                        encryptServer.value
                                    )?.getString("token").toString()
                                } catch (e: Exception) {
                                    showLoadingProgressBar.value = false
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.spotify_token_error),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    return@launch
                                }
                            }
                            val client = OkHttpClient()
                            var request = Request.Builder()
                            when (selectedSourceApp.intValue) {
                                1 -> {
                                    val encrypted = Tools().encryptString(
                                        """{"id":${playlistId[firstIndex1]},"n":1000,"shareUserId":0,"csrf_token":"${
                                            "__csrf=\\w+".toRegex()
                                                .find(cookie.value)?.value?.substring(7)
                                        }"}""",
                                        "netease",
                                        encryptServer.value
                                    )
                                    val formBody = encrypted?.let {
                                        FormBody.Builder()
                                            .add("params", it.getString("encText"))
                                            .add("encSecKey", it.getString("encSecKey"))
                                            .build()
                                    }
                                    formBody?.let {
                                        request = request.url(
                                            "${url}?csrf_token=${
                                                "__csrf=\\w+".toRegex()
                                                    .find(cookie.value)?.value?.substring(7)
                                            }"
                                        ).addHeader("Cookie", cookie.value)
                                            .post(it)
                                    }
                                }

                                2 -> {
                                    val json =
                                        """{"comm":{"ct":"1","cv":"10080511","v":"10080511"},"GetPlayList":{"module":"music.srfDissInfo.DissInfo","method":"CgiGetDiss","param":{"disstid":${playlistId[firstIndex1]}}}}"""
                                    val requestBody =
                                        json.toRequestBody("application/json; charset=utf-8".toMediaType())
                                    request = request
                                        .url(url)
                                        .addHeader("Cookie", cookie.value)
                                        .addHeader("Referer", "https://y.qq.com/")
                                        .addHeader("Accept", "application/json")
                                        .post(requestBody)
                                }

                                3 -> {
                                    val getParams =
                                        """appid=1005&area_code=1&clientver=12189&global_collection_id=${playlistId[firstIndex1]}&mode=1&module=CloudMusic&need_sort=1&page=1&pagesize=300&type=0"""
                                    val signature = Tools().md5(
                                        input = "OIlwieks28dk2k092lksi2UIkp${
                                            getParams.replace(
                                                "&",
                                                ""
                                            )
                                        }OIlwieks28dk2k092lksi2UIkp"
                                    )
                                    request = request
                                        .url("${url}?signature=${signature}&${getParams}")
                                        .addHeader("Host", "gateway.kugou.com")
                                        .addHeader(
                                            "clienttime",
                                            "${System.currentTimeMillis() / 1000}"
                                        )
                                        .addHeader(
                                            "kg-clienttimems",
                                            "${System.currentTimeMillis()}"
                                        )
                                        .addHeader(
                                            "user-agent",
                                            "Android13-AndroidPhone-12189-130-0-playlist-wifi"
                                        )
                                        .addHeader("kg-rc", "1")
                                        .addHeader("Connection", "Keep-Alive")
                                        .get()
                                }

                                4 -> {
                                    val getParams =
                                        """pid=${playlistId[firstIndex1]}&pn=1&rn=50&httpsStatus=1&plat=web_www"""
                                    kuwoSecret = Tools().encryptString(
                                        cookie.value,
                                        "kuwo",
                                        encryptServer.value
                                    )?.getString("Secret")
                                    kuwoSecret?.let {
                                        request = request
                                            .url("${url}?${getParams}")
                                            .addHeader("Cookie", cookie.value)
                                            .addHeader(
                                                "Referer",
                                                "https://kuwo.cn/playlist_detail/${playlistId[firstIndex1]}"
                                            )
                                            .addHeader("Secret", it)
                                            .addHeader(
                                                "User-Agent",
                                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0"
                                            )
                                            .addHeader("Connection", "Keep-Alive")
                                            .addHeader(
                                                "Accept",
                                                "application/json, text/plain, */*"
                                            )
                                            .get()
                                    }
                                }

                                5 -> {
                                    request = request
                                        .url("${url}?aid=386088&app_name=luna_pc&region=cn&geo_region=cn&os_region=cn&device_id=${lunaDeviceId.value}&iid=${lunaInstallId.value}&version_name=1.6.3&version_code=10060300&channel=master&build_mode=master&ac=wifi&tz_name=Asia%2FShanghai&device_platform=windows&device_type=Windows&os_version=Windows+10+Pro&fp=${lunaDeviceId.value}&playlist_id=${playlistId[firstIndex1]}&count=1000")
                                        .addHeader("Cookie",
                                            cookie.value.ifBlank { lunaCookie.value }
                                        )
                                        .addHeader("Host", "api.qishui.com")
                                        .addHeader("User-Agent", "LunaPC/1.6.3(11741945)")
                                        .addHeader("Accept", "*/*")
                                        .addHeader("Connection", "keep-alive")
                                        .get()
                                }

                                6 -> {
                                    request = request
                                        .url("${url}/${playlistId[firstIndex1]}/tracks?offset=0&limit=100")
                                        .addHeader("Authorization", "Bearer ${spotifyToken}")
                                        .addHeader("Accept", "application/json")
                                        .addHeader("Connection", "keep-alive")
                                        .get()
                                }
                            }

                            var response: JSONObject
                            client.newCall(request.build()).execute().use { responses ->
                                response = JSON.parseObject(responses.body?.string())
                            }

                            when (selectedSourceApp.intValue) {
                                1 -> {
                                    val db = SQLiteDatabase.openDatabase(
                                        databaseFilePath.value,
                                        null,
                                        SQLiteDatabase.OPEN_READWRITE
                                    )
                                    db.execSQL("DELETE FROM ${sourceApp.songListSongInfoTableName}")
                                    db.execSQL("DELETE FROM ${sourceApp.songInfoTableName}")
                                    try {
                                        db.execSQL("DELETE FROM sqlite_sequence WHERE name = '${sourceApp.songListSongInfoTableName}'")
                                        db.execSQL("DELETE FROM sqlite_sequence WHERE name = '${sourceApp.songInfoTableName}'")
                                    } catch (_: Exception) {
                                    }
                                    db.execSQL("VACUUM")

                                    val playListDetailInfo =
                                        response.getJSONObject("playlist")
                                            .getJSONArray("tracks")
                                    playListDetailInfo.forEachIndexed { index, it ->
                                        val song = it as JSONObject
                                        val playlistId =
                                            response.getJSONObject("playlist").getString("id")
                                        val songId = song.getString("id")
                                        val songName = song.getString("name")
                                        val songArtistsBuilder = StringBuilder()
                                        song.getJSONArray("ar").forEach { it1 ->
                                            val artistsInfo = it1 as JSONObject
                                            songArtistsBuilder.append(artistsInfo.getString("name"))
                                            songArtistsBuilder.append("/")
                                        }
                                        songArtistsBuilder.deleteCharAt(songArtistsBuilder.length - 1)
                                        var songArtists = songArtistsBuilder.toString()
                                        var songAlbum =
                                            song.getJSONObject("al").getString("name")
                                        if (songArtists.isBlank() || songArtists == "null") {
                                            songArtists = context.getString(R.string.unknown)
                                        }
                                        if (songAlbum.isNullOrBlank() || songAlbum == "null") {
                                            songAlbum = context.getString(R.string.unknown)
                                        }
                                        db.execSQL(
                                            "INSERT INTO ${sourceApp.songListSongInfoTableName} (${sourceApp.songListSongInfoPlaylistId}, ${sourceApp.songListSongInfoSongId}, ${sourceApp.sortField}) VALUES (?, ?, ?)",
                                            arrayOf(
                                                playlistId,
                                                songId,
                                                index
                                            )
                                        )
                                        db.execSQL(
                                            "INSERT INTO ${sourceApp.songInfoTableName} (${sourceApp.songInfoSongId}, ${sourceApp.songInfoSongName}, ${sourceApp.songInfoSongArtist}, ${sourceApp.songInfoSongAlbum}) VALUES (?, ?, ?, ?)",
                                            arrayOf(
                                                songId,
                                                songName,
                                                songArtists,
                                                songAlbum
                                            )
                                        )
                                    }
                                    db.close()
                                }

                                2 -> {
                                    val db = SQLiteDatabase.openDatabase(
                                        databaseFilePath.value,
                                        null,
                                        SQLiteDatabase.OPEN_READWRITE
                                    )
                                    db.execSQL("DELETE FROM ${sourceApp.songListSongInfoTableName}")
                                    db.execSQL("DELETE FROM ${sourceApp.songInfoTableName}")
                                    try {
                                        db.execSQL("DELETE FROM sqlite_sequence WHERE name = '${sourceApp.songListSongInfoTableName}'")
                                        db.execSQL("DELETE FROM sqlite_sequence WHERE name = '${sourceApp.songInfoTableName}'")
                                    } catch (_: Exception) {
                                    }
                                    db.execSQL("VACUUM")

                                    val playListDetailInfo =
                                        response.getJSONObject("GetPlayList")
                                            .getJSONObject("data")
                                    playListDetailInfo.getJSONArray("songlist")
                                        .forEachIndexed { index, it ->
                                            val song = it as JSONObject
                                            val playlistId =
                                                playListDetailInfo.getJSONObject("dirinfo")
                                                    .getString("id")
                                            val songId = song.getString("id")
                                            val songName = song.getString("title")
                                            val songArtistsBuilder = StringBuilder()
                                            song.getJSONArray("singer").forEach { it1 ->
                                                val artistsInfo = it1 as JSONObject
                                                songArtistsBuilder.append(
                                                    artistsInfo.getString(
                                                        "title"
                                                    )
                                                )
                                                songArtistsBuilder.append("/")
                                            }
                                            songArtistsBuilder.deleteCharAt(songArtistsBuilder.length - 1)
                                            var songArtists = songArtistsBuilder.toString()
                                            var songAlbum =
                                                song.getJSONObject("album").getString("title")
                                            if (songArtists.isBlank() || songArtists == "null") {
                                                songArtists =
                                                    context.getString(R.string.unknown)
                                            }
                                            if (songAlbum.isNullOrBlank() || songAlbum == "null") {
                                                songAlbum = context.getString(R.string.unknown)
                                            }
                                            db.execSQL(
                                                "INSERT INTO ${sourceApp.songListSongInfoTableName} (${sourceApp.songListSongInfoPlaylistId}, ${sourceApp.songListSongInfoSongId}, ${sourceApp.sortField}) VALUES (?, ?, ?)",
                                                arrayOf(
                                                    playlistId,
                                                    songId,
                                                    index
                                                )
                                            )
                                            db.execSQL(
                                                "INSERT INTO ${sourceApp.songInfoTableName} (${sourceApp.songInfoSongId}, ${sourceApp.songInfoSongName}, ${sourceApp.songInfoSongArtist}, ${sourceApp.songInfoSongAlbum}) VALUES (?, ?, ?, ?)",
                                                arrayOf(
                                                    songId,
                                                    songName,
                                                    songArtists,
                                                    songAlbum
                                                )
                                            )
                                        }
                                    db.close()
                                }

                                3 -> {  // TODO 该接口只能获取歌单中未失效（变灰）的歌曲
                                    val db = SQLiteDatabase.openDatabase(
                                        databaseFilePath.value,
                                        null,
                                        SQLiteDatabase.OPEN_READWRITE
                                    )
                                    db.execSQL("DELETE FROM ${sourceApp.songListSongInfoTableName}")
                                    db.execSQL("DELETE FROM ${sourceApp.songInfoTableName}")
                                    try {
                                        db.execSQL("DELETE FROM sqlite_sequence WHERE name = '${sourceApp.songListSongInfoTableName}'")
                                        db.execSQL("DELETE FROM sqlite_sequence WHERE name = '${sourceApp.songInfoTableName}'")
                                    } catch (_: Exception) {
                                    }
                                    db.execSQL("VACUUM")
                                    var times = 1

                                    while (true) {
                                        try {
                                            if (response.getJSONObject("data")
                                                    .getJSONObject("info").size == 0
                                            ) {
                                                db.close()
                                                break
                                            }
                                        } catch (_: Exception) {
                                        }

                                        val playListDetailInfo =
                                            response.getJSONObject("data").getJSONArray("info")
                                        playListDetailInfo.forEachIndexed { index, it ->
                                            val song = it as JSONObject
                                            val playlistId = playlistId[firstIndex1]
                                            val songId = song.getString("audio_id")
                                            val songName = song.getString("name").substring(
                                                song.getString("name").indexOf(" - ") + 3
                                            )
                                            val songArtistsBuilder = StringBuilder()
                                            song.getJSONArray("singerinfo").forEach { it1 ->
                                                val artistsInfo = it1 as JSONObject
                                                songArtistsBuilder.append(
                                                    artistsInfo.getString(
                                                        "name"
                                                    )
                                                )
                                                songArtistsBuilder.append("/")
                                            }
                                            songArtistsBuilder.deleteCharAt(songArtistsBuilder.length - 1)
                                            var songArtists = songArtistsBuilder.toString()
                                            var songAlbum =
                                                song.getJSONObject("albuminfo")
                                                    .getString("name")
                                            if (songArtists.isBlank() || songArtists == "null") {
                                                songArtists =
                                                    context.getString(R.string.unknown)
                                            }
                                            if (songAlbum.isNullOrBlank() || songAlbum == "null") {
                                                songAlbum = context.getString(R.string.unknown)
                                            }
                                            db.execSQL(
                                                "INSERT INTO ${sourceApp.songListSongInfoTableName} (${sourceApp.songListSongInfoPlaylistId}, ${sourceApp.songListSongInfoSongId}, ${sourceApp.sortField}) VALUES (?, ?, ?)",
                                                arrayOf(
                                                    playlistId,
                                                    songId,
                                                    (times - 1) * 300 + index
                                                )
                                            )
                                            db.execSQL(
                                                "INSERT INTO ${sourceApp.songInfoTableName} (${sourceApp.songInfoSongId}, ${sourceApp.songInfoSongName}, ${sourceApp.songInfoSongArtist}, ${sourceApp.songInfoSongAlbum}) VALUES (?, ?, ?, ?)",
                                                arrayOf(
                                                    songId,
                                                    songName,
                                                    songArtists,
                                                    songAlbum
                                                )
                                            )
                                        }
                                        val cursor = db.rawQuery(
                                            "SELECT COUNT(*) FROM ${sourceApp.songListSongInfoTableName} WHERE ${sourceApp.songListSongInfoPlaylistId} = ?",
                                            arrayOf(playlistId[firstIndex1])
                                        )
                                        cursor.moveToFirst()
                                        val dbPlaylistSongNum = cursor.getInt(0)
                                        if (dbPlaylistSongNum ==
                                            response.getJSONObject("data").getInteger("count")
                                        ) {
                                            cursor.close()
                                            db.close()
                                            break
                                        } else {
                                            cursor.close()
                                            val getParams =
                                                """appid=1005&area_code=1&clientver=12189&global_collection_id=${playlistId[firstIndex1]}&mode=1&module=CloudMusic&need_sort=1&page=${++times}&pagesize=300&type=0"""
                                            val signature = Tools().md5(
                                                input = "OIlwieks28dk2k092lksi2UIkp${
                                                    getParams.replace(
                                                        "&",
                                                        ""
                                                    )
                                                }OIlwieks28dk2k092lksi2UIkp"
                                            )
                                            request = Request.Builder()
                                                .url("${url}?signature=${signature}&${getParams}")
                                                .addHeader("Host", "gateway.kugou.com")
                                                .addHeader(
                                                    "clienttime",
                                                    "${System.currentTimeMillis() / 1000}"
                                                )
                                                .addHeader(
                                                    "kg-clienttimems",
                                                    "${System.currentTimeMillis()}"
                                                )
                                                .addHeader(
                                                    "user-agent",
                                                    "Android13-AndroidPhone-12189-130-0-playlist-wifi"
                                                )
                                                .addHeader("kg-rc", "1")
                                                .addHeader("Connection", "Keep-Alive")
                                                .get()
                                            client.newCall(request.build()).execute()
                                                .use { responses ->
                                                    response =
                                                        JSON.parseObject(responses.body?.string())
                                                }
                                        }
                                    }
                                }

                                4 -> {
                                    val db = SQLiteDatabase.openDatabase(
                                        databaseFilePath.value,
                                        null,
                                        SQLiteDatabase.OPEN_READWRITE
                                    )
                                    db.execSQL("DELETE FROM ${sourceApp.songInfoTableName}")
                                    try {
                                        db.execSQL("DELETE FROM sqlite_sequence WHERE name = '${sourceApp.songInfoTableName}'")
                                    } catch (_: Exception) {
                                    }
                                    db.execSQL("VACUUM")
                                    var times = 1

                                    while (true) {
                                        val playListDetailInfo =
                                            response.getJSONObject("data")
                                                .getJSONArray("musicList")
                                        if (playListDetailInfo.size == 0) {
                                            db.close()
                                            break
                                        }
                                        playListDetailInfo.forEachIndexed { index, it ->
                                            val song = it as JSONObject
                                            val playlistId =
                                                response.getJSONObject("data").getString("id")
                                            val songId = song.getString("rid")
                                            val songName = song.getString("name")
                                            var songArtists =
                                                song.getString("artist").replace("&", "/")
                                            var songAlbum = song.getString("album")
                                            if (songArtists.isBlank() || songArtists == "null") {
                                                songArtists =
                                                    context.getString(R.string.unknown)
                                            }
                                            if (songAlbum.isNullOrBlank() || songAlbum == "null") {
                                                songAlbum = context.getString(R.string.unknown)
                                            }
                                            db.execSQL(
                                                "INSERT INTO ${sourceApp.songListSongInfoTableName} (${sourceApp.songListSongInfoSongId}, ${sourceApp.songListSongInfoPlaylistId}, ${sourceApp.sortField}, ${sourceApp.songInfoSongName}, ${sourceApp.songInfoSongArtist}, ${sourceApp.songInfoSongAlbum}) VALUES (?, ?, ?, ?, ?, ?)",
                                                arrayOf(
                                                    songId,
                                                    playlistId,
                                                    (times - 1) * 50 + index,
                                                    songName,
                                                    songArtists,
                                                    songAlbum
                                                )
                                            )
                                        }
                                        val cursor = db.rawQuery(
                                            "SELECT COUNT(*) FROM ${sourceApp.songListSongInfoTableName} WHERE ${sourceApp.songListSongInfoPlaylistId} = ?",
                                            arrayOf(playlistId[firstIndex1])
                                        )
                                        cursor.moveToFirst()
                                        val dbPlaylistSongNum = cursor.getInt(0)
                                        if (dbPlaylistSongNum ==
                                            response.getJSONObject("data").getInteger("total")
                                        ) {
                                            cursor.close()
                                            db.close()
                                            break
                                        } else {
                                            cursor.close()
                                            val getParams =
                                                """pid=${playlistId[firstIndex1]}&pn=${++times}&rn=50&httpsStatus=1&plat=web_www"""
                                            kuwoSecret?.let {
                                                request = Request.Builder()
                                                    .url("${url}?${getParams}")
                                                    .addHeader("Cookie", cookie.value)
                                                    .addHeader(
                                                        "Referer",
                                                        "https://kuwo.cn/playlist_detail/${playlistId[firstIndex1]}"
                                                    )
                                                    .addHeader("Secret", it)
                                                    .addHeader(
                                                        "User-Agent",
                                                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0"
                                                    )
                                                    .addHeader("Connection", "Keep-Alive")
                                                    .addHeader(
                                                        "Accept",
                                                        "application/json, text/plain, */*"
                                                    )
                                                    .get()
                                                client.newCall(request.build()).execute()
                                                    .use { responses ->
                                                        response =
                                                            JSON.parseObject(responses.body?.string())
                                                    }
                                            }
                                        }
                                    }
                                }

                                5 -> {
                                    val db = SQLiteDatabase.openDatabase(
                                        databaseFilePath.value,
                                        null,
                                        SQLiteDatabase.OPEN_READWRITE
                                    )
                                    db.execSQL("DELETE FROM ${sourceApp.songListSongInfoTableName}")
                                    db.execSQL("DELETE FROM ${sourceApp.songInfoTableName}")
                                    try {
                                        db.execSQL("DELETE FROM sqlite_sequence WHERE name = '${sourceApp.songListSongInfoTableName}'")
                                        db.execSQL("DELETE FROM sqlite_sequence WHERE name = '${sourceApp.songInfoTableName}'")
                                    } catch (_: Exception) {
                                    }
                                    db.execSQL("VACUUM")

                                    val playListDetailInfo = response.getJSONObject("playlist")
                                    response.getJSONArray("media_resources")
                                        .forEachIndexed { index, it ->
                                            val song =
                                                (it as JSONObject).getJSONObject("entity")
                                                    .getJSONObject("track_wrapper")
                                                    .getJSONObject("track")
                                            val playlistId = playListDetailInfo.getString("id")
                                            val songId = song.getString("id")
                                            val songName = song.getString("name")
                                            val songArtistsBuilder = StringBuilder()
                                            song.getJSONArray("artists").forEach { it1 ->
                                                val artistsInfo = it1 as JSONObject
                                                songArtistsBuilder.append(
                                                    artistsInfo.getString(
                                                        "name"
                                                    )
                                                )
                                                songArtistsBuilder.append("/")
                                            }
                                            songArtistsBuilder.deleteCharAt(songArtistsBuilder.length - 1)
                                            var songArtists = songArtistsBuilder.toString()
                                            var songAlbum =
                                                song.getJSONObject("album").getString("name")
                                            if (songArtists.isBlank() || songArtists == "null") {
                                                songArtists =
                                                    context.getString(R.string.unknown)
                                            }
                                            if (songAlbum.isNullOrBlank() || songAlbum == "null") {
                                                songAlbum = context.getString(R.string.unknown)
                                            }
                                            db.execSQL(
                                                "INSERT INTO ${sourceApp.songListSongInfoTableName} (${sourceApp.songListSongInfoPlaylistId}, ${sourceApp.songListSongInfoSongId}, ${sourceApp.sortField}) VALUES (?, ?, ?)",
                                                arrayOf(
                                                    playlistId,
                                                    songId,
                                                    index
                                                )
                                            )
                                            db.execSQL(
                                                "INSERT INTO ${sourceApp.songInfoTableName} (${sourceApp.songInfoSongId}, ${sourceApp.songInfoSongName}, ${sourceApp.songInfoSongArtist}, ${sourceApp.songInfoSongAlbum}) VALUES (?, ?, ?, ?)",
                                                arrayOf(
                                                    songId,
                                                    songName,
                                                    songArtists,
                                                    songAlbum
                                                )
                                            )
                                        }
                                    db.close()
                                }

                                6 -> {
                                    val db = SQLiteDatabase.openDatabase(
                                        databaseFilePath.value,
                                        null,
                                        SQLiteDatabase.OPEN_READWRITE
                                    )
                                    db.execSQL("DELETE FROM ${sourceApp.songListSongInfoTableName}")
                                    db.execSQL("DELETE FROM ${sourceApp.songInfoTableName}")
                                    try {
                                        db.execSQL("DELETE FROM sqlite_sequence WHERE name = '${sourceApp.songListSongInfoTableName}'")
                                        db.execSQL("DELETE FROM sqlite_sequence WHERE name = '${sourceApp.songInfoTableName}'")
                                    } catch (_: Exception) {
                                    }
                                    db.execSQL("VACUUM")
                                    var times = 1
                                    var abnormalSongNum = 0

                                    while (true) {
                                        if (response.getInteger("total") == 0) {
                                            db.close()
                                            break
                                        }
                                        val playListDetailInfo =
                                            response.getJSONArray("items")

                                        playListDetailInfo.forEachIndexed { index, it ->
                                            val song = (it as JSONObject).getJSONObject("track")
                                            if (song.get("id") == null || song.get("id") == "null") {
                                                abnormalSongNum++
                                                return@forEachIndexed
                                            }
                                            val playlistId = playlistId[firstIndex1]
                                            val songId = song.getString("id")
                                            val songName = song.getString("name")
                                            val songArtistsBuilder = StringBuilder()
                                            song.getJSONArray("artists").forEach { it1 ->
                                                val artistsInfo = it1 as JSONObject
                                                songArtistsBuilder.append(
                                                    artistsInfo.getString(
                                                        "name"
                                                    )
                                                )
                                                songArtistsBuilder.append("/")
                                            }
                                            songArtistsBuilder.deleteCharAt(songArtistsBuilder.length - 1)
                                            var songArtists = songArtistsBuilder.toString()
                                            var songAlbum =
                                                song.getJSONObject("album").getString("name")

                                            if (songArtists.isBlank() || songArtists == "null") {
                                                songArtists =
                                                    context.getString(R.string.unknown)
                                            }
                                            if (songAlbum.isNullOrBlank() || songAlbum == "null") {
                                                songAlbum = context.getString(R.string.unknown)
                                            }

                                            db.execSQL(
                                                "INSERT INTO ${sourceApp.songListSongInfoTableName} (${sourceApp.songListSongInfoPlaylistId}, ${sourceApp.songListSongInfoSongId}, ${sourceApp.sortField}) VALUES (?, ?, ?)",
                                                arrayOf(
                                                    playlistId,
                                                    songId,
                                                    (times - 1) * 100 + index
                                                )
                                            )
                                            db.execSQL(
                                                "INSERT INTO ${sourceApp.songInfoTableName} (${sourceApp.songInfoSongId}, ${sourceApp.songInfoSongName}, ${sourceApp.songInfoSongArtist}, ${sourceApp.songInfoSongAlbum}) VALUES (?, ?, ?, ?)",
                                                arrayOf(
                                                    songId,
                                                    songName,
                                                    songArtists,
                                                    songAlbum
                                                )
                                            )
                                        }
                                        val cursor = db.rawQuery(
                                            "SELECT COUNT(*) FROM ${sourceApp.songListSongInfoTableName} WHERE ${sourceApp.songListSongInfoPlaylistId} = ?",
                                            arrayOf(playlistId[firstIndex1])
                                        )
                                        cursor.moveToFirst()
                                        val dbPlaylistSongNum = cursor.getInt(0)
                                        if (dbPlaylistSongNum + abnormalSongNum ==
                                            response.getInteger("total")
                                        ) {
                                            cursor.close()
                                            db.close()
                                            break
                                        } else {
                                            times++
                                            cursor.close()
                                            request = Request.Builder()
                                                .url(response.getString("next"))
                                                .addHeader(
                                                    "Authorization",
                                                    "Bearer ${spotifyToken}"
                                                )
                                                .addHeader("Accept", "application/json")
                                                .addHeader("Connection", "keep-alive")
                                                .get()
                                            client.newCall(request.build()).execute()
                                                .use { responses ->
                                                    response =
                                                        JSON.parseObject(responses.body?.string())
                                                }
                                        }
                                    }
                                    if (abnormalSongNum != 0) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.spotify_abnormal_song_num)
                                                    .replace("#", abnormalSongNum.toString()),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            }
                            delay(500L)
                            showNumberProgressBar.value = true
                            showLoadingProgressBar.value = false
                        } catch (e: Exception) {
                            errorDialogTitle.value =
                                context.getString(R.string.error_while_getting_data_dialog_title)
                            errorDialogContent.value =
                                "- ${context.getString(R.string.get_playlist_failed)}\n  - $e\n"
                            errorDialogCustomAction.value = {}
                            showErrorDialog.value = true
                            showLoadingProgressBar.value = false
                            return@launch
                        }
                    }
                }
            }

            showNumberProgressBar.value = true
            val music3InfoList = if (useCustomResultFile.value) {
                val db = SQLiteDatabase.openOrCreateDatabase(File(resultFilePath), null)
                val musicInfoList = mutableListOf<MusicInfo>()
                db.rawQuery(
                    "SELECT song, artist, albumArtist, album, absolutePath, id FROM music",
                    null
                )
                    .use { cursor ->
                        while (cursor.moveToNext()) {
                            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                            val song = cursor.getString(cursor.getColumnIndexOrThrow("song"))
                            val artist = cursor.getString(cursor.getColumnIndexOrThrow("artist"))
                            val album = cursor.getString(cursor.getColumnIndexOrThrow("album"))
                            val absolutePath =
                                cursor.getString(cursor.getColumnIndexOrThrow("absolutePath"))
                            val albumArtist =
                                cursor.getString(cursor.getColumnIndexOrThrow("albumArtist"))
                            val musicInfo = MusicInfo(
                                id = id,
                                song = song,
                                artist = artist,
                                album = album,
                                releaseYear = null,
                                trackNumber = null,
                                albumArtist = albumArtist,
                                genre = null,
                                absolutePath = absolutePath,
                                lyricist = null,
                                composer = null,
                                arranger = null,
                                modifyTime = null,
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

            val db = SQLiteDatabase.openOrCreateDatabase(File(databaseFilePath.value), null)
            val cursor = db.rawQuery(
                "SELECT ${sourceApp.songListSongInfoSongId} FROM ${sourceApp.songListSongInfoTableName} WHERE ${sourceApp.songListSongInfoPlaylistId} = ? ORDER BY ${sourceApp.sortField}",
                arrayOf(
                    playlistId[firstIndex1]
                )
            )
            val totalNum = cursor.count
            if (totalNum != playlistSum[firstIndex1]) {
                if (totalNum == 0) {
                    errorDialogTitle.value =
                        context.getString(R.string.error)
                    errorDialogContent.value =
                        "- ${playlistName[firstIndex1]}:\n  - ${
                            context.getString(R.string.no_song_in_playlist).replace(
                                "#",
                                when (selectedSourceApp.intValue) {
                                    1 -> context.getString(R.string.source_netease_cloud_music)
                                    2 -> context.getString(R.string.source_qq_music)
                                    3 -> context.getString(R.string.source_kugou_music)
                                    4 -> context.getString(R.string.source_kuwo_music)
                                    5 -> context.getString(R.string.source_luna_music)
                                    6 -> context.getString(R.string.source_spotify)
                                    7 -> context.getString(R.string.tune_my_music)
                                    else -> ""
                                }
                            )
                        }**${context.getString(R.string.will_skip_this_playlist)}**\n"
                    errorDialogCustomAction.value = {
                        saveCurrentConvertResult(
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
                    }
                    showErrorDialog.value = true
                    showNumberProgressBar.value = false
                    showLoadingProgressBar.value = false
                    return@launch
                }
                if (directlyStart) {
                    playlistSum[firstIndex1] = totalNum
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(
//                            context,
//                            "${context.getString(R.string.continue_button_name)}…",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
                } else {
                    errorDialogContent.value =
                        "- ${context.getString(R.string.playlist_song_num_not_match)}\n  - ${
                            context.getString(
                                R.string.playlist_song_num_not_match_detail
                            ).replace("#1", playlistSum[firstIndex1].toString())
                                .replace("#2", totalNum.toString()).replace("#n", "\n")
                                .replace("#b", " ")
                        }\n"
                    errorDialogContent.value +=
                        if (totalNum > 1000) {
                            "- ${context.getString(R.string.solution)}\n  - ${
                                context.getString(
                                    R.string.playlist_song_num_not_match_solution
                                )
                            }\n"
                        } else if (totalNum == 20) {
                            "- ${context.getString(R.string.solution)}\n  - ${
                                context.getString(
                                    R.string.playlist_song_num_not_match_solution1
                                )
                            }\n"
                        } else {
                            "- ${context.getString(R.string.solution)}\n  - ${
                                context.getString(
                                    R.string.playlist_song_num_not_match_solution2
                                )
                            }\n"
                        }
                    errorDialogContent.value +=
                        "- ${context.getString(R.string.optional_operation)}\n  - ${
                            context.getString(
                                R.string.skip_operation_detail
                            )
                        }\n  - ${
                            context.getString(
                                R.string.continue_operation_detail
                            )
                        }\n"
                    showSongNumMismatchDialog.value = true
                    showNumberProgressBar.value = false
                    showLoadingProgressBar.value = false
                    return@launch
                }
            }
            while (cursor.moveToNext()) {
                val trackId =
                    cursor.getString(cursor.getColumnIndexOrThrow(sourceApp.songListSongInfoSongId))
                val songInfoCursor = db.rawQuery(
                    "SELECT ${sourceApp.songInfoSongName} , ${sourceApp.songInfoSongArtist} , ${sourceApp.songInfoSongAlbum} FROM ${sourceApp.songInfoTableName} WHERE ${sourceApp.songInfoSongId} = ?",
                    arrayOf(
                        trackId
                    )
                )
                songInfoCursor.moveToFirst()
                songName =
                    songInfoCursor.getString(songInfoCursor.getColumnIndexOrThrow(sourceApp.songInfoSongName))
                songArtist =
                    songInfoCursor.getString(songInfoCursor.getColumnIndexOrThrow(sourceApp.songInfoSongArtist))
                if (sourceApp.sourceEng == "CloudMusic" && selectedMethod.intValue == 0) {
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
                songArtist = songArtist.replace("\\s?&\\s?".toRegex(), "/").replace("、", "/")
                songAlbum =
                    songInfoCursor.getString(songInfoCursor.getColumnIndexOrThrow(sourceApp.songInfoSongAlbum))

                if (selectedMatchingMode.intValue == 1) {
                    val songSimilarityArray = mutableMapOf<Int, Double>()
                    val artistSimilarityArray = mutableMapOf<Int, Double>()
                    val albumSimilarityArray = mutableMapOf<Int, Double>()

                    var songArtistMaxSimilarity = 0.0
                    var songAlbumMaxSimilarity = 0.0
//                    var songArtistMaxKey = 0
//                    var songAlbumMaxKey = 0

                    val songThread = async {
                        //歌曲名相似度列表
                        if (enableBracketRemoval.value) for (k in music3InfoList.indices) {
                            songSimilarityArray[k] = Tools().similarityRatio(
                                songName.replace(
                                    "(?i)\\s?\\((?!inst|[^()]*\\sver)[^)]*\\)\\s?".toRegex(),
                                    ""
                                ).lowercase(),
                                music3InfoList[k].song
                                    .replace(
                                        "(?i)\\s?\\((?!inst|[^()]*\\sver)[^)]*\\)\\s?".toRegex(),
                                        ""
                                    ).lowercase()
                            )
                        } else for (k in music3InfoList.indices) {
                            songSimilarityArray[k] = Tools().similarityRatio(
                                songName.lowercase(), music3InfoList[k].song.lowercase()
                            )
                        }
                    }

                    val artistThread = async {
                        //歌手名相似度列表
                        if (enableArtistNameMatch.value) {
                            if (enableBracketRemoval.value) for (k in music3InfoList.indices) {
                                artistSimilarityArray[k] =
                                    Tools().similarityRatio(
                                        songArtist.replace(
                                            "(?i)\\s?\\((?!inst|[^()]*\\sver)[^)]*\\)\\s?".toRegex(),
                                            ""
                                        ).lowercase(),
                                        music3InfoList[k].artist
                                            .replace(
                                                "(?i)\\s?\\((?!inst|[^()]*\\sver)[^)]*\\)\\s?".toRegex(),
                                                ""
                                            ).lowercase()
                                    )
                            } else for (k in music3InfoList.indices) {
                                artistSimilarityArray[k] =
                                    Tools().similarityRatio(
                                        songArtist.lowercase(),
                                        music3InfoList[k].artist.lowercase()
                                    )
                            }
                            songArtistMaxSimilarity =
                                Tools().getMaxValueIntDouble(artistSimilarityArray)?.value!! //获取相似度的最大值
//                            songArtistMaxKey =
//                                Tools().getMaxValueIntDouble(artistSimilarityArray)?.key!! //获取相似度的最大值对应的歌手名的位置
                        } else {
                            songArtistMaxSimilarity = 1.0
                        }
                    }

                    val albumThread = async {
                        //专辑名相似度列表
                        if (enableAlbumNameMatch.value) {
                            if (enableBracketRemoval.value) for (k in music3InfoList.indices) {
                                albumSimilarityArray[k] =
                                    Tools().similarityRatio(
                                        songAlbum.replace(
                                            "(?i)\\s?\\((?!inst|[^()]*\\sver)[^)]*\\)\\s?".toRegex(),
                                            ""
                                        ).lowercase(),
                                        music3InfoList[k].album
                                            .replace(
                                                "(?i)\\s?\\((?!inst|[^()]*\\sver)[^)]*\\)\\s?".toRegex(),
                                                ""
                                            )
                                            .lowercase()
                                    )
                            } else for (k in music3InfoList.indices) {
                                albumSimilarityArray[k] =
                                    Tools().similarityRatio(
                                        songAlbum.lowercase(),
                                        music3InfoList[k].album.lowercase()
                                    )
                            }
                            songAlbumMaxSimilarity =
                                Tools().getMaxValueIntDouble(albumSimilarityArray)?.value!! //获取相似度的最大值
//                            songAlbumMaxKey =
//                                Tools().getMaxValueIntDouble(albumSimilarityArray)?.key!! //获取相似度的最大值对应的专辑名的位置
                        } else {
                            songAlbumMaxSimilarity = 1.0
                        }
                    }  //TODO 待优化，直接使用歌名对应的Key来确定歌曲？
                    songThread.await()
                    artistThread.await()
                    albumThread.await()

                    val songNameMaxSimilarity =
                        Tools().getMaxValueIntDouble(songSimilarityArray)?.value!!
                    val songNameMaxKey = Tools().getMaxValueIntDouble(songSimilarityArray)?.key!!

                    val autoSuccess =
                        (songNameMaxSimilarity >= similarity.floatValue / 100
                                && songArtistMaxSimilarity >= similarity.floatValue / 100
                                && songAlbumMaxSimilarity >= similarity.floatValue / 100)

                    val songConvertResult = music3InfoList[songNameMaxKey]
                    convertResultMap[num++] =
                        songConvertResult.albumArtist?.let {
                            arrayOf(
                                if (autoSuccess) "0"
                                else "1",  //是否自动匹配成功
                                songConvertResult.song,  //本地音乐歌曲名
                                songName,  //云音乐歌曲名
                                songConvertResult.artist,  //本地音乐歌手名
                                songArtist,  //云音乐歌手名
                                songConvertResult.album,  //本地音乐专辑名
                                songAlbum,  //云音乐专辑名
                                songConvertResult.absolutePath,  //本地音乐绝对路径
                                it
                            )
                        }!!

                } else if (selectedMatchingMode.intValue == 2) {
                    val similarityArray = mutableMapOf<Int, Double>()
                    var songInfo = songName
                    if (enableArtistNameMatch.value)
                        songInfo = "$songInfo$songArtist"
                    if (enableAlbumNameMatch.value)
                        songInfo = "$songInfo$songAlbum"

                    if (enableBracketRemoval.value)
                        for (k in music3InfoList.indices) {
                            similarityArray[k] = Tools().similarityRatio(
                                songInfo.replace(
                                    "(?i)\\s?\\((?!inst|[^()]*\\sver)[^)]*\\)\\s?".toRegex(),
                                    ""
                                ).lowercase(),
                                "${music3InfoList[k].song}${
                                    if (enableAlbumNameMatch.value) music3InfoList[k].artist else ""
                                }${if (enableAlbumNameMatch.value) music3InfoList[k].album else ""}"
                                    .replace(
                                        "(?i)\\s?\\((?!inst|[^()]*\\sver)[^)]*\\)\\s?".toRegex(),
                                        ""
                                    ).lowercase()
                            )
                        }
                    else
                        for (k in music3InfoList.indices) {
                            similarityArray[k] = Tools().similarityRatio(
                                songInfo.lowercase(),
                                "${music3InfoList[k].song}${
                                    if (enableAlbumNameMatch.value) music3InfoList[k].artist else ""
                                }${if (enableAlbumNameMatch.value) music3InfoList[k].album else ""}".lowercase()
                            )
                        }
                    val maxSimilarity = Tools().getMaxValueIntDouble(similarityArray)
                    val songMaxSimilarity = maxSimilarity?.value!!
                    val songMaxKey = maxSimilarity.key

                    val autoSuccess = songMaxSimilarity >= similarity.floatValue / 100

                    val songConvertResult = music3InfoList[songMaxKey]

                    convertResultMap[num++] =
                        songConvertResult.albumArtist?.let {
                            arrayOf(
                                if (autoSuccess) "0"
                                else "1",  //是否自动匹配成功
                                songConvertResult.song,  //本地音乐歌曲名
                                songName,  //云音乐歌曲名
                                songConvertResult.artist,  //本地音乐歌手名
                                songArtist,  //云音乐歌手名
                                songConvertResult.album,  //本地音乐专辑名
                                songAlbum,  //云音乐专辑名
                                songConvertResult.absolutePath,  //本地音乐绝对路径
                                it,  //
                            )
                        }!!
                }
                songInfoCursor.close()
                if (num % 10 == 0)
                    numberProgress.floatValue = num / totalNum.toFloat()
            }
            cursor.close()
            db.close()
            numberProgress.floatValue = 1.0f
            convertResult.putAll(convertResultMap)
            lifecycleOwner.lifecycleScope.launch {
                delay(650L)
                showNumberProgressBar.value = false
            }
            MyVibrationEffect(
                context,
                (context as MainActivity).enableHaptic.value,
                context.hapticStrength.intValue
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
                    arrayOf(
                        "%${inputSearchWords.value}%",
                        "%${inputSearchWords.value}%",
                        "%${inputSearchWords.value}%"
                    )
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                        val song = cursor.getString(cursor.getColumnIndexOrThrow("song"))
                        val artist = cursor.getString(cursor.getColumnIndexOrThrow("artist"))
                        val album = cursor.getString(cursor.getColumnIndexOrThrow("album"))
                        val absolutePath =
                            cursor.getString(cursor.getColumnIndexOrThrow("absolutePath"))
                        val musicInfo = MusicInfo(
                            id = id,
                            song = song,
                            artist = artist,
                            album = album,
                            releaseYear = null,
                            trackNumber = null,
                            albumArtist = null,
                            genre = null,
                            absolutePath = absolutePath,
                            lyricist = null,
                            composer = null,
                            arranger = null,
                            modifyTime = null,
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
                (context as MainActivity).enableHaptic.value,
                context.hapticStrength.intValue
            ).done()
        }
    }

    fun saveModificationSong(songPosition: Int, songId: Int) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val songInfo = if (useCustomResultFile.value) {
                val db = SQLiteDatabase.openOrCreateDatabase(File(resultFilePath), null)
                val cursor = db.rawQuery(
                    "SELECT song, artist, album, absolutePath, albumArtist FROM music WHERE id = ?",
                    arrayOf(songId.toString())
                )
                cursor.moveToFirst()
                val song = cursor.getString(cursor.getColumnIndexOrThrow("song"))
                val artist = cursor.getString(cursor.getColumnIndexOrThrow("artist"))
                val album = cursor.getString(cursor.getColumnIndexOrThrow("album"))
                val albumArtist = cursor.getString(cursor.getColumnIndexOrThrow("albumArtist"))
                val absolutePath =
                    cursor.getString(cursor.getColumnIndexOrThrow("absolutePath"))
                cursor.close()
                db.close()
                MusicInfo(
                    id = songId,
                    song = song,
                    artist = artist,
                    album = album,
                    releaseYear = null,
                    trackNumber = null,
                    albumArtist = albumArtist,
                    genre = null,
                    absolutePath = absolutePath,
                    lyricist = null,
                    composer = null,
                    arranger = null,
                    modifyTime = null,
                )
            } else
                db.musicDao().getMusicById(songId)
            convertResult[songPosition]?.set(0, "2")
            convertResult[songPosition]?.set(1, songInfo.song)
            convertResult[songPosition]?.set(3, songInfo.artist)
            convertResult[songPosition]?.set(5, songInfo.album)
            convertResult[songPosition]?.set(7, songInfo.absolutePath)
            songInfo.albumArtist?.let { convertResult[songPosition]?.set(8, it) }
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
    var resultFileLocation = mutableStateListOf<String>()
    fun saveCurrentConvertResult(
        saveSuccessSongs: Boolean,
        saveCautionSongs: Boolean,
        saveManualSongs: Boolean,
        fileName: String
    ) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val firstIndex1 = playlistEnabled.indexOfFirst { it == 1 }
            if (!convertResult.isEmpty()) {
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
                try {
                    val invalidChars = arrayOf("\"", "*", "<", ">", "?", "\\", "/", "|", ":")
                    var correctFileName = fileName
                    invalidChars.forEach { ch ->
                        correctFileName = correctFileName.replace(ch, "")
                    }
                    val file = File(
                        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${
                            context.getString(
                                R.string.app_name
                            )
                        }/${
                            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        }",
                        correctFileName
                    )
                    if (file.exists())
                        file.delete()
                    if (file.parentFile?.exists() == false)
                        file.parentFile?.mkdirs()
                    val fileWriter = FileWriter(file, true)
                    when (selectedTargetApp.intValue) {
                        3 -> fileWriter.write(
                            onlineToZune(
                                convertResult,
                                correctFileName,
                                saveSuccessSongs,
                                saveCautionSongs,
                                saveManualSongs
                            )
                        )

                        4 -> {
                            for (i in 0 until convertResult.size) {
                                if (convertResult[i] == null)
                                    continue
                                if (convertResult[i]!![0] == "0" && saveSuccessSongs) {
                                    fileWriter.write(
                                        "${
                                            convertResult[i]!![7].replace(
                                                localMusicPath.value,
                                                webdavPath.value
                                            )
                                        }?username=${webdavUsername.value}\n"
                                    )
                                    continue
                                }
                                if (convertResult[i]!![0] == "1" && saveCautionSongs) {
                                    fileWriter.write(
                                        "${
                                            convertResult[i]!![7].replace(
                                                localMusicPath.value,
                                                webdavPath.value
                                            )
                                        }?username=${webdavUsername.value}\n"
                                    )
                                    continue
                                }
                                if (convertResult[i]!![0] == "2" && saveManualSongs) {
                                    fileWriter.write(
                                        "${
                                            convertResult[i]!![7].replace(
                                                localMusicPath.value,
                                                webdavPath.value
                                            )
                                        }?username=${webdavUsername.value}\n"
                                    )
                                    continue
                                }
                            }
                        }

                        else -> {
                            for (i in 0 until convertResult.size) {
                                if (convertResult[i] == null)
                                    continue
                                if (convertResult[i]!![0] == "0" && saveSuccessSongs) {
                                    fileWriter.write("${convertResult[i]!![7]}\n")
                                    continue
                                }
                                if (convertResult[i]!![0] == "1" && saveCautionSongs) {
                                    fileWriter.write("${convertResult[i]!![7]}\n")
                                    continue
                                }
                                if (convertResult[i]!![0] == "2" && saveManualSongs) {
                                    fileWriter.write("${convertResult[i]!![7]}\n")
                                    continue
                                }
                            }
                        }
                    }
                    fileWriter.close()
                    resultFileLocation.add(file.absolutePath)
                } catch (e: Exception) {
                    showDialogProgressBar.value = false
                    errorDialogTitle.value =
                        context.getString(R.string.error_while_saving_result_dialog_title)
                    errorDialogContent.value =
                        "- ${context.getString(R.string.saving_convert_result_failed)}\n  - $e"
                    errorDialogCustomAction.value = {}
                    showErrorDialog.value = true
                    return@launch
                }

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
                    (context as MainActivity).enableHaptic.value,
                    context.hapticStrength.intValue
                ).done()
                showSaveDialog.value = false
                playlistEnabled[firstIndex1] = 2
            } else {
                playlistEnabled[firstIndex1] = 3
            }
            convertResult.clear()
            if (playlistEnabled.count { it == 1 } == 0) {
                currentPage.intValue = 3
                File("${resultFilePath}-journal").delete()
            }
        }
    }

    fun launchLocalPlayer() {
        val targetAppList = listOf(
            arrayOf("com.salt.music", "com.salt.music.ui.MainActivity"),
            arrayOf("remix.myplayer", "remix.myplayer.ui.activity.MainActivity"),
            arrayOf("com.maxmpz.audioplayer", "com.maxmpz.audioplayer.MainActivity"),
            arrayOf("", ""),
            arrayOf("com.xuncorp.qinalt.music", "com.xuncorp.qinalt.music.MainActivity"),
        )
        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName(
                targetAppList[selectedTargetApp.intValue][0],
                targetAppList[selectedTargetApp.intValue][1]
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.other_app_not_installed).replace(
                    "#", when (selectedTargetApp.intValue) {
                        0 -> "Salt Player"
                        1 -> "APlayer"
                        2 -> "Poweramp"
                        3 -> "Microsoft Zune"
                        4 -> "Qinalt"
                        else -> ""
                    }
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun copyFolderPathToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = newPlainText(
            "PLNBTargetFolder",
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath}/${
                context.getString(
                    R.string.app_name
                )
            }"
        )
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
    }

    fun autoFillCookie(): String {
        if (selectedSourceApp.intValue != 3) {
            if (System.currentTimeMillis() - lastLoginTimestamp.longValue > 1209600000) {
                return ""
            }
        } else {
            if (System.currentTimeMillis() - lastLoginTimestamp.longValue > 2592000000) {
                return ""
            }
        }
        if (selectedLoginMethod.intValue == 2) {
            val temp = when (selectedSourceApp.intValue) {
                1 -> "${
                    CookieManager.getInstance().getCookie("music.163.com")
                }; uid=${neteaseUserId.value}"

                2 -> CookieManager.getInstance().getCookie("y.qq.com")
                3 -> "KUGOU"
                4 -> CookieManager.getInstance().getCookie("kuwo.cn")
                5 -> lunaCookie.value
                6 -> spotifyUserId.value
                else -> ""
            }
            if (temp.isNullOrBlank()) {
                return ""
            }
            val cookieValid = when (selectedSourceApp.intValue) {
                1 -> {
                    temp.contains("\\bMUSIC_U=\\w+".toRegex()) &&
                            temp.contains("\\b__csrf=\\w+".toRegex()) &&
                            temp.contains("\\buid=\\d+".toRegex())
                }

                2 -> {
                    (temp.contains("\\buin=\\d+".toRegex())
                            ||
                            temp.contains("\\bwxuin=\\d+".toRegex()))
                            &&
                            temp.contains("\\bqm_keyst=\\w+".toRegex())
                }

                3 -> true
                4 -> temp.contains("\\bHm_Iuvt.*=\\w+".toRegex())
                5 -> {
                    temp.contains("\\bsessionid(_ss)?=\\w+".toRegex())
                }

                6 -> "\\w+".toRegex().matches(temp)

                else -> false
            }
            if (cookieValid) {
                if (selectedSourceApp.intValue != 3)
                    selectedLoginMethod.intValue = 1
                return temp
            }
        }
        return ""
    }

    suspend fun kugouActiveDevice(currentIp: String): Boolean {
        showDialogProgressBar.value = true
        val deviceId = Tools().md5(
            input = Settings.System.getString(
                context.contentResolver, Settings.Secure.ANDROID_ID
            )
        )
        val nonce =
            Tools().generateRandomString(
                length = 32,
                includeLowerCase = true,
                includeDigits = true
            )
        val time = System.currentTimeMillis() / 1000
        val url = "https://thirdsso.kugou.com/v2/device/activation"
        val client = OkHttpClient()
        val json =
            """{"device_id":"${deviceId}","client_ver":"133-62b8ece-20240223165034","pid":"203051","client_ip":"${currentIp}","apk_ver":"9845","nonce":"${nonce}","sp":"KG","userid":"anonymous","timestamp":${time},"token":"password"}"""
        val requestBody =
            json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .addHeader(
                "User-Agent",
                "Android13-androidCar-133-62b8ece-203051-0-UltimateSdk-wifi"
            )
            .addHeader(
                "signature",
                Tools().md5(input = json + "9046ad4ecae74a70aa750c1bb2307ae6")
            )
            .addHeader("Content-Type", "application/json; charset=UTF-8")
            .addHeader("Host", "thirdsso.kugou.com")
            .addHeader("Connection", "Keep-Alive")
            .addHeader("Accept-Encoding", "gzip")
            .post(requestBody)
        val response: JSONObject
        try {
            client.newCall(request.build()).execute().use { responses ->
                response = JSON.parseObject(responses.body?.string())
            }
            if (response.getInteger("error_code") != 0)
                throw Exception()
        } catch (e: Exception) {
            showDialogProgressBar.value = false
            showLoginDialog.value = false
            selectedLoginMethod.intValue = 2
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.failed_get_login_qr_code),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return false
        }
        return true
    }

    suspend fun kugouGetLoginQrCodeUrl(
        type: Int,
        currentIp: String
    ): SnapshotStateMap<String, String> {
        val result = SnapshotStateMap<String, String>()
        val deviceId = Tools().md5(
            input = Settings.System.getString(
                context.contentResolver, Settings.Secure.ANDROID_ID
            )
        )
        val url = when (type) {
            0 -> "https://thirdsso.kugou.com/v2/user/qrcode/get"
            1 -> "https://thirdsso.kugou.com/v2/user/gzh/qrcode/get"
            else -> ""
        }
        val nonce =
            Tools().generateRandomString(
                length = 32,
                includeLowerCase = true,
                includeDigits = true
            )
        val time = System.currentTimeMillis() / 1000
        val client = OkHttpClient()
        val json =
            """{"device_id":"${deviceId}","client_ver":"133-62b8ece-20240223165034","pid":"203051","client_ip":"${currentIp}","apk_ver":"9845","nonce":"${nonce}","sp":"KG","userid":"anonymous","timestamp":${time},"token":"password"}"""
        val requestBody =
            json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .addHeader(
                "User-Agent",
                "Android13-androidCar-133-62b8ece-203051-0-UltimateSdk-wifi"
            )
            .addHeader("Content-Type", "application/json; charset=UTF-8")
            .addHeader("Host", "thirdsso.kugou.com")
            .addHeader("Connection", "Keep-Alive")
            .addHeader("Accept-Encoding", "gzip")
            .addHeader(
                "signature",
                Tools().md5(input = json + "9046ad4ecae74a70aa750c1bb2307ae6")
            )
            .post(requestBody)
        val response: JSONObject
        try {
            client.newCall(request.build()).execute().use { responses ->
                response = JSON.parseObject(responses.body?.string())
            }
            if (response.getInteger("error_code") != 0)
                throw Exception()
            val qrCodeUrl = response.getJSONObject("data").getString("qrcode")
            val ticket = response.getJSONObject("data").getString("ticket")
            result["qrCodeUrl"] = qrCodeUrl
            result["ticket"] = ticket
            showDialogProgressBar.value = false
        } catch (e: Exception) {
            showDialogProgressBar.value = false
            showLoginDialog.value = false
            selectedLoginMethod.intValue = 2
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.failed_get_login_qr_code),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        return result
    }

    private var cancelLogin = mutableStateOf(false)
    fun stopGetLoginStatus() {
        cancelLogin.value = true
    }

    suspend fun kugouGetLoginStatus(
        type: Int,
        ticket: String,
        currentIp: String
    ): SnapshotStateMap<String, String> {
        val result = SnapshotStateMap<String, String>()
        val deviceId = Tools().md5(
            input = Settings.System.getString(
                context.contentResolver, Settings.Secure.ANDROID_ID
            )
        )
        val url = when (type) {
            0 -> "https://thirdsso.kugou.com/v2/user/qrcode/auth"
            1 -> "https://thirdsso.kugou.com/v2/user/gzh/qrcode/auth"
            else -> ""
        }
        delay(2000L)
        if (cancelLogin.value) {
            delay(1500L)
        }
        cancelLogin.value = false
        while (!cancelLogin.value) {
            val nonce =
                Tools().generateRandomString(
                    length = 32,
                    includeLowerCase = true,
                    includeDigits = true
                )
            val time = System.currentTimeMillis() / 1000
            val client = OkHttpClient()
            val json = when (type) {
                0 -> """{"ticket":"${ticket}","device_id":"${deviceId}","client_ver":"133-62b8ece-20240223165034","pid":"203051","client_ip":"${currentIp}","apk_ver":"9845","nonce":"${nonce}","sp":"KG","userid":"anonymous","timestamp":${time},"token":"password"}"""
                1 -> """{"ticket":"${ticket}","device_id":"${deviceId}","client_ver":"133-62b8ece-20240223165034","refresh":0,"pid":"203051","client_ip":"${currentIp}","apk_ver":"9845","nonce":"${nonce}","sp":"KG","userid":"anonymous","timestamp":${time},"token":"password"}"""
                else -> ""
            }
            val requestBody =
                json.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader(
                    "User-Agent",
                    "Android13-androidCar-133-62b8ece-203051-0-UltimateSdk-wifi"
                )
                .addHeader("Content-Type", "application/json; charset=UTF-8")
                .addHeader("Host", "thirdsso.kugou.com")
                .addHeader("Connection", "Keep-Alive")
                .addHeader("Accept-Encoding", "gzip")
                .addHeader(
                    "signature",
                    Tools().md5(input = json + "9046ad4ecae74a70aa750c1bb2307ae6")
                )
                .post(requestBody)
            val response: JSONObject
            client.newCall(request.build()).execute().use { responses ->
                response = JSON.parseObject(responses.body?.string())
            }
            try {
                val token = response.getJSONObject("data").getString("token")
                val userId = response.getJSONObject("data").getString("userid")
                if (token != null && token.isNotBlank() && userId != null && userId.isNotBlank()) {
                    dataStore.edit { settings ->
                        settings[DataStoreConstants.LAST_LOGIN_TIMESTAMP] =
                            System.currentTimeMillis()
                    }
                    result["token"] = token
                    result["userId"] = userId
                    return result
                }
            } catch (_: Exception) {
            }
            delay(1250L)
        }
        return result
    }

    fun convertLocalPlaylist(): String {
        val targetFile = File(
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${
                context.getString(
                    R.string.app_name
                )
            }/${
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            }",
            "${
                sourcePlaylistFileName.value.substring(
                    0,
                    sourcePlaylistFileName.value.lastIndexOf(".")
                )
            }.${
                when (selectedTargetApp.intValue) {
                    0 -> "txt"
                    1 -> "m3u"
                    2 -> "m3u8"
                    3 -> "zpl"
                    4 -> "txt"
                    else -> ""
                }
            }"
        )
        if (targetFile.exists())
            targetFile.delete()
        if (targetFile.parentFile?.exists() == false)
            targetFile.parentFile?.mkdirs()
        val sourceFile = File(sourcePlaylistFilePath)
        when (selectedSourceLocalApp.intValue) {
            0 -> { //来源：Salt Player
                when (selectedTargetApp.intValue) {
                    3 -> {
                        when (localToZune(targetFile, sourceFile)) {
                            0 -> TODO()
                            else -> {
                            }
                        }
                    }

                    4 -> {
                        val inputFile = sourceFile.readLines()
                        val fileWriter = FileWriter(targetFile, true)
                        var qinaltPlaylist = ""
                        inputFile.forEach {
                            qinaltPlaylist += it.replace(localMusicPath.value, webdavPath.value)
                                .replace("\\", "/").replace(""""""", "&quot;") +
                                    "?username=${webdavUsername.value}" + "\n"
                            fileWriter.write(qinaltPlaylist)
                        }
                        fileWriter.close()
                    }

                    else -> {
                        sourceFile.copyTo(targetFile, true)
                    }
                }
                return targetFile.absolutePath.replace("/storage/emulated/0/", "")
            }

            1 -> { //来源：APlayer
                when (selectedTargetApp.intValue) {
                    3 -> {
                        when (localToZune(targetFile, sourceFile)) {
                            0 -> TODO()
                            else -> {
                            }
                        }
                    }

                    4 -> {
                        val inputFile = sourceFile.readLines()
                        val fileWriter = FileWriter(targetFile, true)
                        var qinaltPlaylist = ""
                        inputFile.forEach {
                            qinaltPlaylist += it.replace(localMusicPath.value, webdavPath.value)
                                .replace("\\", "/").replace(""""""", "&quot;") +
                                    "?username=${webdavUsername.value}" + "\n"
                            fileWriter.write(qinaltPlaylist)
                        }
                        fileWriter.close()
                    }

                    else -> {
                        sourceFile.copyTo(targetFile, true)
                    }
                }
                return targetFile.absolutePath.replace("/storage/emulated/0/", "")
            }

            2 -> { //来源：Poweramp
                when (selectedTargetApp.intValue) {
                    3 -> {
                        when (localToZune(targetFile, sourceFile)) {
                            0 -> TODO()
                            else -> {
                            }
                        }
                    }

                    4 -> {
                        val fileWriter = FileWriter(targetFile, true)
                        val inputFile = sourceFile.readLines()
                        var qinaltPlaylist = ""
                        inputFile.forEach {
                            qinaltPlaylist += it.replace("#.*((\\r\\n)|\\r|\\n)".toRegex(), "")
                                .replace("(\\r\\n)|\\r".toRegex(), "\n")
                                .replace("primary/", "/storage/emulated/0/")
                                .replace(localMusicPath.value, webdavPath.value).replace("\\", "/")
                                .replace(""""""", "&quot;") +
                                    "?username=${webdavUsername.value}" + "\n"
                        }
                        fileWriter.write(qinaltPlaylist)
                        fileWriter.close()
                    }

                    else -> {
                        var powerampPlaylist = sourceFile.readText()
                        powerampPlaylist = powerampPlaylist
                            .replace("#.*((\\r\\n)|\\r|\\n)".toRegex(), "")
                            .replace("(\\r\\n)|\\r".toRegex(), "\n")
                            .replace("primary/", "/storage/emulated/0/")
                        val fileWriter = FileWriter(targetFile, true)
                        fileWriter.write(powerampPlaylist)
                        fileWriter.close()
                    }
                }
                return targetFile.absolutePath.replace("/storage/emulated/0/", "")
            }

            3 -> { //来源：Microsoft Zune
                val zunePlaylist = sourceFile.readText()
                val fileWriter = FileWriter(targetFile, true)
                val zuneInputFile = mutableStateListOf<String>()
                try {
                    val doc: Document = Ksoup.parse(zunePlaylist)
                    val playlistItems = doc.select("smil seq media")
                    playlistItems.forEach {
                        zuneInputFile.add(
                            it.select("media").attr("src")
                                .replace(winPath.value, localMusicPath.value)
                                .replace("\\", "/") + "\n"
                        )
                    }
                } catch (_: Exception) {
                    return ""
                }
                when (selectedTargetApp.intValue) {
                    4 -> {
                        zuneInputFile.forEach {
                            fileWriter.write(
                                it.replace(localMusicPath.value, webdavPath.value)
                                    .replace("\\", "/").replace(""""""", "&quot;") +
                                        "?username=${webdavUsername.value}" + "\n"
                            )
                        }
                        fileWriter.close()
                    }

                    else -> {
                        zuneInputFile.forEach {
                            fileWriter.write(it)
                        }
                    }
                }
                return targetFile.absolutePath.replace("/storage/emulated/0/", "")
            }
        }
        return ""
    }

    suspend fun lunaGetLoginQrCodeUrl(): SnapshotStateMap<String, String> {
        val result = SnapshotStateMap<String, String>()
        if (lunaDeviceId.value.isBlank() && lunaInstallId.value.isBlank()) {
            val temp =
                Tools().generateRandomString(length = 13, includeDigits = true)
            dataStore.edit {
                it[DataStoreConstants.LUNA_DEVICE_ID] =
                    temp + Tools().generateRandomString(length = 3, includeDigits = true)
                it[DataStoreConstants.LUNA_INSTALL_ID] =
                    temp + Tools().generateRandomString(length = 3, includeDigits = true)
            }
        }
        val client = OkHttpClient()
        val url =
            "https://api.qishui.com/passport/web/get_qrcode/?next=https%3A%2F%2Fluna-pc.bytedance.net%2Flogin&device_id=${lunaDeviceId.value}&install_id=${lunaInstallId.value}&did=${lunaDeviceId.value}&iid=${lunaInstallId.value}&aid=386088&language=zh&passport_jssdk_version=3.0.3&device_platform=PC&version_code=1.6.3"
        val request = Request.Builder()
            .url(url)
            .addHeader("Host", "api.qishui.com")
            .addHeader("User-Agent", "LunaPC/1.6.3(11741945)")
            .addHeader("Accept", "*/*")
            .addHeader("Connection", "keep-alive")
            .get()
        val response: JSONObject
        try {
            client.newCall(request.build()).execute().use { responses ->
                response = JSON.parseObject(responses.body?.string())
            }
            if (response.getString("message") != "success")
                throw Exception()
            val qrCodeUrl = response.getJSONObject("data").getString("qrcode_index_url")
            val token = response.getJSONObject("data").getString("token")
            result["qrCodeUrl"] = qrCodeUrl
            result["token"] = token
            showDialogProgressBar.value = false
        } catch (e: Exception) {
            showDialogProgressBar.value = false
            selectedLoginMethod.intValue = 0
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.failed_get_login_qr_code),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        return result
    }

    suspend fun lunaGetLoginStatus(
        token: String,
        now: Boolean = false
    ): Boolean {
        val client = OkHttpClient()
        val url =
            "https://api.qishui.com/passport/web/check_qrconnect/?next=https%3A%2F%2Fluna-pc.bytedance.net%2Flogin&token=${token}&device_id=${lunaDeviceId.value}&install_id=${lunaInstallId.value}&did=${lunaDeviceId.value}&iid=${lunaInstallId.value}&aid=386088&language=zh&passport_jssdk_version=3.0.3&device_platform=PC&version_code=1.6.3"
        val request = Request.Builder()
            .url(url)
            .addHeader("Host", "api.qishui.com")
            .addHeader("User-Agent", "LunaPC/1.6.3(11741945)")
            .addHeader("Accept", "*/*")
            .addHeader("Connection", "keep-alive")
            .get()
        var needSmsVerify = false
        if (!now)
            delay(2000L)
        if (cancelLogin.value && !now) {
            delay(1500L)
        }
        cancelLogin.value = false
        while (!cancelLogin.value) {
            try {
                client.newCall(request.build()).execute().use { response ->
                    val responseJson =
                        JSON.parseObject(response.body?.string()).getJSONObject("data")
                            ?: throw Exception(context.getString(R.string.failed_get_login_qr_code))
                    if (responseJson.getInteger("error_code") != 0) {
                        if (responseJson.containsKey("verify_ticket")) {
                            responseJson.getJSONArray("verify_ways").forEach {
                                val way = it as JSONObject
                                if (way.getString("verify_way") == "mobile_up_sms_verify") {
                                    needSmsVerify = true
                                    lunaVerifyRelated.putAll(
                                        mapOf(
                                            "ticket" to responseJson.getString("verify_ticket"),
                                            "target" to way.getString("channel_mobile"),
                                            "phone" to way.getString("mobile"),
                                            "content" to way.getString("sms_content")
                                        )
                                    )
                                }
                            }
                            if (needSmsVerify) {
                                showLunaVerifyDialog.value = true
                                return false
                            } else {
                                throw Exception(context.getString(R.string.unsupported_auth_method))
                            }
                        } else
                            throw Exception(context.getString(R.string.failed_get_login_status))
                    }
                    val status = responseJson.getString("status")
                    if (status == "confirmed") {
                        var cookie = ""
                        response.headers.values("Set-Cookie").forEach {
                            cookie += it.substring(0, it.indexOf("; ") + 2)
                        }
                        dataStore.edit { settings ->
                            settings[DataStoreConstants.LAST_LOGIN_TIMESTAMP] =
                                System.currentTimeMillis()
                            settings[DataStoreConstants.LUNA_COOKIE] = cookie
                        }
                        return true
                    }
                }
            } catch (_: SocketTimeoutException) {
            } catch (e: Exception) {
                selectedLoginMethod.intValue = 0
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        e.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
                return false
            }
            delay(1250L)
        }
        return false
    }

    suspend fun lunaGetVerifyStatus(): Boolean {
        val client = OkHttpClient()
        val url =
            "https://api.qishui.com/passport/upsms/verify/?new_authn_sdk_version=1.0.16-web&device_id=${lunaDeviceId.value}&install_id=${lunaInstallId.value}&did=${lunaDeviceId.value}&iid=${lunaInstallId.value}&device_platform=PC&version_code=1.6.3"
        val formBody = FormBody.Builder()
            .add("aid", "386088")
            .add("verify_ticket", lunaVerifyRelated["ticket"] ?: "")
            .add("encrypt_uid", "")
            .add("new_authn_sdk_version", "1.0.16-web")
            .build()
        val request = Request.Builder()
            .url(url)
            .addHeader("Host", "api.qishui.com")
            .addHeader("User-Agent", "LunaPC/1.6.3(11741945)")
            .addHeader("Accept", "*/*")
            .addHeader("Connection", "keep-alive")
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .post(formBody)

        val response: JSONObject
        client.newCall(request.build()).execute().use { responses ->
            response = JSON.parseObject(responses.body?.string())
        }
        if (response.getString("message") == "success") {
            return true
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.verify_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return false
        }
    }

    suspend fun lunaLogout() {
        val client = OkHttpClient()
        val url =
            "https://api.qishui.com/passport/web/logout/?fp=${lunaDeviceId.value}&need_redirect=0&aid=386088&device_id=${lunaDeviceId.value}"
        val request = Request.Builder()
            .url(url)
            .addHeader("Cookie",
                cookie.value.ifBlank { lunaCookie.value }
            )
            .addHeader("Host", "api.qishui.com")
            .addHeader("User-Agent", "LunaPC/1.6.3(11741945)")
            .addHeader("Accept", "*/*")
            .addHeader("Connection", "keep-alive")
        client.newCall(request.build()).execute().close()
        dataStore.edit {
            it[DataStoreConstants.LUNA_COOKIE] = ""
        }
    }

    private fun lunaJsonToDatabase(): Boolean {
        val dirPath = context.getExternalFilesDir(null)?.absolutePath + "/lunaJsonDir"
        val dir = File(dirPath)
        if (!dir.exists())
            return false
        val files = dir.listFiles()
        if (files == null || files.isEmpty())
            return false
        Tools().copyAssetFileToExternalFilesDir(context, "QQMusic")
        val databaseFile = File(context.getExternalFilesDir(null), "QQMusic")
        val db = SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
        databaseFilePath.value = databaseFile.absolutePath
        try {
            files.forEach { file ->
                val content = file.readText()

                val json: JSONObject
                try {
                    json = JSON.parseObject(content)
                } catch (_: Exception) {
                    return@forEach
                }

                if (json.containsKey("media_resources")) {
                    // 歌单详情
                    json.getJSONArray("media_resources").forEachIndexed { index, it ->
                        val song = (it as JSONObject).getJSONObject("entity")
                            .getJSONObject("track_wrapper")
                            .getJSONObject("track")
                        val playlistId = json.getJSONObject("playlist").getString("id")
                        val songId = song.getString("id")
                        val songName = song.getString("name")

                        val songArtistsBuilder = StringBuilder()
                        song.getJSONArray("artists").forEach { it1 ->
                            val artistsInfo = it1 as JSONObject
                            songArtistsBuilder.append(artistsInfo.getString("name"))
                            songArtistsBuilder.append("/")
                        }
                        songArtistsBuilder.deleteCharAt(songArtistsBuilder.length - 1)
                        var songArtists = songArtistsBuilder.toString()

                        var songAlbum = song.getJSONObject("album").getString("name")
                        if (songArtists.isBlank() || songArtists == "null") {
                            songArtists = context.getString(R.string.unknown)
                        }
                        if (songAlbum.isNullOrBlank() || songAlbum == "null") {
                            songAlbum = context.getString(R.string.unknown)
                        }

                        db.execSQL(
                            "INSERT INTO ${sourceApp.songListSongInfoTableName} (${sourceApp.songListSongInfoPlaylistId}, ${sourceApp.songListSongInfoSongId}, ${sourceApp.sortField}) VALUES (?, ?, ?)",
                            arrayOf(
                                playlistId,
                                songId,
                                index
                            )
                        )

                        db.rawQuery(
                            "SELECT COUNT(*) FROM ${sourceApp.songInfoTableName} WHERE ${sourceApp.songInfoSongId} = ?",
                            arrayOf(songId)
                        ).use { cursor ->
                            cursor.moveToFirst()
                            if (cursor.getInt(0) == 0) {
                                db.execSQL(
                                    "INSERT INTO ${sourceApp.songInfoTableName} (${sourceApp.songInfoSongId}, ${sourceApp.songInfoSongName}, ${sourceApp.songInfoSongArtist}, ${sourceApp.songInfoSongAlbum}) VALUES (?, ?, ?, ?)",
                                    arrayOf(
                                        songId,
                                        songName,
                                        songArtists,
                                        songAlbum
                                    )
                                )
                            }
                        }
                    }
                } else if (json.containsKey("playlists")) {
                    // 歌单列表
                    val playlistInfo =
                        json.getJSONArray("playlists")
                    playlistInfo.forEach pForEach@{
                        val playlist = it as JSONObject
                        if (!playlist.containsKey("count_tracks") || playlist.getInteger("count_tracks") == 0) {
                            return@pForEach
                        }
                        db.execSQL(
                            "INSERT INTO ${sourceApp.songListTableName} (${sourceApp.songListId}, ${sourceApp.songListName}, ${sourceApp.musicNum}) VALUES (?, ?, ?)",
                            arrayOf(
                                playlist.getString("id"),
                                playlist.getString("title"),
                                playlist.getInteger("count_tracks")
                            )
                        )
                    }
                } else return@forEach
            }
        } catch (_: Exception) {
            return false
        } finally {
            db.close()
        }
        return true
    }

    suspend fun spotifyTestUserExist(inputSpotifyUserId: String): Boolean {
        showDialogProgressBar.value = true
        if (spotifyUserId.value == inputSpotifyUserId) {
            showDialogProgressBar.value = false
            return true
        }

        if (inputSpotifyUserId.isBlank() || !"\\w+".toRegex().matches(inputSpotifyUserId)) {
            showDialogProgressBar.value = false
            return false
        }

        if (encryptServer.value == "cf") {
            errorDialogTitle.value =
                context.getString(R.string.error_while_getting_data_dialog_title)
            errorDialogContent.value =
                "- ${context.getString(R.string.spotify_token_cf)}\n"
            errorDialogCustomAction.value = {}
            showErrorDialog.value = true
            showDialogProgressBar.value = false
            return false
        }

        val spotifyToken: String
        try {
            spotifyToken = Tools().encryptString(
                "0248",
                "spotify",
                encryptServer.value
            )?.getString("token").toString()
        } catch (e: Exception) {
            showDialogProgressBar.value = false
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.spotify_token_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return false
        }

        try {
            val client = OkHttpClient()
            val url = "https://api.spotify.com/v1/users/${inputSpotifyUserId}"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${spotifyToken}")
                .addHeader("Accept", "application/json")
                .addHeader("Connection", "keep-alive")
                .get()

            val response: JSONObject
            client.newCall(request.build()).execute().use { responses ->
                response = JSON.parseObject(responses.body?.string())
            }
            if (response.containsKey("error")) {
                showDialogProgressBar.value = false
                return false
            }
        } catch (e: Exception) {
            showDialogProgressBar.value = false
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.spotify_token_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return false
        }
        showDialogProgressBar.value = false
        dataStore.edit { settings ->
            settings[DataStoreConstants.SPOTIFY_USER_ID] = inputSpotifyUserId
        }
        return true
    }

    private fun tuneMyMusicCsvToDatabase(): Boolean {
        if (csvFilePath.isBlank() || !csvFilePath.endsWith("csv", true)) {
            return false
        }
        val csvFile = File(csvFilePath)
        if (!csvFile.exists() || !csvFile.canRead()) {
            return false
        }

        Tools().copyAssetFileToExternalFilesDir(context, "QQMusic")
        val databaseFile = File(context.getExternalFilesDir(null), "QQMusic")
        val db = SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
        databaseFilePath.value = databaseFile.absolutePath

        try {
            val csvMap = mutableListOf<Array<String>>()
            FileReader(csvFile).use { csvFileReader ->
                CSVParser(csvFileReader, CSVFormat.DEFAULT).use { parser ->
                    for (record in parser.records) {
                        if (record.recordNumber == 1L)
                            continue
                        csvMap.add(record.values())
                    }
                }
            }

            val playList = mutableListOf<Map<Int, String>>()
            var playListIndex = 0

            csvMap.forEachIndexed { index, songInfo ->
                if (!playList.contains(mapOf(playListIndex to songInfo[3]))) {
                    playList.add(mapOf(playListIndex to songInfo[3]))
                }

                val song = songInfo[0]
                var songArtists = songInfo[1].replace(" & ", "/")
                var songAlbum = songInfo[2]
                if (songArtists.isBlank() || songArtists == "null") {
                    songArtists = context.getString(R.string.unknown)
                }
                if (songAlbum.isBlank() || songAlbum == "null") {
                    songAlbum = context.getString(R.string.unknown)
                }

                db.execSQL(
                    "INSERT INTO ${sourceApp.songListSongInfoTableName} (${sourceApp.songListSongInfoPlaylistId}, ${sourceApp.songListSongInfoSongId}, ${sourceApp.sortField}) VALUES (?, ?, ?)",
                    arrayOf(
                        playList[playListIndex].keys.first(), // 歌单ID
                        index, // 歌曲ID
                        index  // 歌曲顺序
                    )
                )

                db.rawQuery(
                    "SELECT COUNT(*) FROM ${sourceApp.songInfoTableName} WHERE ${sourceApp.songInfoSongId} = ?",
                    arrayOf(index.toString())
                ).use { cursor ->
                    cursor.moveToFirst()
                    if (cursor.getInt(0) == 0) {
                        db.execSQL(
                            "INSERT INTO ${sourceApp.songInfoTableName} (${sourceApp.songInfoSongId}, ${sourceApp.songInfoSongName}, ${sourceApp.songInfoSongArtist}, ${sourceApp.songInfoSongAlbum}) VALUES (?, ?, ?, ?)",
                            arrayOf(
                                index, // 歌曲ID
                                song, // 歌曲名称
                                songArtists,
                                songAlbum
                            )
                        )
                    }
                }

                if (index == csvMap.size - 1 || csvMap[index][3] != csvMap[index + 1][3]) {
                    db.execSQL(
                        "INSERT INTO ${sourceApp.songListTableName} (${sourceApp.songListId}, ${sourceApp.songListName}, ${sourceApp.musicNum}) VALUES (?, ?, ?)",
                        arrayOf(
                            playList[playListIndex].keys.first(), //歌单ID
                            songInfo[3], // 歌单名
                            csvMap.count { it[3] == songInfo[3] } // 歌单歌曲数量
                        )
                    )
                    playListIndex++
                }
            }
        } catch (_: Exception) {
            return false
        } finally {
            db.close()
        }
        return true
    }
}

