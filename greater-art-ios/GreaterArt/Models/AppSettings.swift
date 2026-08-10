import SwiftUI

enum AppLanguage: String, CaseIterable, Identifiable {
    case english
    case traditionalChinese
    var id: String { rawValue }
}

enum LibraryRowSize: String, CaseIterable, Identifiable {
    case small
    case medium
    case large
    var id: String { rawValue }

    var height: CGFloat {
        switch self {
        case .small: 58
        case .medium: 74
        case .large: 94
        }
    }

    var artworkSize: CGFloat { height - 12 }
}

enum AppTheme: String, CaseIterable, Identifiable {
    case system
    case light
    case dark
    var id: String { rawValue }

    var colorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}

enum MiniPlayerSize: String, CaseIterable, Identifiable {
    case compact
    case comfortable
    var id: String { rawValue }
    var height: CGFloat { self == .compact ? 54 : 68 }
}

@MainActor
final class AppSettings: ObservableObject {
    static let shared = AppSettings()

    @Published var language: AppLanguage { didSet { save(language.rawValue, for: .language) } }
    @Published var rowSize: LibraryRowSize { didSet { save(rowSize.rawValue, for: .rowSize) } }
    @Published var theme: AppTheme { didSet { save(theme.rawValue, for: .theme) } }
    @Published var miniPlayerSize: MiniPlayerSize { didSet { save(miniPlayerSize.rawValue, for: .miniPlayerSize) } }
    @Published var showThumbnails: Bool { didSet { save(showThumbnails, for: .showThumbnails) } }
    @Published var showFileDetails: Bool { didSet { save(showFileDetails, for: .showFileDetails) } }
    @Published var showFormatBadge: Bool { didSet { save(showFormatBadge, for: .showFormatBadge) } }
    @Published var preloadArtwork: Bool { didSet { save(preloadArtwork, for: .preloadArtwork) } }
    @Published var resumePlayback: Bool { didSet { save(resumePlayback, for: .resumePlayback) } }
    @Published var automaticPictureInPicture: Bool { didSet { save(automaticPictureInPicture, for: .automaticPictureInPicture) } }

    private let defaults: UserDefaults

    private enum Key: String {
        case language, rowSize, theme, miniPlayerSize, showThumbnails, showFileDetails
        case showFormatBadge, preloadArtwork, resumePlayback, automaticPictureInPicture
    }

    private init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        language = AppLanguage(rawValue: defaults.string(forKey: Key.language.rawValue) ?? "") ?? .english
        rowSize = LibraryRowSize(rawValue: defaults.string(forKey: Key.rowSize.rawValue) ?? "") ?? .small
        theme = AppTheme(rawValue: defaults.string(forKey: Key.theme.rawValue) ?? "") ?? .system
        miniPlayerSize = MiniPlayerSize(rawValue: defaults.string(forKey: Key.miniPlayerSize.rawValue) ?? "") ?? .compact
        showThumbnails = defaults.object(forKey: Key.showThumbnails.rawValue) as? Bool ?? true
        showFileDetails = defaults.object(forKey: Key.showFileDetails.rawValue) as? Bool ?? true
        showFormatBadge = defaults.object(forKey: Key.showFormatBadge.rawValue) as? Bool ?? true
        preloadArtwork = defaults.object(forKey: Key.preloadArtwork.rawValue) as? Bool ?? true
        resumePlayback = defaults.object(forKey: Key.resumePlayback.rawValue) as? Bool ?? true
        automaticPictureInPicture = defaults.object(forKey: Key.automaticPictureInPicture.rawValue) as? Bool ?? true
    }

    func resetPresentation() {
        language = .english
        rowSize = .small
        theme = .system
        miniPlayerSize = .compact
        showThumbnails = true
        showFileDetails = true
        showFormatBadge = true
        preloadArtwork = true
        resumePlayback = true
        automaticPictureInPicture = true
    }

    private func save(_ value: Any, for key: Key) {
        defaults.set(value, forKey: key.rawValue)
    }
}

