package com.hwinzniej.musichelper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.hwinzniej.musichelper.R
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

@OptIn(UnstableSaltApi::class, ExperimentalFoundationApi::class)
@Composable
fun AboutPageUi(
    settingsPageState: PagerState,
    showNewVersionAvailableDialog: MutableState<Boolean>,
    latestVersion: MutableState<String>,
    latestDescription: MutableState<String>,
    latestDownloadLink: MutableState<String>
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

            },
            title = yesNoDialogTitle,
            content = yesNoDialogContent
        )
    }

    if (showYesDialog) {
        YesDialog(
            onDismissRequest = { showYesDialog = false },
            title = yesDialogTitle,
            content = yesDialogContent,
            fontSize = 14.sp
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {
        TitleBar(
            onBack = {
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
                        text = stringResource(
                            id = R.string.app_version
                        ), color = SaltTheme.colors.text
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                RoundedColumn {
//                ItemTitle(text = stringResource(id = R.string.update))
                    Item(
                        onClick = { /*TODO*/ }, text = stringResource(id = R.string.developers)
                    )
                    Item(
                        enabled = !showLoadingProgressBar,
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                showLoadingProgressBar = true
                                val client = OkHttpClient()
                                val request = Request.Builder()
                                    .url("https://gitee.com/winnie0408/MusicID3TagGetter/releases/latest")  //TODO 待更换为正式项目地址
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
                                    if (latestVersion.value != context.getString(R.string.app_version)) {
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
                                    }
                                } catch (e: Exception) {
                                    showYesDialog = true
                                    yesDialogTitle = context.getString(R.string.error)
                                    yesDialogContent =
                                        "${context.getString(R.string.check_connectivity)}\n${
                                            context.getString(
                                                R.string.error_details
                                            )
                                        }\n- ${e.message.toString()}"
                                } finally {
                                    showLoadingProgressBar = false
                                }
                            }
                        },
                        text = stringResource(id = R.string.check_for_updates)
                    )
                    Item(
                        onClick = { /*TODO*/ },
                        text = stringResource(id = R.string.open_source_licence)
                    )
                    Item(
                        onClick = { /*TODO*/ },
                        text = stringResource(id = R.string.pc_salt_converter)
                    )
                    Item(
                        onClick = { /*TODO*/ }, text = stringResource(id = R.string.buy_me_a_coffee)
                    )
                }

                RoundedColumn {
                    Item(
                        onClick = { /*TODO*/ },
                        text = stringResource(id = R.string.join_qq_group),
                        iconPainter = painterResource(id = R.drawable.ic_check)
                    )
                    Item(
                        onClick = { /*TODO*/ },
                        text = stringResource(id = R.string.follow_on_bilibili),
                        iconPainter = painterResource(id = R.drawable.ic_check)
                    )
                    Item(
                        onClick = { /*TODO*/ },
                        text = stringResource(id = R.string.open_source_github),
                        sub = stringResource(id = R.string.open_source_sub),
                        iconPainter = painterResource(id = R.drawable.ic_check)
                    )
                    Item(
                        onClick = { /*TODO*/ },
                        text = stringResource(id = R.string.open_source_gitee),
                        sub = stringResource(id = R.string.open_source_sub),
                        iconPainter = painterResource(id = R.drawable.ic_check)
                    )
                }

            }
        }
    }
}

@OptIn(UnstableSaltApi::class, ExperimentalFoundationApi::class)
@Preview
@Composable
fun Preview1() {
//    AboutPageUi()
}