package com.hwinzniej.musichelper.utils
//
//import com.hwinzniej.musichelper.data.model.MusicInfo
//import okhttp3.OkHttpClient
//import okio.IOException
//import org.htmlunit.WebClient
//import org.htmlunit.html.HtmlPage
//import org.jsoup.Jsoup
//
//class DoubanMusicApi {
//    companion object {
//        fun getMusicInfo(musicName: String, artistName: String, albumName: String): MusicInfo? {
//
//            val url = "https://search.douban.com/music/subject_search?search_text=Moon halo"
////            val url =
////                "https://music.douban.com/subject_search?search_text=要嫁就嫁灰太狼 周艳泓"
//
//            val webClient = WebClient().apply {
//                options.isCssEnabled = false
//                options.isJavaScriptEnabled = true
//                options.isThrowExceptionOnScriptError = false
//                options.isThrowExceptionOnFailingStatusCode = false
//            }
//
//            webClient.waitForBackgroundJavaScript(500) // 等待JavaScript执行完成
//            val htmlPage: HtmlPage = webClient.getPage(url)
//
//            val musicInfoPage =
//                "https://music.douban.com/subject/[0-9]*/".toRegex().find(htmlPage.asXml())?.value
//
//            val client = OkHttpClient()
//            val request = okhttp3.Request.Builder()
//                .url(musicInfoPage.toString())
//                .build()
//
//            client.newCall(request).execute().use { response ->
//                if (!response.isSuccessful) throw IOException("Unexpected code $response")
//
//                val doc = Jsoup.parse(response.body?.string())
//                val title = doc.title()
//
//                println("Title: $title")
//            }
//
//            return null
//        }
//    }
//}