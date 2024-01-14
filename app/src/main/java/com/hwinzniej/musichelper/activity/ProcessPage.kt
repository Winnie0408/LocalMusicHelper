package com.hwinzniej.musichelper.activity

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.hwinzniej.musichelper.data.database.MusicDatabase
import com.hwinzniej.musichelper.data.model.MusicInfo
import com.hwinzniej.musichelper.utils.DoubanMusicApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
