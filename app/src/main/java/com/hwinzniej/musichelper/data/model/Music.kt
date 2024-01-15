package com.hwinzniej.musichelper.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Music(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "song") val song: String, //歌名
    @ColumnInfo(name = "artist") val artist: String,  //歌手
    @ColumnInfo(name = "album") val album: String,  //专辑
    @ColumnInfo(name = "absolutePath") val absolutePath: String,  //绝对路径
    @ColumnInfo(name = "releaseYear") val releaseYear: String,  //发行年份
    @ColumnInfo(name = "trackNumber") val trackNumber: String,  //音轨号
    @ColumnInfo(name = "albumArtist") val albumArtist: String,  //专辑艺术家
    @ColumnInfo(name = "genre") val genre: String,  //风格（流派）
)
