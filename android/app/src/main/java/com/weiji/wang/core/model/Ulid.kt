package com.weiji.wang.core.model

import java.security.SecureRandom

/**
 * 26 字符 ULID：前 10 位是毫秒时间戳（Crockford Base32），后 16 位随机。
 * 好处：离线可生成、按时间可排序、导入导出不变，适合作为菜谱/媒体的稳定 ID。
 */
object Ulid {
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private val random = SecureRandom()

    fun next(now: Long = System.currentTimeMillis()): String {
        val chars = CharArray(26)
        var ts = now
        for (i in 9 downTo 0) {
            chars[i] = ALPHABET[(ts and 31L).toInt()]
            ts = ts ushr 5
        }
        val bytes = ByteArray(10)
        random.nextBytes(bytes)
        var buffer = 0L
        var bits = 0
        var index = 10
        for (b in bytes) {
            buffer = (buffer shl 8) or (b.toLong() and 0xFF)
            bits += 8
            while (bits >= 5 && index < 26) {
                bits -= 5
                chars[index++] = ALPHABET[((buffer ushr bits) and 31L).toInt()]
            }
        }
        while (index < 26) chars[index++] = ALPHABET[random.nextInt(32)]
        return String(chars)
    }

    fun isValid(value: String): Boolean =
        value.length == 26 && value.all { it in ALPHABET }

    fun timestampOf(value: String): Long? {
        if (!isValid(value)) return null
        var ts = 0L
        for (i in 0 until 10) {
            val digit = ALPHABET.indexOf(value[i])
            if (digit < 0) return null
            ts = (ts shl 5) or digit.toLong()
        }
        return ts
    }
}
