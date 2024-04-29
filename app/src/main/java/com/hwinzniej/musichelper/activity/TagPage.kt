package com.hwinzniej.musichelper.activity

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.data.database.MusicDatabase
import com.hwinzniej.musichelper.data.model.MusicInfo
import com.hwinzniej.musichelper.utils.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    fun getMusicList(
        songList: SnapshotStateMap<Int, Array<String>>,
        sortMethod: Int,
        selectedSongList: SnapshotStateList<Int>
    ) {
        songList.clear()
        selectedSongList.clear()
        var i = 0
        when (sortMethod) {
            0 -> db.musicDao().getMusic3Info().sortedBy { it.id }
            1 -> db.musicDao().getMusic3Info().sortedBy { it.song }
            2 -> db.musicDao().getMusic3Info().sortedByDescending { it.song }
            3 -> db.musicDao().getMusic3Info().sortedBy { it.modifyTime }
            4 -> db.musicDao().getMusic3Info().sortedByDescending { it.modifyTime }
            else -> db.musicDao().getMusic3Info().sortedBy { it.id }
        }.forEach {
            songList[i++] = arrayOf(it.song.ifBlank {
                it.absolutePath.substring(
                    it.absolutePath.lastIndexOf(
                        '/'
                    ) + 1
                )
            }, it.artist, it.album, it.id.toString())
            selectedSongList.add(0)
        }
    }

    fun getSongInfo(id: Int, cover: MutableState<ByteArray?>): Map<String, String?> {
        cover.value = null
        val audioFile: AudioFile
        val musicInfo = db.musicDao().getMusicById(id)
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
            "arranger" to audioFile.tag.getFirst(FieldKey.ARRANGER),
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
        val musicInfo = db.musicDao().getMusicById(id)

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
                "arranger" to FieldKey.ARRANGER,
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

        db.musicDao().updateMusicInfo(
            id = id,
            song = songInfoModified["song"] ?: "",
            artist = songInfoModified["artist"] ?: "",
            album = songInfoModified["album"] ?: "",
            albumArtist = songInfoModified["albumArtist"] ?: "",
            genre = songInfoModified["genre"] ?: "",
            trackNumber = songInfoModified["trackNumber"] ?: "",
            releaseYear = songInfoModified["releaseYear"] ?: "",
            lyricist = songInfoModified["lyricist"] ?: "",
            composer = songInfoModified["composer"] ?: "",
            arranger = songInfoModified["arranger"] ?: "",
            modifyTime = System.currentTimeMillis()
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

    suspend fun searchDuplicateAlbum(
        completeResult: MutableList<Map<String, Int>>,
        multiSelect: Boolean,
        selectedSongList: SnapshotStateList<Int>
    ): Boolean {
        val nullAlbumArtistCount = if (selectedSongList.all { it == 0 }) {
            if (multiSelect)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.no_song_selected_default_all),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            db.musicDao().countNullAlbumArtist()
        } else {
            db.musicDao().countSelectedNullAlbumArtist(
                selectedSongList.withIndex()
                    .filter { it.value == 1 }
                    .map { it.index }
            )
        }
        if (nullAlbumArtistCount == 0) {
            completeResult.add(
                mapOf(
                    context.getString(R.string.no_tagname_null_count_in_song_list)
                        .replace(
                            "#location",
                            if (selectedSongList.all { it == 0 }) context.getString(R.string.song_library)
                            else context.getString(R.string.selected_songs)
                        )
                        .replace("#", context.getString(R.string.album_artist_tag_name)) to 2
                )
            )
            completeResult.add(mapOf(context.getString(R.string.click_ok_to_start2) to 2))
            return false
        }
        completeResult.add(
            mapOf(
                context.getString(R.string.total_tagname_null_count_in_song_list)
                    .replace("#tagName", context.getString(R.string.album_artist_tag_name))
                    .replace(
                        "#location",
                        if (selectedSongList.all { it == 0 }) context.getString(R.string.song_library)
                        else context.getString(R.string.selected_songs)
                    )
                    .replace("#", nullAlbumArtistCount.toString()) to 2
            )
        )
        completeResult.add(mapOf(context.getString(R.string.click_ok_to_start) to 2))
        return true
    }

    suspend fun handleDuplicateAlbum(
        overwrite: Boolean,
        slow: Boolean,
        completeResult: MutableList<Map<String, Int>>,
        selectedSongList: SnapshotStateList<Int>
    ) {
        val searchResult: List<MusicInfo> = if (overwrite) {
            if (selectedSongList.all { it == 0 })
                db.musicDao().getAll()
            else {
                db.musicDao().getSelectedMusic(selectedSongList.withIndex()
                    .filter { it.value == 1 }
                    .map { it.index })
            }
        } else {
            if (selectedSongList.all { it == 0 })
                db.musicDao().searchDuplicateAlbumNoOverwrite()
            else {
                db.musicDao().searchSelectedDuplicateAlbumNoOverwrite(
                    selectedSongList.withIndex()
                        .filter { it.value == 1 }
                        .map { it.index })
            }
        }
        var lastAlbumArtist = ""
        var lastAlbum = ""
        var haveError = false
        searchResult.forEach {
            if (it.album.isBlank()) {
                return@forEach
            }
            completeResult.add(0, mapOf("" to 1))
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
            db.musicDao().updateAlbumArtist(it.id, lastAlbumArtist, System.currentTimeMillis())
            completeResult.add(
                0,
                mapOf(
                    context.getString(R.string.modify_tagName_success)
                        .replace("#1", it.song)
                        .replace("#2", lastAlbumArtist)
                        .replace("#3", context.getString(R.string.album_artist_tag_name)) to 1
                )
            )
            if (slow)
                delay(1248L)
        }
        if (haveError) {
            completeResult.sortBy { it.values.first() }
        }
        completeResult.add(0, mapOf(context.getString(R.string.all_done) to 2))
    }

    suspend fun searchBlankLyricistComposerArranger(
        completeResult: MutableList<Map<String, Int>>,
        multiSelect: Boolean,
        selectedSongList: SnapshotStateList<Int>
    ): Boolean {
        val nullLyricistCount: Int
        val nullComposerCount: Int
        val nullArrangerCount: Int

        if (selectedSongList.all { it == 0 }) {
            if (multiSelect)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.no_song_selected_default_all),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            nullLyricistCount = db.musicDao().countNullLyricist()
            nullComposerCount = db.musicDao().countNullComposer()
            nullArrangerCount = db.musicDao().countNullArranger()
        } else {
            val innerSelectedSongList =
                selectedSongList.withIndex().filter { it.value == 1 }.map { it.index }
            nullLyricistCount = db.musicDao().countSelectedNullLyricist(innerSelectedSongList)
            nullComposerCount = db.musicDao().countSelectedNullComposer(innerSelectedSongList)
            nullArrangerCount = db.musicDao().countSelectedNullArranger(innerSelectedSongList)
        }
        if (nullLyricistCount == 0) {
            completeResult.add(
                mapOf(
                    context.getString(R.string.no_tagname_null_count_in_song_list)
                        .replace(
                            "#location",
                            if (selectedSongList.all { it == 0 }) context.getString(R.string.song_library)
                            else context.getString(R.string.selected_songs)
                        )
                        .replace("#", context.getString(R.string.lyricist)) to 2
                )
            )
        } else {
            completeResult.add(
                mapOf(
                    context.getString(R.string.total_tagname_null_count_in_song_list)
                        .replace("#tagName", context.getString(R.string.lyricist))
                        .replace(
                            "#location",
                            if (selectedSongList.all { it == 0 }) context.getString(R.string.song_library)
                            else context.getString(R.string.selected_songs)
                        )
                        .replace("#", nullLyricistCount.toString()) to 2
                )
            )
        }

        if (nullComposerCount == 0) {
            completeResult.add(
                mapOf(
                    context.getString(R.string.no_tagname_null_count_in_song_list)
                        .replace(
                            "#location",
                            if (selectedSongList.all { it == 0 }) context.getString(R.string.song_library)
                            else context.getString(R.string.selected_songs)
                        )
                        .replace("#", context.getString(R.string.composer)) to 2
                )
            )
        } else {
            completeResult.add(
                mapOf(
                    context.getString(R.string.total_tagname_null_count_in_song_list)
                        .replace("#tagName", context.getString(R.string.composer))
                        .replace(
                            "#location",
                            if (selectedSongList.all { it == 0 }) context.getString(R.string.song_library)
                            else context.getString(R.string.selected_songs)
                        )
                        .replace("#", nullComposerCount.toString()) to 2
                )
            )
        }

        if (nullArrangerCount == 0) {
            completeResult.add(
                mapOf(
                    context.getString(R.string.no_tagname_null_count_in_song_list)
                        .replace(
                            "#location",
                            if (selectedSongList.all { it == 0 }) context.getString(R.string.song_library)
                            else context.getString(R.string.selected_songs)
                        )
                        .replace("#", context.getString(R.string.arranger)) to 2
                )
            )
        } else {
            completeResult.add(
                mapOf(
                    context.getString(R.string.total_tagname_null_count_in_song_list)
                        .replace("#tagName", context.getString(R.string.arranger))
                        .replace(
                            "#location",
                            if (selectedSongList.all { it == 0 }) context.getString(R.string.song_library)
                            else context.getString(R.string.selected_songs)
                        )
                        .replace("#", nullArrangerCount.toString()) to 2
                )
            )
        }
        return if (nullLyricistCount != 0 || nullComposerCount != 0 || nullArrangerCount != 0) {
            completeResult.add(mapOf(context.getString(R.string.click_ok_to_start) to 2))
            true
        } else {
            completeResult.add(mapOf(context.getString(R.string.click_ok_to_start2) to 2))
            false
        }
    }

    suspend fun handleBlankLyricistComposerArranger(
        overwrite: Boolean,
        lyricist: Boolean,
        composer: Boolean,
        arranger: Boolean,
        slow: Boolean,
        completeResult: MutableList<Map<String, Int>>,
        selectedSongList: SnapshotStateList<Int>
    ) {
        val searchResult =
            if (selectedSongList.all { it == 0 }) db.musicDao().getAll()
            else {
                db.musicDao().getSelectedMusic(selectedSongList.withIndex()
                    .filter { it.value == 1 }
                    .map { it.index })
            }
        val lyricistRegex =
            "\\[\\d{2}:\\d{2}\\.\\d{2}](((作)?[词詞]\\s?(Lyrics)?\\s?[：:]?\\s?)|((Lyrics|Written)\\sby\\s?[：:]?\\s?))(.*)\\n?".toRegex()
        val composerRegex =
            "\\[\\d{2}:\\d{2}\\.\\d{2}](((作)?曲\\s?(Composer)?\\s?[：:]?\\s?)|((Composed|Written)\\sby\\s?[：:]?\\s?))(.*)\\n?".toRegex()
        val arrangerRegex =
            "\\[\\d{2}:\\d{2}\\.\\d{2}](([编編]曲\\s?(Arranger|Arrangement)?\\s?[：:]?\\s?)|(Arranged\\sby\\s?[：:]?\\s?))(.*)\\n?".toRegex()
        val cleanRegex = "\\s?([/&|,，])\\s?".toRegex()
        searchResult.forEach {
//            var modified = false
            val audioFile = AudioFileIO.read(File(it.absolutePath))
            val songLyrics = audioFile.tag.getFirst(FieldKey.LYRICS)
            if (songLyrics.isBlank()) {
                completeResult.add(0, mapOf("" to 1))
                completeResult.add(0, mapOf(context.getString(R.string.lrc_empty_skip) to 0))
                completeResult.add(0, mapOf(it.song to 1))
                return@forEach
            }
            completeResult.add(0, mapOf("" to 1))
            var arrangerString = ""
            var composerString = ""
            var lyricistString = ""

            if (arranger) {
                val songArranger = audioFile.tag.getFirst(FieldKey.ARRANGER)
                val tempData = arrangerRegex.find(songLyrics)?.groupValues?.last()
                if ((overwrite || songArranger.isBlank()) && !tempData.isNullOrBlank()) {
                    arrangerString = cleanRegex.replace(tempData, "/")
                    audioFile.tag.setField(FieldKey.ARRANGER, arrangerString)
//                    modified = true
                    completeResult.add(
                        0,
                        mapOf(
                            "${context.getString(R.string.arranger)}: ${arrangerString}" to 1
                        )
                    )
                } else if (tempData.isNullOrBlank()) {
                    completeResult.add(
                        0,
                        mapOf(
                            "${context.getString(R.string.arranger)}: ${context.getString(R.string.lrc_not_contain_info)}" to 0
                        )
                    )
                } else if (songArranger.isNotBlank()) {
                    completeResult.add(
                        0,
                        mapOf(
                            "${context.getString(R.string.arranger)}: ${context.getString(R.string.keep_original_value)}" to 1
                        )
                    )
                }
            }

            if (composer) {
                val songComposer = audioFile.tag.getFirst(FieldKey.COMPOSER)
                val tempData = composerRegex.find(songLyrics)?.groupValues?.last()
                if ((overwrite || songComposer.isBlank()) && !tempData.isNullOrBlank()) {
                    composerString = cleanRegex.replace(tempData, "/")
                    audioFile.tag.setField(FieldKey.COMPOSER, composerString)
//                    modified = true
                    completeResult.add(
                        0,
                        mapOf(
                            "${context.getString(R.string.composer)}: ${composerString}" to 1
                        )
                    )
                } else if (tempData.isNullOrBlank()) {
                    completeResult.add(
                        0,
                        mapOf(
                            "${context.getString(R.string.composer)}: ${context.getString(R.string.lrc_not_contain_info)}" to 0
                        )
                    )
                } else if (songComposer.isNotBlank()) {
                    completeResult.add(
                        0,
                        mapOf(
                            "${context.getString(R.string.composer)}: ${context.getString(R.string.keep_original_value)}" to 1
                        )
                    )
                }
            }

            if (lyricist) {
                val songLyricist = audioFile.tag.getFirst(FieldKey.LYRICIST)
                val tempData = lyricistRegex.find(songLyrics)?.groupValues?.last()
                if ((overwrite || songLyricist.isBlank()) && !tempData.isNullOrBlank()) {
                    lyricistString = cleanRegex.replace(tempData, "/")
                    audioFile.tag.setField(FieldKey.LYRICIST, lyricistString)
//                    modified = true
                    completeResult.add(
                        0,
                        mapOf(
                            "${context.getString(R.string.lyricist)}: ${lyricistString}" to 1
                        )
                    )
                } else if (tempData.isNullOrBlank()) {
                    completeResult.add(
                        0,
                        mapOf(
                            "${context.getString(R.string.lyricist)}: ${context.getString(R.string.lrc_not_contain_info)}" to 0
                        )
                    )
                } else if (songLyricist.isNotBlank()) {
                    completeResult.add(
                        0,
                        mapOf(
                            "${context.getString(R.string.lyricist)}: ${context.getString(R.string.keep_original_value)}" to 1
                        )
                    )
                }
            }
//            if (modified) {
            audioFile.commit()
            db.musicDao().updateLyricistComposerArranger(
                id = it.id,
                lyricist = lyricistString,
                composer = composerString,
                arranger = arrangerString,
                modifyTime = System.currentTimeMillis()
            )
//            }
            completeResult.add(
                0,
                mapOf(
                    "${it.song} - ${it.artist}" to 1
                )
            )
            if (slow)
                delay(1248L)
        }
        completeResult.add(0, mapOf(context.getString(R.string.all_done) to 2))
    }
}
