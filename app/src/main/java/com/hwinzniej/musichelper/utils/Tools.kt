package com.hwinzniej.musichelper.utils

import android.net.Uri
import java.io.File
import java.io.RandomAccessFile

class Tools {
    fun uriToAbsolutePath(uri: Uri): String {
        val uriPath = uri.pathSegments?.get(uri.pathSegments!!.size - 1).toString()
        val absolutePath = if (uriPath.contains("primary")) {  //内部存储
            uriPath.replace("primary:", "/storage/emulated/0/")
        } else {  //SD卡
            "/storage/${uriPath.split(":")[0]}/${uriPath.split(":")[1]}"
        }
        return absolutePath
    }

    fun readLastNChars(file: File, N: Int): String {
        val raf = RandomAccessFile(file, "r")
        val length = raf.length()
        raf.seek(length - N)
        val lastChars = raf.readLine()
        raf.close()
        return lastChars
    }
}