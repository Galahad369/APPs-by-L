import Foundation

struct LocalPlaylist: Identifiable, Codable, Hashable, Sendable {
    let id: UUID
    var name: String
    var itemIDs: [UUID]
}

struct LibrarySnapshot: Codable, Sendable {
    var items: [MediaRecord] = []
    var customOrder: [UUID] = []
    var playlists: [LocalPlaylist] = []
    var activePlaylistID: UUID?
    var sort: LibrarySort = .nameAscending
}

