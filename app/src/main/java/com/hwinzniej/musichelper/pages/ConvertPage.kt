package com.hwinzniej.musichelper.pages

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.hwinzniej.musichelper.ItemPopup
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.YesDialog
import com.hwinzniej.musichelper.utils.UsefulTools
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.ItemValue
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TextButton
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.popup.PopupMenuItem
import com.moriafly.salt.ui.popup.rememberPopupState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class ConvertPage(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val openFileLauncher: ActivityResultLauncher<Array<String>>,
) {
    var databaseFileName = mutableStateOf("")
    var selectedSourceApp = mutableIntStateOf(0)
    var useCustomResultFile = mutableStateOf(false)
    var customResultFileName = mutableStateOf("")
    var selectedFileName = mutableStateOf("")
    var showLoadingProgressBar = mutableStateOf(false)
    var showErrorDialog = mutableStateOf(false)
    var errorDialogTitle = mutableStateOf("")
    var errorDialogContent =
        mutableStateOf(context.getString(R.string.error_while_getting_data_dialog_content))
    var databaseFilePath = ""
    var resultFilePath = ""

    fun selectDatabaseFile() {
        openFileLauncher.launch(arrayOf("*/*"))
    }

    fun selectResultFile() {
        openFileLauncher.launch(arrayOf("text/plain"))
    }

    fun handleUri(uri: Uri?) {
        // 这是用户选择的目录的Uri
        // 你可以在这里处理用户选择的目录
        if (uri == null) {
            return
        }
        selectedFileName.value = uri.pathSegments[uri.pathSegments.size - 1]
        selectedFileName.value =
            selectedFileName.value.substring(selectedFileName.value.lastIndexOf("/") + 1)
        if (selectedFileName.value.endsWith(".txt")) {
            resultFilePath = UsefulTools().uriToAbsolutePath(uri)
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                delay(300L)
                customResultFileName.value = selectedFileName.value
            }
        } else {
            databaseFilePath = UsefulTools().uriToAbsolutePath(uri)
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                delay(300L)
                databaseFileName.value = selectedFileName.value
            }
        }
    }

    fun checkSelectedFiles() {
        showLoadingProgressBar.value = true
        errorDialogContent.value =
            context.getString(R.string.error_while_getting_data_dialog_content)
        checkDatabaseFile()
        checkResultFile()
//        showLoadingProgressBar.value = false

    }

    fun checkResultFile() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val file = File(resultFilePath)
            try {
                val localMusicFile =
                    file.readText().split("\n")
                val localMusic = Array(localMusicFile.size) {
                    arrayOfNulls<String>(
                        5
                    )
                }
                var a = 0
                for (i in localMusicFile) {
                    localMusic[a][0] =
                        i.split("#\\*#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                    localMusic[a][1] =
                        i.split("#\\*#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                    localMusic[a][2] =
                        i.split("#\\*#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[2]
                    localMusic[a][3] =
                        i.split("#\\*#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[3]
                    localMusic[a][4] =
                        i.split("#\\*#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[4]
                    if (a.toString().equals(localMusic[a][4]))
                        break
                    a++
                }
            } catch (e: Exception) {
                showErrorDialog.value = true
                errorDialogTitle.value =
                    context.getString(R.string.error_while_getting_data_dialog_title)
                errorDialogContent.value =
                    "${errorDialogContent.value}: \n${context.getString(R.string.result_file)}"
//            Toast.makeText(context, "出错啦~", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun checkDatabaseFile() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            delay(3000L)
            showErrorDialog.value = true
            errorDialogContent.value =
                "${errorDialogContent.value}\n${context.getString(R.string.database_file)}"
            showLoadingProgressBar.value = false
        }
    }
}

@OptIn(UnstableSaltApi::class)
@Composable
fun ConvertPageUi(
    convertPage: ConvertPage,
    selectedSourceApp: MutableState<Int>,
    databaseFileName: MutableState<String>,
    useCustomResultFile: MutableState<Boolean>,
    customResultFileName: MutableState<String>,
    showLoadingProgressBar: MutableState<Boolean>,
    showErrorDialog: MutableState<Boolean>,
    errorDialogTitle: MutableState<String>,
    errorDialogContent: MutableState<String>,
) {
    val context = LocalContext.current
    val popupMenuState = rememberPopupState()
    var sourceApp by remember { mutableStateOf("") }

    if (showErrorDialog.value) {
        YesDialog(
            onDismissRequest = { showErrorDialog.value = false },
            title = errorDialogTitle.value,
            content = errorDialogContent.value
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {
        TitleBar(
            onBack = {},
            text = context.getString(R.string.convert_function_name),
            showBackBtn = false
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
                    .padding(top = 4.dp)
                    .fillMaxSize()
                    .background(color = SaltTheme.colors.background)
                    .verticalScroll(rememberScrollState())
            ) {
                RoundedColumn {
                    ItemTitle(text = context.getString(R.string.source_of_songlist_app))
                    ItemPopup(
                        state = popupMenuState,
                        text = context.getString(R.string.select_source_of_songlist),
                        selectedItem = sourceApp
                    ) {
                        PopupMenuItem(
                            onClick = {
                                selectedSourceApp.value = 1
                                popupMenuState.dismiss()
                                databaseFileName.value = ""
                            },
                            selected = selectedSourceApp.value == 1,
                            text = context.getString(R.string.source_netease_cloud_music)
                        )
                        PopupMenuItem(
                            onClick = {
                                selectedSourceApp.value = 2
                                popupMenuState.dismiss()
                                databaseFileName.value = ""
                            },
                            selected = selectedSourceApp.value == 2,
                            text = context.getString(R.string.source_qq_music)
                        )

                        PopupMenuItem(
                            onClick = {
                                selectedSourceApp.value = 3
                                popupMenuState.dismiss()
                                databaseFileName.value = ""
                            },
                            selected = selectedSourceApp.value == 3,
                            text = context.getString(R.string.source_kugou_music),
//                        iconPainter = painterResource(id = R.drawable.ic_qr_code),
//                        iconColor = SaltTheme.colors.text
                        )
                        PopupMenuItem(
                            onClick = {
                                selectedSourceApp.value = 4
                                popupMenuState.dismiss()
                                databaseFileName.value = ""
                            },
                            selected = selectedSourceApp.value == 4,
                            text = context.getString(R.string.source_kuwo_music)
                        )
                    }
                }

                sourceApp = when (selectedSourceApp.value) {
                    1 -> context.getString(R.string.source_netease_cloud_music)
                    2 -> context.getString(R.string.source_qq_music)
                    3 -> context.getString(R.string.source_kugou_music)
                    4 -> context.getString(R.string.source_kuwo_music)
                    else -> ""
                }

                RoundedColumn {
                    ItemTitle(text = context.getString(R.string.import_database))
                    Item(
                        enabled = selectedSourceApp.value != 0,
                        onClick = { convertPage.selectDatabaseFile() },
                        text = if (selectedSourceApp.value == 0) {
                            context.getString(R.string.please_select_source_app_first)
                        } else {
                            context.getString(R.string.select_database_file_match_to_source_1) + sourceApp + context.getString(
                                R.string.select_database_file_match_to_source_2
                            )
                        },
                    )
                    AnimatedVisibility(
                        visible = databaseFileName.value != ""
                    ) {
                        ItemValue(
                            text = context.getString(R.string.you_have_selected),
                            sub = databaseFileName.value
                        )
                    }

                }

                RoundedColumn {
                    ItemTitle(text = context.getString(R.string.import_result_file))
                    ItemSwitcher(
                        state = useCustomResultFile.value,
                        onChange = {
                            useCustomResultFile.value = it
                        },
                        text = context.getString(R.string.use_custom_result_file),
                        sub = context.getString(R.string.use_other_result_file)
                    )
                    AnimatedVisibility(
                        visible = useCustomResultFile.value
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(color = SaltTheme.colors.subBackground)
                        ) {
                            Item(
                                onClick = { convertPage.selectResultFile() },
                                text = context.getString(R.string.select_result_file_item_title),
                            )
                            AnimatedVisibility(
                                visible = customResultFileName.value != ""
                            ) {
                                ItemValue(
                                    text = context.getString(R.string.you_have_selected),
                                    sub = customResultFileName.value
                                )
                            }
                        }
                    }
                }

//                RoundedColumn {
                ItemContainer {
                    TextButton(
                        onClick = { convertPage.checkSelectedFiles() },
                        text = context.getString(R.string.next_step_text)
                    )
                }
//                }
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun Preview() {
//    val convertPage = ConvertPage()
//    SaltTheme(
//        colors = lightSaltColors()
//    ) {
//        ConvertPageUi(
//            convertPage = convertPage,
//            selectedSourceApp = mutableIntStateOf(0),
//            databaseFileName = mutableStateOf("111.db")
//        )
//    }
//}