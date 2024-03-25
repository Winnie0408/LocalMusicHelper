package com.hwinzniej.musichelper.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hwinzniej.musichelper.data.model.Music
import com.hwinzniej.musichelper.data.model.MusicInfo

@Dao
interface MusicDao {
    @Query("SELECT * FROM music")
    fun getAll(): List<Music>

    @Insert
    fun insertAll(vararg music: Music)

    @Insert
    fun insert(music: Music)

    @Query("DELETE FROM music")
    fun deleteAll()

//    @Query("SELECT song, artist, album, releaseYear, trackNumber, albumArtist, genre FROM music")
//    fun getMusicInfo(): List<MusicInfo>

    @Query("SELECT song, artist, album, absolutePath, id FROM music")
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

    @Query("UPDATE music SET song = :song, artist = :artist, album = :album, albumArtist = :albumArtist, genre = :genre, trackNumber = :trackNumber, releaseYear = :releaseYear WHERE id = :id")
    fun updateMusicInfo(
        id: Int,
        song: String,
        artist: String,
        album: String,
        albumArtist: String?,
        genre: String?,
        trackNumber: String?,
        releaseYear: String?
    )

//    @Insert
//    fun updateMusicInfo(musicInfo: MusicInfo)
}