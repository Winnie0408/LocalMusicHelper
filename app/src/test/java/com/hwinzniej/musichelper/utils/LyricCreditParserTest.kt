package com.hwinzniej.musichelper.utils

import com.hwinzniej.musichelper.utils.LyricCreditParser.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricCreditParserTest {

    private fun roleOf(line: String): Role? = LyricCreditParser.parseLine(line)?.roles?.firstOrNull()

    @Test
    fun multiRoleLine_takesEverythingAfterColon() {
        val credit = LyricCreditParser.parseLine("[00:03.21]编曲/吉他Arranger/Guitar：谭侃侃")!!
        assertEquals(setOf(Role.ARRANGER), credit.roles)
        assertEquals("谭侃侃", credit.value)
    }

    @Test
    fun allLyricistKeywords() {
        listOf(
            "Written by：", "Lyrics by：", "作词：", "作詞：", "词：", "詞：",
            "作词 Lyrics：", "作詞 Lyrics：", "词 Lyrics：", "詞 Lyrics：",
            "作词Lyrics：", "作詞Lyrics：", "词Lyrics：", "詞Lyrics：",
            "作词 Lyricist：", "作詞 Lyricist：", "词 Lyricist：", "詞 Lyricist：",
            "作词Lyricist：", "作詞Lyricist：", "词Lyricist：", "詞Lyricist：",
            "作词/Lyricist：", "作詞/Lyricist：", "词/Lyricist：", "詞/Lyricist："
        ).forEach { head ->
            val credit = LyricCreditParser.parseLine("[00:00.00]$head 张三 ")
            assertEquals(head, setOf(Role.LYRICIST), credit?.roles)
            assertEquals(head, "张三", credit?.value)
        }
    }

    @Test
    fun allComposerKeywords() {
        listOf(
            "Composed by：", "作曲：", "曲：", "作曲 Composer：", "曲 Composer：",
            "作曲Composer：", "曲Composer：", "作曲/Composer：", "曲/Composer："
        ).forEach { head ->
            val credit = LyricCreditParser.parseLine("[00:00.000]${head}李四")
            assertEquals(head, setOf(Role.COMPOSER), credit?.roles)
            assertEquals(head, "李四", credit?.value)
        }
    }

    @Test
    fun allArrangerKeywords() {
        listOf(
            "Arranged by：", "编曲：", "編曲：", "编曲 Arranger：", "編曲 Arranger：",
            "编曲 Arrangement：", "編曲 Arrangement：", "编曲Arranger：", "編曲Arranger：",
            "编曲Arrangement：", "編曲Arrangement：", "编曲/Arranger：", "編曲/Arranger："
        ).forEach { head ->
            assertEquals(head, Role.ARRANGER, roleOf("[00:00.00]${head}王五"))
        }
    }

    @Test
    fun allProducerKeywords() {
        listOf(
            "Produced by：", "制作人：", "音乐制作人：", "制作：", "制作人 Produced by：",
            "制作人 Producer：", "制作人Producer：", "制作人/Producer："
        ).forEach { head ->
            val credit = LyricCreditParser.parseLine("[00:00.00]${head}赵六")
            assertEquals(head, setOf(Role.PRODUCER), credit?.roles)
            assertEquals(head, "赵六", credit?.value)
        }
    }

    @Test
    fun halfWidthColonAndSpaces() {
        val credit = LyricCreditParser.parseLine("[00:01.00] 作曲 : 周杰伦 ")!!
        assertEquals(setOf(Role.COMPOSER), credit.roles)
        assertEquals("周杰伦", credit.value)
    }

    @Test
    fun wordByWordTimestamps() {
        val credit = LyricCreditParser.parseLine(
            "[00:01.000]<00:01.000>作<00:01.100>词<00:01.200>：<00:01.300>方<00:01.400>文<00:01.500>山"
        )!!
        assertEquals(setOf(Role.LYRICIST), credit.roles)
        assertEquals("方文山", credit.value)
        val enhanced = LyricCreditParser.parseLine("[00:01.00]制作人[00:01.10]:[00:01.20]Someone")!!
        assertEquals(setOf(Role.PRODUCER), enhanced.roles)
        assertEquals("Someone", enhanced.value)
    }

    @Test
    fun multipleRolesInOneLine() {
        val credit = LyricCreditParser.parseLine("[00:00.00]作曲/编曲：张三")!!
        assertEquals(setOf(Role.COMPOSER, Role.ARRANGER), credit.roles)
        val ciQu = LyricCreditParser.parseLine("词曲：李四")!!
        assertEquals(setOf(Role.LYRICIST, Role.COMPOSER), ciQu.roles)
    }

    @Test
    fun shortKeywordsDoNotMismatch() {
        // 「制作人」不能被识别为「制作」后跟其他内容，「作曲」不能被识别为「曲」
        assertEquals(setOf(Role.PRODUCER), LyricCreditParser.parseLine("音乐制作人：A")!!.roles)
        assertEquals(setOf(Role.COMPOSER), LyricCreditParser.parseLine("作曲：A")!!.roles)
        // 非行首关键词、普通歌词、其他职位
        assertNull(LyricCreditParser.parseLine("[00:10.00]歌曲原唱：某人"))
        assertNull(LyricCreditParser.parseLine("[00:10.00]曲终人散：都已不在"))
        assertNull(LyricCreditParser.parseLine("[00:10.00]制作公司：某公司"))
        assertNull(LyricCreditParser.parseLine("[00:10.00]混音：某人"))
        assertNull(LyricCreditParser.parseLine("[00:10.00]词不达意"))
        assertNull(LyricCreditParser.parseLine("[00:10.00]Lyricsss: abc"))
        assertNull(LyricCreditParser.parseLine("[00:10.00]作词："))
    }

    @Test
    fun parseWholeLyrics_firstOccurrenceWinsAndNamesNormalized() {
        val lyrics = """
            [ti:测试]
            [00:00.00]测试 - 歌手
            [00:01.00]词：张三 / 李四
            [00:02.00]曲 Composer：王五、赵六
            [00:03.21]编曲/吉他Arranger/Guitar：谭侃侃
            [00:04.00]制作人 Producer：钱七
            [00:05.00]作词：不应覆盖
            [00:10.00]正文歌词
        """.trimIndent()
        val result = LyricCreditParser.parse(lyrics)
        assertEquals("张三/李四", result[Role.LYRICIST])
        assertEquals("王五/赵六", result[Role.COMPOSER])
        assertEquals("谭侃侃", result[Role.ARRANGER])
        assertEquals("钱七", result[Role.PRODUCER])
    }
}
