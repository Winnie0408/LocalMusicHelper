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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.lifecycleScope
import com.hwinzniej.musichelper.ui.theme.MusicHelperTheme
import com.moriafly.salt.ui.BottomBar
import com.moriafly.salt.ui.BottomBarItem
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.ItemSpacer
import com.moriafly.salt.ui.ItemText
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TextButton
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import kotlinx.coroutines.Dispatchers
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusicHelperTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
//                        Greeting("Android")
                    MainUI(this, scanResult)
                }
            }
        }
    }

    fun ScanMusic() {
        scanResult.value = ""
        progressPercent = 0
//        for (i in 1..100) {
//            scanResult.value =
//                "第 $i 首歌：aaabbb-cccddd-eeefff\n" + scanResult.value
//        }
        openDirectoryLauncher.launch(null)
    }

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
                    lifecycleScope.launch(Dispatchers.IO) { handleFile(curFile) }
                }
            }
        }
    }

    fun handleFile(file: File) {
        // 在这里处理文件
        val audioFile: AudioFile
        try {
            audioFile = AudioFileIO.read(file)
        } catch (e: Exception) {
            return
        }
        scanResult.value = file.path + "\n" + scanResult.value
        val tag = audioFile.tag
        writeToFile(tag, file.path)

    }

    var progressPercent = 0

    @Synchronized
    private fun writeToFile(tag: Tag, filePath: String) {
//        val fileName = resources.getString(R.string.outputFileName)
        val fileName = "MusicHelper.txt"
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        try {
            val fileWriter = FileWriter(file, true)
            fileWriter.write(
                tag.getFirst(FieldKey.TITLE) + "#*#" + tag.getFirst(FieldKey.ARTIST) + "#*#" + tag.getFirst(
                    FieldKey.ALBUM
                ) + "#*#" + filePath + "#*#" + progressPercent + "\n"
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

    @Synchronized
    private fun increment() {
        ++progressPercent
    }

    private val openDirectoryLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            // 这是用户选择的目录的Uri
            // 你可以在这里处理用户选择的目录
            var absolutePath = ""
            val uriPath = uri?.pathSegments?.get(uri.pathSegments!!.size - 1).toString()
            Toast.makeText(this, "Selected Directory: $uri", Toast.LENGTH_SHORT).show()
            if (uriPath.contains("primary")) {  //内部存储
                absolutePath = uriPath.replace("primary:", "/storage/emulated/0/")
            } else {  //SD卡
                absolutePath = "/storage/" + uriPath.split(":")[0] + "/" + uriPath.split(":")[1]
            }
            scanResult.value += "Selected Directory: $absolutePath"
            val directory = File(absolutePath)
            scanDirectory(directory)
        }

    fun requestPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { //Android 11+
            if (Environment.isExternalStorageManager()) {
                ScanMusic()
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
                    context as Activity,
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }
    }

    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUIRED_PERMISSIONS =
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

    fun allPermissionsGranted(context: Context): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted(this)) {
                // All permissions have been granted, you can proceed with the next operation
                // For example, you can call a method here to start scanning music
                ScanMusic()
            } else {
                // Permissions have been denied, you can inform the user about the need for these permissions
                // You can use a Toast, Snackbar, or a dialog to inform the user
                Toast.makeText(this, "Permissions are required to scan music", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

}

@OptIn(UnstableSaltApi::class)
@Composable
private fun MainUI(mainActivity: MainActivity, scanResult: MutableState<String>) {
    val coroutineScope = rememberCoroutineScope()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {
        TitleBar(
            onBack = {

            },
            text = "扫描"
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
                ItemSpacer()
                TextButton(onClick = {
                    mainActivity.requestPermission(mainActivity)

                }, text = "开始")
            }
            RoundedColumn {
                ItemTitle(text = "扫描结果")
                ItemContainer {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(color = SaltTheme.colors.background)
                    ) {
                        item {
                            Text(
                                text = scanResult.value,
                                fontSize = 16.sp,
                                style = TextStyle(
                                    lineHeight = 1.5.em,
                                ),
                            )
                        }
                    }
                }
            }
        }
        BottomBar {
            BottomBarItem(
                state = true,
                onClick = {

                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = "扫描"
            )
            BottomBarItem(
                state = false,
                onClick = {

                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = "转换"
            )
            BottomBarItem(
                state = false,
                onClick = {

                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = "处理"
            )
            BottomBarItem(
                state = false,
                onClick = {

                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = "关于"
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