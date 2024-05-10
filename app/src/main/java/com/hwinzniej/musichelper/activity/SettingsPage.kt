package com.hwinzniej.musichelper.activity

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.data.DataStoreConstants
import com.hwinzniej.musichelper.utils.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsPage(
    val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    val openUmExecutableFileLauncher: ActivityResultLauncher<Array<String>>,
    val dataStore: DataStore<Preferences>,
) {
    val enableAutoCheckUpdate = mutableStateOf(true)
    val encryptServer = mutableStateOf("")
    val serverPing = mutableStateMapOf(
        0 to context.getString(R.string.pinging),
        1 to context.getString(R.string.pinging),
        2 to context.getString(R.string.pinging)
    )
    val showDialogProgressBar = mutableStateOf(false)
    val initialPage = mutableIntStateOf(0)

    fun checkServerPing() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            showDialogProgressBar.value = true
            val regex = "\\d+\\.\\d+".toRegex()
            val t1 = async {
                val text = Tools().execShellCmd("ping -c 1 -q -W 3 'plnb-cf.hwinzniej.top'", true)
                delay(300L)
                if (text.contains("1 received"))
                    serverPing[0] = "${regex.findAll(text).toList()[3].value} ms"
                else
                    serverPing[0] = context.getString(R.string.timeout)
            }
            val t2 = async {
                val text = Tools().execShellCmd("ping -c 1 -q -W 3 'plnb1.hwinzniej.top'", true)
                delay(300L)
                if (text.contains("1 received"))
                    serverPing[1] = "${regex.findAll(text).toList()[3].value} ms"
                else
                    serverPing[1] = context.getString(R.string.timeout)
            }
            val t3 = async {
                val text = Tools().execShellCmd("ping -c 1 -q -W 3 'plnb2.hwinzniej.top'", true)
                delay(300L)
                if (text.contains("1 received"))
                    serverPing[2] = "${regex.findAll(text).toList()[3].value} ms"
                else
                    serverPing[2] = context.getString(R.string.timeout)
            }
            t1.await()
            t2.await()
            t3.await()
            showDialogProgressBar.value = false
        }
    }

    fun selectUmFile() {
        try {
            openUmExecutableFileLauncher.launch(arrayOf("*/*"))
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.unable_start_documentsui),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    var umFileLegal = mutableStateOf(false)
    var umSupportOverWrite = mutableStateOf(false)

    fun checkUmFile(uri: Uri?) {
        if (uri == null)
            return
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)

            val internalFile = File(context.filesDir, "um_executable")
            if (internalFile.exists())
                internalFile.delete()

            val outputStream = internalFile.outputStream()

            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            try {
                Tools().execShellCmd(
                    cmd = "chmod 700 ${internalFile.absolutePath}",
                    withoutRoot = true
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val result = Tools().execShellCmd(
                    cmd = "${internalFile.absolutePath} -v",
                    withoutRoot = true
                )
                if (result.contains("Unlock Music CLI version", true)) {
                    dataStore.edit { settings ->
                        settings[DataStoreConstants.UM_FILE_LEGAL] = true
                    }
                    umFileLegal.value = true
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.valid_um_file),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    val supportOverwrite = Tools().execShellCmd(
                        cmd = "${internalFile.absolutePath} -h",
                        withoutRoot = true
                    )
                    if (supportOverwrite.contains("--overwrite", true)) {
                        dataStore.edit { settings ->
                            settings[DataStoreConstants.UM_SUPPORT_OVERWRITE] = true
                        }
                        umSupportOverWrite.value = true
                    } else {
                        dataStore.edit { settings ->
                            settings[DataStoreConstants.UM_SUPPORT_OVERWRITE] = false
                        }
                        umSupportOverWrite.value = false
                    }
                } else {
                    dataStore.edit { settings ->
                        settings[DataStoreConstants.UM_FILE_LEGAL] = false
                    }
                    umFileLegal.value = false
                    internalFile.delete()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.invalid_um_file),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}