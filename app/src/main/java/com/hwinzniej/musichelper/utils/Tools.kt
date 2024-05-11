package com.hwinzniej.musichelper.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.util.DisplayMetrics
import androidx.documentfile.provider.DocumentFile
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class Tools {
    fun uriToAbsolutePath(uri: Uri): String {
        val uriPath = uri.pathSegments?.get(uri.pathSegments!!.size - 1).toString()
        return if (uri.toString().contains("com.android.externalstorage.documents")) {
            if (uriPath.contains("primary")) {  //内部存储
                uriPath.replace("primary:", "/storage/emulated/0/")
            } else {  //SD卡
                "/storage/${uriPath.split(":")[0]}/${uriPath.split(":")[1]}"
            }
        } else if (uri.toString().contains("raw:")) {
            uriPath.substring(uriPath.indexOf("raw:") + 4)
        } else {
            ""
        }
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var result = ""
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor.use {
                if (it != null && it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = it.getString(columnIndex)
                    }
                }
            }
        }
        if (result.isBlank()) {
            result = uri.path ?: ""
            val cut = result.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    fun fromUriCopyFileToExternalFilesDir(context: Context, uri: Uri, filename: String): String {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)

        val outputFile = File(context.getExternalFilesDir(null), filename)
        val outputStream = FileOutputStream(outputFile, false)

        inputStream?.copyTo(outputStream)

        inputStream?.close()
        outputStream.close()

        return outputFile.absolutePath
    }

    fun copyFilesToExternalFilesDir(
        uri: Uri,
        context: Context,
        dirName: String,
        clean: Boolean = false
    ): String {
        val directory = DocumentFile.fromTreeUri(context, uri)
        val files = directory?.listFiles()
        if (clean) {
            val externalFilesDir =
                File("${context.getExternalFilesDir(null)?.absolutePath}/${dirName}")
            if (externalFilesDir.isDirectory)
                externalFilesDir.listFiles()?.forEach { file ->
                    file.delete()
                }
        }

        files?.forEach { file ->
            if (file.isFile) {
                val inputStream: InputStream? = context.contentResolver.openInputStream(file.uri)
                val outputFile = File(
                    "${context.getExternalFilesDir(null)?.absolutePath}/${dirName}",
                    file.name ?: ""
                )
                if (outputFile.parentFile?.exists() == false) {
                    outputFile.parentFile?.mkdirs()
                }
                val outputStream: OutputStream = FileOutputStream(outputFile)

                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
        return directory?.name ?: ""
    }

    fun deleteOldFiles(context: Context) {
        val directory = context.getExternalFilesDir(null)
        val currentTime = System.currentTimeMillis()

        directory?.listFiles()?.forEach { file ->
            val fileAge = currentTime - file.lastModified()
            val fileAgeInDays = TimeUnit.MILLISECONDS.toDays(fileAge)

            if (fileAgeInDays > 3) {
                file.deleteRecursively()
            }
        }
    }

    fun readLastNChars(file: File, N: Int): String {
        val raf = RandomAccessFile(file, "r")
        val length = raf.length()
        raf.seek(length - N)
        val lastChars = raf.readLine()
        raf.close()
        return lastChars
    }

    private fun longestCommonSubstringNoOrder(strA: String, strB: String): String {
        return if (strA.length >= strB.length) {
            longestCommonSubstring(strA, strB)
        } else {
            longestCommonSubstring(strB, strA)
        }
    }

    /**
     * 获取最长子串 （长串在前，短串在后）
     *
     * @param strLong
     * @param strShort
     * @return
     *
     *summary:较长的字符串放到前面有助于提交效率
     */
    private fun longestCommonSubstring(strLong: String, strShort: String): String {
        val charsStrA = strLong.toCharArray()
        val charsStrB = strShort.toCharArray()
        var m = charsStrA.size
        var n = charsStrB.size
        val matrix = Array(m + 1) {
            IntArray(
                n + 1
            )
        }
        for (i in 1..m) {
            for (j in 1..n) {
                if (charsStrA[i - 1] == charsStrB[j - 1]) {
                    matrix[i][j] = matrix[i - 1][j - 1] + 1
                } else {
                    matrix[i][j] = matrix[i][j - 1].coerceAtLeast(matrix[i - 1][j])
                }
            }
        }
        val result = CharArray(matrix[m][n])
        var currentIndex = result.size - 1
        while (matrix[m][n] != 0) {
            if (matrix[n].contentEquals(matrix[n - 1])) {
                n--
            } else if (matrix[m][n] == matrix[m - 1][n]) {
                m--
            } else {
                result[currentIndex] = charsStrA[m - 1]
                currentIndex--
                n--
                m--
            }
        }
        return String(result)
    }

    private fun charReg(charValue: Char): Boolean {
        return charValue.code in 0x4E00..0X9FA5 || charValue in 'a'..'z' || charValue in 'A'..'Z' || charValue in '0'..'9'
    }

    private fun removeSign(str: String): String {
        val sb = StringBuilder()
        for (item in str.toCharArray()) {
            if (charReg(item)) {
                sb.append(item)
            }
        }
        return sb.toString()
    }

    /**
     * 比较俩个字符串的相似度（方式一）
     * 步骤1：获取两个串中最长共同子串（有序非连续）
     * 步骤2：共同子串长度 除以 较长串的长度
     *
     * @param strA
     * @param strB
     * @return 两个字符串的相似度
     */
    fun similarDegree(strA: String, strB: String): Double {
        val newStrA = removeSign(strA)
        val newStrB = removeSign(strB)
        val temp = newStrA.length.coerceAtLeast(newStrB.length)
        val temp2 = longestCommonSubstringNoOrder(newStrA, newStrB).length
        return temp2 * 1.0 / temp
    }

    /**
     * 第二种实现方式 (获取两串不匹配字符数)
     *
     * @param str
     * @param target
     * @return
     */
    private fun compare(str: String, target: String): Int {
        val d: Array<IntArray> // 矩阵
        val n = str.length
        val m = target.length
        var j: Int // 遍历target
        var ch1: Char // str
        var ch2: Char // target
        var temp: Int // 记录相同字符,在某个矩阵位置值的增量,不是0就是1
        if (n == 0) {
            return m
        }
        if (m == 0) {
            return n
        }
        d = Array(n + 1) { IntArray(m + 1) }
        // 初始化第一列
        var i = 0 // 遍历str
        while (i <= n) {
            d[i][0] = i
            i++
        }
        // 初始化第一行
        j = 0
        while (j <= m) {
            d[0][j] = j
            j++
        }
        // 遍历str
        i = 1
        while (i <= n) {
            ch1 = str[i - 1]
            // 去匹配target
            j = 1
            while (j <= m) {
                ch2 = target[j - 1]
                temp = if (ch1 == ch2) {
                    0
                } else {
                    1
                }
                // 左边+1,上边+1, 左上角+temp取最小
                d[i][j] = min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + temp)
                j++
            }
            i++
        }
        return d[n][m]
    }

    private fun min(one: Int, two: Int, three: Int): Int {
        var one = one
        return if (one.coerceAtMost(two).also { one = it } < three) one else three
    }

    /**
     * 比较俩个字符串的相似度（方式一）
     * 步骤1：获取两个串中不相同的字符数
     * 步骤2：不同字符数 除以 较长串的长度
     *
     * @param strA
     * @param strB
     * @return
     */
    fun similarityRatio(strA: String, strB: String): Double {
        return 1 - compare(strA, strB).toDouble() / strA.length.coerceAtLeast(strB.length)
    }


    /**
     * 对Map类型的数据根据值(Value)进行排序
     *
     * @param map   要计算的Map集合
     * @param order A:升序 D:降序
     * @return 排序完成后的List
     */
    fun sortByValue(map: Map<String, Double>, order: Char): List<Map.Entry<String, Double>> {
        val entryList2 = ArrayList(map.entries)
        entryList2.sortWith { me1, me2 ->
            if (order == 'A') {
                me1.value.compareTo(me2.value) // 升序排序
            } else {
                me2.value.compareTo(me1.value) // 降序排序
            }
        }
        return entryList2
    }

    /**
     * 对Map类型的数据根据键(Key)进行排序
     *
     * @param map   要计算的Map集合
     * @param order A:升序 D:降序
     * @return 排序完成后的List
     */
    fun sortByKey(map: Map<Int, Int>, order: Char): List<Map.Entry<Int, Int>> {
        val entryList1 = ArrayList(map.entries)
        entryList1.sortWith { me1, me2 ->
            if (order == 'A') {
                me1.key.compareTo(me2.key) // 升序排序
            } else {
                me2.key.compareTo(me1.key) // 降序排序
            }
        }
        return entryList1
    }

    /**
     * 获取Map中Value最大的键值对
     *
     * @param map 要计算的Map集合
     * @return Map.Entry<String, Double> 集合中最大值(Value)的键值对
     */
    fun getMaxValueStringDouble(map: Map<String, Double>): Map.Entry<String, Double>? {
        var maxEntry: Map.Entry<String, Double>? = null
        for (entry in map.entries) {
            if (maxEntry == null || entry.value > maxEntry.value) {
                maxEntry = entry
            }
        }
        return maxEntry
    }

    fun getMaxValueIntDouble(map: Map<Int, Double>): Map.Entry<Int, Double>? {
        var maxEntry: Map.Entry<Int, Double>? = null
        for (entry in map.entries) {
            if (maxEntry == null || entry.value > maxEntry.value) {
                maxEntry = entry
            }
        }
        return maxEntry
    }

    /**
     * 获取Map中Value最小的键值对
     *
     * @param map 要计算的Map集合
     * @return Map.Entry<String, Double> 集合中最小值(Value)的键值对
     */
    fun getMinValue(map: Map<String, Double>): Map.Entry<String, Double>? {
        var minEntry: Map.Entry<String, Double>? = null
        for (entry in map.entries) {
            if (minEntry == null || entry.value < minEntry.value) {
                minEntry = entry
            }
        }
        return minEntry
    }

    fun execShellCmd(cmd: String, withoutRoot: Boolean = false): String {
        val process = if (withoutRoot)
            Runtime.getRuntime().exec("sh")
        else
            Runtime.getRuntime().exec("su --mount-master")
        val outputStream = DataOutputStream(process.outputStream)
        outputStream.writeBytes("${cmd}\n")
        outputStream.flush()
        outputStream.writeBytes("exit\n")
        outputStream.flush()
        val inputStream =
            BufferedReader(InputStreamReader(process.inputStream))
        val errorStream =
            BufferedReader(InputStreamReader(process.errorStream))
        val result = StringBuilder()
        var line: String?
        while (inputStream.readLine().also { line = it } != null) {
            result.append(line + "\n")
        }
        while (errorStream.readLine().also { line = it } != null) {
            result.append(line + "\n")
        }
        process.waitFor()
        process.destroy()
        return result.toString()
    }

    fun encryptString(text: String, type: String, server: String): JSONObject? {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(
                "https://${
                    when (server) {
                        "cf" -> "plnb-cf.hwinzniej.top"
                        "gx1" -> "plnb1.hwinzniej.top:8443"
                        "gx2" -> "plnb2.hwinzniej.top:5862"
                        else -> "plnb-cf.hwinzniej.top"
                    }
                }/?textString=${text}&type=${type}"
            )
            .get()
            .build()
        client.newCall(request).execute().use {
            return JSON.parseObject(it.body?.string())
        }
    }

    fun copyAssetFileToExternalFilesDir(context: Context, filename: String) {
        val assetManager = context.assets

        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            inputStream = assetManager.open(filename)
            val outFile = File(context.getExternalFilesDir(null), filename)
            if (outFile.exists()) {
                outFile.delete()
            }
            outputStream = FileOutputStream(outFile, false)

            val buffer = ByteArray(1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    fun getExceptionDetail(e: Exception): String {
        val sb = StringBuilder()
        sb.append(e.toString())
        sb.append("\n")
        for (element in e.stackTrace) {
            sb.append(element.toString())
            sb.append("\n")
        }
        return sb.toString()
    }

    fun isVersionNewer(curVersion: String, newVersion: String): Boolean {
        return versionStringToDouble(curVersion) < versionStringToDouble(newVersion)
    }

    private fun versionStringToDouble(version: String): Double {
        val versionArray = version.split(".")
        val integerPart = versionArray[0]
        val decimalPart = versionArray.drop(1).joinToString("")
        return "$integerPart.$decimalPart".toDouble()
    }

    fun calPopupLocation(
        density: Float,
        clickOffsetX: Float,
        popupWidth: Int,
        screenWidthDp: Int
    ): Float {
        val temp = (clickOffsetX / density)
        return if (temp < 42 + popupWidth / 2) {
            16f
        } else if (temp > (screenWidthDp - popupWidth)) {
            (screenWidthDp - popupWidth - 48).toFloat()
        } else {
            temp - popupWidth + 65
        }
    }

    fun generateRandomString(
        length: Int,
        includeLowerCase: Boolean = false,
        includeUpperCase: Boolean = false,
        includeDigits: Boolean = false,
        includePunctuation: Boolean = false
    ): String {
        val lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz"
        val upperCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val digits = "0123456789"
        val punctuation = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"

        var allowedChars = ""
        if (includeLowerCase) allowedChars += lowerCaseLetters
        if (includeUpperCase) allowedChars += upperCaseLetters
        if (includeDigits) allowedChars += digits
        if (includePunctuation) allowedChars += punctuation

        return (1..length)
            .map { allowedChars.random(Random) }
            .joinToString("")
    }

    fun md5(input: String, length: Int = 32, toUpperCase: Boolean = false): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        val md5String = digest.joinToString("") {
            String.format("%02x", it)
        }

        val result = when (length) {
            16 -> md5String.substring(8, 24)
            else -> md5String
        }

        return if (toUpperCase) result.uppercase(Locale.ROOT) else result
    }

    fun generateQRCode(content: String, width: Int = 450, height: Int = 450): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        return bitmap
    }

    fun getCurrentIp(): String {
        var currentIp: String
        try {
            val client = OkHttpClient()
            var request = Request.Builder()
            request = request.url("https://myip.ipip.net/s").get()
            client.newCall(request.build()).execute().use {
                currentIp = it.body?.string()?.replace("\n", "") ?: ""
            }
            if (currentIp.isBlank()) {
                request = Request.Builder()
                request = request.url("https://ip.3322.net").get()
                client.newCall(request.build()).execute().use {
                    currentIp = it.body?.string()?.replace("\n", "") ?: ""
                }
                if (currentIp.isBlank())
                    currentIp = "1.180.115.20"
            }
        } catch (e: Exception) {
            currentIp = "1.180.115.20"
        }
        return currentIp
    }

    fun determineImageMimeType(imageBytes: ByteArray): String? {
        val jpegSignature = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        val pngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        val gifSignature = byteArrayOf(0x47, 0x49, 0x46)

        return when {
            imageBytes.startsWith(jpegSignature) -> "image/jpeg"
            imageBytes.startsWith(pngSignature) -> "image/png"
            imageBytes.startsWith(gifSignature) -> "image/gif"
            else -> null
        }
    }

    private fun ByteArray.startsWith(signature: ByteArray): Boolean {
        return this.size >= signature.size && this.sliceArray(signature.indices)
            .contentEquals(signature)
    }

    fun getRealUrl(shortUrl: String): String {
        val client = OkHttpClient.Builder()
            .followRedirects(false)
            .build()

        val request = Request.Builder()
            .url(shortUrl)
            .build()

        client.newCall(request).execute().use { response ->
            return response.header("Location") ?: shortUrl
        }
    }

    fun isTablet(displayMetrics: DisplayMetrics): Boolean {
        val screenWidthInches = displayMetrics.widthPixels / displayMetrics.xdpi
        val screenHeightInches = displayMetrics.heightPixels / displayMetrics.ydpi
        val diagonalInches = sqrt(
            screenWidthInches.toDouble().pow(2.0) + screenHeightInches.toDouble().pow(2.0)
        )
        return diagonalInches >= 7
    }

}