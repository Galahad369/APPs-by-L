import AVFoundation
import Foundation
import SwiftUI

@MainActor
final class LibraryStore: ObservableObject {
    static let shared = LibraryStore()

    @Published private(set) var items: [MediaRecord] = []
    @Published private(set) var playlists: [LocalPlaylist] = []
    @Published var activePlaylistID: UUID? { didSet { save() } }
    @Published var sort: LibrarySort { didSet { ensureCustomOrder(); save() } }
    @Published var query = ""
    @Published private(set) var isImporting = false
    @Published var importMessage: String?

    private var customOrder: [UUID] = []
    private let fileManager: FileManager
    let mediaDirectory: URL
    private let metadataURL: URL

    private init(fileManager: FileManager = .default) {
        self.fileManager = fileManager
        let applicationSupport = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        let root = applicationSupport.appendingPathComponent("GreaterArt", isDirectory: true)
        mediaDirectory = root.appendingPathComponent("Media", isDirectory: true)
        metadataURL = root.appendingPathComponent("library.json")
        activePlaylistID = nil
        sort = .nameAscending

        try? fileManager.createDirectory(at: mediaDirectory, withIntermediateDirectories: true)
        load()
        removeMissingRecords()
    }

    var visibleItems: [MediaRecord] {
        let available = playbackQueue

        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return available }
        return available.filter { $0.displayName.localizedCaseInsensitiveContains(trimmed) }
    }

    var playbackQueue: [MediaRecord] {
        activePlaylistID.flatMap { id in
            playlists.first(where: { $0.id == id })?.itemIDs.compactMap(itemByID)
        } ?? Self.sorted(items, by: sort, customOrder: customOrder)
    }

    var activePlaylist: LocalPlaylist? {
        activePlaylistID.flatMap { id in playlists.first(where: { $0.id == id }) }
    }

    func url(for item: MediaRecord) -> URL {
        mediaDirectory.appendingPathComponent(item.storedFilename)
    }

    func itemByID(_ id: UUID) -> MediaRecord? {
        items.first(where: { $0.id == id })
    }

    func importURLs(_ urls: [URL]) {
        guard !urls.isEmpty, !isImporting else { return }
        isImporting = true
        importMessage = nil
        let destination = mediaDirectory

        Task {
            let result = await MediaImporter().importMedia(from: urls, into: destination)
            let knownSignatures = Set(items.map(Self.signature))
            let unique = result.records.filter { !knownSignatures.contains(Self.signature($0)) }
            let duplicateFiles = result.records.filter { !unique.contains($0) }
            duplicateFiles.forEach { try? fileManager.removeItem(at: url(for: $0)) }
            items.append(contentsOf: unique)
            customOrder.append(contentsOf: unique.map(\.id))
            isImporting = false
            importMessage = result.rejectedCount > 0 ? "unsupported" : nil
            save()
            if AppSettings.shared.preloadArtwork {
                await ArtworkCache.shared.preheat(Array(visibleItems.prefix(80)), mediaDirectory: mediaDirectory)
            }
        }
    }

    func remove(_ item: MediaRecord) {
        try? fileManager.removeItem(at: url(for: item))
        items.removeAll { $0.id == item.id }
        customOrder.removeAll { $0 == item.id }
        playlists = playlists.map { playlist in
            var copy = playlist
            copy.itemIDs.removeAll { $0 == item.id }
            return copy
        }
        Task { await ArtworkCache.shared.remove(itemID: item.id) }
        save()
    }

    func moveVisibleItems(from offsets: IndexSet, to destination: Int) {
        guard query.isEmpty else { return }
        if let activeID = activePlaylistID,
           let index = playlists.firstIndex(where: { $0.id == activeID }) {
            playlists[index].itemIDs.move(fromOffsets: offsets, toOffset: destination)
        } else {
            guard sort == .custom else { return }
            customOrder.move(fromOffsets: offsets, toOffset: destination)
        }
        objectWillChange.send()
        save()
    }

    @discardableResult
    func createPlaylist(named name: String) -> UUID? {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        let playlist = LocalPlaylist(id: UUID(), name: trimmed, itemIDs: [])
        playlists.append(playlist)
        save()
        return playlist.id
    }

    func renamePlaylist(_ id: UUID, to name: String) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, let index = playlists.firstIndex(where: { $0.id == id }) else { return }
        playlists[index].name = trimmed
        save()
    }

    func deletePlaylist(_ id: UUID) {
        playlists.removeAll { $0.id == id }
        if activePlaylistID == id { activePlaylistID = nil }
        save()
    }

    func add(_ item: MediaRecord, to playlistID: UUID) {
        guard let index = playlists.firstIndex(where: { $0.id == playlistID }),
              !playlists[index].itemIDs.contains(item.id) else { return }
        playlists[index].itemIDs.append(item.id)
        save()
    }

    func remove(_ item: MediaRecord, from playlistID: UUID) {
        guard let index = playlists.firstIndex(where: { $0.id == playlistID }) else { return }
        playlists[index].itemIDs.removeAll { $0 == item.id }
        save()
    }

    static func sorted(_ items: [MediaRecord], by sort: LibrarySort, customOrder: [UUID]) -> [MediaRecord] {
        switch sort {
        case .custom:
            let rank = Dictionary(uniqueKeysWithValues: customOrder.enumerated().map { ($0.element, $0.offset) })
            return items.sorted {
                let left = rank[$0.id] ?? Int.max
                let right = rank[$1.id] ?? Int.max
                return left == right
                    ? $0.displayName.localizedCaseInsensitiveCompare($1.displayName) == .orderedAscending
                    : left < right
            }
        case .nameAscending:
            return items.sorted { $0.displayName.localizedCaseInsensitiveCompare($1.displayName) == .orderedAscending }
        case .nameDescending:
            return items.sorted { $0.displayName.localizedCaseInsensitiveCompare($1.displayName) == .orderedDescending }
        }
    }

    private func load() {
        guard let data = try? Data(contentsOf: metadataURL),
              let snapshot = try? JSONDecoder().decode(LibrarySnapshot.self, from: data) else { return }
        items = snapshot.items
        customOrder = snapshot.customOrder
        playlists = snapshot.playlists
        activePlaylistID = snapshot.activePlaylistID
        sort = snapshot.sort
        ensureCustomOrder()
    }

    private func save() {
        let snapshot = LibrarySnapshot(
            items: items,
            customOrder: customOrder,
            playlists: playlists,
            activePlaylistID: activePlaylistID,
            sort: sort
        )
        guard let data = try? JSONEncoder().encode(snapshot) else { return }
        try? data.write(to: metadataURL, options: .atomic)
    }

    private func ensureCustomOrder() {
        let current = Set(items.map(\.id))
        customOrder = customOrder.filter(current.contains)
        customOrder.append(contentsOf: items.map(\.id).filter { !customOrder.contains($0) })
    }

    private func removeMissingRecords() {
        let valid = items.filter { fileManager.fileExists(atPath: url(for: $0).path) }
        guard valid.count != items.count else { return }
        items = valid
        ensureCustomOrder()
        let validIDs = Set(valid.map(\.id))
        playlists = playlists.map { LocalPlaylist(id: $0.id, name: $0.name, itemIDs: $0.itemIDs.filter(validIDs.contains)) }
        save()
    }

    private static func signature(_ item: MediaRecord) -> String {
        "\(item.displayName.lowercased())|\(item.fileSize)"
    }
}

private actor MediaImporter {
    struct Result: Sendable {
        var records: [MediaRecord]
        var rejectedCount: Int
    }

    func importMedia(from selections: [URL], into destination: URL) async -> Result {
        let scopedSelections = selections.map { ($0, $0.startAccessingSecurityScopedResource()) }
        defer {
            scopedSelections.forEach { url, accessed in
                if accessed { url.stopAccessingSecurityScopedResource() }
            }
        }
        let sources = collectFiles(from: selections)
        var records: [MediaRecord] = []
        var rejected = 0

        for source in sources {
            guard MediaSupport.recognizes(source) else { rejected += 1; continue }
            let accessed = source.startAccessingSecurityScopedResource()
            defer { if accessed { source.stopAccessingSecurityScopedResource() } }

            let id = UUID()
            let ext = source.pathExtension.lowercased()
            let storedFilename = ext.isEmpty ? id.uuidString : "\(id.uuidString).\(ext)"
            let target = destination.appendingPathComponent(storedFilename)
            do {
                try FileManager.default.copyItem(at: source, to: target)
                let asset = AVURLAsset(url: target)
                let playable = (try? await asset.load(.isPlayable)) ?? false
                guard playable else {
                    try? FileManager.default.removeItem(at: target)
                    rejected += 1
                    continue
                }
                let loadedDuration = try? await asset.load(.duration)
                let duration = loadedDuration?.seconds.isFinite == true ? (loadedDuration?.seconds ?? 0) : 0
                let tracks = (try? await asset.load(.tracks)) ?? []
                let kind: MediaKind = tracks.contains(where: { $0.mediaType == .video }) ? .video : MediaSupport.kind(for: source)
                let size = Int64((try? target.resourceValues(forKeys: [.fileSizeKey]))?.fileSize ?? 0)
                records.append(MediaRecord(
                    id: id,
                    storedFilename: storedFilename,
                    displayName: source.lastPathComponent,
                    fileSize: size,
                    duration: duration,
                    kind: kind,
                    importedAt: Date()
                ))
            } catch {
                try? FileManager.default.removeItem(at: target)
                rejected += 1
            }
        }
        return Result(records: records, rejectedCount: rejected)
    }

    private func collectFiles(from selections: [URL]) -> [URL] {
        selections.flatMap { selection in
            let values = try? selection.resourceValues(forKeys: [.isDirectoryKey])
            guard values?.isDirectory == true else { return [selection] }
            let keys: [URLResourceKey] = [.isRegularFileKey, .isHiddenKey]
            let enumerator = FileManager.default.enumerator(
                at: selection,
                includingPropertiesForKeys: keys,
                options: [.skipsHiddenFiles, .skipsPackageDescendants]
            )
            return (enumerator?.allObjects as? [URL] ?? []).filter { url in
                let values = try? url.resourceValues(forKeys: Set(keys))
                return values?.isRegularFile == true && MediaSupport.recognizes(url)
            }
        }
    }
}
