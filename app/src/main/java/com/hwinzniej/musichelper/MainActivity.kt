package com.hwinzniej.musichelper

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.hwinzniej.musichelper.data.database.MusicDatabase
import com.moriafly.salt.ui.BottomBar
import com.moriafly.salt.ui.BottomBarItem
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.ItemText
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.ItemValue
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TextButton
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.darkSaltColors
import com.moriafly.salt.ui.lightSaltColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagException
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.RandomAccessFile

class MainActivity : ComponentActivity() {
    val scanResult = mutableStateOf("")

    val showLoadingProgressBar = mutableStateOf(false)

    var inScanning: Boolean = false

    val showConflictDialog = mutableStateOf(false)

    var conflictDialogResult = mutableIntStateOf(0)

    var progressPercent = mutableIntStateOf(0)

    lateinit var db: MusicDatabase

    var lastIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val colors = if (isSystemInDarkTheme) {
                darkSaltColors()
            } else {
                lightSaltColors()
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)
            scanResult.value = getString(R.string.scan_result_hint)
            db = Room.databaseBuilder(
                applicationContext, MusicDatabase::class.java, "music"
            ).build()
            CompositionLocalProvider {
                SaltTheme(
                    colors = colors
                ) {
                    TransparentSystemBars()
                    Pages(this)
//                    MainUI(
//                        this,
//                        scanResult,
//                        showLoadingProgressBar,
//                        progressPercent,
//                        showConflictDialog,
//                        conflictDialogResult
//                    )
                }
            }
//            MusicHelperTheme {
//                // A surface container using the 'background' color from the theme
//                Surface(
//                    modifier = Modifier.fillMaxSize(), color = SaltTheme.colors.background
//                ) {
////                        Greeting("Android")
//                    MainUI(
//                        this,
//                        scanResult,
//                        showLoadingProgressBar,
//                        progressPercent,
//                        showConflictDialog,
//                        conflictDialogResult
//                    )
//                }
//            }
        }
    }

    /**
     * 功能入口，初始化变量
     */

    fun init() {
        scanResult.value = ""
        progressPercent.intValue = 0
        conflictDialogResult.intValue = 0
        requestPermission(this)
    }

    /**
     * 请求存储权限
     */

//==================== Android 11+ 使用====================

    fun requestPermission(context: Context) {
        if (inScanning) {
            Toast.makeText(this, R.string.scanning_try_again_later, Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { //Android 11+
            if (Environment.isExternalStorageManager()) {
                checkFileExist {
                    when (conflictDialogResult.intValue) {
                        0 -> {
                            inScanning = true
                            openDirectoryLauncher.launch(null)
                        }

                        1 -> {
                            showConflictDialog.value = false
                            lastIndex++
                            inScanning = true
                            openDirectoryLauncher.launch(null)
                        }

                        2 -> {
                            showConflictDialog.value = false
                            lastIndex = 0
                            lifecycleScope.launch(Dispatchers.IO) {
                                val file = File(
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                    getString(R.string.result_file_name)
                                )
                                file.delete()
                                db.musicDao().deleteAll()
                            }
                            inScanning = true
                            openDirectoryLauncher.launch(null)
                        }

                        3 -> {
                            showConflictDialog.value = false
                            return@checkFileExist
                        }
                    }
                }
            } else {
                Toast.makeText(this, R.string.request_permission_toast, Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                startActivity(context, intent, null)
            }
        } else { //Android 10-
            if (!allPermissionsGranted(context)) {
                ActivityCompat.requestPermissions(
                    context as Activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        }
    }

//==================== Android 11+ 使用====================

//==================== Android 10- 使用====================

    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    fun allPermissionsGranted(context: Context): Boolean {
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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted(this)) {
                checkFileExist {
                    when (conflictDialogResult.intValue) {
                        0 -> {
                            inScanning = true
                            openDirectoryLauncher.launch(null)
                        }

                        1 -> {
                            showConflictDialog.value = false
                            lastIndex++
                            inScanning = true
                            openDirectoryLauncher.launch(null)
                        }

                        2 -> {
                            showConflictDialog.value = false
                            lastIndex = 0
                            lifecycleScope.launch(Dispatchers.IO) {
                                val file = File(
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                    getString(R.string.result_file_name)
                                )
                                file.delete()
                                db.musicDao().deleteAll()
                            }
                            inScanning = true
                            openDirectoryLauncher.launch(null)
                        }

                        3 -> {
                            showConflictDialog.value = false
                            return@checkFileExist
                        }
                    }
                }
            } else {
                // Permissions have been denied, you can inform the user about the need for these permissions
                // You can use a Toast, Snackbar, or a dialog to inform the user
                Toast.makeText(this, R.string.request_permission_toast, Toast.LENGTH_SHORT).show()
            }
        }
    }

//==================== Android 10- 使用====================


    /**
     * 检查文件是否已存在
     */
    fun checkFileExist(onUserChoice: () -> Unit) {
        val fileName = getString(R.string.result_file_name)
//        lifecycleScope.launch(Dispatchers.IO) {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName
        )
        if (file.exists()) {
            showConflictDialog.value = true
            val raf = RandomAccessFile(file, "r")
            val length = raf.length()
            raf.seek(length - 8)
            val lastChars = raf.readLine()
            lastIndex =
                lastChars.substring(lastChars.lastIndexOf("#") + 1, lastChars.length).toInt()
            println(lastChars)
            raf.close()
        }
//        }

        lifecycleScope.launch(Dispatchers.Default) {
            while (showConflictDialog.value && conflictDialogResult.intValue == 0) {
                delay(250L)
            }
            onUserChoice()
        }
    }

    /**
     * 打开原生的“文件”APP，让用户选择需要操作的目录
     */
    private val openDirectoryLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            // 这是用户选择的目录的Uri
            // 你可以在这里处理用户选择的目录
            if (uri == null) {
                inScanning = false
                return@registerForActivityResult
            }
            val absolutePath: String
            val uriPath = uri.pathSegments?.get(uri.pathSegments!!.size - 1).toString()
            Toast.makeText(
                this, getString(R.string.selected_directory_path) + "$uri", Toast.LENGTH_SHORT
            ).show()
            if (uriPath.contains("primary")) {  //内部存储
                absolutePath = uriPath.replace("primary:", "/storage/emulated/0/")
            } else {  //SD卡
                absolutePath = "/storage/" + uriPath.split(":")[0] + "/" + uriPath.split(":")[1]
            }
            val directory = File(absolutePath)
            showLoadingProgressBar.value = true
            lifecycleScope.launch(Dispatchers.IO) { scanDirectory(directory) }
            lifecycleScope.launch(Dispatchers.Main) {
                while (true) {
                    val lastScanResult = scanResult.value.length
                    delay(250L)
                    if (scanResult.value.length == lastScanResult) {
                        showLoadingProgressBar.value = false
                        inScanning = false
                        Toast.makeText(
                            this@MainActivity, R.string.scan_complete, Toast.LENGTH_SHORT
                        ).show()
                        break
                    }
                }
            }
        }

    /**
     * 递归扫描所选目录及其子目录
     */
    fun scanDirectory(directory: File) {
        val files = directory.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    // 如果是目录，递归地遍历
                    scanDirectory(file)
                } else {
                    // 如果是文件，进行处理
                    val curFile = File(file.path)
                    try {
                        AudioFileIO.read(curFile)
                    } catch (e: CannotReadException) {
                        continue
                    } catch (e: IOException) {
                        continue
                    } catch (e: TagException) {
                        continue
                    } catch (e: ReadOnlyFileException) {
                        continue
                    } catch (e: InvalidAudioFrameException) {
                        continue
                    }
                    handleFile(curFile)
                }
            }
        }
    }

    /**
     * 处理扫描到的音乐文件
     */
    fun handleFile(file: File) {
        // 在这里处理文件
        val audioFile: AudioFile
        try {
            audioFile = AudioFileIO.read(file)
        } catch (e: Exception) {
            return
        }
        lifecycleScope.launch(Dispatchers.Main) {
            scanResult.value = file.name + "\n" + scanResult.value
        }
        val tag = audioFile.tag
        writeToFile(tag, file.path)

    }

    /**
     * 将扫描到音乐的标签信息写入到文件中
     */
    @Synchronized
    private fun writeToFile(tag: Tag, filePath: String) {
//        val fileName = resources.getString(R.string.outputFileName)
        val fileName = getString(R.string.result_file_name)
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName
        )
        try {
            val fileWriter = FileWriter(file, true)
            fileWriter.write(
                tag.getFirst(FieldKey.TITLE) + "#*#" + tag.getFirst(FieldKey.ARTIST) + "#*#" + tag.getFirst(
                    FieldKey.ALBUM
                ) + "#*#" + filePath + "#*#" + lastIndex + "\n"
            )
            fileWriter.close()
            val music = com.hwinzniej.musichelper.data.model.Music(
                lastIndex,
                tag.getFirst(FieldKey.TITLE),
                tag.getFirst(FieldKey.ARTIST),
                tag.getFirst(FieldKey.ALBUM),
                filePath
            )
            lifecycleScope.launch(Dispatchers.IO) {
                db.musicDao().insert(music)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return
        } catch (e: Exception) {
            return
        }
        increment()
    }

    /**
     * 歌曲数量计数器
     */
    @Synchronized
    private fun increment() {
        ++progressPercent.intValue
        ++lastIndex
    }

    fun getMusicList() {
        lifecycleScope.launch(Dispatchers.IO) {
            val musicList = db.musicDao().getAll()
            println(musicList)
        }
    }

}

/**
 * UI
 */

@OptIn(ExperimentalFoundationApi::class, UnstableSaltApi::class)
@Composable
private fun Pages(mainActivity: MainActivity) {
    val context = LocalContext.current
    val pages = listOf("扫描", "转换", "处理", "关于")
    val pageState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()

    ) {
        HorizontalPager(
            state = pageState, modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp)
        ) { page ->
            when (page) {
                0 -> {
                    ScanPageUi(
                        mainActivity,
                        mainActivity.scanResult,
                        mainActivity.showLoadingProgressBar,
                        mainActivity.progressPercent,
                        mainActivity.showConflictDialog,
                        mainActivity.conflictDialogResult
                    )
                }

                1 -> {
                    ConvertPageUi()
                }

                2 -> {
                    ProcessPageUi()
                }

                3 -> {
                    AboutPageUi()
                }
            }

        }

        BottomBar(modifier = Modifier.align(Alignment.BottomCenter)) {
            BottomBarItem(
                state = pageState.currentPage == 0,
                onClick = {
                    coroutineScope.launch { pageState.animateScrollToPage(0) }
                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = context.getString(R.string.scan_function_name)
            )
            BottomBarItem(
                state = pageState.currentPage == 1,
                onClick = {
                    coroutineScope.launch { pageState.animateScrollToPage(1) }
                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = context.getString(R.string.convert_function_name)
            )
            BottomBarItem(
                state = pageState.currentPage == 2,
                onClick = {
                    coroutineScope.launch { pageState.animateScrollToPage(2) }
                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = context.getString(R.string.process_function_name)
            )
            BottomBarItem(
                state = pageState.currentPage == 3,
                onClick = {
                    coroutineScope.launch { pageState.animateScrollToPage(3) }
                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = context.getString(R.string.about_function_name)
            )
        }
    }
}

@OptIn(UnstableSaltApi::class)
@Composable
private fun ScanPageUi(
    mainActivity: MainActivity,
    scanResult: MutableState<String>,
    showLoadingProgressBar: MutableState<Boolean>,
    progressPercent: MutableState<Int>,
    showConflictDialog: MutableState<Boolean>,
    conflictDialogResult: MutableIntState,
) {
    val context = LocalContext.current
    if (showConflictDialog.value) {
        YesNoDialog(
            onNegative = { conflictDialogResult.intValue = 1 },
            onPositive = { conflictDialogResult.intValue = 2 },
            onDismiss = { conflictDialogResult.intValue = 3 },
            title = context.getString(R.string.file_conflict_dialog_title),
            content = context.getString(R.string.file_conflict_dialog_content).replace("#n", "\n"),
            noText = context.getString(R.string.file_conflict_dialog_no_text),
            yesText = context.getString(R.string.file_conflict_dialog_yes_text),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {
        TitleBar(
            onBack = {}, text = context.getString(R.string.scan_function_name), showBackBtn = false
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(color = SaltTheme.colors.background)
                .verticalScroll(rememberScrollState())
        ) {
            RoundedColumn {
                ItemTitle(text = context.getString((R.string.scan_control)))
                ItemText(text = context.getString(R.string.touch_button_to_start_scanning))
                ItemContainer {
                    TextButton(onClick = {
                        mainActivity.init()
                    }, text = context.getString(R.string.start_text))
                }
            }
            RoundedColumn {
                ItemTitle(text = context.getString(R.string.scan_result))
                ItemValue(
                    text = context.getString(R.string.number_of_total_songs),
                    sub = progressPercent.value.toString()
                )

                ItemContainer {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .size((LocalConfiguration.current.screenHeightDp / 2).dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color = SaltTheme.colors.background)
                    ) {
                        item {
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
                                Text(
                                    modifier = Modifier.padding(
                                        top = 3.dp, start = 7.dp, end = 7.dp
                                    ),
                                    text = scanResult.value,
                                    fontSize = 16.sp,
                                    style = TextStyle(
                                        lineHeight = 1.5.em, color = SaltTheme.colors.subText
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
//        BottomBar {
//            BottomBarItem(
//                state = true,
//                onClick = {
//
//                },
//                painter = painterResource(id = R.drawable.ic_launcher_foreground),
//                text = context.getString(R.string.scan_function_name)
//            )
//            BottomBarItem(
//                state = false,
//                onClick = {
//                    mainActivity.getMusicList()
//                },
//                painter = painterResource(id = R.drawable.ic_launcher_foreground),
//                text = context.getString(R.string.convert_function_name)
//            )
//            BottomBarItem(
//                state = false,
//                onClick = {
//
//                },
//                painter = painterResource(id = R.drawable.ic_launcher_foreground),
//                text = context.getString(R.string.process_function_name)
//            )
//            BottomBarItem(
//                state = false,
//                onClick = {
//
//                },
//                painter = painterResource(id = R.drawable.ic_launcher_foreground),
//                text = context.getString(R.string.about_function_name)
//            )
//        }
    }
}


@Composable
private fun ConvertPageUi() {
    Text(text = "测试1")
}

@Composable
private fun ProcessPageUi() {
    Text(text = "测试2")
}

@Composable
private fun AboutPageUi() {
    Text(text = "测试3")
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