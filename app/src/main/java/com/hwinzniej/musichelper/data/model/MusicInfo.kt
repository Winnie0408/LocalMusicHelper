package com.hwinzniej.musichelper.data.model

class MusicInfo(
    val id: Int,
    var song: String,
    var artist: String,
    var album: String,
    var releaseYear: String?,
    var trackNumber: String?,
    var albumArtist: String?,
    var genre: String?,
    var absolutePath: String,
    var lyricist: String?,
    var composer: String?,
    var arranger: String?
)