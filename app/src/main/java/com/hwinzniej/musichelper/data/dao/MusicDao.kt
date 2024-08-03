package com.hwinzniej.musichelper.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hwinzniej.musichelper.data.model.Music
import com.hwinzniej.musichelper.data.model.MusicInfo

@Dao
interface MusicDao {
    @Query("SELECT id, song, artist, album, absolutePath FROM music ORDER BY modifyTime ASC")
    fun getAll(): List<MusicInfo>

    @Query("SELECT id, song, artist, album, absolutePath FROM music WHERE id IN (:selection) ORDER BY modifyTime ASC")
    fun getSelectedMusic(selection: List<Int>): List<MusicInfo>

    @Insert
    fun insertAll(vararg music: Music)

    @Insert
    fun insert(music: Music)

    @Query("DELETE FROM music")
    fun deleteAll()

//    @Query("SELECT song, artist, album, releaseYear, trackNumber, albumArtist, genre FROM music")
//    fun getMusicInfo(): List<MusicInfo>

    @Query("SELECT song, artist, album, absolutePath, id, modifyTime, albumArtist FROM music")
    fun getMusic3Info(): List<MusicInfo>

    @Query("SELECT COUNT(*) FROM music")
    fun getMusicCount(): Int

    @Query("SELECT song, artist, album, absolutePath, id FROM music WHERE song LIKE :keyword OR artist LIKE :keyword OR album LIKE :keyword LIMIT 3")
    fun searchMusic(keyword: String): List<MusicInfo>

    @Query("SELECT song, artist, album, absolutePath, id FROM music WHERE song LIKE :keyword OR artist LIKE :keyword OR album LIKE :keyword OR absolutePath LIKE :keyword")
    fun searchMusicAll(keyword: String): List<MusicInfo>

    @Query("SELECT song, artist, album, absolutePath, id FROM music WHERE id = :id")
    fun getMusicById(id: Int): MusicInfo

    @Query("SELECT song, artist, album, absolutePath, id, albumArtist, genre, trackNumber, releaseYear FROM music WHERE id = :id")
    fun getMusicAllInfo(id: Int): MusicInfo

    @Query("UPDATE music SET song = :song, artist = :artist, album = :album, albumArtist = :albumArtist, genre = :genre, trackNumber = :trackNumber, releaseYear = :releaseYear, lyricist = :lyricist, composer = :composer, arranger = :arranger, modifyTime = :modifyTime WHERE id = :id")
    fun updateMusicInfo(
        id: Int,
        song: String,
        artist: String,
        album: String,
        albumArtist: String?,
        genre: String?,
        trackNumber: String?,
        releaseYear: String?,
        lyricist: String?,
        composer: String?,
        arranger: String?,
        modifyTime: Long,
    )

    @Query(
        """
        SELECT *
        FROM music
        WHERE album IN (
            SELECT album 
            FROM music
            GROUP BY album
            HAVING COUNT(*) >= 2
        )
    """
    )
    fun searchDuplicateAlbum(): List<MusicInfo>

    //    @Query(
//        """
//        SELECT *
//        FROM music
//        WHERE album IN (
//            SELECT album
//            FROM music
//            WHERE albumArtist IS NULL OR albumArtist = ''
//            GROUP BY album
//            HAVING COUNT(*) >= 2
//        ) AND (albumArtist IS NULL OR albumArtist = '')
//    """
//    )
    @Query("SELECT id, song, artist, album, absolutePath FROM music WHERE albumArtist IS NULL OR albumArtist = '' ORDER BY modifyTime ASC")
    fun searchDuplicateAlbumNoOverwrite(): List<MusicInfo>

    @Query("SELECT id, song, artist, album, absolutePath FROM music WHERE id IN (:selection) AND (albumArtist IS NULL OR albumArtist = '') ORDER BY modifyTime ASC")
    fun searchSelectedDuplicateAlbumNoOverwrite(selection: List<Int>): List<MusicInfo>

    @Query("SELECT DISTINCT artist FROM music WHERE album = :album")
    fun getDuplicateAlbumArtistList(album: String): List<String>

    @Query("UPDATE music SET albumArtist = :albumArtist, modifyTime = :modifyTime WHERE id = :id")
    fun updateAlbumArtist(
        id: Int,
        albumArtist: String?,
        modifyTime: Long
    )

    @Query("SELECT COUNT(*) FROM music WHERE albumArtist IS NULL OR albumArtist = ''")
    fun countNullAlbumArtist(): Int

    @Query("SELECT COUNT(*) FROM music WHERE id IN (:selection) AND (albumArtist IS NULL OR albumArtist = '')")
    fun countSelectedNullAlbumArtist(selection: List<Int>): Int

    @Query("SELECT COUNT(*) FROM music WHERE lyricist IS NULL OR lyricist = ''")
    fun countNullLyricist(): Int

    @Query("SELECT COUNT(*) FROM music WHERE composer IS NULL OR composer = ''")
    fun countNullComposer(): Int

    @Query("SELECT COUNT(*) FROM music WHERE arranger IS NULL OR arranger = ''")
    fun countNullArranger(): Int

    @Query("SELECT COUNT(*) FROM music WHERE id IN (:selection) AND (lyricist IS NULL OR lyricist = '')")
    fun countSelectedNullLyricist(selection: List<Int>): Int

    @Query("SELECT COUNT(*) FROM music WHERE id IN (:selection) AND (composer IS NULL OR composer = '')")
    fun countSelectedNullComposer(selection: List<Int>): Int

    @Query("SELECT COUNT(*) FROM music WHERE id IN (:selection) AND (arranger IS NULL OR arranger = '')")
    fun countSelectedNullArranger(selection: List<Int>): Int

    @Query("UPDATE music SET lyricist = :lyricist, composer = :composer, arranger = :arranger, modifyTime = :modifyTime WHERE id = :id")
    fun updateLyricistComposerArranger(
        id: Int,
        lyricist: String?,
        composer: String?,
        arranger: String?,
        modifyTime: Long
    )

//    @Insert
//    fun updateMusicInfo(musicInfo: MusicInfo)
}