package com.local.listentomusic.graph

import org.junit.Assert.*
import org.junit.Test

class FilenameSimilarityTest {
    @Test fun everyScannedExtensionIsRemoved() {
        com.local.listentomusic.data.MediaScanner.supportedExtensions.forEach { extension ->
            assertEquals(extension, 1.0, FilenameSimilarity.score("song.$extension", "song.flac"), 0.00001)
        }
    }
    @Test fun extensionsAndCaseAreIgnored() { assertEquals(1.0, FilenameSimilarity.score("CARAVAN.MP3", "caravan.flac"), 0.00001) }
    @Test fun sharedLettersReceivePartialCredit() { assertEquals(0.053333, FilenameSimilarity.score("Caravan", "Gambling"), 0.00001) }
    @Test fun sharedTokensOutrankSharedLetters() { assertTrue(FilenameSimilarity.score("Artist Caravan Live", "Artist Caravan Studio") > FilenameSimilarity.score("Caravan", "Gambling")) }
    @Test fun cjkRunsAreNotSingleCharacterTokens() {
        val f = FilenameSimilarity.features("夜に駆ける 歌曲.mp3")
        assertEquals(setOf("夜に駆ける", "歌曲"), f.tokens)
    }
    @Test fun cjkPartialNamesConnect() { assertTrue(FilenameSimilarity.score("夜に駆ける", "夜に咲く") > 0) }
    @Test fun japaneseLongVowelsStayInsideTheWord() {
        assertEquals(setOf("アーティスト"), FilenameSimilarity.features("ｱｰﾃｨｽﾄ.mp3").tokens)
    }
    @Test fun fullWidthNormalization() { assertEquals(1.0, FilenameSimilarity.score("ＡＢＣ　音樂.mp3", "abc 音樂.wav"), 0.00001) }
    @Test fun supplementaryHanIsOneCodePoint() {
        val f = FilenameSimilarity.features("𠮷野.mp3")
        assertEquals(2, f.points.size)
        assertEquals(setOf("𠮷野"), f.tokens)
    }
    @Test fun unrelatedAndEmptyAreZero() {
        assertEquals(0.0, FilenameSimilarity.score("abc", "漢字"), 0.0)
        assertEquals(0.0, FilenameSimilarity.score("[]", "[]"), 0.0)
    }
    @Test fun internalDotsAreNotExtensions() { assertEquals("dr artist version", FilenameSimilarity.features("Dr.Artist.Version.mp3").text) }
    @Test fun punctuationDoesNotArtificiallyIncreaseScore() { assertEquals(1.0, FilenameSimilarity.score("[Artist] - Song.mp4", "Artist Song.mp3"), 0.00001) }
    @Test fun scoreIsSymmetricAndBounded() {
        val names = listOf("a", "aaa", "Caravan", "Gambling", "音樂", "音樂歌曲", "𠮷野", "")
        names.forEach { a -> names.forEach { b ->
            val score = FilenameSimilarity.score(a, b)
            assertTrue(score in 0.0..1.0)
            assertEquals(score, FilenameSimilarity.score(b, a), 0.00001)
        } }
    }
}
