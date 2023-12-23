package com.hwinzniej.musichelper.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Music(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "Song") val song: String, //歌名
    @ColumnInfo(name = "Artist") val artist: String,  //歌手
    @ColumnInfo(name = "Album") val album: String,  //专辑
    @ColumnInfo(name = "AbsolutePath") val absolutePath: String,  //绝对路径
    @ColumnInfo(name="ReleaseYear") val releaseYear: String?,  //发行年份
    @ColumnInfo(name="TrackNumber") val trackNumber: String?,  //音轨号
    @ColumnInfo(name="AlbumArtist") val albumArtist: String?,  //专辑艺术家
    @ColumnInfo(name="Genre") val genre: String?,  //风格（流派）
)