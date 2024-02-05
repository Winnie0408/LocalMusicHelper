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

    fun checkServerPing() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val regex = "\\d+\\.\\d+".toRegex()
            async {
                val text = Tools().execShellCmd("ping -c 2 -q -W 3 'plnb-cf.hwinzniej.top'", true)
                if (text.contains("[1-2] received".toRegex()))
                    serverPing[0] = "${regex.findAll(text).toList()[3].value} ms"
                else
                    serverPing[0] = context.getString(R.string.timeout)
            }
            async {
                val text = Tools().execShellCmd("ping -c 2 -q -W 3 'dns.hwinzniej.top'", true)
                if (text.contains("[1-2] received".toRegex()))
                    serverPing[1] = "${regex.findAll(text).toList()[3].value} ms"
                else
                    serverPing[1] = context.getString(R.string.timeout)

            }
            async {
                val text = Tools().execShellCmd("ping -c 2 -q -W 3 'mom.hwinzniej.top'", true)
                if (text.contains("[1-2] received".toRegex()))
                    serverPing[2] = "${regex.findAll(text).toList()[3].value} ms"
                else
                    serverPing[2] = context.getString(R.string.timeout)
            }
        }
    }
}