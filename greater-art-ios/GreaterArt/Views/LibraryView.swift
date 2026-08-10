import SwiftUI
import UniformTypeIdentifiers

struct LibraryView: View {
    @ObservedObject var settings: AppSettings
    @ObservedObject var library: LibraryStore
    @ObservedObject var player: PlayerController
    let openPlayer: () -> Void
    let openSettings: () -> Void

    @State private var showImporter = false
    @State private var showCreatePlaylist = false
    @State private var playlistName = ""
    @State private var editMode: EditMode = .inactive
    @State private var pendingRemoval: MediaRecord?

    private var language: AppLanguage { settings.language }
    private var canReorder: Bool {
        library.query.isEmpty && (library.activePlaylistID != nil || library.sort == .custom)
    }

    var body: some View {
        VStack(spacing: 0) {
            libraryControls
            if library.items.isEmpty && !library.isImporting {
                emptyState
            } else {
                mediaList
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { toolbarContent }
        .searchable(text: $library.query, prompt: language.text("search"))
        .safeAreaInset(edge: .bottom, spacing: 0) {
            MiniPlayerView(player: player, library: library, settings: settings, openPlayer: openPlayer)
        }
        .environment(\.editMode, $editMode)
        .fileImporter(
            isPresented: $showImporter,
            allowedContentTypes: [.audio, .movie, .folder],
            allowsMultipleSelection: true
        ) { result in
            switch result {
            case .success(let urls): library.importURLs(urls)
            case .failure: library.importMessage = "failedImport"
            }
        }
        .alert(language.text("newPlaylist"), isPresented: $showCreatePlaylist) {
            TextField(language.text("playlistName"), text: $playlistName)
            Button(language.text("save")) {
                if let id = library.createPlaylist(named: playlistName) { library.activePlaylistID = id }
                playlistName = ""
            }
            Button(language.text("cancel"), role: .cancel) { playlistName = "" }
        }
        .alert(language.text("confirmRemove"), isPresented: Binding(
            get: { pendingRemoval != nil },
            set: { if !$0 { pendingRemoval = nil } }
        )) {
            Button(language.text("delete"), role: .destructive) {
                if let pendingRemoval { library.remove(pendingRemoval) }
                pendingRemoval = nil
            }
            Button(language.text("cancel"), role: .cancel) { pendingRemoval = nil }
        } message: {
            Text(language.text("mediaUntouched"))
        }
        .alert(language.text(library.importMessage ?? "failedImport"), isPresented: Binding(
            get: { library.importMessage != nil },
            set: { if !$0 { library.importMessage = nil } }
        )) {
            Button("OK") { library.importMessage = nil }
        }
        .overlay(alignment: .top) {
            if library.isImporting {
                Label(language.text("importing"), systemImage: "square.and.arrow.down")
                    .font(.subheadline.weight(.semibold))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 9)
                    .background(.ultraThinMaterial, in: Capsule())
                    .padding(.top, 8)
            }
        }
    }

    private var libraryControls: some View {
        HStack(spacing: 10) {
            Menu {
                Button {
                    library.activePlaylistID = nil
                } label: {
                    Label(language.text("allSongs"), systemImage: library.activePlaylistID == nil ? "checkmark" : "music.note.list")
                }
                ForEach(library.playlists) { playlist in
                    Button {
                        library.activePlaylistID = playlist.id
                    } label: {
                        Label(playlist.name, systemImage: library.activePlaylistID == playlist.id ? "checkmark" : "music.note.list")
                    }
                }
                Divider()
                Button { showCreatePlaylist = true } label: {
                    Label(language.text("newPlaylist"), systemImage: "plus")
                }
            } label: {
                Label(library.activePlaylist?.name ?? language.text("allSongs"), systemImage: "music.note.list")
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 12)
                    .frame(height: 38)
                    .background(Color.secondary.opacity(0.10), in: RoundedRectangle(cornerRadius: 9))
            }
            if library.activePlaylistID == nil {
                Menu {
                    sortButton(.custom, key: "customOrder")
                    sortButton(.nameAscending, key: "nameAZ")
                    sortButton(.nameDescending, key: "nameZA")
                } label: {
                    Image(systemName: "arrow.up.arrow.down")
                        .frame(width: 38, height: 38)
                        .background(Color.secondary.opacity(0.10), in: RoundedRectangle(cornerRadius: 9))
                }
            }
            if canReorder {
                Button {
                    withAnimation { editMode = editMode == .active ? .inactive : .active }
                } label: {
                    Text(language.text(editMode == .active ? "done" : "editOrder"))
                        .font(.subheadline.weight(.semibold))
                }
            }
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
    }

    private var mediaList: some View {
        List {
            ForEach(library.visibleItems) { item in
                MediaRow(item: item, settings: settings, library: library)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        player.play(item, in: library.playbackQueue)
                        openPlayer()
                    }
                    .contextMenu { rowMenu(item) }
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        Button(role: .destructive) { pendingRemoval = item } label: {
                            Label(language.text("delete"), systemImage: "trash")
                        }
                    }
                    .listRowInsets(EdgeInsets(top: 2, leading: 14, bottom: 2, trailing: 14))
                    .listRowSeparator(.hidden)
            }
            .onMove { offsets, destination in
                guard canReorder else { return }
                library.moveVisibleItems(from: offsets, to: destination)
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .overlay {
            if library.visibleItems.isEmpty && !library.query.isEmpty {
                ContentUnavailableViewCompat(systemImage: "magnifyingglass", title: language.text("search"))
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 14) {
            Spacer()
            Image(systemName: "music.note.house")
                .font(.system(size: 48, weight: .light))
                .foregroundStyle(.mint)
            Text(language.text("emptyTitle"))
                .font(.title3.weight(.bold))
            Text(language.text("emptyBody"))
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 32)
            Button { showImporter = true } label: {
                Label(language.text("importMedia"), systemImage: "square.and.arrow.down")
                    .fontWeight(.semibold)
            }
            .buttonStyle(.borderedProminent)
            Spacer()
        }
    }

    @ToolbarContentBuilder private var toolbarContent: some ToolbarContent {
        ToolbarItem(placement: .principal) {
            HStack(spacing: 9) {
                Image(systemName: "music.note")
                    .font(.headline.weight(.black))
                    .frame(width: 31, height: 31)
                    .background(Color.primary, in: RoundedRectangle(cornerRadius: 8))
                    .foregroundStyle(Color(uiColor: .systemBackground))
                VStack(alignment: .leading, spacing: 0) {
                    Text("Greater Art").font(.headline.weight(.black))
                    Text("\(library.items.count) · \(language.text("offline"))")
                        .font(.caption2).foregroundStyle(.secondary)
                }
            }
        }
        ToolbarItemGroup(placement: .navigationBarTrailing) {
            Button { showImporter = true } label: { Image(systemName: "plus") }
            Button(action: openSettings) { Image(systemName: "gearshape") }
        }
    }

    @ViewBuilder private func rowMenu(_ item: MediaRecord) -> some View {
        if !library.playlists.isEmpty {
            Menu(language.text("addToPlaylist")) {
                ForEach(library.playlists) { playlist in
                    Button(playlist.name) { library.add(item, to: playlist.id) }
                }
            }
        }
        if let activeID = library.activePlaylistID {
            Button(language.text("removeFromPlaylist"), role: .destructive) {
                library.remove(item, from: activeID)
            }
        }
        Button(language.text("removeDownload"), role: .destructive) { pendingRemoval = item }
    }

    @ViewBuilder private func sortButton(_ value: LibrarySort, key: String) -> some View {
        Button {
            library.sort = value
            if value != .custom { editMode = .inactive }
        } label: {
            Label(language.text(key), systemImage: library.sort == value ? "checkmark" : "line.3.horizontal")
        }
    }
}

private struct MediaRow: View {
    let item: MediaRecord
    @ObservedObject var settings: AppSettings
    @ObservedObject var library: LibraryStore

    var body: some View {
        HStack(spacing: 11) {
            if settings.showThumbnails {
                ArtworkView(item: item, mediaURL: library.url(for: item), cornerRadius: 8)
                    .frame(width: settings.rowSize.artworkSize, height: settings.rowSize.artworkSize)
            }
            VStack(alignment: .leading, spacing: 3) {
                Text(item.title)
                    .font(settings.rowSize == .large ? .headline : .subheadline)
                    .fontWeight(.semibold)
                    .lineLimit(settings.rowSize == .large ? 2 : 1)
                if settings.showFileDetails {
                    Text("\(PlaybackMath.clock(item.duration))  ·  \(ByteCountFormatter.string(fromByteCount: item.fileSize, countStyle: .file))")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 5)
            if settings.showFormatBadge {
                Text(item.fileExtension)
                    .font(.caption2.weight(.bold).monospaced())
                    .padding(.horizontal, 6)
                    .padding(.vertical, 3)
                    .background(Color.secondary.opacity(0.12), in: RoundedRectangle(cornerRadius: 5))
            }
            Image(systemName: "chevron.right")
                .font(.caption.weight(.bold))
                .foregroundStyle(.tertiary)
        }
        .frame(minHeight: settings.rowSize.height)
    }
}

private struct ContentUnavailableViewCompat: View {
    let systemImage: String
    let title: String
    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: systemImage).font(.largeTitle).foregroundStyle(.secondary)
            Text(title).font(.headline).foregroundStyle(.secondary)
        }
    }
}
