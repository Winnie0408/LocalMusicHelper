package com.hwinzniej.musichelper.data

class SourceApp {
    lateinit var sourceEng: String
    lateinit var sourceChn: String
    lateinit var databaseName: String
    lateinit var songListTableName: String
    lateinit var songListId: String
    lateinit var songListName: String
    lateinit var songListSongInfoTableName: String
    lateinit var songListSongInfoPlaylistId: String
    lateinit var songListSongInfoSongId: String
    lateinit var songInfoTableName: String
    lateinit var sortField: String
    lateinit var songInfoSongId: String
    lateinit var songInfoSongName: String
    lateinit var songInfoSongArtist: String
    lateinit var songInfoSongAlbum: String
    lateinit var pakageName: String

    fun init(sourceApp: String) {
        when (sourceApp) {
            "QQMusic" -> {
                sourceEng = "QQMusic"
                sourceChn = "QQ音乐"
                databaseName = "QQMusic"
                songListTableName = "User_Folder_table"
                songListId = "folderid"
                songListName = "foldername"
                songListSongInfoTableName = "User_Folder_Song_table"
                songListSongInfoPlaylistId = "folderid"
                songListSongInfoSongId = "id"
                songInfoTableName = "Song_table"
                sortField = "position"
                songInfoSongId = "id"
                songInfoSongName = "name"
                songInfoSongArtist = "singername"
                songInfoSongAlbum = "albumname"
                pakageName = "com.tencent.qqmusic"
            }

            "CloudMusic" -> {
                sourceEng = "CloudMusic"
                sourceChn = "网易云音乐"
                databaseName = "cloudmusic.db"
                songListTableName = "playlist"
                songListId = "_id"
                songListName = "name"
                songListSongInfoTableName = "playlist_track"
                songListSongInfoPlaylistId = "playlist_id"
                songListSongInfoSongId = "track_id"
                songInfoTableName = "track"
                sortField = "track_order"
                songInfoSongId = "id"
                songInfoSongName = "name"
                songInfoSongArtist = "artists"
                songInfoSongAlbum = "album_name"
                pakageName =
                    "com.netease.cloudmusic"  //TODO 适配荣耀定制版网易云音乐 com.hihonor.cloudmusic 数据库结构相同
            }

            "KugouMusic" -> {
                sourceEng = "KuGouMusic"
                sourceChn = "酷狗音乐"
                databaseName = "kugou_music_phone_v7.db"
                songListTableName = "kugou_playlists"
                songListId = "_id"
                songListName = "name"
                songListSongInfoTableName = "playlistsong"
                songListSongInfoPlaylistId = "plistid"
                songListSongInfoSongId = "songid"
                songInfoTableName = "kugou_songs"
                sortField = "cloudfileorderweight"
                songInfoSongId = "_id"
                songInfoSongName = "trackName"
                songInfoSongArtist = "artistName"
                songInfoSongAlbum = "albumName"
                pakageName = "com.kugou.android"
            }

            "KuwoMusic" -> {
                sourceEng = "KuWoMusic"
                sourceChn = "酷我音乐"
                databaseName = "kwplayer.db"
                songListTableName = "v3_list"
                songListId = "id"
                songListName = "showname"
                songListSongInfoTableName = "v3_music"
                songListSongInfoPlaylistId = "listid"
                songListSongInfoSongId = "rid"
                songInfoTableName = "v3_music"
                sortField = "id"
                songInfoSongId = "rid"
                songInfoSongName = "name"
                songInfoSongArtist = "artist"
                songInfoSongAlbum = "album"
                pakageName = "cn.kuwo.player"
            }
        }
    }
}