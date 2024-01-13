package com.hwinzniej.musichelper.activity

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.data.database.MusicDatabase
import com.hwinzniej.musichelper.utils.Tools
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
            val lastChars = Tools().readLastNChars(file, 8)
            lastIndex =
                lastChars.substring(lastChars.lastIndexOf("#") + 1, lastChars.length).toInt()
        }
//        }

        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            //TODO: 优化冲突对话框的判断 Semaphore？
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
        Toast.makeText(
            context,
            getString(context, R.string.selected_directory_path) + "$uri",
            Toast.LENGTH_SHORT
        ).show()
        val absolutePath: String = Tools().uriToAbsolutePath(uri)
        val directory = File(absolutePath)
        showLoadingProgressBar.value = true
        progressPercent.intValue = 0
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            delay(300L)
            scanDirectory(directory)
        }
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            //TODO: 优化扫描完成的判断 Semaphore？
            delay(500L)
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
