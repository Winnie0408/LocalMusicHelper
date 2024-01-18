package com.hwinzniej.musichelper.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.utils.MyVibrationEffect
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@OptIn(UnstableSaltApi::class, ExperimentalFoundationApi::class)
@Composable
fun AboutPageUi(
    settingsPageState: PagerState,
    showNewVersionAvailableDialog: MutableState<Boolean>,
    latestVersion: MutableState<String>,
    latestDescription: MutableState<String>,
    latestDownloadLink: MutableState<String>,
    enableHaptic: MutableState<Boolean>
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showLoadingProgressBar by remember { mutableStateOf(false) }
    var showYesNoDialog by remember { mutableStateOf(false) }
    var yesNoDialogTitle by remember { mutableStateOf("") }
    var yesNoDialogContent by remember { mutableStateOf("") }
    var showYesDialog by remember { mutableStateOf(false) }
    var yesDialogTitle by remember { mutableStateOf("") }
    var yesDialogContent by remember { mutableStateOf("") }
    var yesDialogCustomContent by remember { mutableStateOf<@Composable () -> Unit>({}) }
    var yesNoDialogOnConfirm by remember { mutableStateOf({}) }


    BackHandler(enabled = settingsPageState.currentPage == 1) {
        coroutineScope.launch {
            settingsPageState.animateScrollToPage(
                0, animationSpec = spring(2f)
            )
        }
    }

    if (showYesNoDialog) {
        YesNoDialog(
            onDismiss = { showYesNoDialog = false },
            onCancel = { showYesNoDialog = false },
            onConfirm = {
                showYesNoDialog = false
                yesNoDialogOnConfirm()
            },
            title = yesNoDialogTitle,
            content = yesNoDialogContent.ifEmpty { null },
            enableHaptic = enableHaptic.value
        )
    }

    if (showYesDialog) {
        YesDialog(
            onDismissRequest = { showYesDialog = false },
            title = yesDialogTitle,
            content = yesDialogContent,
            fontSize = 14.sp,
            enableHaptic = enableHaptic.value,
            customContent = yesDialogCustomContent,
            onlyComposeView = yesDialogContent.isEmpty()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {
        TitleBar(
            onBack = {
                MyVibrationEffect(context, enableHaptic.value).click()
                coroutineScope.launch {
                    settingsPageState.animateScrollToPage(
                        0, animationSpec = spring(2f)
                    )
                }
            },
            text = stringResource(id = R.string.about_function_name),
        )
        Box {
            if (showLoadingProgressBar) {
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
                    .background(color = SaltTheme.colors.background)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(
                            id = R.string.app_name
                        ), fontSize = 20.sp, color = SaltTheme.colors.text
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = context.packageManager.getPackageInfo(
                            context.packageName,
                            0
                        ).versionName, color = SaltTheme.colors.text
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                RoundedColumn {
                    ItemTitle(text = stringResource(id = R.string.related_info))
                    Item(
                        onClick = {
                            yesDialogContent = ""
                            yesDialogCustomContent = {
                                Column {
                                    RoundedColumn {
                                        ItemTitle(text = stringResource(id = R.string.app_developer))
                                        Text(
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 8.dp
                                            ),
                                            text = "HWinZnieJ",
                                            color = SaltTheme.colors.text,
                                            fontSize = 14.sp
                                        )
                                    }
                                    RoundedColumn {
                                        ItemTitle(text = stringResource(id = R.string.app_translator))
                                        Text(
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 8.dp
                                            ),
                                            text = "HWinZnieJ & DeepL Translator",
                                            color = SaltTheme.colors.text,
                                            fontSize = 14.sp
                                        )
                                    }
                                    RoundedColumn {
                                        ItemTitle(text = stringResource(id = R.string.app_technical_supportor))
                                        Text(
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 8.dp
                                            ),
                                            text = "Microsoft Copilot & GitHub Copilot & OpenAI GPT-4",
                                            color = SaltTheme.colors.text,
                                            fontSize = 14.sp
                                        )
                                    }
                                    RoundedColumn {
                                        ItemTitle(text = stringResource(id = R.string.app_special_thanks))
                                        Text(
                                            modifier = Modifier
                                                .padding(
                                                    horizontal = 16.dp,
                                                    vertical = 8.dp
                                                )
                                                .heightIn(max = 120.dp)
                                                .verticalScroll(rememberScrollState()),
                                            text = "${
                                                stringResource(id = R.string.coolapk)
                                            }\n叶谖儿、网恋被骗九八上单、路还要走、星辰与月、破晓px、OmegaFallen、鷄你太魅、暮雨江天、PO8的惊堂木、叁陈小洋楼、白给少年又来了、梦中之城你和TA、大帅帅帅帅逼、不良人有品大帅、大泉麻衣、zz_xmy、迷茫和乐观的小陈\n\n${
                                                stringResource(id = R.string.qq_group)
                                            }\n过客、　、.、王八仨水、路还要走、天中HD、曾喜樂、。、ZERO、60、MATURE、Xik-、Zj、这是一个名字、唯爱、天择\uD83D\uDCAB、九江、迷雾水珠、K、七、xmy、大泉麻衣、w、Sandmい旧梦、奔跑吧，兄弟！、吔—、匿名用户、S\n\n${
                                                stringResource(
                                                    id = R.string.rank_no_order
                                                )
                                            }",
                                            color = SaltTheme.colors.text,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                            yesDialogTitle = context.getString(R.string.staff)
                            showYesDialog = true
                        }, text = stringResource(id = R.string.staff),
                        iconPainter = painterResource(id = R.drawable.developer),
                        iconColor = SaltTheme.colors.text
                    )
                    Item(
                        enabled = !showLoadingProgressBar,
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                showLoadingProgressBar = true
                                val client = OkHttpClient()
                                val request = Request.Builder()
                                    .url("https://gitee.com/winnie0408/LocalMusicHelper/releases/latest")
                                    .header("Accept", "application/json")
                                    .get()
                                    .build()
                                try {
                                    val response = JSON.parseObject(
                                        client.newCall(request).execute().body?.string()
                                    )
                                    val latestTag =
                                        (response.get("release") as JSONObject).get("release")
                                    latestVersion.value =
                                        (latestTag as JSONObject).getString("title")
                                            .replace("v", "")
                                    if (latestVersion.value != context.packageManager.getPackageInfo(
                                            context.packageName,
                                            0
                                        ).versionName
                                    ) {
                                        latestDescription.value = latestTag.getString("description")
                                            .replace("</?[a-z]+>".toRegex(), "")
                                            .replace("\n\n", "\n")

                                        latestTag.getJSONArray("attach_files").forEach {
                                            if ((it as JSONObject).getString("name")
                                                    .contains("release")
                                            ) {
                                                latestDownloadLink.value =
                                                    "https://gitee.com${it.getString("download_url")}"
                                                return@forEach
                                            }
                                        }
                                        showNewVersionAvailableDialog.value = true
                                    } else {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.no_update_available),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    yesDialogCustomContent = {}
                                    yesDialogTitle = context.getString(R.string.error)
                                    yesDialogContent =
                                        "${context.getString(R.string.check_connectivity)}\n${
                                            context.getString(
                                                R.string.error_details
                                            )
                                        }\n- ${e.message.toString()}"
                                    showYesDialog = true
                                } finally {
                                    showLoadingProgressBar = false
                                }
                            }
                        },
                        text = stringResource(id = R.string.check_for_updates),
                        iconPainter = painterResource(id = R.drawable.check_update),
                        iconColor = SaltTheme.colors.text,
                        iconPaddingValues = PaddingValues(
                            start = 1.5.dp,
                            end = 1.5.dp,
                            top = 1.5.dp,
                            bottom = 1.5.dp
                        )
                    )
                    Item(
                        onClick = {
                            yesDialogContent = ""
                            yesDialogCustomContent = {
                                val currentTheme = SaltTheme.colors.text.red
                                Column(
                                    modifier = Modifier
                                        .heightIn(
                                            min = 0.1.dp,
                                            max = (LocalConfiguration.current.screenHeightDp / 1.8).dp
                                        )
                                ) {
                                    AndroidView(factory = { WebView(context) }) { webView ->
                                        val licensesHtml = if (currentTheme < 0.5f) {
                                            context.assets.open("licenses.html")
                                                .use { inputStream ->
                                                    inputStream.bufferedReader().use {
                                                        it.readText()
                                                    }
                                                }
                                        } else {
                                            context.assets.open("licenses_dark.html")
                                                .use { inputStream ->
                                                    inputStream.bufferedReader().use {
                                                        it.readText()
                                                    }
                                                }
                                        }
                                        webView.setBackgroundColor(0)
                                        webView.settings.javaScriptEnabled = false
                                        webView.loadDataWithBaseURL(
                                            null,
                                            licensesHtml,
                                            "text/html",
                                            "utf-8",
                                            null
                                        )
                                    }
                                }
                            }
                            yesDialogTitle = context.getString(R.string.open_source_licence)
                            showYesDialog = true
                        },
                        text = stringResource(id = R.string.open_source_licence),
                        iconPainter = painterResource(id = R.drawable.license),
                        iconColor = SaltTheme.colors.text
                    )
                    Item(
                        onClick = {
                            yesNoDialogOnConfirm = {
                                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(
                                        "https://saltconv.hwinzniej.top:45999"
                                    )
                                })
                            }
                            yesNoDialogTitle = context.getString(R.string.visit_the_link_below)
                            yesNoDialogContent = "https://saltconv.hwinzniej.top:45999"
                            showYesNoDialog = true
                        },
                        text = stringResource(id = R.string.pc_salt_converter),
                        iconPainter = painterResource(id = R.drawable.computer),
                        iconColor = SaltTheme.colors.text
                    )
                    Item(
                        onClick = {
                            yesDialogContent = ""
                            yesDialogCustomContent = {
                                Column(
                                    modifier = Modifier
                                        .heightIn(
                                            min = 0.1.dp,
                                            max = (LocalConfiguration.current.screenHeightDp / 1.5).dp
                                        )
                                        .align(Alignment.CenterHorizontally)
                                ) {
                                    Text(
                                        modifier = Modifier.align(Alignment.CenterHorizontally),
                                        text = stringResource(id = R.string.thank_you_very_much),
                                        color = SaltTheme.colors.text,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Image(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .size(200.dp)
                                            .clickable {
                                                val bitmap = BitmapFactory.decodeResource(
                                                    context.resources,
                                                    R.drawable.alipay
                                                )
                                                try {
                                                    val path =
                                                        Environment.getExternalStoragePublicDirectory(
                                                            Environment.DIRECTORY_PICTURES
                                                        ).absolutePath
                                                    val directory = File(path)
                                                    if (!directory.exists()) {
                                                        directory.mkdir()
                                                    }
                                                    val file =
                                                        File(directory, "hwinzniej_alipay.jpg")
                                                    val out = file.outputStream()
                                                    bitmap.compress(
                                                        Bitmap.CompressFormat.JPEG,
                                                        100,
                                                        out
                                                    )
                                                    out.flush()
                                                    out.close()
                                                    context.sendBroadcast(
                                                        Intent(
                                                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                                            Uri.fromFile(file)
                                                        )
                                                    )
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            context.getString(R.string.save_success),
                                                            Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                } catch (_: Exception) {
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            context.getString(R.string.save_failed),
                                                            Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                }
                                            },
                                        painter = painterResource(id = R.drawable.alipay),
                                        contentDescription = ""
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Image(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .size(200.dp)
                                            .clickable {
                                                val bitmap = BitmapFactory.decodeResource(
                                                    context.resources,
                                                    R.drawable.wechat
                                                )
                                                try {
                                                    val path =
                                                        Environment.getExternalStoragePublicDirectory(
                                                            Environment.DIRECTORY_PICTURES
                                                        ).absolutePath
                                                    val directory = File(path)
                                                    if (!directory.exists()) {
                                                        directory.mkdir()
                                                    }
                                                    val file =
                                                        File(directory, "hwinzniej_wechat.png")
                                                    val out = file.outputStream()
                                                    bitmap.compress(
                                                        Bitmap.CompressFormat.PNG,
                                                        100,
                                                        out
                                                    )
                                                    out.flush()
                                                    out.close()
                                                    context.sendBroadcast(
                                                        Intent(
                                                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                                            Uri.fromFile(file)
                                                        )
                                                    )
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            context.getString(R.string.save_success),
                                                            Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                } catch (_: Exception) {
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            context.getString(R.string.save_failed),
                                                            Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                }
                                            },
                                        painter = painterResource(id = R.drawable.wechat),
                                        contentDescription = ""
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        modifier = Modifier.align(Alignment.CenterHorizontally),
                                        text = stringResource(id = R.string.save_image),
                                        color = SaltTheme.colors.text,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            yesDialogTitle = context.getString(R.string.buy_me_a_coffee)
                            showYesDialog = true
                        },
                        text = stringResource(id = R.string.buy_me_a_coffee),
                        iconPainter = painterResource(id = R.drawable.coffee),
                        iconColor = SaltTheme.colors.text,
                        iconPaddingValues = PaddingValues(
                            start = 0.5.dp,
                            end = 0.5.dp,
                            top = 0.5.dp,
                            bottom = 0.5.dp
                        )
                    )
                }

                RoundedColumn {
                    ItemTitle(text = stringResource(id = R.string.contact_developer))
                    Item(
                        onClick = {
                            yesNoDialogOnConfirm = {
                                try {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("mqqapi://card/show_pslcard?src_type=internal&version=1&card_type=group&uin=931819834")
                                        )
                                    )
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.launching_app)
                                            .replace("#", "QQ"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "${
                                            context.getString(R.string.app_not_installed)
                                                .replace("#", "QQ")
                                        }, ${
                                            context.getString(R.string.will_open_in_browser)
                                        }",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://qm.qq.com/q/dMPQiCYp8c")
                                        )
                                    )
                                }
                            }
                            yesNoDialogTitle = context.getString(R.string.open_in_some_app)
                                .replace("#", "QQ")
                            yesNoDialogContent = ""
                            showYesNoDialog = true
                        },
                        text = stringResource(id = R.string.join_qq_group),
                        iconPainter = painterResource(id = R.drawable.qq_group),
                        iconPaddingValues = PaddingValues(
                            start = 1.dp,
                            end = 1.dp,
                            top = 1.dp,
                            bottom = 1.dp
                        )
                    )
                    Item(
                        onClick = {
                            yesNoDialogOnConfirm = {
                                try {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("coolmarket://u/1844460")
                                        )
                                    )
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.launching_app).replace(
                                            "#", context.getString(
                                                R.string.coolapk
                                            ).replace("(：)|(: )".toRegex(), "")
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "${
                                            context.getString(R.string.app_not_installed).replace(
                                                "#", context.getString(R.string.coolapk)
                                                    .replace("(：)|(: )".toRegex(), "")
                                            )
                                        }, ${
                                            context.getString(R.string.will_open_in_browser)
                                        }",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("http://www.coolapk.com/u/1844460")
                                        )
                                    )
                                }
                            }
                            yesNoDialogTitle = context.getString(R.string.open_in_some_app)
                                .replace(
                                    "#",
                                    context.getString(R.string.coolapk)
                                        .replace("(：)|(: )".toRegex(), "")
                                )
                            yesNoDialogContent = ""
                            showYesNoDialog = true
                        },
                        text = stringResource(id = R.string.follow_on_coolapk),
                        iconPainter = painterResource(id = R.drawable.coolapk)
                    )
                    Item(
                        onClick = {
                            yesNoDialogOnConfirm = {
                                try {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("bilibili://space/221114757")
                                        )
                                    )
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.launching_app)
                                            .replace("#", "BiliBili"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "${
                                            context.getString(R.string.app_not_installed)
                                                .replace("#", "BiliBili")
                                        }, ${
                                            context.getString(R.string.will_open_in_browser)
                                        }",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://space.bilibili.com/221114757")
                                        )
                                    )
                                }
                            }
                            yesNoDialogTitle = context.getString(R.string.open_in_some_app)
                                .replace("#", "BiliBili")
                            yesNoDialogContent = ""
                            showYesNoDialog = true
                        },
                        text = stringResource(id = R.string.follow_on_bilibili),
                        iconPainter = painterResource(id = R.drawable.bilibili),
                        iconPaddingValues = PaddingValues(
                            start = 1.dp,
                            end = 1.dp,
                            top = 1.dp,
                            bottom = 1.dp
                        )
                    )
                    Item(
                        onClick = {
                            yesNoDialogOnConfirm = {
                                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(
                                        "https://github.com/Winnie0408/LocalMusicHelper"
                                    )
                                })
                            }
                            yesNoDialogTitle = context.getString(R.string.visit_the_link_below)
                            yesNoDialogContent = "https://github.com/Winnie0408/LocalMusicHelper"
                            showYesNoDialog = true
                        },
                        text = stringResource(id = R.string.open_source_github),
                        sub = stringResource(id = R.string.open_source_sub),
                        iconPainter = painterResource(id = R.drawable.github),
                        iconColor = SaltTheme.colors.text
                    )
                    Item(
                        onClick = {
                            yesNoDialogOnConfirm = {
                                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(
                                        "https://gitee.com/winnie0408/LocalMusicHelper"
                                    )
                                })
                            }
                            yesNoDialogTitle = context.getString(R.string.visit_the_link_below)
                            yesNoDialogContent = "https://gitee.com/winnie0408/LocalMusicHelper"
                            showYesNoDialog = true
                        },
                        text = stringResource(id = R.string.open_source_gitee),
                        sub = stringResource(id = R.string.open_source_sub),
                        iconPainter = painterResource(id = R.drawable.gitee),
                        iconPaddingValues = PaddingValues(
                            start = 1.dp,
                            end = 1.dp,
                            top = 1.dp,
                            bottom = 1.dp
                        )
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun Preview1() {
//    AboutPageUi()
}