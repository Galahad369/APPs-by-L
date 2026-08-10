import SwiftUI

private enum AppRoute: Hashable {
    case player
    case settings
}

struct RootView: View {
    @ObservedObject var settings: AppSettings
    @ObservedObject var library: LibraryStore
    @ObservedObject var player: PlayerController
    @State private var path: [AppRoute] = []

    var body: some View {
        NavigationStack(path: $path) {
            LibraryView(
                settings: settings,
                library: library,
                player: player,
                openPlayer: { path.append(.player) },
                openSettings: { path.append(.settings) }
            )
            .navigationDestination(for: AppRoute.self) { route in
                switch route {
                case .player:
                    NowPlayingView(settings: settings, library: library, player: player)
                case .settings:
                    SettingsView(settings: settings, library: library, player: player)
                }
            }
        }
        .tint(.mint)
    }
}

