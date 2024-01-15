package com.hwinzniej.musichelper.data.model

class MusicInfo(
    val id: Int,
    val song: String,
    val artist: String,
    val album: String,
    var releaseYear: String?,
    var trackNumber: String?,
    var albumArtist: String?,
    var genre: String?,
    var absolutePath: String,
)