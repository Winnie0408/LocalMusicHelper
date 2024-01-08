package com.hwinzniej.musichelper.pages

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.TextButton
import com.hwinzniej.musichelper.YesNoDialog
import com.hwinzniej.musichelper.data.database.MusicDatabase
import com.hwinzniej.musichelper.data.model.MusicInfo
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okio.IOException
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import org.htmlunit.WebClient
import org.htmlunit.html.HtmlPage
import org.jsoup.Jsoup


class ProcessPage(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val db: MusicDatabase
) {
    var processAllScannedMusic = mutableStateOf(true)
    var overwriteOriginalTag = mutableStateOf(true)
    var showSelectTagTypeDialog = mutableStateOf(false)
    var enableAlbumArtist = mutableStateOf(true)
    var enableReleaseYear = mutableStateOf(true)
    var enableGenre = mutableStateOf(true)
    var enableTrackNumber = mutableStateOf(true)
    var showProgressBar = mutableStateOf(false)
    var showSelectSourceDialog = mutableStateOf(false)
    val useDoubanMusicSource = mutableStateOf(true)
    val useMusicBrainzSource = mutableStateOf(true)
    val useBaiduBaikeSource = mutableStateOf(true)

    lateinit var musicInfoList: List<MusicInfo>

    fun getMusicList() {
//        showProgressBar.value = true
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            if (processAllScannedMusic.value) {
                musicInfoList = db.musicDao().getMusicInfo()
                startProcess()
            } else {
//            db.musicDao().getMusicByPath("")
            }
        }

    }

    fun startProcess() {
        if (musicInfoList.isEmpty()) {
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                Toast.makeText(context, "数据库为空，请先扫描本地歌曲", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (useDoubanMusicSource.value) {
            getDoubanMusicInfo()
        }
        if (useMusicBrainzSource.value) {
            //TODO
        }
        if (useBaiduBaikeSource.value) {
            //TODO
        }
    }

    fun getDoubanMusicInfo() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            musicInfoList.forEach {
                val musicInfo = it
                val musicName = it.song
                val artistName = it.artist
                val albumName = it.album
                val musicInfoFromDouban =
                    DoubanMusicApi.getMusicInfo(musicName, artistName, albumName)
                return@launch
//                if (musicInfoFromDouban != null) {
//                    musicInfo.releaseYear = musicInfoFromDouban.releaseYear
//                    musicInfo.trackNumber = musicInfoFromDouban.trackNumber
//                    musicInfo.albumArtist = musicInfoFromDouban.albumArtist
//                    musicInfo.genre = musicInfoFromDouban.genre
////                    db.musicDao().updateMusicInfo(musicInfo)
//                }
            }
        }
    }
}

class DoubanMusicApi {
    companion object {
        fun getMusicInfo(musicName: String, artistName: String, albumName: String): MusicInfo? {

            val url = "https://search.douban.com/music/subject_search?search_text=Moon halo"
//            val url =
//                "https://music.douban.com/subject_search?search_text=要嫁就嫁灰太狼 周艳泓"

            val webClient = WebClient().apply {
                options.isCssEnabled = false
                options.isJavaScriptEnabled = true
                options.isThrowExceptionOnScriptError = false
                options.isThrowExceptionOnFailingStatusCode = false
            }

            webClient.waitForBackgroundJavaScript(500) // 等待JavaScript执行完成
            val htmlPage: HtmlPage = webClient.getPage(url)

            val musicInfoPage =
                "https://music.douban.com/subject/[0-9]*/".toRegex().find(htmlPage.asXml())?.value

            val client = OkHttpClient()
            val request = okhttp3.Request.Builder()
                .url(musicInfoPage.toString())
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val doc = Jsoup.parse(response.body?.string())
                val title = doc.title()

                println("Title: $title")
            }

            return null
        }
    }
}

@OptIn(UnstableSaltApi::class)
@Composable
fun ProcessPageUi(
    processPage: ProcessPage,
    processAllScannedMusic: MutableState<Boolean>,
    overwriteOriginalTag: MutableState<Boolean>,
    showSelectTagTypeDialog: MutableState<Boolean>,
    enableAlbumArtist: MutableState<Boolean>,
    enableReleaseYear: MutableState<Boolean>,
    enableGenre: MutableState<Boolean>,
    enableTrackNumber: MutableState<Boolean>,
    showProgressBar: MutableState<Boolean>,
    showSelectSourceDialog: MutableState<Boolean>,
    useDoubanMusicSource: MutableState<Boolean>,
    useMusicBrainzSource: MutableState<Boolean>,
    useBaiduBaikeSource: MutableState<Boolean>,
) {
    val context = LocalContext.current

    data class SourceState(var name: String, var state: MutableState<Boolean>)

    var sources by remember {
        mutableStateOf(
            listOf(
                SourceState(context.getString(R.string.source_of_tag_1), useDoubanMusicSource),
                SourceState(context.getString(R.string.source_of_tag_2), useMusicBrainzSource),
                SourceState(context.getString(R.string.source_of_tag_3), useBaiduBaikeSource)
            )
        )
    }

    if (showSelectTagTypeDialog.value) {
        YesNoDialog(
            onNegative = { },
            onPositive = { },
            onDismiss = { },
            title = context.getString(R.string.select_needed_tag_type_dialog_title),
            content = "",
            noText = context.getString(R.string.cancel_button_text),
            yesText = context.getString(R.string.ok_button_text),
            onlyComposeView = true,
            customContent = {
                ItemSwitcher(
                    state = enableAlbumArtist.value, onChange = {
                        enableAlbumArtist.value = it
                    }, text = context.getString(R.string.album_artist_tag_name)
                )
                ItemSwitcher(
                    state = enableReleaseYear.value, onChange = {
                        enableReleaseYear.value = it
                    }, text = context.getString(R.string.release_year_tag_name)
                )
                ItemSwitcher(
                    state = enableGenre.value, onChange = {
                        enableGenre.value = it
                    }, text = context.getString(R.string.genre_tag_name)
                )
                ItemSwitcher(
                    state = enableTrackNumber.value, onChange = {
                        enableTrackNumber.value = it
                    }, text = context.getString(R.string.track_number_tag_name)
                )
            })
    }

    fun initState() {
        showSelectSourceDialog.value = false
        useBaiduBaikeSource.value = true
        useDoubanMusicSource.value = true
        useMusicBrainzSource.value = true
        sources = listOf(
            SourceState(context.getString(R.string.source_of_tag_1), useDoubanMusicSource),
            SourceState(context.getString(R.string.source_of_tag_2), useMusicBrainzSource),
            SourceState(context.getString(R.string.source_of_tag_3), useBaiduBaikeSource)
        )
    }

    if (showSelectSourceDialog.value) {
        val state = rememberReorderableLazyListState(onMove = { from, to ->
            sources = sources.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        })
        YesNoDialog(
            onNegative = {
                initState()
            },
            onPositive = { },
            onDismiss = {
                initState()
            },
            title = context.getString(R.string.select_source_dialog_title),
            content = "",
            noText = context.getString(R.string.cancel_button_text),
            yesText = context.getString(R.string.ok_button_text),
            onlyComposeView = true,
            customContent = {
                LazyColumn(
                    state = state.listState,
                    modifier = Modifier
                        .reorderable(state)
                        .detectReorderAfterLongPress(state)
                ) {
                    items(sources, { it.name }) { item ->
                        ReorderableItem(state, key = item.name) { isDragging ->
                            val elevation by animateDpAsState(if (isDragging) 16.dp else 0.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(elevation)
                                    .background(SaltTheme.colors.background)
                            ) {
                                ItemSwitcher(
                                    state = item.state.value,
                                    onChange = {
                                        item.state.value = it
                                    },
                                    text = item.name,
                                )
                            }
                        }
                    }
                }
            })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {
        TitleBar(
            onBack = {},
            text = context.getString(R.string.process_function_name),
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
                ItemTitle(text = context.getString(R.string.process_control))
                ItemSwitcher(
                    state = processAllScannedMusic.value,
                    onChange = {
                        processAllScannedMusic.value = it
                    },
                    text = context.getString(R.string.process_all_scanned_music_switch_title),
                    sub = context.getString(R.string.process_all_scanned_music_switch_sub)
                )
                Item(
                    onClick = {

                    },
                    enabled = !processAllScannedMusic.value,
                    text = context.getString(R.string.select_needed_processing_music_directory_item_title),
                )
//                ItemSpacer()
                ItemSwitcher(
                    state = overwriteOriginalTag.value,
                    onChange = {
                        overwriteOriginalTag.value = it
                    },
                    text = context.getString(R.string.overwrite_original_tag_switch_title),
                    sub = context.getString(R.string.overwrite_original_tag_switch_sub)
                )
                Item(
                    onClick = {
                        showSelectTagTypeDialog.value = true
                    },
                    text = context.getString(R.string.select_needed_tag_type_item_title),
                )

                Item(
                    onClick = {
                        showSelectSourceDialog.value = true
                    },
                    text = context.getString(R.string.select_source_item_title),
                )

//                AnimatedContent(
//                    targetState = showLoadingProgressBar.value,
//                    label = "",
//                    transitionSpec = {
//                        if (targetState != initialState) {
//                            fadeIn() togetherWith fadeOut()
//                        } else {
//                            fadeIn() togetherWith fadeOut()
//                        }
//                    }) {
                ItemContainer {
                    TextButton(
                        onClick = { processPage.getMusicList() },
                        text = context.getString(R.string.start_text),
//                        enabled = !it
                    )
                }
//                }
            }

            RoundedColumn {
                ItemTitle(text = context.getString(R.string.processing_result))
//                ItemValue(
//                    text = context.getString(R.string.number_of_total_songs),
//                    sub = progressPercent.value.toString()
//                )

                ItemContainer {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .size((LocalConfiguration.current.screenHeightDp / 4).dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color = SaltTheme.colors.background)
                    ) {
                        item {
                            Box {
                                if (showProgressBar.value) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .zIndex(1f),
                                        color = SaltTheme.colors.highlight,
                                        trackColor = SaltTheme.colors.background
                                    )
                                }
                                Text(
                                    modifier = Modifier.padding(
                                        top = 3.dp, start = 7.dp, end = 7.dp
                                    ),
                                    text = "//TODO",
                                    fontSize = 16.sp,
                                    style = TextStyle(
                                        lineHeight = 1.5.em, color = SaltTheme.colors.subText
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

//@Preview(showBackground = true)
//@Composable
//fun Preview() {
//    SaltTheme(
//        colors = lightSaltColors()
//    ) {
//        ProcessPageUi()
//    }
//}