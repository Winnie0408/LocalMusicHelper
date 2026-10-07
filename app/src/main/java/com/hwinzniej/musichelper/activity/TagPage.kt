package com.hwinzniej.musichelper.activity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
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
import java.util.Collections

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
            }, it.artist, it.album, it.id.toString(), it.absolutePath)
            selectedSongList.add(0)
        }
    }

    fun getSongInfo(id: Int, cover: MutableState<ByteArray?>): Map<String, String?> {
        cover.value = null
        val audioFile: AudioFile
        val musicInfo = db.musicDao().getMusicById(id)
        audioFile = AudioFileIO.read(File(musicInfo.absolutePath))
        val lyricFile = File(musicInfo.absolutePath.substringBefore('.') + ".lrc")
        val lyricFileContent = if (lyricFile.exists()) {
            lyricFile.readText()
        } else {
            ""
        }
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
            "lyrics" to audioFile.tag.getFirst(FieldKey.LYRICS).ifBlank { lyricFileContent },
            "lyricsFromOuterLrc" to if (lyricFileContent.isNotBlank()) "true" else "false"
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
                    if (it.key == "lyrics") {
                        if (songInfoModified["lyricsFromOuterLrc"].equals("true")) {
                            val lyricFile =
                                File(musicInfo.absolutePath.substringBefore('.') + ".lrc")
                            // 将修改后的歌词写入lyricFile文件
                            lyricFile.writeText(songInfoModified[it.key]!!)
                        } else {
                            audioFile.tag.setField(it.value, songInfoModified[it.key])
                        }
                    } else {
                        audioFile.tag.setField(it.value, songInfoModified[it.key])
                    }
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
            invalidateCoverThumbnail(musicInfo.absolutePath)
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

    /**
     * 歌曲列表封面缩略图缓存。key 为歌曲绝对路径，value 为缩略图；
     * 没有封面的歌曲记录在 [pathsWithoutCover] 中，避免重复读取文件。
     */
    private val coverThumbnailCache = object : LruCache<String, Bitmap>(
        ((Runtime.getRuntime().maxMemory() / 1024) / 8).toInt().coerceAtLeast(2048)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val pathsWithoutCover = Collections.synchronizedSet(mutableSetOf<String>())

    suspend fun getCoverThumbnail(absolutePath: String, targetSizePx: Int = 128): Bitmap? =
        withContext(Dispatchers.IO) {
            coverThumbnailCache.get(absolutePath)?.let { return@withContext it }
            if (pathsWithoutCover.contains(absolutePath)) return@withContext null
            val binaryData = try {
                AudioFileIO.read(File(absolutePath)).tag?.firstArtwork?.binaryData
            } catch (_: Exception) {
                null
            }
            if (binaryData == null || binaryData.isEmpty()) {
                pathsWithoutCover.add(absolutePath)
                return@withContext null
            }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(binaryData, 0, binaryData.size, options)
            var sampleSize = 1
            while (options.outHeight / sampleSize > targetSizePx && options.outWidth / sampleSize > targetSizePx) {
                sampleSize *= 2
            }
            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize
            val bitmap = try {
                BitmapFactory.decodeByteArray(binaryData, 0, binaryData.size, options)
            } catch (_: Exception) {
                null
            }
            if (bitmap == null) {
                pathsWithoutCover.add(absolutePath)
                return@withContext null
            }
            coverThumbnailCache.put(absolutePath, bitmap)
            bitmap
        }

    private fun invalidateCoverThumbnail(absolutePath: String) {
        coverThumbnailCache.remove(absolutePath)
        pathsWithoutCover.remove(absolutePath)
    }

    /**
     * 批量修改选中歌曲的标签。[fields] 中只包含用户勾选要写入的标签，
     * 空字符串表示清除该标签。[coverAction] 为 1 时写入 [cover]，为 -1 时删除封面，为 0 时不改动封面。
     * [keepModifyTime] 为 true 时写入后恢复文件原有的修改时间，并保留列表中的原排序。
     */
    suspend fun batchEditTags(
        fields: Map<String, String>,
        cover: ByteArray?,
        coverAction: Int,
        keepModifyTime: Boolean,
        slow: Boolean,
        completeResult: MutableList<Map<String, Int>>,
        selectedSongList: SnapshotStateList<Int>
    ) {
        val selection = selectedSongList.withIndex().filter { it.value == 1 }.map { it.index }
        if (selection.isEmpty()) {
            completeResult.add(0, mapOf(context.getString(R.string.no_song_selected) to 0))
            return
        }
        var coverMimeType: String? = null
        if (coverAction == 1) {
            coverMimeType = cover?.let { Tools().determineImageMimeType(it) }
            if (coverMimeType == null) {
                completeResult.add(
                    0,
                    mapOf(context.getString(R.string.cover_image_not_support) to 0)
                )
                return
            }
        }
        val musicTag = mapOf(
            "artist" to FieldKey.ARTIST,
            "album" to FieldKey.ALBUM,
            "albumArtist" to FieldKey.ALBUM_ARTIST,
            "genre" to FieldKey.GENRE,
            "discNumber" to FieldKey.DISC_NO,
            "releaseYear" to FieldKey.YEAR,
        )
        var haveError = false
        db.musicDao().getSelectedMusic(selection).forEach { musicInfo ->
            completeResult.add(0, mapOf("" to 1))
            val file = File(musicInfo.absolutePath)
            val originalFileModifyTime = file.lastModified()
            try {
                val audioFile = AudioFileIO.read(file)
                musicTag.forEach { (key, fieldKey) ->
                    val value = fields[key] ?: return@forEach
                    if (value.isBlank())
                        audioFile.tag.deleteField(fieldKey)
                    else
                        audioFile.tag.setField(fieldKey, value)
                }
                if (coverAction != 0) {
                    audioFile.tag.deleteArtworkField()
                    if (coverAction == 1) {
                        val artwork = ArtworkFactory.getNew()
                        artwork.binaryData = cover
                        artwork.mimeType = coverMimeType
                        audioFile.tag.setField(artwork)
                    }
                }
                AudioFileIO.write(audioFile)
                invalidateCoverThumbnail(musicInfo.absolutePath)
                if (keepModifyTime)
                    file.setLastModified(originalFileModifyTime)
            } catch (_: Exception) {
                completeResult.add(
                    0,
                    mapOf(
                        context.getString(R.string.batch_edit_failed).replace("#", musicInfo.song)
                                to 0
                    )
                )
                haveError = true
                return@forEach
            }

            val modifyTime = if (keepModifyTime)
                db.musicDao().getModifyTime(musicInfo.id) ?: System.currentTimeMillis()
            else
                System.currentTimeMillis()
            fields["artist"]?.let { db.musicDao().updateArtist(musicInfo.id, it, modifyTime) }
            fields["album"]?.let { db.musicDao().updateAlbum(musicInfo.id, it, modifyTime) }
            fields["albumArtist"]?.let {
                db.musicDao().updateAlbumArtist(musicInfo.id, it, modifyTime)
            }
            fields["genre"]?.let { db.musicDao().updateGenre(musicInfo.id, it, modifyTime) }
            fields["releaseYear"]?.let {
                db.musicDao().updateReleaseYear(musicInfo.id, it, modifyTime)
            }

            completeResult.add(
                0,
                mapOf(
                    context.getString(R.string.batch_edit_success).replace("#", musicInfo.song) to 1
                )
            )
            if (slow && !keepModifyTime)
                delay(1248L)
        }
        if (haveError)
            completeResult.sortBy { it.values.first() }
        completeResult.add(0, mapOf(context.getString(R.string.all_done) to 2))
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
            }, it.artist, it.album, it.id.toString(), it.absolutePath)
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
                db.musicDao().getSelectedMusic(
                    selectedSongList.withIndex()
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
                db.musicDao().updateAlbumArtist(it.id, lastAlbumArtist, System.currentTimeMillis())
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
                db.musicDao().getSelectedMusic(
                    selectedSongList.withIndex()
                        .filter { it.value == 1 }
                        .map { it.index })
            }
        val lyricistRegex = buildCreditRegex(
            chinese = "(作)?[词詞]",
            english = "Lyricist|Lyrics|Lyric",
            englishBy = "Lyrics|Lyric|Written"
        )
        val composerRegex = buildCreditRegex(
            chinese = "(作)?曲",
            english = "Composer|Compose",
            englishBy = "Composed|Written"
        )
        val arrangerRegex = buildCreditRegex(
            chinese = "[编編]曲",
            english = "Arranger|Arrangement|Arrange",
            englishBy = "Arranged"
        )
        val cleanRegex = "\\s?([/&|,，])\\s?".toRegex()
        searchResult.forEach {
//            var modified = false
            val audioFile = AudioFileIO.read(File(it.absolutePath))
            var songLyrics = audioFile.tag.getFirst(FieldKey.LYRICS)

            val lyricFile = File(it.absolutePath.substringBefore('.') + ".lrc")
            if (songLyrics.isBlank() && lyricFile.exists()) {
                songLyrics = lyricFile.readText()
            }

            if (songLyrics.isBlank()) {
                completeResult.add(0, mapOf("" to 1))
                completeResult.add(0, mapOf(context.getString(R.string.lrc_empty_skip) to 0))
                completeResult.add(0, mapOf(it.song to 1))
                return@forEach
            }
            songLyrics = processTextByTextLyrics(songLyrics)
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

    /**
     * 构造用于从歌词中提取「作词/作曲/编曲」的正则。支持的标签写法：
     * 作词：、作词 Lyrics：、作词/Lyricist：、词/Lyricist：、Lyricist/作词：、Lyrics by 等
     */
    private fun buildCreditRegex(chinese: String, english: String, englishBy: String): Regex {
        val timestamp = "\\[\\d{2}:\\d{2}\\.\\d{2,3}]\\s*"
        // 中英文标签之间的连接符，如「作词/Lyricist」「作词 Lyrics」
        val connector = "(\\s*[/／·・丨|｜、-]\\s*|\\s*)"
        return ("${timestamp}(" +
                // 中文在前：作词、作词/Lyricist、作词 Lyrics
                "($chinese)($connector($english))?\\s*[：:]?\\s*" +
                "|" +
                // 英文在前：Lyricist：、Lyricist/作词：（必须带冒号，避免误匹配正文歌词）
                "($english)($connector($chinese))?\\s*[：:]\\s*" +
                "|" +
                // Lyrics by 形式
                "($englishBy)\\s+by\\s*[：:]?\\s*" +
                ")(.*)\\n?").toRegex()
    }

    suspend fun processTextByTextLyrics(input: String): String {
        val lines = input.split("\n")
        val timestampRegex = Regex("""(?:\[\d{2}:\d{2}\.\d{2,3}\]|<\d{2}:\d{2}\.\d{2,3}>)""")

        val processedLines = lines.map { line ->
            val matches = timestampRegex.findAll(line).toList()
            if (matches.isNotEmpty()) {
                val firstTimestamp = matches[0].value
                val textWithoutTimestamps = line.replace(timestampRegex, "")
                "$firstTimestamp$textWithoutTimestamps"
            } else {
                line // 没有时间戳的行保持不变
            }
        }

        return processedLines.joinToString("\n")
    }
}
