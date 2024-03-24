package com.hwinzniej.musichelper.activity

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.LifecycleOwner
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.data.database.MusicDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

class TagPage(
    val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    val db: MusicDatabase,
) {
    fun getMusicList(songList: SnapshotStateMap<Int, Array<String>>) {
        songList.clear()
        db.musicDao().getMusic3Info().forEach {
            songList[it.id] = arrayOf(it.song.ifBlank {
                it.absolutePath.substring(
                    it.absolutePath.lastIndexOf(
                        '/'
                    ) + 1
                )
            }, it.artist, it.album)
        }
    }

    fun getSongInfo(id: Int): Map<String, String?> {
        val musicInfo = db.musicDao().getMusicAllInfo(id)
        return mapOf(
            "id" to id.toString(),
            "song" to musicInfo.song,
            "artist" to musicInfo.artist,
            "album" to musicInfo.album,
            "albumArtist" to musicInfo.albumArtist,
            "genre" to musicInfo.genre,
            "trackNumber" to musicInfo.trackNumber,
            "releaseYear" to musicInfo.releaseYear,
        )
    }

    suspend fun saveSongInfo(songInfoModified: Map<String, String?>): Boolean {
        val id = songInfoModified["id"]!!.toInt()
        val musicInfo = db.musicDao().getMusicAllInfo(id)

        val audioFile: AudioFile
        try {
            audioFile = AudioFileIO.read(File(musicInfo.absolutePath))
            audioFile.tag.setField(FieldKey.TITLE, songInfoModified["song"])
            audioFile.tag.setField(FieldKey.ARTIST, songInfoModified["artist"])
            audioFile.tag.setField(FieldKey.ALBUM, songInfoModified["album"])
            audioFile.tag.setField(FieldKey.ALBUM_ARTIST, songInfoModified["albumArtist"])
            audioFile.tag.setField(FieldKey.GENRE, songInfoModified["genre"])
            audioFile.tag.setField(FieldKey.TRACK, songInfoModified["trackNumber"])
            audioFile.tag.setField(FieldKey.YEAR, songInfoModified["releaseYear"])
            AudioFileIO.write(audioFile)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_LONG)
                    .show()
            }
            return false
        }

        musicInfo.song = songInfoModified["song"].toString()
        musicInfo.artist = songInfoModified["artist"].toString()
        musicInfo.album = songInfoModified["album"].toString()
        musicInfo.albumArtist = songInfoModified["albumArtist"]
        musicInfo.genre = songInfoModified["genre"]
        musicInfo.trackNumber = songInfoModified["trackNumber"]
        musicInfo.releaseYear = songInfoModified["releaseYear"]
        db.musicDao().updateMusicInfo(
            id,
            musicInfo.song,
            musicInfo.artist,
            musicInfo.album,
            musicInfo.albumArtist,
            musicInfo.genre,
            musicInfo.trackNumber,
            musicInfo.releaseYear,
        )
        withContext(Dispatchers.Main) {
            Toast.makeText(context, context.getString(R.string.save_success), Toast.LENGTH_LONG)
                .show()
        }
        return true
    }
}
