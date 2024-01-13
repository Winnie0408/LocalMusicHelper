package com.hwinzniej.musichelper.activity

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.hwinzniej.musichelper.data.database.MusicDatabase
import com.hwinzniej.musichelper.data.model.MusicInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okio.IOException
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
                musicInfoList = db.musicDao().getMusic3Info()  //应为getMusicInfo
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
