package com.hwinzniej.musichelper.utils

/**
 * 从歌词中提取「作词/作曲/编曲/制作人」信息（Winnie0408/LocalMusicHelper#23）。
 *
 * 提取规则：
 * 1. 去除行内所有时间轴（`[00:00.00]`、`[00:00.000]`、`<00:00.00>`、`<00:00.000>` 等，兼容增强型逐字歌词）；
 * 2. 行首必须是关键词（按最长优先匹配，避免「曲」「词」「制作」等短关键词误匹配「作曲」「歌词」「制作人」）；
 * 3. 关键词之后向后查找第一个冒号（全角「：」或半角「:」），取冒号后的全部内容并去除首尾空格作为结果；
 * 4. 一行中可兼任多个职位，如「作曲/编曲：张三」「编曲/吉他Arranger/Guitar：谭侃侃」。
 *
 * 本类不依赖 Android API，便于在 JVM 单元测试中验证。
 */
object LyricCreditParser {

    enum class Role { LYRICIST, COMPOSER, ARRANGER, PRODUCER }

    /** 关键词表（大小写不敏感）。匹配时按长度降序尝试，保证最长优先。 */
    private val keywordTable: List<Pair<String, Role>> = listOf(
        // 作词
        "作词" to Role.LYRICIST,
        "作詞" to Role.LYRICIST,
        "填词" to Role.LYRICIST,
        "填詞" to Role.LYRICIST,
        "词" to Role.LYRICIST,
        "詞" to Role.LYRICIST,
        "Written by" to Role.LYRICIST,
        "Lyrics by" to Role.LYRICIST,
        "Lyric by" to Role.LYRICIST,
        "Lyricist" to Role.LYRICIST,
        "Lyrics" to Role.LYRICIST,
        "Lyric" to Role.LYRICIST,
        // 作曲
        "作曲" to Role.COMPOSER,
        "曲" to Role.COMPOSER,
        "Composed by" to Role.COMPOSER,
        "Music by" to Role.COMPOSER,
        "Composer" to Role.COMPOSER,
        "Compose" to Role.COMPOSER,
        // 编曲
        "编曲" to Role.ARRANGER,
        "編曲" to Role.ARRANGER,
        "Arranged by" to Role.ARRANGER,
        "Arrangement" to Role.ARRANGER,
        "Arranger" to Role.ARRANGER,
        "Arrange" to Role.ARRANGER,
        // 制作人
        "音乐制作人" to Role.PRODUCER,
        "音樂製作人" to Role.PRODUCER,
        "制作人" to Role.PRODUCER,
        "製作人" to Role.PRODUCER,
        "制作" to Role.PRODUCER,
        "製作" to Role.PRODUCER,
        "Produced by" to Role.PRODUCER,
        "Producer" to Role.PRODUCER,
    ).sortedByDescending { it.first.length }

    /** 普通 LRC 时间轴与增强型（逐字）时间轴 */
    private val timestampRegex =
        Regex("""\[\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?]|<\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?>""")

    /** 职位之间的分隔符，如「作曲/编曲」「作词 Lyricist」「词&曲」 */
    private const val ROLE_SEPARATORS = "/／\\&＆、|｜·・,，+＋-–— \t\u3000"

    /** 多个人名之间的分隔符统一替换为「/」 */
    private val nameSeparatorRegex = Regex("""\s*[/／&＆|｜,，、]\s*""")

    /** 冒号前的标签部分最长长度，超过则视为普通歌词 */
    private const val MAX_HEAD_LENGTH = 40

    data class CreditLine(val roles: Set<Role>, val value: String)

    /** 去除一行中所有的时间轴与首尾空白 */
    fun stripTimestamps(line: String): String =
        line.replace(timestampRegex, "").replace("\uFEFF", "").trim()

    /**
     * 解析单行歌词。若该行是职位信息行，返回其包含的职位与冒号后的内容；否则返回 null。
     */
    fun parseLine(rawLine: String): CreditLine? {
        val line = stripTimestamps(rawLine)
        if (line.isEmpty()) return null
        val colonIndex = line.indexOfFirst { it == '：' || it == ':' }
        if (colonIndex <= 0) return null
        val head = line.substring(0, colonIndex).trim()
        if (head.isEmpty() || head.length > MAX_HEAD_LENGTH) return null
        val value = line.substring(colonIndex + 1).trim()
        if (value.isEmpty()) return null

        val roles = linkedSetOf<Role>()
        var pos = 0
        // 行首必须是关键词
        val first = matchKeyword(head, pos) ?: return null
        roles.add(first.second)
        pos += first.first.length
        // 首个关键词后必须是结尾、分隔符、英文字母或另一个关键词，避免「曲终人散：」之类的误匹配
        if (pos < head.length) {
            val next = head[pos]
            if (!(next in ROLE_SEPARATORS || next.isAsciiLetter() || matchKeyword(head, pos) != null))
                return null
        }
        // 继续识别同一行中兼任的其他职位，遇到无法识别的内容（如「吉他」）时跳过该段
        while (pos < head.length) {
            if (head[pos] in ROLE_SEPARATORS) {
                pos++
                continue
            }
            val keyword = matchKeyword(head, pos)
            if (keyword != null) {
                roles.add(keyword.second)
                pos += keyword.first.length
            } else {
                // 跳到下一个分隔符或英文单词起点
                pos++
                while (pos < head.length && head[pos] !in ROLE_SEPARATORS &&
                    !(head[pos].isAsciiLetter() && !head[pos - 1].isAsciiLetter())
                ) pos++
            }
        }
        return CreditLine(roles, value)
    }

    /**
     * 解析整段歌词，返回每个职位第一次出现时对应的内容（多个人名之间统一以「/」分隔）。
     */
    fun parse(lyrics: String): Map<Role, String> {
        val result = linkedMapOf<Role, String>()
        lyrics.lineSequence().forEach { line ->
            val credit = parseLine(line) ?: return@forEach
            val value = normalizeNames(credit.value)
            if (value.isEmpty()) return@forEach
            credit.roles.forEach { role -> result.putIfAbsent(role, value) }
            if (result.size == Role.entries.size) return result
        }
        return result
    }

    fun normalizeNames(value: String): String =
        nameSeparatorRegex.replace(value.trim(), "/").trim('/', ' ')

    /**
     * 在 [text] 的 [start] 处按最长优先匹配关键词。英文关键词要求完整单词（后面不能紧跟英文字母）。
     */
    private fun matchKeyword(text: String, start: Int): Pair<String, Role>? {
        for (entry in keywordTable) {
            val keyword = entry.first
            if (!text.regionMatches(start, keyword, 0, keyword.length, ignoreCase = true)) continue
            val end = start + keyword.length
            if (keyword.last().isAsciiLetter()) {
                if (start > 0 && text[start - 1].isAsciiLetter()) continue
                if (end < text.length && text[end].isAsciiLetter()) continue
            }
            return entry
        }
        return null
    }

    private fun Char.isAsciiLetter() = this in 'a'..'z' || this in 'A'..'Z'
}
