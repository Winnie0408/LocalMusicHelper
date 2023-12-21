package com.hwinzniej.musichelper.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Music(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "Song") val song: String,
    @ColumnInfo(name = "Artist") val artist: String,
    @ColumnInfo(name = "Album") val album: String,
    @ColumnInfo(name = "AbsolutePath") val absolutePath: String,
)