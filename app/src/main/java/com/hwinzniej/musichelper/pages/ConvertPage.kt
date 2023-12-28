package com.hwinzniej.musichelper.pages

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import com.hwinzniej.musichelper.ItemPopup
import com.hwinzniej.musichelper.R
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.ItemValue
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TextButton
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.popup.PopupMenuItem
import com.moriafly.salt.ui.popup.rememberPopupState

class ConvertPage(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val openFileLauncher: ActivityResultLauncher<Array<String>>,
) {
    var databaseFileName = mutableStateOf("")
    var selectedSourceApp = mutableIntStateOf(0)

    fun test() {
        openFileLauncher.launch(arrayOf("*/*"))
    }

    fun handleUri(uri: Uri?) {
        // 这是用户选择的目录的Uri
        // 你可以在这里处理用户选择的目录
        if (uri == null) {
            return
        }
        databaseFileName.value = uri.pathSegments.get(uri.pathSegments.size - 1)
        databaseFileName.value =
            databaseFileName.value.substring(databaseFileName.value.lastIndexOf("/") + 1)

    }


}

@OptIn(UnstableSaltApi::class)
@Composable
fun ConvertPageUi(
    convertPage: ConvertPage,
    selectedSourceApp: MutableState<Int>,
    databaseFileName: MutableState<String>
) {
    val context = LocalContext.current
    val popupMenuState = rememberPopupState()
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
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(color = SaltTheme.colors.background)
                .verticalScroll(rememberScrollState())
        ) {
            RoundedColumn {
                ItemTitle(text = context.getString(R.string.source_of_songlist_app))
                ItemPopup(
                    state = popupMenuState,
                    text = context.getString(R.string.select_source_of_songlist),
                ) {
                    PopupMenuItem(
                        onClick = {
                            selectedSourceApp.value = 1
                            popupMenuState.dismiss()
                        },
                        selected = selectedSourceApp.value == 1,
                        text = context.getString(R.string.source_netease_cloud_music)
                    )
                    PopupMenuItem(
                        onClick = {
                            selectedSourceApp.value = 2
                            popupMenuState.dismiss()
                        },
                        selected = selectedSourceApp.value == 2,
                        text = context.getString(R.string.source_qq_music)
                    )

                    PopupMenuItem(
                        onClick = {
                            selectedSourceApp.value = 3
                            popupMenuState.dismiss()
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
                        },
                        selected = selectedSourceApp.value == 4,
                        text = context.getString(R.string.source_kuwo_music)
                    )
                }
            }

            val sourceApp = when (selectedSourceApp.value) {
                1 -> context.getString(R.string.source_netease_cloud_music)
                2 -> context.getString(R.string.source_qq_music)
                3 -> context.getString(R.string.source_kugou_music)
                4 -> context.getString(R.string.source_kuwo_music)
                else -> context.getString(R.string.source_netease_cloud_music)
            }

            RoundedColumn {
                ItemTitle(text = context.getString(R.string.import_database))
                Item(
                    onClick = { convertPage.test() },
                    text = context.getString(R.string.select_database_file_match_to_source_1) + sourceApp + context.getString(
                        R.string.select_database_file_match_to_source_2
                    )
                )
                ItemValue(
                    text = context.getString(R.string.you_have_selected),
                    sub = databaseFileName.value
                )
            }
            ItemContainer {
                TextButton(
                    onClick = { }, text = context.getString(R.string.next_step_text)
                )
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