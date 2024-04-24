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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.alibaba.fastjson2.parseObject
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.utils.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class UnlockPage(
    val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    componentActivity: ComponentActivity,
    val openEncryptDirectoryLauncher: ActivityResultLauncher<Uri?>,
    val openDecryptDirectoryLauncher: ActivityResultLauncher<Uri?>,
    val settingsPage: SettingsPage
) : PermissionResultHandler {
    var operateResult = mutableStateListOf<Map<String, Boolean>>()
    private var operateResultString = mutableStateOf("")
    var selectedInputPath = mutableStateOf("")
    var selectedOutputPath = mutableStateOf("")
    var deleteOriginalFile = mutableStateOf(false)
    var overwriteOutputFile = mutableStateOf(true)
    var showLoadingProgressBar = mutableStateOf(false)
    var showUmStdoutDialog = mutableStateOf(false)

    /**
     * 请求存储权限
     */

//==================== Android 11+ 使用====================

    @RequiresApi(Build.VERSION_CODES.R)
    private val requestPermissionLauncher = componentActivity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Environment.isExternalStorageManager()) {
            init()
        } else {
            Toast.makeText(context, R.string.permission_not_granted_toast, Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun requestPermission() {
        showLoadingProgressBar.value = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { //Android 11+
            if (Environment.isExternalStorageManager()) {
                init()
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
                init()
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
                init()
            } else {
                Toast.makeText(context, R.string.permission_not_granted_toast, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
//==================== Android 10- 使用====================

    fun selectInputDir() {
        try {
            openEncryptDirectoryLauncher.launch(null)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.unable_start_documentsui),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun selectOutputDir() {
        try {
            openDecryptDirectoryLauncher.launch(null)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.unable_start_documentsui),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun handleSelectedInputPath(uri: Uri?) {
        if (uri == null)
            return
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            val temp = Tools().uriToAbsolutePath(uri)
            if (isAllAscii(temp)) {
                delay(200L)
                selectedInputPath.value = Tools().uriToAbsolutePath(uri)
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.only_ascii),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun handleSelectedOutputPath(uri: Uri?) {
        if (uri == null)
            return
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            val temp = Tools().uriToAbsolutePath(uri)
            if (isAllAscii(temp)) {
                delay(200L)
                selectedOutputPath.value = Tools().uriToAbsolutePath(uri)
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.only_ascii),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun init() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            operateResult.clear()
            operateResultString.value = ""
            val internalFile = File(context.filesDir, "um_executable")
            if (!internalFile.exists()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.um_file_not_found),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                showLoadingProgressBar.value = false
                return@launch
            } else {
                try {
                    val result = Tools().execShellCmd(
                        cmd = "${internalFile.absolutePath} -v",
                        withoutRoot = true
                    )
                    if (!result.contains("Unlock Music CLI version", true)) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.um_file_not_found),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        showLoadingProgressBar.value = false
                        return@launch
                    }
                    var cmd =
                        "${internalFile.absolutePath} -i ${selectedInputPath.value} -o ${selectedOutputPath.value}"
                    if (deleteOriginalFile.value)
                        cmd += " --rs"

                    if (overwriteOutputFile.value && settingsPage.umSupportOverWrite.value)
                        cmd += " --overwrite"

                    operateResultString.value = Tools().execShellCmd(
                        cmd = cmd,
                        withoutRoot = true,
                    )
                    val resultArray = operateResultString.value.split("\n")
                    for (i in resultArray) {
                        if (i.isBlank())
                            continue
                        if (i.contains("successfully converted", true)) {
                            val filePath =
                                i.split("\t")[i.count { it == '\t' }].parseObject()
                                    .getString("destination")
                            val fileName = filePath.substring(filePath.lastIndexOf("/") + 1)
                            operateResult.add(mapOf(fileName to true))
                        } else {
                            val filePath =
                                i.split("\t")[i.count { it == '\t' }].parseObject()
                                    .getString("source")
                            val fileName = filePath.substring(filePath.lastIndexOf("/") + 1)
                            operateResult.add(0, mapOf(fileName to false))
                        }
                    }
                    showLoadingProgressBar.value = false
                    showUmStdoutDialog.value = true
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.error_while_um_convert),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    showLoadingProgressBar.value = false
                    return@launch
                }
            }
        }
    }

    private fun isAllAscii(input: String): Boolean {
        input.forEach { char ->
            if (char.code > 0x7F) {
                return false
            }
        }
        return true
    }
}