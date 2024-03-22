package com.hwinzniej.musichelper.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.activity.SettingsPage
import com.hwinzniej.musichelper.activity.UnlockPage
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi

@OptIn(UnstableSaltApi::class)
@Composable
fun UnlockPageUi(
    unlockPage: UnlockPage,
    enableHaptic: MutableState<Boolean> = mutableStateOf(true),
    settingsPage: SettingsPage
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showLoadingProgressBar by remember { mutableStateOf(false) }
    var inputPath by remember { mutableStateOf("") }
    var outputPath by remember { mutableStateOf("") }
    var deleteEncryptedFile by remember { mutableStateOf(false) }
    var overwriteOutputFile by remember { mutableStateOf(true) }
    var umSupportOverWrite by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = settingsPage.umSupportOverWrite.value) {
        umSupportOverWrite = settingsPage.umSupportOverWrite.value
    }

    LaunchedEffect(key1 = unlockPage.selectedEncryptedPath.value) {
        inputPath = unlockPage.selectedEncryptedPath.value
    }

    LaunchedEffect(key1 = unlockPage.selectedDecryptedPath.value) {
        outputPath = unlockPage.selectedDecryptedPath.value
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {
        TitleBar(
            onBack = {},
            text = stringResource(id = R.string.unlock_function_name),
            showBackBtn = false
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
                RoundedColumn {
                    ItemTitle(text = stringResource(id = R.string.input_options))
                    Item(
                        text = stringResource(id = R.string.select_the_directory_for_encrypted_files),
                        onClick = {
                            unlockPage.selectInputDir()
                        }
                    )
                    AnimatedVisibility(
                        visible = inputPath != ""
                    ) {
                        ItemValue(
                            text = stringResource(R.string.input_path),
                            rightSub = inputPath
                        )
                    }
                    ItemSwitcher(
                        state = deleteEncryptedFile,
                        onChange = { deleteEncryptedFile = !deleteEncryptedFile },
                        text = stringResource(id = R.string.delete_encrypted_file),
                        sub = stringResource(id = R.string.delete_encrypted_file_sub),
                        enableHaptic = enableHaptic.value
                    )
                }
                RoundedColumn {
                    ItemTitle(text = stringResource(id = R.string.output_options))
                    Item(
                        text = stringResource(id = R.string.select_the_directory_for_decrypted_files),
                        onClick = {
                            unlockPage.selectOutputDir()
                        }
                    )
                    AnimatedVisibility(
                        visible = outputPath != ""
                    ) {
                        ItemValue(
                            text = stringResource(R.string.output_path),
                            rightSub = outputPath
                        )
                    }
                    ItemSwitcher(
                        enabled = umSupportOverWrite,
                        state = overwriteOutputFile && umSupportOverWrite,
                        onChange = { overwriteOutputFile = !overwriteOutputFile },
                        text = stringResource(id = R.string.overwrite_output_file),
                        sub = stringResource(id = R.string.overwrite_output_file_sub),
                        enableHaptic = enableHaptic.value
                    )
                }
                AnimatedContent(
                    targetState = showLoadingProgressBar,
                    label = "",
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    }) {
                    ItemContainer {
                        TextButton(
                            onClick = {
                                unlockPage.requestPermission()
                            }, text = stringResource(R.string.start_text),
                            enabled = !it,
                            enableHaptic = enableHaptic.value
                        )
                    }
                }
                AnimatedVisibility(
                    visible = false,// TODO
                ) {
                    RoundedColumn {
                        ItemTitle(text = stringResource(R.string.unlocking_result))
                        ItemContainer {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .heightIn(
                                        min = 25.dp,
                                        max = (LocalConfiguration.current.screenHeightDp / 2.55).dp
                                    )
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(color = SaltTheme.colors.background)
                            ) {
                                items(unlockPage.unlockResult.size) { index ->
                                    Text(
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 4.dp
                                        ),
                                        text = unlockPage.unlockResult[index].keys.first(),
                                        fontSize = 16.sp,
                                        style = TextStyle(
                                            color = SaltTheme.colors.subText
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
