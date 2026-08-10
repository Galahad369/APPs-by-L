import SwiftUI

struct NowPlayingView: View {
    @ObservedObject var settings: AppSettings
    @ObservedObject var library: LibraryStore
    @ObservedObject var player: PlayerController
    @State private var showFullScreenVideo = false
    @State private var pictureInPictureRequest = 0

    private var language: AppLanguage { settings.language }

    var body: some View {
        Group {
            if let item = player.current {
                if item.kind == .video {
                    videoLayout(item)
                } else {
                    audioLayout(item)
                }
            } else {
                VStack(spacing: 12) {
                    Image(systemName: "music.note").font(.largeTitle).foregroundStyle(.secondary)
                    Text(language.text("nothingPlaying")).foregroundStyle(.secondary)
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .fullScreenCover(isPresented: $showFullScreenVideo) {
            FullScreenVideoView(player: player) { showFullScreenVideo = false }
        }
    }

    private func videoLayout(_ item: MediaRecord) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                VideoPlaybackPanel(
                    player: player,
                    settings: settings,
                    pictureInPictureRequest: $pictureInPictureRequest,
                    showFullScreen: { showFullScreenVideo = true }
                )
                .aspectRatio(16 / 9, contentMode: .fit)
                .background(Color.black)

                VStack(alignment: .leading, spacing: 18) {
                    Text(item.title)
                        .font(.title2.weight(.bold))
                        .lineLimit(2)
                    ProgressScrubber(player: player)
                    HStack(spacing: 12) {
                        SpeedMenu(player: player, language: language)
                        RepeatButton(player: player, language: language)
                    }
                }
                .padding(18)
            }
        }
        .background(Color(uiColor: .systemBackground))
    }

    private func audioLayout(_ item: MediaRecord) -> some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 22) {
                    Spacer(minLength: 10)
                    ArtworkView(item: item, mediaURL: library.url(for: item), cornerRadius: 18)
                        .frame(
                            width: min(geometry.size.width - 58, 390),
                            height: min(geometry.size.width - 58, 390)
                        )
                        .shadow(color: .black.opacity(0.20), radius: 22, y: 12)
                    VStack(spacing: 6) {
                        Text(item.title)
                            .font(.title2.weight(.bold))
                            .multilineTextAlignment(.center)
                            .lineLimit(2)
                        Text(item.fileExtension)
                            .font(.caption.weight(.bold).monospaced())
                            .foregroundStyle(.secondary)
                    }
                    WaveformView(progress: player.progress)
                        .frame(height: 48)
                        .padding(.horizontal, 24)
                    ProgressScrubber(player: player)
                        .padding(.horizontal, 8)
                    TransportControls(player: player, large: true)
                    HStack(spacing: 12) {
                        SpeedMenu(player: player, language: language)
                        RepeatButton(player: player, language: language)
                    }
                    Spacer(minLength: 22)
                }
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 18)
            }
        }
        .background(
            LinearGradient(
                colors: [Color.mint.opacity(0.09), Color(uiColor: .systemBackground), Color(uiColor: .systemBackground)],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }
}

private struct VideoPlaybackPanel: View {
    @ObservedObject var player: PlayerController
    @ObservedObject var settings: AppSettings
    @Binding var pictureInPictureRequest: Int
    let showFullScreen: () -> Void
    @State private var controlsVisible = true

    var body: some View {
        ZStack {
            VideoPlayerSurface(
                player: player.player,
                pictureInPictureRequest: pictureInPictureRequest,
                automaticPictureInPicture: settings.automaticPictureInPicture
            )
            if controlsVisible {
                LinearGradient(
                    colors: [.black.opacity(0.54), .clear, .black.opacity(0.68)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                VStack {
                    HStack {
                        Spacer()
                        Button { pictureInPictureRequest += 1 } label: {
                            Image(systemName: "pip.enter").controlCircle()
                        }
                        Button(action: showFullScreen) {
                            Image(systemName: "arrow.up.left.and.arrow.down.right").controlCircle()
                        }
                    }
                    .padding(12)
                    Spacer()
                    TransportControls(player: player, large: false, light: true)
                    Spacer()
                    ProgressScrubber(player: player, light: true)
                        .padding(.horizontal, 14)
                        .padding(.bottom, 10)
                }
                .transition(.opacity)
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { withAnimation(.easeOut(duration: 0.16)) { controlsVisible.toggle() } }
    }
}

private struct FullScreenVideoView: View {
    @ObservedObject var player: PlayerController
    let dismiss: () -> Void
    @State private var controlsVisible = true

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VideoPlayerSurface(player: player.player, pictureInPictureRequest: 0, automaticPictureInPicture: false)
                .ignoresSafeArea()
            if controlsVisible {
                LinearGradient(colors: [.black.opacity(0.62), .clear, .black.opacity(0.66)], startPoint: .top, endPoint: .bottom)
                    .ignoresSafeArea()
                VStack {
                    HStack {
                        Button(action: dismiss) {
                            Image(systemName: "xmark").controlCircle()
                        }
                        Spacer()
                    }
                    .padding()
                    Spacer()
                    TransportControls(player: player, large: true, light: true)
                    Spacer()
                    ProgressScrubber(player: player, light: true)
                        .padding(.horizontal, 18)
                        .padding(.bottom, 14)
                }
            }
        }
        .statusBarHidden(true)
        .onTapGesture { withAnimation(.easeOut(duration: 0.16)) { controlsVisible.toggle() } }
    }
}

struct ProgressScrubber: View {
    @ObservedObject var player: PlayerController
    var light = false
    @State private var isScrubbing = false
    @State private var scrubPosition: Double = 0

    var body: some View {
        VStack(spacing: 2) {
            Slider(
                value: Binding(
                    get: { isScrubbing ? scrubPosition : min(player.position, max(player.duration, 1)) },
                    set: { scrubPosition = $0 }
                ),
                in: 0...max(player.duration, 1),
                onEditingChanged: { editing in
                    if editing {
                        scrubPosition = min(player.position, max(player.duration, 1))
                        isScrubbing = true
                    } else {
                        player.seek(to: scrubPosition)
                        isScrubbing = false
                    }
                }
            )
            .tint(.mint)
            .disabled(player.duration <= 0)
            HStack {
                Text(PlaybackMath.clock(isScrubbing ? scrubPosition : player.position))
                Spacer()
                Text(PlaybackMath.clock(player.duration))
            }
            .font(.caption.monospacedDigit())
            .foregroundStyle(light ? Color.white.opacity(0.92) : Color.secondary)
        }
    }
}

struct TransportControls: View {
    @ObservedObject var player: PlayerController
    var large: Bool
    var light = false

    var body: some View {
        HStack(spacing: large ? 42 : 32) {
            Button(action: player.previous) {
                Image(systemName: "backward.end.fill")
                    .font(.system(size: large ? 28 : 23, weight: .semibold))
            }
            .disabled(!player.canGoPrevious)
            Button(action: player.toggle) {
                Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                    .font(.system(size: large ? 30 : 25, weight: .black))
                    .frame(width: large ? 68 : 58, height: large ? 68 : 58)
                    .background(light ? Color.white : Color.primary, in: Circle())
                    .foregroundStyle(light ? Color.black : Color(uiColor: .systemBackground))
            }
            Button(action: player.next) {
                Image(systemName: "forward.end.fill")
                    .font(.system(size: large ? 28 : 23, weight: .semibold))
            }
            .disabled(!player.canGoNext)
        }
        .buttonStyle(.plain)
        .foregroundStyle(light ? Color.white : Color.primary)
    }
}

private struct SpeedMenu: View {
    @ObservedObject var player: PlayerController
    let language: AppLanguage
    private let speeds: [Float] = [0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2, 2.5, 3]

    var body: some View {
        Menu {
            ForEach(speeds, id: \.self) { value in
                Button {
                    player.speed = value
                } label: {
                    Label("\(value.formatted())×", systemImage: player.speed == value ? "checkmark" : "speedometer")
                }
            }
        } label: {
            Label("\(player.speed.formatted())×", systemImage: "speedometer")
                .playerOptionStyle()
        }
        .accessibilityLabel(language.text("speed"))
    }
}

private struct RepeatButton: View {
    @ObservedObject var player: PlayerController
    let language: AppLanguage

    var body: some View {
        Button(action: player.cycleRepeat) {
            Label(label, systemImage: player.repeatMode == .one ? "repeat.1" : "repeat")
                .playerOptionStyle()
        }
        .buttonStyle(.plain)
    }

    private var label: String {
        switch player.repeatMode {
        case .off: language.text("repeatOff")
        case .one: language.text("repeatOne")
        case .all: language.text("repeatAll")
        }
    }
}

private extension View {
    func controlCircle() -> some View {
        self.font(.system(size: 18, weight: .bold))
            .foregroundStyle(.white)
            .frame(width: 40, height: 40)
            .background(.black.opacity(0.42), in: Circle())
    }

    func playerOptionStyle() -> some View {
        self.font(.subheadline.weight(.semibold))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .padding(.horizontal, 15)
            .background(Color.secondary.opacity(0.11), in: RoundedRectangle(cornerRadius: 12))
    }
}
