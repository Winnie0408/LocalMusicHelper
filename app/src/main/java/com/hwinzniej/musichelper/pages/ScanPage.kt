package com.hwinzniej.musichelper.pages

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.YesNoDialog
import com.hwinzniej.musichelper.data.database.MusicDatabase
import com.hwinzniej.musichelper.utils.UsefulTools
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.ItemText
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.ItemValue
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TextButton
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
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

interface PermissionResultHandler {
    fun onPermissionResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    )
}

class ScanPage(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val openDirectoryLauncher: ActivityResultLauncher<Uri?>,
    val db: MusicDatabase,
    componentActivity: ComponentActivity
) : PermissionResultHandler {
    val scanResult = mutableStateOf(getString(context, R.string.scan_result_hint))
    val showLoadingProgressBar = mutableStateOf(false)
    var inScanning: Boolean = false
    val showConflictDialog = mutableStateOf(false)
    var conflictDialogResult = mutableIntStateOf(0)
    var progressPercent = mutableIntStateOf(-1)
    var lastIndex = 0

    /**
     * 功能入口，初始化变量
     */

    fun init() {
        lastIndex = 0
        conflictDialogResult.intValue = 0
        requestPermission()
    }


    /**
     * 请求存储权限
     */

//==================== Android 11+ 使用====================

    @RequiresApi(Build.VERSION_CODES.R)
    private val requestPermissionLauncher = componentActivity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Environment.isExternalStorageManager()) {
            afterPermissionGranted()
        } else {
            Toast.makeText(context, R.string.permission_not_granted_toast, Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun requestPermission() {
        if (inScanning) {
            Toast.makeText(context, R.string.scanning_try_again_later, Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { //Android 11+
            if (Environment.isExternalStorageManager()) {
                afterPermissionGranted()
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
                afterPermissionGranted()
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

    private val REQUEST_CODE_PERMISSIONS = 1001
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
                afterPermissionGranted()
            } else {
                Toast.makeText(context, R.string.permission_not_granted_toast, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

//==================== Android 10- 使用====================

    fun afterPermissionGranted() {
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
                    inScanning = true
                    openDirectoryLauncher.launch(null)
                }

                3 -> {
                    showConflictDialog.value = false
                    return@checkFileExist
                }
            }
        }
    }

    /**
     * 检查文件是否已存在
     */
    fun checkFileExist(onUserChoice: () -> Unit) {
        val fileName = getString(context, R.string.result_file_name)
//        lifecycleScope.launch(Dispatchers.IO) {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName
        )
        if (file.exists()) {
            showConflictDialog.value = true
            val lastChars = UsefulTools().readLastNChars(file, 8)
            lastIndex =
                lastChars.substring(lastChars.lastIndexOf("#") + 1, lastChars.length).toInt()
        }
//        }

        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            while (showConflictDialog.value && conflictDialogResult.intValue == 0) {
                delay(250L)
            }
            onUserChoice()
        }
    }

    /**
     * 获取用户选择需要操作的目录的Uri
     */

    fun handleUri(uri: Uri?) {
        // 这是用户选择的目录的Uri
        // 你可以在这里处理用户选择的目录
        if (uri == null) {
            inScanning = false
            return
        }

        if (conflictDialogResult.intValue == 2) {
            lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    getString(context, R.string.result_file_name)
                )
                file.delete()
                db.musicDao().deleteAll()
            }
        }

        scanResult.value = ""
        val absolutePath: String
        Toast.makeText(
            context,
            getString(context, R.string.selected_directory_path) + "$uri",
            Toast.LENGTH_SHORT
        ).show()
        absolutePath = UsefulTools().uriToAbsolutePath(uri)
        val directory = File(absolutePath)
        showLoadingProgressBar.value = true
        progressPercent.intValue = 0
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            delay(300L)
            scanDirectory(directory)
        }
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            delay(300L)
            while (true) {
                val lastScanResult = scanResult.value.length
                delay(250L)
                if (scanResult.value.length == lastScanResult) {
                    showLoadingProgressBar.value = false
                    inScanning = false
                    Toast.makeText(
                        context, R.string.scan_complete, Toast.LENGTH_SHORT
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
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            scanResult.value = "${file.name}\n${scanResult.value}"
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
        val fileName = getString(context, R.string.result_file_name)
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName
        )
        try {
            val songName = tag.getFirst(FieldKey.TITLE)
            val artistName = tag.getFirst(FieldKey.ARTIST)
            val albumName = tag.getFirst(FieldKey.ALBUM)
            val releaseYear = tag.getFirst(FieldKey.YEAR)
            val trackNumber = tag.getFirst(FieldKey.TRACK)
            val albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST)
            val genre = tag.getFirst(FieldKey.GENRE)

            val fileWriter = FileWriter(file, true)
            fileWriter.write(
                "$songName#*#$artistName#*#$albumName#*#$filePath#*#$lastIndex\n"
            )
            fileWriter.close()
            val music = com.hwinzniej.musichelper.data.model.Music(
                lastIndex,
                songName,
                artistName,
                albumName,
                filePath,
                releaseYear,
                trackNumber,
                albumArtist,
                genre
            )
            lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
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
}

@OptIn(UnstableSaltApi::class)
@Composable
fun ScanPageUi(
    scanPage: ScanPage,
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
            yesText = context.getString(R.string.file_conflict_dialog_yes_text)
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp)
                    .background(color = SaltTheme.colors.background)
                    .verticalScroll(rememberScrollState())
            ) {
                RoundedColumn {
                    ItemTitle(text = context.getString(R.string.scan_control))
                    ItemText(text = context.getString(R.string.touch_button_to_start_scanning))
                    ItemContainer {
                        TextButton(onClick = {
                            scanPage.init()
                        }, text = context.getString(R.string.start_text))
                    }
                }
                AnimatedVisibility(
                    visible = progressPercent.value != -1,
                ) {
                    RoundedColumn {
                        ItemTitle(text = context.getString(R.string.scanning_result))
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
        }
    }
}