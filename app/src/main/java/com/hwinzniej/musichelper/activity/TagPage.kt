package com.hwinzniej.musichelper.activity

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.data.database.MusicDatabase
import com.hwinzniej.musichelper.utils.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File

class TagPage(
    val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    val db: MusicDatabase,
    val openMusicCoverLauncher: ActivityResultLauncher<Array<String>>
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
            }, it.artist, it.album, it.id.toString())
        }
    }

    fun getSongInfo(id: Int, cover: MutableState<ByteArray?>): Map<String, String?> {
        cover.value = null
        val audioFile: AudioFile
        val musicInfo = db.musicDao().getMusicAllInfo(id)
        audioFile = AudioFileIO.read(File(musicInfo.absolutePath))
        try {
            cover.value = audioFile.tag.artworkList.first().binaryData
        } catch (_: Exception) {
        }
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

    suspend fun saveSongInfo(
        songInfoModified: Map<String, String?>,
        cover: MutableState<ByteArray?>
    ): Boolean {
        val id = songInfoModified["id"]!!.toInt()
        var coverMimeType: String? = null
        if (cover.value != null) {
            coverMimeType = Tools().determineImageMimeType(cover.value!!)
            if (coverMimeType == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.cover_image_not_support),
                        Toast.LENGTH_LONG
                    ).show()
                }
                return false
            }
        }
        val musicInfo = db.musicDao().getMusicAllInfo(id)

        val audioFile: AudioFile
        try {
            audioFile = AudioFileIO.read(File(musicInfo.absolutePath))
            audioFile.tag.setField(FieldKey.TITLE, songInfoModified["song"] ?: "")
            audioFile.tag.setField(FieldKey.ARTIST, songInfoModified["artist"] ?: "")
            audioFile.tag.setField(FieldKey.ALBUM, songInfoModified["album"] ?: "")
            audioFile.tag.setField(FieldKey.ALBUM_ARTIST, songInfoModified["albumArtist"] ?: "")
            audioFile.tag.setField(FieldKey.GENRE, songInfoModified["genre"] ?: "")
            audioFile.tag.setField(FieldKey.TRACK, songInfoModified["trackNumber"] ?: "")
            audioFile.tag.setField(FieldKey.YEAR, songInfoModified["releaseYear"] ?: "")
            audioFile.tag.deleteArtworkField()
            coverMimeType?.let {
                val artwork = ArtworkFactory.getNew()
                artwork.binaryData = cover.value
                artwork.mimeType = it
                audioFile.tag.setField(artwork)
            }
            AudioFileIO.write(audioFile)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_LONG)
                    .show()
            }
            return false
        }

        musicInfo.song = songInfoModified["song"] ?: ""
        musicInfo.artist = songInfoModified["artist"] ?: ""
        musicInfo.album = songInfoModified["album"] ?: ""
        musicInfo.albumArtist = songInfoModified["albumArtist"] ?: ""
        musicInfo.genre = songInfoModified["genre"] ?: ""
        musicInfo.trackNumber = songInfoModified["trackNumber"] ?: ""
        musicInfo.releaseYear = songInfoModified["releaseYear"] ?: ""
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

    fun selectCoverImage() {
        try {
            openMusicCoverLauncher.launch(arrayOf("image/*"))
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.unable_start_documentsui),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    var coverImage = mutableStateOf<ByteArray?>(null)
    fun handleSelectedCoverUri(uri: Uri?) {
        if (uri == null)
            return
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val temp = inputStream?.readBytes()
            if (Tools().determineImageMimeType(temp!!) == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.cover_image_not_support),
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@launch
            }
            coverImage.value = temp
            inputStream.close()
        }
    }

    fun searchSong(
        inputSearchWords: String,
        searchResult: SnapshotStateMap<Int, Array<String>>
    ) {
        searchResult.clear()
        var i = 0
        db.musicDao().searchMusicAll("%${inputSearchWords}%").forEach {
            searchResult[i++] = arrayOf(it.song.ifBlank {
                it.absolutePath.substring(
                    it.absolutePath.lastIndexOf(
                        '/'
                    ) + 1
                )
            }, it.artist, it.album, it.id.toString())
        }
    }
}
