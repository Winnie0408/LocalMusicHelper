package com.hwinzniej.musichelper.activity

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.utils.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsPage(
    val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) {
    var enableAutoCheckUpdate = mutableStateOf(true)
    var encryptServer = mutableStateOf("")
    var serverPing = mutableStateMapOf(
        0 to context.getString(R.string.pinging),
        1 to context.getString(R.string.pinging),
        2 to context.getString(R.string.pinging)
    )
    var showDialogProgressBar = mutableStateOf(false)

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
                val text = Tools().execShellCmd("ping -c 1 -q -W 3 'dc.hwinzniej.top'", true)
                delay(300L)
                if (text.contains("1 received"))
                    serverPing[1] = "${regex.findAll(text).toList()[3].value} ms"
                else
                    serverPing[1] = context.getString(R.string.timeout)
            }
            val t3 = async {
                val text = Tools().execShellCmd("ping -c 1 -q -W 3 'mom.hwinzniej.top'", true)
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
}