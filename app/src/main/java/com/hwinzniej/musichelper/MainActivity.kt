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
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.lifecycleScope
import com.moriafly.salt.ui.BottomBar
import com.moriafly.salt.ui.BottomBarItem
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.ItemSpacer
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

class MainActivity : ComponentActivity() {
    val scanResult = mutableStateOf("扫描结果将会显示在这里")

    val showLoadingProgressBar = mutableStateOf(false)

    var inScanning: Boolean = false

    val showConflictDialog = mutableStateOf(false)

    var conflictDialogResult = mutableIntStateOf(0)

    var progressPercent = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val colors = if (isSystemInDarkTheme) {
                darkSaltColors()
            } else {
                lightSaltColors()
            }
            CompositionLocalProvider {
                SaltTheme(
                    colors = colors
                ) {
                    MainUI(
                        this,
                        scanResult,
                        showLoadingProgressBar,
                        progressPercent,
                        showConflictDialog,
                        conflictDialogResult
                    )
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
            Toast.makeText(this, "正在扫描中，请稍后再试", Toast.LENGTH_SHORT).show()
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
                            inScanning = true
                            openDirectoryLauncher.launch(null)
                        }

                        2 -> {
                            showConflictDialog.value = false
                            val file = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                "MusicHelper.txt"
                            )
                            file.delete()
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
                Toast.makeText(this, "Permissions are required to scan music", Toast.LENGTH_SHORT)
                    .show()
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
                // All permissions have been granted, you can proceed with the next operation
                // For example, you can call a method here to start scanning music
                checkFileExist {
                    when (conflictDialogResult.intValue) {
                        0 -> {
                            inScanning = true
                            openDirectoryLauncher.launch(null)
                        }

                        1 -> {
                            showConflictDialog.value = false
                            inScanning = true
                            openDirectoryLauncher.launch(null)
                        }

                        2 -> {
                            showConflictDialog.value = false
                            val file = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                "MusicHelper.txt"
                            )
                            file.delete()
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
                Toast.makeText(this, "Permissions are required to scan music", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

//==================== Android 10- 使用====================


    /**
     * 检查文件是否已存在
     */
    fun checkFileExist(onUserChoice: () -> Unit) {
        val fileName = "MusicHelper.txt"
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName
        )
        if (file.exists()) {
            showConflictDialog.value = true
        }

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
            Toast.makeText(this, "Selected Directory: $uri", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@MainActivity, "扫描完成", Toast.LENGTH_SHORT).show()
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
        val fileName = "MusicHelper.txt"
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName
        )
        try {
            val fileWriter = FileWriter(file, true)
            fileWriter.write(
                tag.getFirst(FieldKey.TITLE) + "#*#" + tag.getFirst(FieldKey.ARTIST) + "#*#" + tag.getFirst(
                    FieldKey.ALBUM
                ) + "#*#" + filePath + "#*#" + progressPercent.intValue + "\n"
            )
            fileWriter.close()
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
    }

}

/**
 * UI
 */
@OptIn(UnstableSaltApi::class)
@Composable
private fun MainUI(
    mainActivity: MainActivity,
    scanResult: MutableState<String>,
    showLoadingProgressBar: MutableState<Boolean>,
    progressPercent: MutableState<Int>,
    showConflictDialog: MutableState<Boolean>,
    conflictDialogResult: MutableIntState
) {

    if (showConflictDialog.value) {
        YesNoDialog(
            onNegative = { conflictDialogResult.intValue = 1 },
            onPositive = { conflictDialogResult.intValue = 2 },
            onDismiss = { conflictDialogResult.intValue = 3 },
            title = "文件冲突",
            content = "检测到输出文件已存在！\n请选择您的操作",
            noText = "追加",
            yesText = "覆盖"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {
        TitleBar(
            onBack = {

            }, text = "扫描"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(color = SaltTheme.colors.background)
//                .verticalScroll(rememberScrollState())
        ) {
            RoundedColumn {
                ItemTitle(text = "扫描控制")
                ItemSpacer()
                ItemText(text = "点击按钮以开始")
                ItemContainer {
                    TextButton(onClick = {
                        mainActivity.init()
                    }, text = "开始")
                }
            }
            RoundedColumn {
                ItemTitle(text = "扫描结果")
                ItemValue(text = "总歌曲数", sub = progressPercent.value.toString())

                ItemContainer {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
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
                                        color = SaltTheme.colors.highlight
                                    )
                                }
                                Text(
                                    modifier = Modifier.padding(top = 3.dp),
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
        BottomBar {
            BottomBarItem(
                state = true, onClick = {

                }, painter = painterResource(id = R.drawable.ic_launcher_foreground), text = "扫描"
            )
            BottomBarItem(
                state = false, onClick = {

                }, painter = painterResource(id = R.drawable.ic_launcher_foreground), text = "转换"
            )
            BottomBarItem(
                state = false, onClick = {

                }, painter = painterResource(id = R.drawable.ic_launcher_foreground), text = "处理"
            )
            BottomBarItem(
                state = false, onClick = {

                }, painter = painterResource(id = R.drawable.ic_launcher_foreground), text = "关于"
            )
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    MusicHelperTheme {
//        MainUI(MainActivity())
//    }
//}