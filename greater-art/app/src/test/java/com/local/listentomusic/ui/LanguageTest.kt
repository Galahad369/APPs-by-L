package com.local.listentomusic.ui

import com.local.listentomusic.data.AppLanguage
import org.junit.Assert.*
import org.junit.Test

class LanguageTest {
    @Test fun newLanguagesTranslateCoreNavigation() {
        assertEquals("設定", uiText(AppLanguage.JAPANESE, "Settings", "設定"))
        assertEquals("Einstellungen", uiText(AppLanguage.GERMAN, "Settings", "設定"))
        assertEquals("Paramètres", uiText(AppLanguage.FRENCH, "Settings", "設定"))
        assertEquals("播歌", uiText(AppLanguage.CANTONESE, "Play", "播放"))
    }
    @Test fun unknownUserTextIsPreservedAndNumbersLocalize() {
        assertEquals("My song", uiText(AppLanguage.JAPANESE, "My song", "My song"))
        assertEquals("10 分", uiText(AppLanguage.JAPANESE, "10 min", "10 分鐘"))
        assertEquals("4 titres", uiText(AppLanguage.FRENCH, "4 songs", "4 首歌曲"))
    }
    @Test fun languageLabelsRemainDistinct() {
        assertEquals(6, AppLanguage.entries.map { it.label }.toSet().size)
    }
}
