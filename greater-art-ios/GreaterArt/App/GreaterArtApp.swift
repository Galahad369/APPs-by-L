import SwiftUI

@main
struct GreaterArtApp: App {
    @StateObject private var settings = AppSettings.shared
    @StateObject private var library = LibraryStore.shared
    @StateObject private var player = PlayerController.shared
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            RootView(settings: settings, library: library, player: player)
                .preferredColorScheme(settings.theme.colorScheme)
                .task {
                    player.restoreIfAvailable(from: library, enabled: settings.resumePlayback)
                    if settings.preloadArtwork {
                        await ArtworkCache.shared.preheat(
                            Array(library.visibleItems.prefix(80)),
                            mediaDirectory: library.mediaDirectory
                        )
                    }
                }
        }
        .onChange(of: scenePhase) { phase in
            if phase != .active { player.persistState() }
        }
    }
}

