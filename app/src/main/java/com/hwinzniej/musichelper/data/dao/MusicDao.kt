package com.hwinzniej.musichelper.data.dao

import androidx.room.Dao
import androidx.room.Delete
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

    @Query("SELECT song, artist, album, releaseYear, trackNumber, albumArtist, genre FROM music")
    fun getMusicInfo(): List<MusicInfo>

//    @Insert
//    fun updateMusicInfo(musicInfo: MusicInfo)
}