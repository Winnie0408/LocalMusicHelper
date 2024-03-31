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
import com.hwinzniej.musichelper.data.model.MusicInfo
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
            "albumArtist" to audioFile.tag.getFirst(FieldKey.ALBUM_ARTIST),
            "genre" to audioFile.tag.getFirst(FieldKey.GENRE),
            "trackNumber" to audioFile.tag.getFirst(FieldKey.TRACK),
            "discNumber" to audioFile.tag.getFirst(FieldKey.DISC_NO),
            "releaseYear" to audioFile.tag.getFirst(FieldKey.YEAR),
            "composer" to audioFile.tag.getFirst(FieldKey.COMPOSER),
            "lyricist" to audioFile.tag.getFirst(FieldKey.LYRICIST),
            "lyrics" to audioFile.tag.getFirst(FieldKey.LYRICS),
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
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return false
            }
        }
        val musicInfo = db.musicDao().getMusicAllInfo(id)

        val audioFile: AudioFile
        try {
            audioFile = AudioFileIO.read(File(musicInfo.absolutePath))
            val musicTag = mapOf(
                "song" to FieldKey.TITLE,
                "artist" to FieldKey.ARTIST,
                "album" to FieldKey.ALBUM,
                "albumArtist" to FieldKey.ALBUM_ARTIST,
                "genre" to FieldKey.GENRE,
                "trackNumber" to FieldKey.TRACK,
                "discNumber" to FieldKey.DISC_NO,
                "releaseYear" to FieldKey.YEAR,
                "composer" to FieldKey.COMPOSER,
                "lyricist" to FieldKey.LYRICIST,
                "lyrics" to FieldKey.LYRICS,
            )
            musicTag.forEach {
                if (songInfoModified[it.key] == null || songInfoModified[it.key]!!.isBlank()) {
                    audioFile.tag.deleteField(it.value)
                } else {
                    audioFile.tag.setField(it.value, songInfoModified[it.key])
                }
            }
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
                Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_SHORT)
                    .show()
            }
            return false
        }

        musicInfo.song = songInfoModified["song"] ?: ""
        musicInfo.artist = songInfoModified["artist"] ?: ""
        musicInfo.album = songInfoModified["album"] ?: ""
        db.musicDao().updateMusicInfo(
            id = id,
            song = musicInfo.song,
            artist = musicInfo.artist,
            album = musicInfo.album,
            albumArtist = "",
            genre = "",
            trackNumber = "",
            releaseYear = "",
        )
        withContext(Dispatchers.Main) {
            Toast.makeText(context, context.getString(R.string.save_success), Toast.LENGTH_SHORT)
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
                        Toast.LENGTH_SHORT
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

    suspend fun searchDuplicateAlbum(completeResult: MutableList<Map<String, Int>>) {
        val nullAlbumArtistCount = db.musicDao().countNullAlbumArtist()
        if (nullAlbumArtistCount == 0) {
            completeResult.add(mapOf(context.getString(R.string.no_album_artist_null_count_in_song_list) to 2))
            return
        }
        completeResult.add(
            mapOf(
                context.getString(R.string.total_album_artist_null_count_in_song_list)
                    .replace("#", nullAlbumArtistCount.toString()) to 2
            )
        )
        completeResult.add(mapOf(context.getString(R.string.click_ok_to_start) to 2))
    }

    suspend fun handleDuplicateAlbum(
        overwrite: Boolean,
        completeResult: MutableList<Map<String, Int>>
    ) {
        val searchResult: List<MusicInfo> = if (overwrite) {
            db.musicDao().getAll()
        } else {
            db.musicDao().searchDuplicateAlbumNoOverwrite()
        }
        var lastAlbumArtist = ""
        var lastAlbum = ""
        var haveError = false
        searchResult.forEach {
            if (it.album.isBlank()) {
                return@forEach
            }
            if (lastAlbum != it.album) {
                val tempAlbumArtistList = mutableSetOf<String>()
                db.musicDao().getDuplicateAlbumArtistList(it.album).forEach { it1 ->
                    if (it1.isNotBlank()) {
                        if (it1.contains("/"))
                            tempAlbumArtistList.addAll(it1.split("/"))
                        else
                            tempAlbumArtistList.add(it1)
                    }
                }
                lastAlbumArtist = tempAlbumArtistList.sorted().joinToString("/")
            }
            lastAlbum = it.album
            val audioFile: AudioFile
            try {
                audioFile = AudioFileIO.read(File(it.absolutePath))
                audioFile.tag.setField(FieldKey.ALBUM_ARTIST, lastAlbumArtist)
                AudioFileIO.write(audioFile)
            } catch (e: Exception) {
                completeResult.add(
                    0,
                    mapOf(
                        context.getString(R.string.modify_album_artist_failed)
                            .replace("#1", it.song)
                            .replace("#2", lastAlbumArtist) to 0
                    )
                )
                haveError = true
                return@forEach
            }
            db.musicDao().updateAlbumArtist(it.id, lastAlbumArtist)
            completeResult.add(
                0,
                mapOf(
                    context.getString(R.string.modify_album_artist_success)
                        .replace("#1", it.song)
                        .replace("#2", lastAlbumArtist) to 1
                )
            )
        }
        if (haveError) {
            completeResult.sortBy { it.values.first() }
        }
        completeResult.add(0, mapOf(context.getString(R.string.all_done) to 2))
    }
}
