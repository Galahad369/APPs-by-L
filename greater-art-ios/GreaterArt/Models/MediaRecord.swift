import Foundation

enum MediaKind: String, Codable, Sendable {
    case audio
    case video
}

struct MediaRecord: Identifiable, Codable, Hashable, Sendable {
    let id: UUID
    var storedFilename: String
    var displayName: String
    var fileSize: Int64
    var duration: TimeInterval
    var kind: MediaKind
    var importedAt: Date

    var fileExtension: String {
        URL(fileURLWithPath: displayName).pathExtension.uppercased()
    }

    var title: String {
        URL(fileURLWithPath: displayName).deletingPathExtension().lastPathComponent
    }
}

enum LibrarySort: String, CaseIterable, Codable, Identifiable, Sendable {
    case custom
    case nameAscending
    case nameDescending

    var id: String { rawValue }
}

enum MediaSupport {
    // iOS does not expose a global Download-folder scan. Users explicitly import
    // files or folders, which are copied into the app's private offline library.
    static let extensions: Set<String> = [
        "mp4", "mov", "m4v", "mp3", "m4a", "aac", "wav", "wave",
        "aif", "aiff", "caf", "flac", "opus", "ogg"
    ]

    static let videoExtensions: Set<String> = ["mp4", "mov", "m4v"]

    static func recognizes(_ url: URL) -> Bool {
        extensions.contains(url.pathExtension.lowercased())
    }

    static func kind(for url: URL) -> MediaKind {
        videoExtensions.contains(url.pathExtension.lowercased()) ? .video : .audio
    }
}
