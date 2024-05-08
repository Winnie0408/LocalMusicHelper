package com.hwinzniej.musichelper

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.hwinzniej.musichelper.activity.ConvertPage
import com.hwinzniej.musichelper.activity.ScanPage
import com.hwinzniej.musichelper.activity.SettingsPage
import com.hwinzniej.musichelper.activity.TagPage
import com.hwinzniej.musichelper.activity.UnlockPage
import com.hwinzniej.musichelper.data.DataStoreConstants
import com.hwinzniej.musichelper.data.database.MusicDatabase
import com.hwinzniej.musichelper.ui.AboutPageUi
import com.hwinzniej.musichelper.ui.ConvertPageUi
import com.hwinzniej.musichelper.ui.ItemTitle
import com.hwinzniej.musichelper.ui.ItemValue
import com.hwinzniej.musichelper.ui.ScanPageUi
import com.hwinzniej.musichelper.ui.SettingsPageUi
import com.hwinzniej.musichelper.ui.TagPageUi
import com.hwinzniej.musichelper.ui.UnlockPageUi
import com.hwinzniej.musichelper.ui.YesNoDialog
import com.hwinzniej.musichelper.utils.MyDataStore
import com.hwinzniej.musichelper.utils.MyVibrationEffect
import com.hwinzniej.musichelper.utils.Tools
import com.moriafly.salt.ui.BottomBar
import com.moriafly.salt.ui.BottomBarItem
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.darkSaltColors
import com.moriafly.salt.ui.lightSaltColors
import com.moriafly.salt.ui.saltColorsByColorScheme
import com.moriafly.salt.ui.saltConfigs
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Locale


class MainActivity : ComponentActivity() {

    private lateinit var openDirectoryLauncher: ActivityResultLauncher<Uri?>
    private lateinit var openEncryptDirectoryLauncher: ActivityResultLauncher<Uri?>
    private lateinit var openDecryptDirectoryLauncher: ActivityResultLauncher<Uri?>
    private lateinit var openMusicPlatformSqlFileLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var openResultSqlFileLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var openPlaylistFileLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var openUmExecutableFileLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var openMusicCoverLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var scanPage: ScanPage

    //    private lateinit var processPage: ProcessPage
    private lateinit var convertPage: ConvertPage
    private lateinit var settingsPage: SettingsPage
    private lateinit var unlockPage: UnlockPage
    private lateinit var tagPage: TagPage

    lateinit var db: MusicDatabase
    lateinit var dataStore: DataStore<Preferences>
    var enableDynamicColor = mutableStateOf(false)
    var selectedThemeMode = mutableIntStateOf(2)
    var enableHaptic = mutableStateOf(true)
    var hapticStrength = mutableIntStateOf(3)
    var language = mutableStateOf("system")
    private val checkUpdate = mutableStateOf(false)
    var updateFileSize = mutableFloatStateOf(0f)
    var isDataLoaded = mutableStateOf(false)

    @SuppressLint("NewApi")
    @OptIn(UnstableSaltApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataStore = (application as MyDataStore).dataStore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val splashScreen = installSplashScreen()
            splashScreen.setKeepOnScreenCondition {
                !isDataLoaded.value
            }
        } else {
            setTheme(R.style.Theme_MusicHelper)
        }

        when (Locale.getDefault().language) {
            Locale.CHINESE.toString() -> language.value = "zh"
            Locale.ENGLISH.toString() -> language.value = "en"
            Locale.KOREAN.toString() -> language.value = "ko"
            else -> language.value = "system"
        }

        openDirectoryLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                scanPage.handleUri(uri)
            }
        openEncryptDirectoryLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                unlockPage.handleSelectedInputPath(uri)
            }
        openDecryptDirectoryLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                unlockPage.handleSelectedOutputPath(uri)
            }
        openMusicPlatformSqlFileLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                convertPage.handleUri(uri, 0)
            }
        openResultSqlFileLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                convertPage.handleUri(uri, 1)
            }
        openPlaylistFileLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                convertPage.handleUri(uri, 2)
            }
        openUmExecutableFileLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                settingsPage.checkUmFile(uri)
            }
        openMusicCoverLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                tagPage.handleSelectedCoverUri(uri)
            }

        db = Room.databaseBuilder(
            applicationContext, MusicDatabase::class.java, "music"
        )
            .fallbackToDestructiveMigration()
//            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

        scanPage = ScanPage(
            context = this,
            lifecycleOwner = this,
            openDirectoryLauncher = openDirectoryLauncher,
            db = db,
            componentActivity = this
        )
//        processPage = ProcessPage(this, this, db)
        settingsPage = SettingsPage(
            context = this,
            lifecycleOwner = this,
            openUmExecutableFileLauncher = openUmExecutableFileLauncher,
            dataStore = dataStore
        )

        convertPage =
            ConvertPage(
                context = this,
                lifecycleOwner = this,
                openMusicPlatformSqlFileLauncher = openMusicPlatformSqlFileLauncher,
                openResultSqlFileLauncher = openResultSqlFileLauncher,
                openPlaylistFileLauncher = openPlaylistFileLauncher,
                db = db,
                componentActivity = this,
                encryptServer = settingsPage.encryptServer,
                dataStore = dataStore
            )

        unlockPage =
            UnlockPage(
                context = this,
                lifecycleOwner = this,
                componentActivity = this,
                openEncryptDirectoryLauncher = openEncryptDirectoryLauncher,
                openDecryptDirectoryLauncher = openDecryptDirectoryLauncher,
                settingsPage = settingsPage
            )

        tagPage = TagPage(
            context = this,
            lifecycleOwner = this,
            db = db,
            openMusicCoverLauncher = openMusicCoverLauncher
        )
        enableEdgeToEdge()
        setContent {
            val colors = when (selectedThemeMode.intValue) {
                0 -> if (enableDynamicColor.value) saltColorsByColorScheme(
                    dynamicLightColorScheme(this)
                ) else lightSaltColors()

                1 -> if (enableDynamicColor.value) saltColorsByColorScheme(
                    dynamicDarkColorScheme(this)
                ) else darkSaltColors()

                2 ->
                    if (isSystemInDarkTheme())
                        if (enableDynamicColor.value) saltColorsByColorScheme(
                            dynamicDarkColorScheme(this)
                        ) else darkSaltColors()
                    else
                        if (enableDynamicColor.value) saltColorsByColorScheme(
                            dynamicLightColorScheme(this)
                        ) else lightSaltColors()

                else -> if (enableDynamicColor.value) saltColorsByColorScheme(
                    dynamicLightColorScheme(this)
                ) else lightSaltColors()
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)
            CompositionLocalProvider {
                SaltTheme(
                    configs = saltConfigs(isSystemInDarkTheme()),
                    colors = colors
                ) {
                    TransparentSystemBars(dark = isSystemInDarkTheme() || (selectedThemeMode.intValue == 1))
                    Pages(
                        mainPage = this,
                        scanPage = scanPage,
                        convertPage = convertPage,
                        unlockPage = unlockPage,
                        settingsPage = settingsPage,
                        checkUpdate = checkUpdate,
                        tagPage = tagPage
                    )
                }
            }
        }
        lifecycleScope.launch {
            delay(1500L)
            if (settingsPage.enableAutoCheckUpdate.value) {
                checkUpdate.value = true
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            delay(3000L)
            Tools().deleteOldFiles(context = this@MainActivity)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        scanPage.onPermissionResult(requestCode, permissions, grantResults)
        convertPage.onPermissionResult(requestCode, permissions, grantResults)
    }

    @Composable
    fun TransparentSystemBars(dark: Boolean) {
        val statusBarColor = SaltTheme.colors.background.toArgb()
        val navigationBarColor = SaltTheme.colors.subBackground.toArgb()
        SideEffect {
            if (dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(statusBarColor),
                    navigationBarStyle = SystemBarStyle.dark(navigationBarColor)
                )
            } else {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.light(statusBarColor, statusBarColor),
                    navigationBarStyle = SystemBarStyle.light(
                        navigationBarColor,
                        navigationBarColor
                    )
                )
            }
        }
    }
}

/**
 * UI
 */

@OptIn(ExperimentalFoundationApi::class, UnstableSaltApi::class)
@Composable
private fun Pages(
    mainPage: MainActivity,
    scanPage: ScanPage,
//    processPage: ProcessPage,
    convertPage: ConvertPage,
    unlockPage: UnlockPage,
    tagPage: TagPage,
    settingsPage: SettingsPage,
    checkUpdate: MutableState<Boolean>
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val targetSdkVersion = context.packageManager.getApplicationInfo(
        context.packageName,
        0
    ).targetSdkVersion
    val pages =
        if (targetSdkVersion <= 28)
            listOf("0", "1", "2", "3", "4")
        else
            listOf("0", "1", "2", "3")
    val pageState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    val settingsPages = listOf("0", "1")
    val settingsPageState = rememberPagerState(pageCount = { settingsPages.size })

    val showNewVersionAvailableDialog = remember { mutableStateOf(false) }
    val latestVersion = remember { mutableStateOf("") }
    val latestDescription = remember { mutableStateOf("") }
    val latestDownloadLink = remember { mutableStateOf("") }

    val overwrite = remember { mutableStateOf(false) }
    val lyricist = remember { mutableStateOf(true) }
    val composer = remember { mutableStateOf(true) }
    val arranger = remember { mutableStateOf(true) }
    val slow = remember { mutableStateOf(false) }
    val sortMethod = remember { mutableIntStateOf(0) }

    mainPage.dataStore.data.collectAsState(initial = null).value?.let { preferences ->
        mainPage.enableDynamicColor.value =
            preferences[DataStoreConstants.KEY_ENABLE_DYNAMIC_COLOR] ?: false
        mainPage.selectedThemeMode.intValue = preferences[DataStoreConstants.KEY_THEME_MODE] ?: 2
        mainPage.enableHaptic.value = preferences[DataStoreConstants.KEY_ENABLE_HAPTIC] ?: true
        mainPage.hapticStrength.intValue = preferences[DataStoreConstants.HAPTIC_STRENGTH] ?: 3
        mainPage.language.value = preferences[DataStoreConstants.KEY_LANGUAGE] ?: "system"
        settingsPage.enableAutoCheckUpdate.value =
            preferences[DataStoreConstants.KEY_ENABLE_AUTO_CHECK_UPDATE] ?: true
        convertPage.useRootAccess.value =
            preferences[DataStoreConstants.KEY_USE_ROOT_ACCESS] ?: false
        settingsPage.encryptServer.value =
            preferences[DataStoreConstants.KEY_ENCRYPT_SERVER] ?: "cf"
        convertPage.loginUserId.value =
            preferences[DataStoreConstants.NETEASE_USER_ID] ?: ""
        convertPage.lastLoginTimestamp.longValue =
            preferences[DataStoreConstants.LAST_LOGIN_TIMESTAMP] ?: 0L
        convertPage.selectedMethod.intValue =
            preferences[DataStoreConstants.GET_PLAYLIST_METHOD] ?: 0
        convertPage.selectedSourceApp.intValue =
            preferences[DataStoreConstants.PLAYLIST_SOURCE_PLATFORM] ?: 0
        settingsPage.umFileLegal.value =
            preferences[DataStoreConstants.UM_FILE_LEGAL] ?: false
        settingsPage.umSupportOverWrite.value =
            preferences[DataStoreConstants.UM_SUPPORT_OVERWRITE] ?: false
        overwrite.value = preferences[DataStoreConstants.TAG_OVERWRITE] ?: false
        lyricist.value = preferences[DataStoreConstants.TAG_LYRICIST] ?: true
        composer.value = preferences[DataStoreConstants.TAG_COMPOSER] ?: true
        arranger.value = preferences[DataStoreConstants.TAG_ARRANGER] ?: true
        sortMethod.intValue = preferences[DataStoreConstants.SORT_METHOD] ?: 0
        slow.value = preferences[DataStoreConstants.SLOW_MODE] ?: false
        convertPage.lunaInstallId.value =
            preferences[DataStoreConstants.LUNA_INSTALL_ID] ?: ""
        convertPage.lunaDeviceId.value =
            preferences[DataStoreConstants.LUNA_DEVICE_ID] ?: ""
        convertPage.lunaCookie.value =
            preferences[DataStoreConstants.LUNA_COOKIE] ?: ""
        coroutineScope.launch(Dispatchers.Default) {
            delay(248L)
            mainPage.isDataLoaded.value = true
        }
    }

    LaunchedEffect(key1 = mainPage.language.value) {
        var locale = Locale(mainPage.language.value)
        if (mainPage.language.value == "system") {
            locale = Resources.getSystem().configuration.locales[0]
        }
        val resources = context.resources
        Locale.setDefault(locale)
        configuration.setLocale(locale)
        resources.updateConfiguration(
            configuration,
            resources.displayMetrics
        )
    }


    if (showNewVersionAvailableDialog.value) {
        YesNoDialog(
            onDismiss = { showNewVersionAvailableDialog.value = false },
            onCancel = { showNewVersionAvailableDialog.value = false },
            onConfirm = {
                showNewVersionAvailableDialog.value = false
                Toast.makeText(
                    context,
                    context.getString(R.string.start_download),
                    Toast.LENGTH_SHORT
                ).show()
                coroutineScope.launch {
                    val downloadManager =
                        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                    val uri = Uri.fromFile(
                        File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "${context.getString(R.string.app_name)}_Update.apk"
                        )
                    )

                    val request = DownloadManager.Request(Uri.parse(latestDownloadLink.value))
                        .setTitle("${context.getString(R.string.app_name)} ${context.getString(R.string.update)}")
                        .setDescription(
                            "${context.getString(R.string.app_name)} ${
                                context.getString(
                                    R.string.latest_version
                                )
                            }: ${latestVersion.value}"
                        )
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setMimeType("application/vnd.android.package-archive")
                        .setDestinationUri(uri)
                    downloadManager.enqueue(request)
                }
            },
            title = stringResource(id = R.string.download_lateset_ver),
            content = null,
            enableHaptic = mainPage.enableHaptic.value,
            hapticStrength = mainPage.hapticStrength.intValue
        ) {
            Column {
                RoundedColumn {
                    ItemTitle(text = stringResource(id = R.string.new_version_available))
                    ItemValue(
                        text = "${stringResource(id = R.string.latest_version)}: ${latestVersion.value}",
                        sub = "${stringResource(id = R.string.current_version)}: ${
                            context.packageManager.getPackageInfo(
                                context.packageName,
                                0
                            ).versionName
                        }",
                        rightSub = "${
                            String.format(
                                Locale(mainPage.language.value),
                                "%.2f",
                                mainPage.updateFileSize.floatValue / 1024 / 1024
                            )
                        }MB"
                    )
                }
                RoundedColumn {
                    ItemTitle(text = "${latestVersion.value} ${stringResource(id = R.string.change_log)}")
                    LazyColumn(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .heightIn(
                                min = 20.dp,
                                max = (configuration.screenHeightDp / 3.5).dp
                            )
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        item {
                            MarkdownText(
                                modifier = Modifier.padding(vertical = 8.dp),
                                markdown = latestDescription.value,
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
//                    RoundedColumn {  //TODO 查看当前版本与最新版本的差异 https://gitlab.com/HWinZnieJ/LocalMusicHelper/-/compare/v0.9.9.1...v0.9.9.5
//                        ItemTitle(text = "${latestVersion.value} ${stringResource(id = R.string.change_log)}")
//                        Item(
//                            onClick = { /*TODO*/ },
//                            text =
//                        )
//                    }
            }
        }
    }

    LaunchedEffect(key1 = checkUpdate.value) { // TODO targetSdk版本不同，更新包下载地址不同
        if (checkUpdate.value) {
            coroutineScope.launch(Dispatchers.IO) {
                checkUpdate.value = false
                val client = OkHttpClient()
                var request = Request.Builder()
                    .url("https://gitlab.com/api/v4/projects/54005438/releases/permalink/latest")
                    .header(
                        "PRIVATE-TOKEN",
                        ""
                    )  //TODO 不要提交到公开仓库！！！
                    .get()
                    .build()
                try {
                    val response: JSONObject
                    client.newCall(request).execute().use { responses ->
                        response = JSON.parseObject(responses.body?.string())
                    }
                    latestVersion.value =
                        response.getString("name").replace("v", "")
                    if (Tools().isVersionNewer(
                            curVersion = context.packageManager.getPackageInfo(
                                context.packageName,
                                0
                            ).versionName,
                            newVersion = latestVersion.value
                        )
                    ) {
                        latestDescription.value = response.getString("description")
                        latestDownloadLink.value = response.getString("description")
                            .substring(
                                latestDescription.value.indexOf("[app-release.apk](") + 18,
                                latestDescription.value.indexOf("/app-release.apk)") + 16
                            )
                        latestDownloadLink.value =
                            "https://gitlab.com/HWinZnieJ/LocalMusicHelper${latestDownloadLink.value}"
                        request = Request.Builder()
                            .url(latestDownloadLink.value)
                            .head()
                            .build()
                        client.newCall(request).execute().use { responses ->
                            responses.header("Content-Length")?.let { header ->
                                mainPage.updateFileSize.floatValue = header.toFloat()
                            }
                        }
                        latestDescription.value = latestDescription.value.substring(
                            0,
                            latestDescription.value.indexOf("\n[app-")
                        )
                        showNewVersionAvailableDialog.value = true
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.auto_check_update_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(SaltTheme.colors.background)
    ) {
        HorizontalPager(
            state = pageState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp),
            beyondBoundsPageCount = 2
        ) { page ->
            when (page) {
                0 -> {
                    ScanPageUi(
                        scanPage = scanPage,
                        scanResult = scanPage.scanResult,
                        showLoadingProgressBar = scanPage.showLoadingProgressBar,
                        progressPercent = scanPage.progressPercent,
                        showConflictDialog = scanPage.showConflictDialog,
                        exportResultFile = scanPage.exportResultFile,
                        selectedExportFormat = scanPage.selectedExportFormat,
                        enableHaptic = mainPage.enableHaptic,
                        hapticStrength = mainPage.hapticStrength,
                    )
                }

                1 -> {
                    ConvertPageUi(
                        convertPage = convertPage,
                        selectedSourceApp = convertPage.selectedSourceApp,
                        databaseFileName = convertPage.databaseFileName,
                        useCustomResultFile = convertPage.useCustomResultFile,
                        customResultFileName = convertPage.customResultFileName,
                        showLoadingProgressBar = convertPage.showLoadingProgressBar,
                        showErrorDialog = convertPage.showErrorDialog,
                        errorDialogTitle = convertPage.errorDialogTitle,
                        errorDialogContent = convertPage.errorDialogContent,
                        playlistName = convertPage.playlistName,
                        playlistEnabled = convertPage.playlistEnabled,
                        playlistSum = convertPage.playlistSum,
                        currentPage = convertPage.currentPage,
                        selectedMatchingMode = convertPage.selectedMatchingMode,
                        enableBracketRemoval = convertPage.enableBracketRemoval,
                        enableArtistNameMatch = convertPage.enableArtistNameMatch,
                        enableAlbumNameMatch = convertPage.enableAlbumNameMatch,
                        similarity = convertPage.similarity,
                        convertResult = convertPage.convertResult,
                        inputSearchWords = convertPage.inputSearchWords,
                        searchResult = convertPage.searchResult,
                        showDialogProgressBar = convertPage.showDialogProgressBar,
                        showSaveDialog = convertPage.showSaveDialog,
                        mainActivityPageState = pageState,
                        enableHaptic = mainPage.enableHaptic,
                        useRootAccess = convertPage.useRootAccess,
                        databaseFilePath = convertPage.databaseFilePath,
                        showSelectSourceDialog = convertPage.showSelectSourceDialog,
                        multiSource = convertPage.multiSource,
                        showNumberProgressBar = convertPage.showNumberProgressBar,
                        selectedMethod = convertPage.selectedMethod,
                        selectedLoginMethod = convertPage.selectedLoginMethod,
                        showLoginDialog = convertPage.showLoginDialog,
                        dataStore = mainPage.dataStore,
                        showSongNumMismatchDialog = convertPage.showSongNumMismatchDialog,
                        hapticStrength = mainPage.hapticStrength,
                    )
                }

                2 -> {
                    TagPageUi(
                        tagPage = tagPage,
                        enableHaptic = mainPage.enableHaptic,
                        hapticStrength = mainPage.hapticStrength,
                        scanPage = scanPage,
                        pageState = pageState,
                        overwrite = overwrite,
                        lyricist = lyricist,
                        composer = composer,
                        arranger = arranger,
                        sortMethod = sortMethod,
                        dataStore = mainPage.dataStore,
                        slow = slow
                    )
                }

//                2 -> {
//                    ProcessPageUi(
//                        processPage = processPage,
//                        processAllScannedMusic = processPage.processAllScannedMusic,
//                        overwriteOriginalTag = processPage.overwriteOriginalTag,
//                        showSelectTagTypeDialog = processPage.showSelectTagTypeDialog,
//                        enableAlbumArtist = processPage.enableAlbumArtist,
//                        enableReleaseYear = processPage.enableReleaseYear,
//                        enableGenre = processPage.enableGenre,
//                        enableTrackNumber = processPage.enableTrackNumber,
//                        showProgressBar = processPage.showProgressBar,
//                        showSelectSourceDialog = processPage.showSelectSourceDialog,
//                        useDoubanMusicSource = processPage.useDoubanMusicSource,
//                        useMusicBrainzSource = processPage.useMusicBrainzSource,
//                        useBaiduBaikeSource = processPage.useBaiduBaikeSource,
//                    )
//                }

                3 -> {
                    if (targetSdkVersion <= 28) {
                        UnlockPageUi(
                            unlockPage = unlockPage,
                            enableHaptic = mainPage.enableHaptic,
                            settingsPage = settingsPage,
                            hapticStrength = mainPage.hapticStrength,
                        )
                    } else {
                        HorizontalPager(
                            state = settingsPageState,
                            modifier = Modifier
                                .fillMaxSize(),
                            userScrollEnabled = false,
                            beyondBoundsPageCount = 1
                        ) { settingPage ->
                            when (settingPage) {
                                0 -> {
                                    SettingsPageUi(
                                        settingsPage = settingsPage,
                                        enableDynamicColor = mainPage.enableDynamicColor,
                                        selectedThemeMode = mainPage.selectedThemeMode,
                                        selectedLanguage = mainPage.language,
                                        useRootAccess = convertPage.useRootAccess,
                                        enableAutoCheckUpdate = settingsPage.enableAutoCheckUpdate,
                                        settingsPageState = settingsPageState,
                                        enableHaptic = mainPage.enableHaptic,
                                        dataStore = mainPage.dataStore,
                                        encryptServer = settingsPage.encryptServer,
                                        hapticStrength = mainPage.hapticStrength,
                                    )
                                }

                                1 -> {
                                    AboutPageUi(
                                        settingsPageState = settingsPageState,
                                        showNewVersionAvailableDialog = showNewVersionAvailableDialog,
                                        latestVersion = latestVersion,
                                        latestDescription = latestDescription,
                                        latestDownloadLink = latestDownloadLink,
                                        enableHaptic = mainPage.enableHaptic,
                                        language = mainPage.language,
                                        updateFileSize = mainPage.updateFileSize,
                                        hapticStrength = mainPage.hapticStrength,
                                    )
                                }
                            }
                        }
                    }
                }

                4 -> {
                    if (targetSdkVersion <= 28) {
                        HorizontalPager(
                            state = settingsPageState,
                            modifier = Modifier
                                .fillMaxSize(),
                            userScrollEnabled = false,
                            beyondBoundsPageCount = 0  //保证切换语言后，页面自动应用新语言
                        ) { settingPage ->
                            when (settingPage) {
                                0 -> {
                                    SettingsPageUi(
                                        settingsPage = settingsPage,
                                        enableDynamicColor = mainPage.enableDynamicColor,
                                        selectedThemeMode = mainPage.selectedThemeMode,
                                        selectedLanguage = mainPage.language,
                                        useRootAccess = convertPage.useRootAccess,
                                        enableAutoCheckUpdate = settingsPage.enableAutoCheckUpdate,
                                        settingsPageState = settingsPageState,
                                        enableHaptic = mainPage.enableHaptic,
                                        dataStore = mainPage.dataStore,
                                        encryptServer = settingsPage.encryptServer,
                                        hapticStrength = mainPage.hapticStrength,
                                    )
                                }

                                1 -> {
                                    AboutPageUi(
                                        settingsPageState = settingsPageState,
                                        showNewVersionAvailableDialog = showNewVersionAvailableDialog,
                                        latestVersion = latestVersion,
                                        latestDescription = latestDescription,
                                        latestDownloadLink = latestDownloadLink,
                                        enableHaptic = mainPage.enableHaptic,
                                        language = mainPage.language,
                                        updateFileSize = mainPage.updateFileSize,
                                        hapticStrength = mainPage.hapticStrength,
                                    )
                                }
                            }
                        }
                    }
                }
            }

        }

        BottomBar(modifier = Modifier.align(Alignment.BottomCenter)) {
            BottomBarItem(
                state = pageState.currentPage == 0,
                onClick = {
                    MyVibrationEffect(
                        context,
                        mainPage.enableHaptic.value,
                        mainPage.hapticStrength.intValue
                    ).click()
                    coroutineScope.launch {
                        settingsPageState.scrollToPage(0)
                    }
                    coroutineScope.launch {
                        pageState.animateScrollToPage(
                            0,
                            animationSpec = spring(2f)
                        )
                    }
                },
                painter = painterResource(id = R.drawable.scan),
                text = stringResource(R.string.scan_function_name)
            )
            BottomBarItem(
                state = pageState.currentPage == 1,
                onClick = {
                    MyVibrationEffect(
                        context,
                        mainPage.enableHaptic.value,
                        mainPage.hapticStrength.intValue
                    ).click()
                    coroutineScope.launch {
                        settingsPageState.scrollToPage(0)
                    }
                    coroutineScope.launch {
                        pageState.animateScrollToPage(
                            1,
                            animationSpec = spring(2f)
                        )
                    }
                },
                painter = painterResource(id = R.drawable.convert),
                text = stringResource(R.string.convert_function_name)
            )
            BottomBarItem(
                state = pageState.currentPage == 2,
                onClick = {
                    MyVibrationEffect(
                        context,
                        mainPage.enableHaptic.value,
                        mainPage.hapticStrength.intValue
                    ).click()
                    coroutineScope.launch {
                        settingsPageState.scrollToPage(0)
                    }
                    coroutineScope.launch {
                        pageState.animateScrollToPage(
                            2,
                            animationSpec = spring(2f)
                        )
                    }
                },
                painter = painterResource(id = R.drawable.tag),
                text = stringResource(R.string.tag_function_name)
            )
//            BottomBarItem(
//                state = pageState.currentPage == 2,
//                onClick = {
//                    coroutineScope.launch {
//                        pageState.animateScrollToPage(
//                            2,
//                            animationSpec = spring(2f)
//                        )
//                        settingsPageState.animateScrollToPage(
//                            0,
//                            animationSpec = spring(2f)
//                        )
//                    }
//                },
//                painter = painterResource(id = R.drawable.ic_launcher_foreground),
//                text = stringResource(R.string.process_function_name)
//            )
            if (targetSdkVersion <= 28) {
                BottomBarItem(
                    state = pageState.currentPage == 3,
                    onClick = {
                        MyVibrationEffect(
                            context,
                            mainPage.enableHaptic.value,
                            mainPage.hapticStrength.intValue
                        ).click()
                        coroutineScope.launch {
                            settingsPageState.scrollToPage(0)
                        }
                        coroutineScope.launch {
                            pageState.animateScrollToPage(
                                3,
                                animationSpec = spring(2f)
                            )
                        }
                    },
                    painter = painterResource(id = R.drawable.unlock),
                    text = stringResource(R.string.unlock_function_name)
                )
            }
            BottomBarItem(
                state = if (targetSdkVersion <= 28)
                    pageState.currentPage == 4
                else
                    pageState.currentPage == 3,
                onClick = {
                    MyVibrationEffect(
                        context,
                        mainPage.enableHaptic.value,
                        mainPage.hapticStrength.intValue
                    ).click()
                    coroutineScope.launch {
                        settingsPageState.animateScrollToPage(
                            0,
                            animationSpec = spring(2f)
                        )
                    }
                    coroutineScope.launch {
                        pageState.animateScrollToPage(
                            if (targetSdkVersion <= 28)
                                4
                            else
                                3,
                            animationSpec = spring(2f)
                        )
                    }
                },
                painter = painterResource(id = R.drawable.settings),
                text = stringResource(R.string.settings_function_name)
            )
        }
    }
}