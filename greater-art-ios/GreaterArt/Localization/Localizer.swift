import Foundation

enum L10n {
    private static let traditionalChinese: [String: String] = [
        "library": "音樂庫", "offline": "離線", "import": "匯入", "settings": "設定",
        "allSongs": "所有歌曲", "customOrder": "自訂排序", "nameAZ": "名稱 A–Z",
        "nameZA": "名稱 Z–A", "search": "搜尋音樂庫", "emptyTitle": "你的音樂庫是空的",
        "emptyBody": "從「檔案」選擇音訊、影片或資料夾。檔案會安全地複製到 Greater Art。",
        "importMedia": "匯入媒體", "unsupported": "部分檔案不受此 iPhone 支援。",
        "previous": "上一首", "next": "下一首", "play": "播放", "pause": "暫停",
        "repeatOff": "不循環", "repeatOne": "單曲循環", "repeatAll": "全部循環",
        "speed": "速度", "fullScreen": "全螢幕", "pictureInPicture": "子母畫面",
        "appearance": "外觀", "language": "語言", "english": "English",
        "traditionalChinese": "繁體中文", "theme": "主題", "system": "系統",
        "light": "淺色", "dark": "深色", "rowSize": "音樂庫列大小",
        "small": "小", "medium": "中", "large": "大", "showThumbnails": "顯示縮圖",
        "showDetails": "顯示檔案詳情", "showBadge": "顯示格式標籤",
        "miniPlayer": "迷你播放器大小", "compact": "精簡", "comfortable": "舒適",
        "playback": "播放", "resume": "接續上次位置", "automaticPiP": "自動子母畫面",
        "preloadArtwork": "預先載入縮圖", "libraryCache": "音樂庫與快取",
        "clearCache": "清除縮圖快取", "playlists": "播放清單", "newPlaylist": "新增播放清單",
        "playlistName": "播放清單名稱", "save": "儲存", "cancel": "取消", "delete": "刪除",
        "rename": "重新命名", "addToPlaylist": "加入播放清單", "removeFromPlaylist": "從清單移除",
        "editOrder": "編輯排序", "done": "完成", "privacy": "私隱",
        "privacyBody": "沒有廣告、分析、帳戶或網絡功能。媒體、播放清單與設定只留在此裝置。",
        "resetSettings": "重設應用程式設定", "nothingPlaying": "尚未播放",
        "importing": "正在匯入…", "failedImport": "無法匯入所選媒體。",
        "removeDownload": "從 Greater Art 移除", "confirmRemove": "移除此媒體？",
        "mediaUntouched": "只會刪除 Greater Art 的本機副本。原始檔案不會改變。"
    ]

    static func text(_ key: String, language: AppLanguage) -> String {
        guard language == .traditionalChinese else { return english[key] ?? key }
        return traditionalChinese[key] ?? english[key] ?? key
    }

    private static let english: [String: String] = [
        "library": "Library", "offline": "offline", "import": "Import", "settings": "Settings",
        "allSongs": "All songs", "customOrder": "Custom order", "nameAZ": "Name A–Z",
        "nameZA": "Name Z–A", "search": "Filter library", "emptyTitle": "Your library is empty",
        "emptyBody": "Choose audio, video, or folders from Files. They are copied safely into Greater Art.",
        "importMedia": "Import media", "unsupported": "Some files are not supported by this iPhone.",
        "previous": "Previous", "next": "Next", "play": "Play", "pause": "Pause",
        "repeatOff": "Repeat off", "repeatOne": "Repeat one", "repeatAll": "Repeat all",
        "speed": "Speed", "fullScreen": "Full screen", "pictureInPicture": "Picture in Picture",
        "appearance": "Appearance", "language": "Language", "english": "English",
        "traditionalChinese": "繁體中文", "theme": "Theme", "system": "System",
        "light": "Light", "dark": "Dark", "rowSize": "Library row size",
        "small": "Small", "medium": "Medium", "large": "Large", "showThumbnails": "Show thumbnails",
        "showDetails": "Show file details", "showBadge": "Show format badge",
        "miniPlayer": "Mini-player size", "compact": "Compact", "comfortable": "Comfortable",
        "playback": "Playback", "resume": "Resume last position", "automaticPiP": "Automatic Picture in Picture",
        "preloadArtwork": "Preload artwork", "libraryCache": "Library & cache",
        "clearCache": "Clear artwork cache", "playlists": "Song lists", "newPlaylist": "New playlist",
        "playlistName": "Playlist name", "save": "Save", "cancel": "Cancel", "delete": "Delete",
        "rename": "Rename", "addToPlaylist": "Add to playlist", "removeFromPlaylist": "Remove from list",
        "editOrder": "Edit order", "done": "Done", "privacy": "Privacy",
        "privacyBody": "No ads, analytics, accounts, or network features. Media, lists, and settings stay on this device.",
        "resetSettings": "Reset app settings", "nothingPlaying": "Nothing playing",
        "importing": "Importing…", "failedImport": "The selected media could not be imported.",
        "removeDownload": "Remove from Greater Art", "confirmRemove": "Remove this media?",
        "mediaUntouched": "Only Greater Art’s local copy is deleted. The original file is unchanged."
    ]
}

extension AppLanguage {
    func text(_ key: String) -> String { L10n.text(key, language: self) }
}
