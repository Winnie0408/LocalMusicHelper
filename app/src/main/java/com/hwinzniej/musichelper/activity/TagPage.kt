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
import com.hwinzniej.musichelper.utils.LyricCreditParser
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
import kotlin.time.Duration.Companion.milliseconds

class TagPage(
    val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    val db: MusicDatabase,
    val openMusicCoverLauncher: ActivityResultLauncher<Array<String>>
) {
    companion object {
        /** 「保持原修改时间顺序」模式：写入后恢复原有的修改时间 */
        const val KEEP_ORDER_RESTORE_MODIFY_TIME = 0

        /** 「保持原修改时间顺序」模式：将处理速度限制为每秒1个文件 */
        const val KEEP_ORDER_LIMIT_SPEED = 1

        /** 限速模式下每个文件之间的间隔，略大于1秒以保证文件修改时间严格递增 */
        private const val LIMIT_SPEED_INTERVAL_MS = 1248L
    }

    /** 写入数据库的修改时间：恢复模式下沿用原有的 modifyTime，以保持列表中的原排序 */
    private fun newModifyTime(id: Int, keepOrder: Boolean, keepOrderMode: Int): Long =
        if (keepOrder && keepOrderMode == KEEP_ORDER_RESTORE_MODIFY_TIME)
            db.musicDao().getModifyTime(id) ?: System.currentTimeMillis()
        else
            System.currentTimeMillis()

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
            "producer" to audioFile.tag.getFirst(FieldKey.PRODUCER),
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
                "producer" to FieldKey.PRODUCER,
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
            producer = songInfoModified["producer"] ?: "",
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
     * [keepOrder] 为 true 时「保持原修改时间顺序」，[keepOrderMode] 决定具体方式：
     * [KEEP_ORDER_RESTORE_MODIFY_TIME] 写入后恢复文件原有的修改时间并保留列表中的原排序；
     * [KEEP_ORDER_LIMIT_SPEED] 将处理速度限制为每秒1个文件。
     */
    suspend fun batchEditTags(
        fields: Map<String, String>,
        cover: ByteArray?,
        coverAction: Int,
        keepOrder: Boolean,
        keepOrderMode: Int,
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
                if (keepOrder && keepOrderMode == KEEP_ORDER_RESTORE_MODIFY_TIME)
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

            val modifyTime = newModifyTime(musicInfo.id, keepOrder, keepOrderMode)
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
            if (keepOrder && keepOrderMode == KEEP_ORDER_LIMIT_SPEED)
                delay(LIMIT_SPEED_INTERVAL_MS.milliseconds)
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
        keepOrder: Boolean,
        keepOrderMode: Int,
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
            val file = File(it.absolutePath)
            val originalFileModifyTime = file.lastModified()
            try {
                audioFile = AudioFileIO.read(file)
                audioFile.tag.setField(FieldKey.ALBUM_ARTIST, lastAlbumArtist)
                AudioFileIO.write(audioFile)
                if (keepOrder && keepOrderMode == KEEP_ORDER_RESTORE_MODIFY_TIME)
                    file.setLastModified(originalFileModifyTime)
                db.musicDao().updateAlbumArtist(
                    it.id,
                    lastAlbumArtist,
                    newModifyTime(it.id, keepOrder, keepOrderMode)
                )
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
            if (keepOrder && keepOrderMode == KEEP_ORDER_LIMIT_SPEED)
                delay(LIMIT_SPEED_INTERVAL_MS.milliseconds)
        }
        if (haveError) {
            completeResult.sortBy { it.values.first() }
        }
        completeResult.add(0, mapOf(context.getString(R.string.all_done) to 2))
    }

    /** 可从歌词中补全的「词/曲」类标签 */
    private data class CreditTag(
        val role: LyricCreditParser.Role,
        val fieldKey: FieldKey,
        val nameRes: Int
    )

    private val creditTags = listOf(
        CreditTag(LyricCreditParser.Role.LYRICIST, FieldKey.LYRICIST, R.string.lyricist),
        CreditTag(LyricCreditParser.Role.COMPOSER, FieldKey.COMPOSER, R.string.composer),
        CreditTag(LyricCreditParser.Role.ARRANGER, FieldKey.ARRANGER, R.string.arranger),
        CreditTag(LyricCreditParser.Role.PRODUCER, FieldKey.PRODUCER, R.string.producer),
    )

    suspend fun searchBlankLyricistComposerArranger(
        completeResult: MutableList<Map<String, Int>>,
        multiSelect: Boolean,
        selectedSongList: SnapshotStateList<Int>
    ): Boolean {
        val selectAll = selectedSongList.all { it == 0 }
        if (selectAll && multiSelect)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.no_song_selected_default_all),
                    Toast.LENGTH_SHORT
                ).show()
            }
        val innerSelectedSongList =
            selectedSongList.withIndex().filter { it.value == 1 }.map { it.index }
        val location = if (selectAll) context.getString(R.string.song_library)
        else context.getString(R.string.selected_songs)
        val nullCounts = creditTags.map { creditTag ->
            val count = when (creditTag.role) {
                LyricCreditParser.Role.LYRICIST -> if (selectAll) db.musicDao().countNullLyricist()
                else db.musicDao().countSelectedNullLyricist(innerSelectedSongList)

                LyricCreditParser.Role.COMPOSER -> if (selectAll) db.musicDao().countNullComposer()
                else db.musicDao().countSelectedNullComposer(innerSelectedSongList)

                LyricCreditParser.Role.ARRANGER -> if (selectAll) db.musicDao().countNullArranger()
                else db.musicDao().countSelectedNullArranger(innerSelectedSongList)

                LyricCreditParser.Role.PRODUCER -> if (selectAll) db.musicDao().countNullProducer()
                else db.musicDao().countSelectedNullProducer(innerSelectedSongList)
            }
            val tagName = context.getString(creditTag.nameRes)
            if (count == 0) {
                completeResult.add(
                    mapOf(
                        context.getString(R.string.no_tagname_null_count_in_song_list)
                            .replace("#location", location)
                            .replace("#", tagName) to 2
                    )
                )
            } else {
                completeResult.add(
                    mapOf(
                        context.getString(R.string.total_tagname_null_count_in_song_list)
                            .replace("#tagName", tagName)
                            .replace("#location", location)
                            .replace("#", count.toString()) to 2
                    )
                )
            }
            count
        }
        return if (nullCounts.any { it != 0 }) {
            completeResult.add(mapOf(context.getString(R.string.click_ok_to_start) to 2))
            true
        } else {
            completeResult.add(mapOf(context.getString(R.string.click_ok_to_start2) to 2))
            false
        }
    }

    /**
     * 从歌词中补全作词、作曲、编曲、制作人（Winnie0408/LocalMusicHelper#23）。
     * 未开启 [overwrite] 时仅补全为空的标签（查漏补缺）。
     */
    suspend fun handleBlankLyricistComposerArranger(
        overwrite: Boolean,
        lyricist: Boolean,
        composer: Boolean,
        arranger: Boolean,
        producer: Boolean,
        keepOrder: Boolean,
        keepOrderMode: Int,
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
        val enabledRoles = buildSet {
            if (lyricist) add(LyricCreditParser.Role.LYRICIST)
            if (composer) add(LyricCreditParser.Role.COMPOSER)
            if (arranger) add(LyricCreditParser.Role.ARRANGER)
            if (producer) add(LyricCreditParser.Role.PRODUCER)
        }
        // 日志按倒序插入，故此处按「制作人、编曲、作曲、作词」的顺序处理，显示时为正序
        val tagsToComplete = creditTags.filter { it.role in enabledRoles }.reversed()
        searchResult.forEach {
            val file = File(it.absolutePath)
            val originalFileModifyTime = file.lastModified()
            val audioFile: AudioFile
            var songLyrics: String
            try {
                audioFile = AudioFileIO.read(file)
                songLyrics = audioFile.tag.getFirst(FieldKey.LYRICS)
                val lyricFile = File(it.absolutePath.substringBefore('.') + ".lrc")
                if (songLyrics.isBlank() && lyricFile.exists()) {
                    songLyrics = lyricFile.readText()
                }
            } catch (_: Exception) {
                completeResult.add(0, mapOf("" to 1))
                completeResult.add(
                    0,
                    mapOf(context.getString(R.string.batch_edit_failed).replace("#", it.song) to 0)
                )
                return@forEach
            }

            if (songLyrics.isBlank()) {
                completeResult.add(0, mapOf("" to 1))
                completeResult.add(0, mapOf(context.getString(R.string.lrc_empty_skip) to 0))
                completeResult.add(0, mapOf(it.song to 1))
                return@forEach
            }
            val credits = LyricCreditParser.parse(songLyrics)
            completeResult.add(0, mapOf("" to 1))

            tagsToComplete.forEach { creditTag ->
                val tagName = context.getString(creditTag.nameRes)
                val originalValue = audioFile.tag.getFirst(creditTag.fieldKey)
                val tempData = credits[creditTag.role]
                if ((overwrite || originalValue.isBlank()) && !tempData.isNullOrBlank()) {
                    audioFile.tag.setField(creditTag.fieldKey, tempData)
                    completeResult.add(0, mapOf("$tagName: $tempData" to 1))
                } else if (tempData.isNullOrBlank()) {
                    completeResult.add(
                        0,
                        mapOf("$tagName: ${context.getString(R.string.lrc_not_contain_info)}" to 0)
                    )
                } else if (originalValue.isNotBlank()) {
                    completeResult.add(
                        0,
                        mapOf("$tagName: ${context.getString(R.string.keep_original_value)}" to 1)
                    )
                }
            }
            try {
                audioFile.commit()
            } catch (_: Exception) {
                completeResult.add(
                    0,
                    mapOf(context.getString(R.string.batch_edit_failed).replace("#", it.song) to 0)
                )
                return@forEach
            }
            if (keepOrder && keepOrderMode == KEEP_ORDER_RESTORE_MODIFY_TIME)
                file.setLastModified(originalFileModifyTime)
            db.musicDao().updateLyricistComposerArrangerProducer(
                id = it.id,
                lyricist = audioFile.tag.getFirst(FieldKey.LYRICIST),
                composer = audioFile.tag.getFirst(FieldKey.COMPOSER),
                arranger = audioFile.tag.getFirst(FieldKey.ARRANGER),
                producer = audioFile.tag.getFirst(FieldKey.PRODUCER),
                modifyTime = newModifyTime(it.id, keepOrder, keepOrderMode)
            )
            completeResult.add(
                0,
                mapOf(
                    "${it.song} - ${it.artist}" to 1
                )
            )
            if (keepOrder && keepOrderMode == KEEP_ORDER_LIMIT_SPEED)
                delay(LIMIT_SPEED_INTERVAL_MS.milliseconds)
        }
        completeResult.add(0, mapOf(context.getString(R.string.all_done) to 2))
    }
}
