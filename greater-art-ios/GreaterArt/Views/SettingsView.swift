import SwiftUI

struct SettingsView: View {
    @ObservedObject var settings: AppSettings
    @ObservedObject var library: LibraryStore
    @ObservedObject var player: PlayerController

    @State private var showPlaylistEditor = false
    @State private var editingPlaylist: LocalPlaylist?
    @State private var playlistName = ""
    @State private var cacheCleared = false

    private var language: AppLanguage { settings.language }

    var body: some View {
        Form {
            appearanceSection
            playbackSection
            playlistsSection
            cacheSection
            privacySection
        }
        .navigationTitle(language.text("settings"))
        .navigationBarTitleDisplayMode(.inline)
        .alert(editingPlaylist == nil ? language.text("newPlaylist") : language.text("rename"), isPresented: $showPlaylistEditor) {
            TextField(language.text("playlistName"), text: $playlistName)
            Button(language.text("save")) {
                if let editingPlaylist {
                    library.renamePlaylist(editingPlaylist.id, to: playlistName)
                } else {
                    _ = library.createPlaylist(named: playlistName)
                }
                self.editingPlaylist = nil
                playlistName = ""
            }
            Button(language.text("cancel"), role: .cancel) {
                editingPlaylist = nil
                playlistName = ""
            }
        }
    }

    private var appearanceSection: some View {
        Section(language.text("appearance")) {
            Picker(language.text("language"), selection: $settings.language) {
                Text(language.text("english")).tag(AppLanguage.english)
                Text(language.text("traditionalChinese")).tag(AppLanguage.traditionalChinese)
            }
            Picker(language.text("theme"), selection: $settings.theme) {
                Text(language.text("system")).tag(AppTheme.system)
                Text(language.text("light")).tag(AppTheme.light)
                Text(language.text("dark")).tag(AppTheme.dark)
            }
            Picker(language.text("rowSize"), selection: $settings.rowSize) {
                Text(language.text("small")).tag(LibraryRowSize.small)
                Text(language.text("medium")).tag(LibraryRowSize.medium)
                Text(language.text("large")).tag(LibraryRowSize.large)
            }
            Toggle(language.text("showThumbnails"), isOn: $settings.showThumbnails)
            Toggle(language.text("showDetails"), isOn: $settings.showFileDetails)
            Toggle(language.text("showBadge"), isOn: $settings.showFormatBadge)
            Picker(language.text("miniPlayer"), selection: $settings.miniPlayerSize) {
                Text(language.text("compact")).tag(MiniPlayerSize.compact)
                Text(language.text("comfortable")).tag(MiniPlayerSize.comfortable)
            }
        }
    }

    private var playbackSection: some View {
        Section(language.text("playback")) {
            Picker(language.text("speed"), selection: $player.speed) {
                ForEach([Float(0.25), 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2, 2.5, 3], id: \.self) {
                    Text("\($0.formatted())×").tag($0)
                }
            }
            Picker(language.text("repeatOne"), selection: $player.repeatMode) {
                Text(language.text("repeatOff")).tag(RepeatMode.off)
                Text(language.text("repeatOne")).tag(RepeatMode.one)
                Text(language.text("repeatAll")).tag(RepeatMode.all)
            }
            Toggle(language.text("resume"), isOn: $settings.resumePlayback)
            Toggle(language.text("automaticPiP"), isOn: $settings.automaticPictureInPicture)
        }
    }

    private var playlistsSection: some View {
        Section {
            ForEach(library.playlists) { playlist in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(playlist.name).fontWeight(.semibold)
                        Text("\(playlist.itemIDs.count)")
                            .font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer()
                    Button {
                        editingPlaylist = playlist
                        playlistName = playlist.name
                        showPlaylistEditor = true
                    } label: {
                        Image(systemName: "pencil")
                    }
                    .buttonStyle(.borderless)
                }
                .swipeActions {
                    Button(role: .destructive) { library.deletePlaylist(playlist.id) } label: {
                        Label(language.text("delete"), systemImage: "trash")
                    }
                }
            }
            Button {
                editingPlaylist = nil
                playlistName = ""
                showPlaylistEditor = true
            } label: {
                Label(language.text("newPlaylist"), systemImage: "plus")
            }
        } header: {
            Text(language.text("playlists"))
        }
    }

    private var cacheSection: some View {
        Section(language.text("libraryCache")) {
            Toggle(language.text("preloadArtwork"), isOn: $settings.preloadArtwork)
                .onChange(of: settings.preloadArtwork) { enabled in
                    guard enabled else { return }
                    Task {
                        await ArtworkCache.shared.preheat(
                            Array(library.visibleItems.prefix(80)),
                            mediaDirectory: library.mediaDirectory
                        )
                    }
                }
            Button {
                Task {
                    await ArtworkCache.shared.clear()
                    cacheCleared = true
                }
            } label: {
                Label(language.text("clearCache"), systemImage: cacheCleared ? "checkmark" : "trash")
            }
        }
    }

    private var privacySection: some View {
        Section {
            VStack(alignment: .leading, spacing: 8) {
                Label(language.text("privacy"), systemImage: "hand.raised.fill")
                    .font(.headline)
                Text(language.text("privacyBody"))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .padding(.vertical, 6)
            Button(role: .destructive) {
                settings.resetPresentation()
                player.speed = 1
                player.repeatMode = .one
            } label: {
                Label(language.text("resetSettings"), systemImage: "arrow.counterclockwise")
            }
        }
    }
}
