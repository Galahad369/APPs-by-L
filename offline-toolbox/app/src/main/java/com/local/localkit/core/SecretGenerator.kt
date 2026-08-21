package com.local.localkit.core

import java.security.SecureRandom

object SecretGenerator {
    private val random = SecureRandom()
    private const val LOWER = "abcdefghijkmnopqrstuvwxyz"
    private const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val DIGITS = "23456789"
    private const val SYMBOLS = "!@#$%&*+-=?"
    private val words = arrayOf("amber", "anchor", "apricot", "atlas", "bamboo", "beacon", "birch", "canyon", "cedar", "comet", "coral", "cosmos", "drift", "ember", "falcon", "fern", "fjord", "granite", "harbor", "hazel", "island", "jungle", "lantern", "maple", "meadow", "meteor", "moss", "nebula", "ocean", "olive", "orchid", "pebble", "pine", "quartz", "raven", "reef", "river", "saffron", "shadow", "silver", "spruce", "stone", "sunset", "thunder", "timber", "valley", "velvet", "willow", "winter", "zephyr")

    fun password(length: Int, symbols: Boolean): String {
        val alphabet = LOWER + UPPER + DIGITS + if (symbols) SYMBOLS else ""
        return buildString { repeat(length.coerceIn(8, 128)) { append(alphabet[random.nextInt(alphabet.length)]) } }
    }

    fun passphrase(wordsCount: Int): String = List(wordsCount.coerceIn(3, 10)) { words[random.nextInt(words.size)] }.joinToString("-")
}

