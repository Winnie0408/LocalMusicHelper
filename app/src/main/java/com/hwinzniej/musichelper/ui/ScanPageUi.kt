package com.hwinzniej.musichelper.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.activity.ScanPage
import com.hwinzniej.musichelper.utils.MyVibrationEffect
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.popup.rememberPopupState
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(UnstableSaltApi::class)
@Composable
fun ScanPageUi(
    scanPage: ScanPage,
    scanResult: MutableList<String>,
    showLoadingProgressBar: MutableState<Boolean>,
    progressPercent: MutableState<Int>,
    showConflictDialog: MutableState<Boolean>,
    exportResultFile: MutableState<Boolean>,
    selectedExportFormat: MutableIntState,
    enableHaptic: MutableState<Boolean>
) {
    val context = LocalContext.current
    if (showConflictDialog.value) {
        YesNoDialog(
            onCancel = { scanPage.userChoice(1) },
            onConfirm = { scanPage.userChoice(2) },
            onDismiss = { showConflictDialog.value = false },
            title = stringResource(R.string.file_conflict_dialog_title),
            content = stringResource(R.string.file_conflict_dialog_content).replace("#n", "\n"),
            cancelText = stringResource(R.string.file_conflict_dialog_no_text),
            confirmText = stringResource(R.string.file_conflict_dialog_yes_text),
            enableHaptic = enableHaptic.value
        )
    }

    if (scanPage.showErrorDialog.value) {
        YesDialog(
            onDismissRequest = { scanPage.showErrorDialog.value = false },
            title = stringResource(id = R.string.scan_complete_but_have_error),
            content = "",
            onlyComposeView = true,
            enableHaptic = enableHaptic.value,
            customContent = {
                RoundedColumn {
                    ItemTitle(text = stringResource(id = R.string.error_details))
                    LazyColumn(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .heightIn(
                                min = 20.dp,
                                max = (LocalConfiguration.current.screenHeightDp / 2.5).dp
                            )
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        item {
                            MarkdownText(
                                modifier = Modifier.padding(bottom = 8.dp),
                                markdown = scanPage.errorLog.value,
                                style = TextStyle(
                                    color = SaltTheme.colors.text,
                                    fontSize = 14.sp
                                ),
                                isTextSelectable = true,
                                disableLinkMovementMethod = true
                            )
                        }
                    }
                }
            }
        )
    }

    val exportTypePopupState = rememberPopupState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {
        TitleBar(
            onBack = {}, text = stringResource(R.string.scan_function_name), showBackBtn = false
        )
        Box {
            if (showLoadingProgressBar.value) {
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
                    .padding(top = 4.dp)
                    .background(color = SaltTheme.colors.background)
                    .verticalScroll(rememberScrollState())
            ) {
                RoundedColumn {
                    ItemTitle(text = stringResource(R.string.scan_control))
//                    ItemText(text = stringResource(R.string.touch_button_to_start_scanning))
                    ItemSwitcher(
                        state = exportResultFile.value,
                        onChange = {
                            exportResultFile.value = it
                        },
                        text = stringResource(id = R.string.export_result_file_switcher_text),
                        sub = stringResource(id = R.string.export_result_file_switcher_sub_text),
                        enableHaptic = enableHaptic.value
                    )
                    AnimatedVisibility(visible = exportResultFile.value) {
                        ItemPopup(
                            state = exportTypePopupState,
                            text = stringResource(id = R.string.format_of_exported_files),
                            selectedItem = when (selectedExportFormat.intValue) {
                                0 -> stringResource(id = R.string.database_file_popup)
                                1 -> stringResource(id = R.string.text_file_popup)
                                else -> ""
                            },
                            popupWidth = 190
                        ) {
                            PopupMenuItem(
                                onClick = {
                                    MyVibrationEffect(context, enableHaptic.value).click()
                                    selectedExportFormat.intValue = 0
                                    exportTypePopupState.dismiss()
                                },
                                text = stringResource(id = R.string.database_file_popup),
                                selected = selectedExportFormat.intValue == 0,
                                iconPainter = painterResource(id = R.drawable.database),
                                iconColor = SaltTheme.colors.text,
                                iconPaddingValues = PaddingValues(
                                    start = 1.dp,
                                    end = 1.dp,
                                    top = 1.dp,
                                    bottom = 1.dp
                                )
                            )
                            PopupMenuItem(
                                onClick = {
                                    MyVibrationEffect(context, enableHaptic.value).click()
                                    selectedExportFormat.intValue = 1
                                    exportTypePopupState.dismiss()
                                },
                                text = stringResource(id = R.string.text_file_popup),
                                selected = selectedExportFormat.intValue == 1,
                                iconPainter = painterResource(id = R.drawable.text),
                                iconColor = SaltTheme.colors.text,
                                iconPaddingValues = PaddingValues(
                                    start = 2.dp,
                                    end = 2.dp,
                                    top = 2.dp,
                                    bottom = 2.dp
                                )
                            )
                        }
                    }
                }
                AnimatedContent(
                    targetState = showLoadingProgressBar.value,
                    label = "",
                    transitionSpec = {
                        if (targetState != initialState) {
                            fadeIn() togetherWith fadeOut()
                        } else {
                            fadeIn() togetherWith fadeOut()
                        }
                    }) {
                    ItemContainer {
                        TextButton(
                            onClick = {
                                scanPage.init()
                            }, text = stringResource(R.string.start_text),
                            enabled = !it,
                            enableHaptic = enableHaptic.value
                        )
                    }
                }
                AnimatedVisibility(
                    visible = progressPercent.value != -1,
                ) {
                    RoundedColumn {
                        ItemTitle(text = stringResource(R.string.scanning_result))
                        ItemValue(
                            text = stringResource(R.string.number_of_total_songs),
                            rightSub = progressPercent.value.toString()
                        )
                        AnimatedVisibility(
                            visible = exportResultFile.value,
                        ) {
                            ItemText(
                                text = when (selectedExportFormat.intValue) {
                                    0 -> "${stringResource(id = R.string.details_of_results)}: Download/${
                                        stringResource(
                                            id = R.string.result_file_name
                                        ).replace(
                                            "#",
                                            stringResource(id = R.string.app_name)
                                        )
                                    }.db"

                                    1 -> "${stringResource(id = R.string.details_of_results)}: Download/${
                                        stringResource(
                                            id = R.string.result_file_name
                                        ).replace(
                                            "#",
                                            stringResource(id = R.string.app_name)
                                        )
                                    }.txt"

                                    else -> ""
                                }
                            )
                        }
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
                                items(scanResult.size) { index ->
                                    Text(
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 4.dp
                                        ),
                                        text = scanResult[index],
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