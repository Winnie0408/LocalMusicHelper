package com.hwinzniej.musichelper

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.room.Room
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.hwinzniej.musichelper.activity.ConvertPage
import com.hwinzniej.musichelper.activity.ProcessPage
import com.hwinzniej.musichelper.activity.ScanPage
import com.hwinzniej.musichelper.activity.SettingsPage
import com.hwinzniej.musichelper.data.database.MusicDatabase
import com.hwinzniej.musichelper.ui.AboutPageUi
import com.hwinzniej.musichelper.ui.ConvertPageUi
import com.hwinzniej.musichelper.ui.ProcessPageUi
import com.hwinzniej.musichelper.ui.ScanPageUi
import com.hwinzniej.musichelper.ui.SettingsPageUi
import com.moriafly.salt.ui.BottomBar
import com.moriafly.salt.ui.BottomBarItem
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.darkSaltColors
import com.moriafly.salt.ui.lightSaltColors
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var openDirectoryLauncher: ActivityResultLauncher<Uri?>
    private lateinit var openMusicPlatformSqlFileLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var openResultSqlFileLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var scanPage: ScanPage
    private lateinit var processPage: ProcessPage
    private lateinit var convertPage: ConvertPage
    private lateinit var settingsPage: SettingsPage
    lateinit var db: MusicDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        openDirectoryLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                scanPage.handleUri(uri)
            }
        openMusicPlatformSqlFileLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                convertPage.handleUri(uri, 0)
            }
        openResultSqlFileLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                convertPage.handleUri(uri, 1)
            }

        db = Room.databaseBuilder(
            applicationContext, MusicDatabase::class.java, "music"
        ).build()

        scanPage = ScanPage(this, this, openDirectoryLauncher, db, this)
        processPage = ProcessPage(this, this, db)
        convertPage =
            ConvertPage(
                this,
                this,
                openMusicPlatformSqlFileLauncher,
                openResultSqlFileLauncher,
                db,
                this
            )
        settingsPage = SettingsPage(this, this)


        setContent {
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val colors = if (isSystemInDarkTheme) {
                darkSaltColors()
            } else {
                lightSaltColors()
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)
            CompositionLocalProvider {
                SaltTheme(  //TODO 适配椒盐莫奈取色
                    colors = colors
                ) {
                    TransparentSystemBars()
                    Pages(scanPage, processPage, convertPage, settingsPage)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        scanPage.onPermissionResult(requestCode, permissions, grantResults)
        convertPage.onPermissionResult(requestCode, permissions, grantResults)
    }
}

/**
 * UI
 */

@OptIn(ExperimentalFoundationApi::class, UnstableSaltApi::class)
@Composable
private fun Pages(
    scanPage: ScanPage,
    processPage: ProcessPage,
    convertPage: ConvertPage,
    settingsPage: SettingsPage
) {
    val pages = listOf("0", "1", "2", "3")
    val pageState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    val settingsPages = listOf("0", "1")
    val settingsPageState = rememberPagerState(pageCount = { settingsPages.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        HorizontalPager(
            state = pageState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp),
            beyondBoundsPageCount = 1
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
                    )
                }

                2 -> {
                    ProcessPageUi(
                        processPage = processPage,
                        processAllScannedMusic = processPage.processAllScannedMusic,
                        overwriteOriginalTag = processPage.overwriteOriginalTag,
                        showSelectTagTypeDialog = processPage.showSelectTagTypeDialog,
                        enableAlbumArtist = processPage.enableAlbumArtist,
                        enableReleaseYear = processPage.enableReleaseYear,
                        enableGenre = processPage.enableGenre,
                        enableTrackNumber = processPage.enableTrackNumber,
                        showProgressBar = processPage.showProgressBar,
                        showSelectSourceDialog = processPage.showSelectSourceDialog,
                        useDoubanMusicSource = processPage.useDoubanMusicSource,
                        useMusicBrainzSource = processPage.useMusicBrainzSource,
                        useBaiduBaikeSource = processPage.useBaiduBaikeSource,
                    )
                }

                3 -> {
                    HorizontalPager(
                        state = settingsPageState,
                        modifier = Modifier
                            .fillMaxSize(),
                        userScrollEnabled = false,
                        beyondBoundsPageCount = 1
                    ) { page ->
                        when (page) {
                            0 -> {
                                SettingsPageUi(
                                    settingsPage = settingsPage,
                                    enableDynamicColor = settingsPage.enableDynamicColor,
                                    selectedThemeMode = settingsPage.selectedThemeMode,
                                    selectedLanguage = settingsPage.selectedLanguage,
                                    useRootAccess = convertPage.useRootAccess,
                                    enableAutoCheckUpdate = settingsPage.enableAutoCheckUpdate,
                                    settingsPageState = settingsPageState,
                                )
                            }

                            1 -> {
                                AboutPageUi(
                                    settingsPageState = settingsPageState
                                )
                            }
                        }
                    }
//                    NavHost(
//                        navController = navController,
//                        startDestination = "SettingsPageUi",
//                        enterTransition = {
//                            slideInHorizontally(
//                                animationSpec = spring(2f),
//                                initialOffsetX = { 1080 })
//                        },
//                        exitTransition = {
//                            slideOutHorizontally(
//                                animationSpec = spring(2f),
//                                targetOffsetX = { -1080 })
//                        },
//                        popEnterTransition = {
//                            slideInHorizontally(
//                                animationSpec = spring(2f),
//                                initialOffsetX = { -1080 })
//                        },
//                        popExitTransition = {
//                            slideOutHorizontally(
//                                animationSpec = spring(2f),
//                                targetOffsetX = { 1080 })
//                        },
//                    ) {
//                        composable("SettingsPageUi") {
//                            SettingsPageUi(
//                                settingsPage = settingsPage,
//                                enableDynamicColor = settingsPage.enableDynamicColor,
//                                selectedThemeMode = settingsPage.selectedThemeMode,
//                                selectedLanguage = settingsPage.selectedLanguage,
//                                useRootAccess = convertPage.useRootAccess,
//                                enableAutoCheckUpdate = settingsPage.enableAutoCheckUpdate,
//                                navController = navController,
//                            )
//                        }
//                        composable("AboutPageUi") {
//                            AboutPageUi(navController = navController)
//                        }
//                    }
                }
            }

        }

        BottomBar(modifier = Modifier.align(Alignment.BottomCenter)) {
            BottomBarItem(
                state = pageState.currentPage == 0,
                onClick = {
                    coroutineScope.launch {
                        pageState.animateScrollToPage(
                            0,
                            animationSpec = spring(2f)
                        )
                    }
                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = stringResource(R.string.scan_function_name)
            )
            BottomBarItem(
                state = pageState.currentPage == 1,
                onClick = {
                    coroutineScope.launch {
                        pageState.animateScrollToPage(
                            1,
                            animationSpec = spring(2f)
                        )
                    }
                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = stringResource(R.string.convert_function_name)
            )
            BottomBarItem(
                state = pageState.currentPage == 2,
                onClick = {
                    coroutineScope.launch {
                        pageState.animateScrollToPage(
                            2,
                            animationSpec = spring(2f)
                        )
                    }
                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = stringResource(R.string.process_function_name)
            )
            BottomBarItem(
                state = pageState.currentPage == 3,
                onClick = {
                    coroutineScope.launch {
                        pageState.animateScrollToPage(
                            3,
                            animationSpec = spring(2f)
                        )
                    }
                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = stringResource(R.string.settings_function_name)
            )
        }
    }
}

@Composable
fun TransparentSystemBars() {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = !isSystemInDarkTheme()
    val statusBarColor = SaltTheme.colors.background
    val navigationBarColor = SaltTheme.colors.subBackground
    SideEffect {
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = useDarkIcons,
        )

        systemUiController.setNavigationBarColor(
            color = navigationBarColor,
            darkIcons = useDarkIcons,
            navigationBarContrastEnforced = false
        )
    }
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    MusicHelperTheme {
//        MainUI(MainActivity())
//    }
//}