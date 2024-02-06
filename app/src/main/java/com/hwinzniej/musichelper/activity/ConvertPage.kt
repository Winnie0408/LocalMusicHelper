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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
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
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ConvertPage(
    val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val openMusicPlatformSqlFileLauncher: ActivityResultLauncher<Array<String>>,
    private val openResultSqlFileLauncher: ActivityResultLauncher<Array<String>>,
    val db: MusicDatabase,
    componentActivity: ComponentActivity,
    val encryptServer: MutableState<String>,
    val dataStore: DataStore<Preferences>,
) : PermissionResultHandler {
    var databaseFileName = mutableStateOf("")
    var selectedSourceApp = mutableIntStateOf(0)
    var useCustomResultFile = mutableStateOf(false)
    var customResultFileName = mutableStateOf("")
    private var selectedFileName = mutableStateOf("")
    var showLoadingProgressBar = mutableStateOf(false)
    var showErrorDialog = mutableStateOf(false)
    var errorDialogTitle = mutableStateOf("")
    var errorDialogContent = mutableStateOf("")
    var databaseFilePath = mutableStateOf("")
    private var resultFilePath = ""
    private var sourceApp = SourceApp()
    val loadingProgressSema = Semaphore(2)
    var currentPage = mutableIntStateOf(0)
    var selectedMatchingMode = mutableIntStateOf(1)
    var enableBracketRemoval = mutableStateOf(false)
    var enableArtistNameMatch = mutableStateOf(true)
    var enableAlbumNameMatch = mutableStateOf(true)
    var similarity = mutableFloatStateOf(85f)
    var useRootAccess = mutableStateOf(false)
    var sourceAppText = mutableStateOf("")
    private var playlistId = mutableStateListOf<String>()
    var playlistName = mutableStateListOf<String>()
    var playlistEnabled = mutableStateListOf<Int>()
    var playlistSum = mutableStateListOf<Int>()
    var showSelectSourceDialog = mutableStateOf(false)
    var multiSource = mutableStateListOf<Array<String>>()
    var showNumberProgressBar = mutableStateOf(false)
    var selectedMethod = mutableIntStateOf(0)
    var selectedLoginMethod = mutableIntStateOf(2)
    var cookie = mutableStateOf("")
    var showLoginDialog = mutableStateOf(false)
    var loginUserId = mutableStateOf("")
    var errorDialogCustomAction = mutableStateOf({})
    var lastLoginTimestamp = mutableStateOf(0L)

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
                checkSelectedFiles(250L)
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

    fun handleUri(uri: Uri?, code: Int) {
        if (uri == null) {
            return
        }
        selectedFileName.value = uri.pathSegments[uri.pathSegments.size - 1]
        selectedFileName.value =
            selectedFileName.value.substring(selectedFileName.value.lastIndexOf("/") + 1)
        if (code == 0) {
            databaseFilePath.value = Tools().uriToAbsolutePath(uri)
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
    private fun checkSelectedFiles(delay: Long = 0L) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            haveError = false
            showLoadingProgressBar.value = true
            playlistEnabled.clear()
            playlistId.clear()
            playlistName.clear()
            playlistSum.clear()
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
                    showErrorDialog.value = true
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
                    showErrorDialog.value = true
                    haveError = true
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
                        showErrorDialog.value = true
                        haveError = true
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
                    showErrorDialog.value = true
                    haveError = true
                } finally {
                    loadingProgressSema.release()
                }
            }
        }
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
                showErrorDialog.value = true
                haveError = true
                loadingProgressSema.release()
                return@launch
            }
        }
    }

    private fun checkAppStatusWithRoot() {
        val appExists =
            Tools().execShellCmd("pm list packages | grep -E '${sourceApp.pakageName}'")
        if (appExists.isNotEmpty()) {
            if (appExists.indexOf("package:") == appExists.lastIndexOf("package:")) {
                val dirPath = context.getExternalFilesDir(null)?.absolutePath + "/userDatabase"
                val dir = File(dirPath)
                if (!dir.exists())
                    dir.mkdirs()

                val copyResult = Tools().execShellCmd(
                    "cp -f '/data/data/${appExists.split(":")[1]}/databases/${
                        sourceApp.databaseName
                    }' '${dir.absolutePath}/${sourceApp.sourceEng}_temp.db'"
                )

                if (copyResult == "") {
                    databaseFilePath.value = "${dir.absolutePath}/${sourceApp.sourceEng}_temp.db"
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
                    showErrorDialog.value = true
                    haveError = true
                    loadingProgressSema.release()
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
                            ).applicationInfo.loadLabel(pm).toString()
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
                    context.getString(R.string.app_not_installed).replace("#", sourceAppText.value)
                }\n"
            showErrorDialog.value = true
            haveError = true
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
                    showErrorDialog.value = true
                    loadingProgressSema.release()
                    haveError = true
                    return@launch
                }

                1 -> sourceApp.init("CloudMusic")
                2 -> sourceApp.init("QQMusic")
                3 -> sourceApp.init("KugouMusic")
                4 -> sourceApp.init("KuwoMusic")
            }
            if (selectedMethod.intValue == 0) {
                if (sourceApp.sourceEng != "") {
                    if (useRootAccess.value)
                        checkAppStatusWithRoot()
                    else {
                        if (databaseFilePath.value.isEmpty()) {
                            errorDialogTitle.value =
                                context.getString(R.string.error_while_getting_data_dialog_title)
                            errorDialogContent.value +=
                                "- ${context.getString(R.string.database_file)} ${
                                    context.getString(
                                        R.string.read_failed
                                    )
                                }:\n  - ${
                                    context.getString(
                                        R.string.please_select_database_file
                                    )
                                }\n"
                            showErrorDialog.value = true
                            loadingProgressSema.release()
                            haveError = true
                            return@launch
                        }
                        var db: SQLiteDatabase? = null
                        try {
                            db = SQLiteDatabase.openDatabase(
                                databaseFilePath.value,
                                null,
                                SQLiteDatabase.OPEN_READONLY
                            )
                            db.rawQuery(
                                "SELECT ${sourceApp.songListId}, ${sourceApp.songListName} FROM ${sourceApp.songListTableName} LIMIT 1",
                                null
                            ).close()
                        } catch (e: Exception) {
                            errorDialogTitle.value =
                                context.getString(R.string.error_while_getting_data_dialog_title)
                            errorDialogContent.value +=
                                "- ${context.getString(R.string.database_file)} ${
                                    context.getString(
                                        R.string.read_failed
                                    )
                                }:\n  - ${e}\n"
                            showErrorDialog.value = true
                            haveError = true
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
                    showErrorDialog.value = true
                    haveError = true
                    loadingProgressSema.release()
                }
            } else {
                if (selectedSourceApp.intValue == 3 || selectedSourceApp.intValue == 4) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.developing),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    haveError = true
                    loadingProgressSema.release()
                    return@launch
                }
                selectedLoginMethod.intValue = 2
                loadingProgressSema.release()
            }
        }
    }

    fun getOnlinePlaylist() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            currentPage.intValue = 1
            showLoadingProgressBar.value = true

            try {
                val url = when (selectedSourceApp.intValue) {
                    1 -> "https://music.163.com/weapi/user/playlist"
                    2 -> "https://c.y.qq.com/rsc/fcgi-bin/fcg_user_created_diss"
                    3 -> ""
                    4 -> ""
                    else -> ""
                }
                val client = OkHttpClient()
                var request = Request.Builder()
                when (selectedSourceApp.intValue) {
                    1 -> {
                        if (cookie.value.contains("\\buid=\\d+".toRegex())) {
                            loginUserId.value =
                                "\\buid=\\d+".toRegex().find(cookie.value)?.value?.substring(4)!!
                            cookie.value = "uid=\\d+;?\\s?".toRegex().replace(cookie.value, "")
                        }
                        dataStore.edit { settings ->
                            settings[DataStoreConstants.NETEASE_USER_ID] = loginUserId.value
                        }
                        val encrypted = Tools().encryptString(
                            """{"limit":100,"offset":0,"uid":${loginUserId.value}}""",
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

                    }

                    4 -> {

                    }
                }

                val response =
                    JSON.parseObject(client.newCall(request.build()).execute().body?.string())

                if (response != null) {
                    val innerPlaylistId = MutableList(0) { "" }
                    val innerPlaylistName = MutableList(0) { "" }
                    val innerPlaylistEnabled = MutableList(0) { 0 }
                    val innerPlaylistSum = MutableList(0) { 0 }
                    var emptyPlaylistNum = 0

                    when (selectedSourceApp.intValue) {
                        1 -> {
                            Tools().copyFileToExternalFilesDir(
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
                                showErrorDialog.value = true
                            }
                        }

                        2 -> {
                            Tools().copyFileToExternalFilesDir(
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

                            val responseQQLikePlaylist =
                                JSON.parseObject(
                                    client.newCall(requestQQLikePlaylist.build())
                                        .execute().body?.string()
                                )
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
                                showErrorDialog.value = true
                            }
                        }

                        3 -> {

                        }

                        4 -> {

                        }
                    }
                    playlistId.addAll(innerPlaylistId)
                    playlistName.addAll(innerPlaylistName)
                    playlistEnabled.addAll(innerPlaylistEnabled)
                    playlistSum.addAll(innerPlaylistSum)
                    showLoadingProgressBar.value = false
                    MyVibrationEffect(
                        context,
                        (context as MainActivity).enableHaptic.value
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
                } else {
                    throw Exception("${sourceApp.sourceEng} server's response was null.")
                }
            } catch (e: Exception) {
                showLoadingProgressBar.value = false
                currentPage.intValue = 0
                errorDialogTitle.value =
                    context.getString(R.string.error_while_getting_data_dialog_title)
                errorDialogContent.value =
                    "- ${context.getString(R.string.get_playlist_failed)}\n  - $e\n"
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
            errorDialogTitle.value =
                context.getString(R.string.error)
            errorDialogContent.value =
                "- ${context.getString(R.string.please_select_at_least_one_playlist)}\n"
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
    fun previewResult() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            numberProgress.floatValue = 0.0f
            convertResult.clear()
            val convertResultMap = mutableMapOf<Int, Array<String>>()
            val firstIndex1 = playlistEnabled.indexOfFirst { it == 1 }

            if (selectedMethod.intValue == 1) {
                val testDb = SQLiteDatabase.openDatabase(
                    databaseFilePath.value,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
                val testCursor = testDb.rawQuery(
                    "SELECT ${sourceApp.songListSongInfoPlaylistId} FROM ${sourceApp.songListSongInfoTableName} WHERE ${sourceApp.songListSongInfoPlaylistId} = ? LIMIT 1",
                    arrayOf(playlistId[firstIndex1])
                )
                if (!testCursor.moveToFirst()) {
                    showLoadingProgressBar.value = true

                    try {
                        val url = when (selectedSourceApp.intValue) {
                            1 -> "https://interface.music.163.com/weapi/v6/playlist/detail"
                            2 -> "https://u.y.qq.com/cgi-bin/musicu.fcg"
                            3 -> ""
                            4 -> ""
                            else -> ""
                        }
                        val client = OkHttpClient()
                        var request = Request.Builder()
                        when (selectedSourceApp.intValue) {
                            1 -> {
                                val encrypted = Tools().encryptString(
                                    """{"id":${playlistId[firstIndex1]},"n":1000,"shareUserId":0}""",
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

                            }

                            4 -> {

                            }
                        }

                        val response =
                            JSON.parseObject(
                                client.newCall(request.build()).execute().body?.string()
                            )

                        if (response != null) {
                            when (selectedSourceApp.intValue) {
                                1 -> {
                                    val db = SQLiteDatabase.openDatabase(
                                        databaseFilePath.value,
                                        null,
                                        SQLiteDatabase.OPEN_READWRITE
                                    )

                                    val playListDetailInfo =
                                        response.getJSONObject("playlist").getJSONArray("tracks")
                                    playListDetailInfo.forEachIndexed { index, it ->
                                        val song = it as JSONObject
                                        val playlistId =
                                            response.getJSONObject("playlist").getString("id")
                                        val songId = song.getString("id")
                                        val songName = song.getString("name")
                                        var songArtistsBuilder = StringBuilder()
                                        song.getJSONArray("ar").forEach { it1 ->
                                            val artistsInfo = it1 as JSONObject
                                            songArtistsBuilder.append(artistsInfo.getString("name"))
                                            songArtistsBuilder.append("/")
                                        }
                                        songArtistsBuilder.deleteCharAt(songArtistsBuilder.length - 1)
                                        val songArtists = songArtistsBuilder.toString()
                                        val songAlbum = song.getJSONObject("al").getString("name")
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

                                    val playListDetailInfo =
                                        response.getJSONObject("GetPlayList").getJSONObject("data")
                                    playListDetailInfo.getJSONArray("songlist")
                                        .forEachIndexed { index, it ->
                                            val song = it as JSONObject
                                            val playlistId =
                                                playListDetailInfo.getJSONObject("dirinfo")
                                                    .getString("id")
                                            val songId = song.getString("id")
                                            val songName = song.getString("title")
                                            var songArtistsBuilder = StringBuilder()
                                            song.getJSONArray("singer").forEach { it1 ->
                                                val artistsInfo = it1 as JSONObject
                                                songArtistsBuilder.append(artistsInfo.getString("title"))
                                                songArtistsBuilder.append("/")
                                            }
                                            songArtistsBuilder.deleteCharAt(songArtistsBuilder.length - 1)
                                            val songArtists = songArtistsBuilder.toString()
                                            val songAlbum =
                                                song.getJSONObject("album").getString("title")
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

                                3 -> {

                                }

                                4 -> {

                                }
                            }
                            delay(500L)
                            showNumberProgressBar.value = true
                            showLoadingProgressBar.value = false
                        } else {
                            throw Exception("${sourceApp.sourceEng} server's response was null.")
                        }
                    } catch (e: Exception) {
                        showLoadingProgressBar.value = false
                        errorDialogTitle.value =
                            context.getString(R.string.error_while_getting_data_dialog_title)
                        errorDialogContent.value =
                            "- ${context.getString(R.string.get_playlist_failed)}\n  - $e\n"
                        showErrorDialog.value = true
                        showLoadingProgressBar.value = false
                        return@launch
                    }
                }
            }

            showNumberProgressBar.value = true
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

            val db = SQLiteDatabase.openOrCreateDatabase(File(databaseFilePath.value), null)
            val cursor = db.rawQuery(
                "SELECT ${sourceApp.songListSongInfoSongId} FROM ${sourceApp.songListSongInfoTableName} WHERE ${sourceApp.songListSongInfoPlaylistId} = '${playlistId[firstIndex1]}' ORDER BY ${sourceApp.sortField}",
                null
            )
            val totalNum = cursor.count.toFloat()
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
                    var songArtistMaxKey = 0
                    var songAlbumMaxKey = 0

                    val songThread = async {
                        //歌曲名相似度列表
                        if (enableBracketRemoval.value) for (k in music3InfoList.indices) {
                            songSimilarityArray[k] = Tools().similarityRatio(
                                songName.replace(
                                    "(?i)\\s?\\((?!inst|[^()]* ver)[^)]*\\)\\s?".toRegex(),
                                    ""
                                ).lowercase(),
                                music3InfoList[k].song
                                    .replace(
                                        "(?i)\\s?\\((?!inst|[^()]* ver)[^)]*\\)\\s?".toRegex(),
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
                                            "(?i)\\s?\\((?!inst|[^()]* ver)[^)]*\\)\\s?".toRegex(),
                                            ""
                                        ).lowercase(),
                                        music3InfoList[k].artist
                                            .replace(
                                                "(?i)\\s?\\((?!inst|[^()]* ver)[^)]*\\)\\s?".toRegex(),
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
                            songArtistMaxKey =
                                Tools().getMaxValueIntDouble(artistSimilarityArray)?.key!! //获取相似度的最大值对应的歌手名的位置
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
                                            "(?i)\\s?\\((?!inst|[^()]* ver)[^)]*\\)\\s?".toRegex(),
                                            ""
                                        ).lowercase(),
                                        music3InfoList[k].album
                                            .replace(
                                                "(?i)\\s?\\((?!inst|[^()]* ver)[^)]*\\)\\s?".toRegex(),
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
                            songAlbumMaxKey =
                                Tools().getMaxValueIntDouble(albumSimilarityArray)?.key!! //获取相似度的最大值对应的专辑名的位置
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
                        )

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
                                    "(?i)\\s?\\((?!inst|[^()]* ver)[^)]*\\)\\s?".toRegex(),
                                    ""
                                ).lowercase(),
                                "${music3InfoList[k].song}${music3InfoList[k].artist}${music3InfoList[k].album}"
                                    .replace(
                                        "(?i)\\s?\\((?!inst|[^()]* ver)[^)]*\\)\\s?".toRegex(),
                                        ""
                                    ).lowercase()
                            )
                        }
                    else
                        for (k in music3InfoList.indices) {
                            similarityArray[k] = Tools().similarityRatio(
                                songInfo.lowercase(),
                                "${music3InfoList[k].song}${music3InfoList[k].artist}${music3InfoList[k].album}".lowercase()
                            )
                        }
                    val maxSimilarity = Tools().getMaxValueIntDouble(similarityArray)
                    val songMaxSimilarity = maxSimilarity?.value!!
                    val songMaxKey = maxSimilarity.key

                    val autoSuccess = songMaxSimilarity >= similarity.floatValue / 100

                    val songConvertResult = music3InfoList[songMaxKey]

                    convertResultMap[num++] =
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
                        )
                }
                songInfoCursor.close()
                if (num % 10 == 0)
                    numberProgress.floatValue = num / totalNum
            }
            cursor.close()
            db.close()
            numberProgress.floatValue = 1.0f
            if (convertResultMap.isEmpty()) {
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
                                else -> ""
                            }
                        )
                    }\n\n${context.getString(R.string.will_skip_this_playlist)}\n"
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
            } else {
                convertResult.putAll(convertResultMap)
            }
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                delay(650L)
                showNumberProgressBar.value = false
            }
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
                    "SELECT song, artist, album, absolutePath, id FROM music WHERE song LIKE '%${inputSearchWords.value}%' OR artist LIKE '%${inputSearchWords.value}%' OR album LIKE '%${inputSearchWords.value}%' LIMIT 3",
                    null
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
                MusicInfo(
                    songId,
                    song,
                    artist,
                    album,
                    null,
                    null,
                    null,
                    null,
                    absolutePath
                )
            } else
                db.musicDao().getMusicById(songId)
            convertResult[songPosition]?.set(0, "2")
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

    fun launchLocalPlayer(targetApp: Int) {
        val targetAppList = listOf(
            arrayOf("com.salt.music", "com.salt.music.ui.MainActivity"),
            arrayOf("remix.myplayer", "remix.myplayer.ui.activity.MainActivity"),
            arrayOf("com.maxmpz.audioplayer", "com.maxmpz.audioplayer.MainActivity"),
        )
        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName(targetAppList[targetApp][0], targetAppList[targetApp][1])
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.other_app_not_installed).replace(
                    "#", when (targetApp) {
                        0 -> "Salt Player"
                        1 -> "APlayer"
                        2 -> "Poweramp"
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
        if (System.currentTimeMillis() - lastLoginTimestamp.value > 259200000) {
            return ""
        }
        if (selectedLoginMethod.intValue == 2) {
            val temp = when (selectedSourceApp.intValue) {
                1 -> "${
                    CookieManager.getInstance().getCookie("music.163.com")
                }; uid=${loginUserId.value}"

                2 -> CookieManager.getInstance().getCookie("y.qq.com")
                3 -> CookieManager.getInstance().getCookie("www.kugou.com")
                4 -> CookieManager.getInstance().getCookie("kuwo.cn")
                else -> ""
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

                3 -> false //TODO 待填写
                4 -> false //TODO 待填写
                else -> false
            }
            if (cookieValid) {
                selectedLoginMethod.intValue = 1
                Toast.makeText(
                    context,
                    context.getString(R.string.use_last_login_info),
                    Toast.LENGTH_SHORT
                ).show()
                return temp
            }
        }
        return ""
    }
}

