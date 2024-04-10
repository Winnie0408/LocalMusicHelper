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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.activity.SettingsPage
import com.hwinzniej.musichelper.activity.UnlockPage
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi

@OptIn(UnstableSaltApi::class)
@Composable
fun UnlockPageUi(
    unlockPage: UnlockPage,
    enableHaptic: MutableState<Boolean>,
    settingsPage: SettingsPage,
    hapticStrength: MutableIntState
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var inputPath by remember { mutableStateOf("") }
    var outputPath by remember { mutableStateOf("") }
    var deleteEncryptedFile by remember { mutableStateOf(false) }
    var overwriteOutputFile by remember { mutableStateOf(true) }
    var umSupportOverWrite by remember { mutableStateOf(false) }
    var showInputOutputPathNotSameDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = settingsPage.umSupportOverWrite.value) {
        umSupportOverWrite = settingsPage.umSupportOverWrite.value
    }

    LaunchedEffect(key1 = unlockPage.selectedEncryptedPath.value) {
        inputPath = unlockPage.selectedEncryptedPath.value
    }

    LaunchedEffect(key1 = unlockPage.selectedDecryptedPath.value) {
        outputPath = unlockPage.selectedDecryptedPath.value
    }

    if (unlockPage.showUmStdoutDialog.value) {
        YesDialog(
            onDismissRequest = { unlockPage.showUmStdoutDialog.value = false },
            title = stringResource(id = R.string.um_stdout),
            content = null,
            enableHaptic = enableHaptic.value,
            hapticStrength = hapticStrength.intValue
        ) {
            ItemContainer {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = 25.dp,
                            max = (LocalConfiguration.current.screenHeightDp / 2.55).dp
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(color = SaltTheme.colors.subBackground)
                ) {
                    items(unlockPage.unlockResult.size) { index ->
                        Text(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 4.dp
                            ),
                            text = unlockPage.unlockResult[index].keys.first(),
                            fontSize = 14.sp,
                            color = if (unlockPage.unlockResult[index].values.first())
                                SaltTheme.colors.subText
                            else
                                colorResource(id = R.color.unmatched),
                        )
                    }
                }
            }
        }
    }

    if (showInputOutputPathNotSameDialog) {
        YesDialog(
            onDismissRequest = {
                showInputOutputPathNotSameDialog = false
            },
            title = stringResource(id = R.string.error),
            content = if (inputPath.isBlank() || outputPath.isBlank())
                stringResource(id = R.string.input_and_output_path_cant_null)
            else
                stringResource(id = R.string.input_output_path_cant_same),
            fontSize = 14.sp,
            enableHaptic = enableHaptic.value,
            hapticStrength = hapticStrength.intValue
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {
        TitleBar(
            onBack = {},
            text = stringResource(id = R.string.unlock_function_name) + if (settingsPage.umFileLegal.value)
                ""
            else
                stringResource(id = R.string.unavailable),
            showBackBtn = false
        )
        Box {
            if (unlockPage.showLoadingProgressBar.value) {
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
                        enabled = settingsPage.umFileLegal.value,
                        text = stringResource(id = R.string.select_the_directory_for_input_files),
                        onClick = {
                            unlockPage.selectInputDir()
                        }
                    )
                    AnimatedVisibility(
                        visible = inputPath != ""
                    ) {
                        ItemValue(
                            text = stringResource(R.string.you_have_selected),
                            rightSub = inputPath
                        )
                    }
                    ItemSwitcher(
                        enabled = settingsPage.umFileLegal.value,
                        state = deleteEncryptedFile,
                        onChange = {
                            deleteEncryptedFile = it
                            unlockPage.deleteEncryptedFile.value = it
                        },
                        text = stringResource(id = R.string.delete_input_file),
                        sub = stringResource(id = R.string.delete_input_file_sub),
                        enableHaptic = enableHaptic.value,
                        hapticStrength = hapticStrength.intValue
                    )
                }
                RoundedColumn {
                    ItemTitle(text = stringResource(id = R.string.output_options))
                    Item(
                        enabled = settingsPage.umFileLegal.value,
                        text = stringResource(id = R.string.select_the_directory_for_output_files),
                        onClick = {
                            unlockPage.selectOutputDir()
                        }
                    )
                    AnimatedVisibility(
                        visible = outputPath != ""
                    ) {
                        ItemValue(
                            text = stringResource(R.string.you_have_selected),
                            rightSub = outputPath
                        )
                    }
                    ItemSwitcher(
                        enabled = umSupportOverWrite && settingsPage.umFileLegal.value,
                        state = overwriteOutputFile && umSupportOverWrite,
                        onChange = {
                            overwriteOutputFile = it
                            unlockPage.overwriteOutputFile.value = it
                        },
                        text = stringResource(id = R.string.overwrite_output_file),
                        sub = if (umSupportOverWrite) stringResource(id = R.string.overwrite_output_file_sub)
                        else stringResource(id = R.string.current_um_not_support_this_fun) + "\n" +
                                stringResource(id = R.string.overwrite_output_file_sub),
                        enableHaptic = enableHaptic.value,
                        hapticStrength = hapticStrength.intValue
                    )
                }
                AnimatedContent(
                    targetState = unlockPage.showLoadingProgressBar.value,
                    label = "",
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    }) {
                    ItemContainer {
                        TextButton(
                            onClick = {
                                if ((inputPath == outputPath) || (inputPath.isBlank() || outputPath.isBlank())) {
                                    showInputOutputPathNotSameDialog = true
                                } else {
                                    unlockPage.requestPermission()
                                }
                            }, text = stringResource(R.string.start_text),
                            enabled = !it && settingsPage.umFileLegal.value,
                            enableHaptic = enableHaptic.value,
                            hapticStrength = hapticStrength.intValue
                        )
                    }
                }
            }
        }
    }
}
