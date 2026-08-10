package com.local.listentomusic.ui

import com.local.listentomusic.data.AppLanguage

internal fun uiText(language: AppLanguage, english: String, traditionalChinese: String): String =
    if (language == AppLanguage.TRADITIONAL_CHINESE) traditionalChinese else english
