import SwiftUI

struct MiniPlayerView: View {
    @ObservedObject var player: PlayerController
    @ObservedObject var library: LibraryStore
    @ObservedObject var settings: AppSettings
    let openPlayer: () -> Void

    var body: some View {
        if let item = player.current {
            VStack(spacing: 0) {
                HStack(spacing: 10) {
                    ArtworkView(item: item, mediaURL: library.url(for: item), cornerRadius: 7)
                        .frame(width: settings.miniPlayerSize.height - 10, height: settings.miniPlayerSize.height - 10)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.title)
                            .font(.subheadline.weight(.semibold))
                            .lineLimit(1)
                        if settings.miniPlayerSize == .comfortable {
                            Text("\(PlaybackMath.clock(player.position)) / \(PlaybackMath.clock(player.duration))")
                                .font(.caption.monospacedDigit())
                                .foregroundStyle(.secondary)
                        }
                    }
                    Spacer(minLength: 4)
                    Button(action: player.previous) {
                        Image(systemName: "backward.end.fill")
                    }
                    .disabled(!player.canGoPrevious)
                    Button(action: player.toggle) {
                        Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                            .font(.title3.weight(.bold))
                            .frame(width: 34, height: 34)
                    }
                    Button(action: player.next) {
                        Image(systemName: "forward.end.fill")
                    }
                    .disabled(!player.canGoNext)
                }
                .buttonStyle(.plain)
                .padding(.horizontal, 8)
                .frame(height: settings.miniPlayerSize.height)
                GeometryReader { proxy in
                    Capsule()
                        .fill(Color.mint)
                        .frame(width: proxy.size.width * player.progress, height: 2)
                }
                .frame(height: 2)
            }
            .foregroundStyle(.primary)
            .background(.regularMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 11, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 11, style: .continuous)
                    .strokeBorder(.primary.opacity(0.08), lineWidth: 1)
            }
            .contentShape(Rectangle())
            .onTapGesture(perform: openPlayer)
            .padding(.horizontal, 8)
            .padding(.bottom, 4)
        }
    }
}
