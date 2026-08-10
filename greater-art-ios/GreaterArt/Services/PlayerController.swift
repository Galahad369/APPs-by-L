import AVFoundation
import Combine
import MediaPlayer
import UIKit

enum RepeatMode: String, CaseIterable, Identifiable {
    case off
    case one
    case all
    var id: String { rawValue }
}

@MainActor
final class PlayerController: ObservableObject {
    static let shared = PlayerController()

    let player = AVQueuePlayer()

    @Published private(set) var current: MediaRecord?
    @Published private(set) var isPlaying = false
    @Published private(set) var position: TimeInterval = 0
    @Published private(set) var duration: TimeInterval = 0
    @Published private(set) var canGoPrevious = false
    @Published private(set) var canGoNext = false
    @Published var playbackError: String?
    @Published var speed: Float {
        didSet {
            defaults.set(speed, forKey: Key.speed)
            if isPlaying { player.playImmediately(atRate: speed) }
            updateNowPlayingInfo()
        }
    }
    @Published var repeatMode: RepeatMode {
        didSet {
            defaults.set(repeatMode.rawValue, forKey: Key.repeatMode)
            configureEndBehavior()
        }
    }

    var progress: Double { PlaybackMath.progress(position: position, duration: duration) }

    private var queue: [MediaRecord] = []
    private var index = 0
    private var itemIDs: [ObjectIdentifier: UUID] = [:]
    private var assets: [UUID: AVURLAsset] = [:]
    private var timeObserver: Any?
    private var currentItemObservation: NSKeyValueObservation?
    private var timeControlObservation: NSKeyValueObservation?
    private var endObserver: NSObjectProtocol?
    private var failureObserver: NSObjectProtocol?
    private var interruptionObserver: NSObjectProtocol?
    private var mediaResetObserver: NSObjectProtocol?
    private var nowPlayingArtwork: UIImage?
    private let defaults = UserDefaults.standard

    private enum Key {
        static let speed = "player.speed"
        static let repeatMode = "player.repeatMode"
        static let lastItemID = "player.lastItemID"
        static let lastPosition = "player.lastPosition"
    }

    private init() {
        let savedSpeed = defaults.float(forKey: Key.speed)
        speed = savedSpeed == 0 ? 1 : savedSpeed
        repeatMode = RepeatMode(rawValue: defaults.string(forKey: Key.repeatMode) ?? "") ?? .one
        player.automaticallyWaitsToMinimizeStalling = false
        configureAudioSession()
        configureObservers()
        configureRemoteCommands()
        configureEndBehavior()
    }

    deinit {
        if let timeObserver { player.removeTimeObserver(timeObserver) }
        if let endObserver { NotificationCenter.default.removeObserver(endObserver) }
        if let failureObserver { NotificationCenter.default.removeObserver(failureObserver) }
        if let interruptionObserver { NotificationCenter.default.removeObserver(interruptionObserver) }
        if let mediaResetObserver { NotificationCenter.default.removeObserver(mediaResetObserver) }
    }

    func play(_ item: MediaRecord, in records: [MediaRecord]) {
        let newQueue = records.isEmpty ? [item] : records
        let newIndex = newQueue.firstIndex(where: { $0.id == item.id }) ?? 0
        rebuildQueue(records: newQueue, startingAt: newIndex, position: 0, autoplay: true)
    }

    func restoreIfAvailable(from library: LibraryStore, enabled: Bool) {
        guard current == nil, enabled,
              let rawID = defaults.string(forKey: Key.lastItemID),
              let id = UUID(uuidString: rawID),
              let item = library.itemByID(id) else { return }
        let records = library.visibleItems
        let restoredIndex = records.firstIndex(where: { $0.id == id }) ?? 0
        let savedPosition = defaults.double(forKey: Key.lastPosition)
        rebuildQueue(records: records, startingAt: restoredIndex, position: savedPosition, autoplay: false)
    }

    func toggle() {
        guard current != nil else { return }
        if isPlaying {
            player.pause()
        } else {
            if player.currentItem == nil { rebuildCurrent(position: position, autoplay: true) }
            else { player.playImmediately(atRate: speed) }
        }
    }

    func pause() { player.pause() }

    func next() {
        guard !queue.isEmpty else { return }
        if index + 1 < queue.count {
            if repeatMode != .one, player.items().count > 1 {
                player.advanceToNextItem()
            } else {
                index += 1
                rebuildCurrent(position: 0, autoplay: isPlaying)
            }
        } else if repeatMode == .all {
            index = 0
            rebuildCurrent(position: 0, autoplay: isPlaying)
        }
    }

    func previous() {
        guard !queue.isEmpty else { return }
        if position > 4 {
            seek(to: 0)
        } else if index > 0 {
            index -= 1
            rebuildCurrent(position: 0, autoplay: isPlaying)
        } else if repeatMode == .all {
            index = queue.count - 1
            rebuildCurrent(position: 0, autoplay: isPlaying)
        } else {
            seek(to: 0)
        }
    }

    func seek(to seconds: TimeInterval) {
        guard seconds.isFinite else { return }
        let upper = duration > 0 ? duration : seconds
        let safe = min(max(seconds, 0), upper)
        player.seek(
            to: CMTime(seconds: safe, preferredTimescale: 600),
            toleranceBefore: .zero,
            toleranceAfter: .zero
        )
        position = safe
        savePlaybackPosition()
        updateNowPlayingInfo()
    }

    func cycleRepeat() {
        switch repeatMode {
        case .off: repeatMode = .one
        case .one: repeatMode = .all
        case .all: repeatMode = .off
        }
    }

    func persistState() {
        savePlaybackPosition()
    }

    func prewarm(_ records: [MediaRecord]) {
        for record in records {
            let asset = assets[record.id] ?? AVURLAsset(url: LibraryStore.shared.url(for: record))
            assets[record.id] = asset
            Task.detached(priority: .utility) {
                _ = try? await asset.load(.isPlayable)
                _ = try? await asset.load(.duration)
            }
        }
    }

    private func rebuildQueue(
        records: [MediaRecord],
        startingAt start: Int,
        position: TimeInterval,
        autoplay: Bool
    ) {
        guard records.indices.contains(start) else { return }
        queue = records
        index = start
        rebuildCurrent(position: position, autoplay: autoplay)
    }

    private func rebuildCurrent(position: TimeInterval, autoplay: Bool) {
        guard queue.indices.contains(index) else { return }
        let record = queue[index]
        let wasPlaying = autoplay
        player.pause()
        player.removeAllItems()
        itemIDs.removeAll()
        let queueEnd = min(index + 2, queue.count)
        let recordsToQueue = repeatMode == .one ? [record] : Array(queue[index..<queueEnd])
        for queuedRecord in recordsToQueue {
            player.insert(makePlayerItem(queuedRecord), after: nil)
        }
        current = record
        self.position = max(0, position)
        duration = record.duration.isFinite ? max(0, record.duration) : 0
        canGoPrevious = index > 0 || self.position > 0 || repeatMode == .all
        canGoNext = index + 1 < queue.count || repeatMode == .all
        playbackError = nil
        nowPlayingArtwork = nil
        if position > 0 { seek(to: position) }
        if wasPlaying { player.playImmediately(atRate: speed) }
        savePlaybackPosition()
        updateNowPlayingInfo()
        loadNowPlayingArtwork(record)
        prewarmNextAsset()
    }

    private func makePlayerItem(_ record: MediaRecord) -> AVPlayerItem {
        let url = LibraryStore.shared.url(for: record)
        let asset = assets[record.id] ?? AVURLAsset(url: url)
        assets[record.id] = asset
        let item = AVPlayerItem(asset: asset)
        item.preferredForwardBufferDuration = 2
        itemIDs[ObjectIdentifier(item)] = record.id
        return item
    }

    private func configureAudioSession() {
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playback, mode: .default, options: [])
            try session.setActive(true)
        } catch {
            playbackError = error.localizedDescription
        }
    }

    private func configureObservers() {
        timeObserver = player.addPeriodicTimeObserver(
            forInterval: CMTime(seconds: 0.25, preferredTimescale: 600),
            queue: .main
        ) { [weak self] time in
            Task { @MainActor in
                guard let self else { return }
                let seconds = time.seconds
                self.position = seconds.isFinite ? max(0, seconds) : 0
                let itemDuration = self.player.currentItem?.duration.seconds ?? 0
                if itemDuration.isFinite, itemDuration > 0 { self.duration = itemDuration }
                self.canGoPrevious = self.index > 0 || self.position > 0 || self.repeatMode == .all
                self.savePlaybackPosition()
                self.updateNowPlayingInfo()
            }
        }

        timeControlObservation = player.observe(\.timeControlStatus, options: [.initial, .new]) { [weak self] player, _ in
            Task { @MainActor in
                self?.isPlaying = player.timeControlStatus == .playing
                self?.updateNowPlayingInfo()
            }
        }

        currentItemObservation = player.observe(\.currentItem, options: [.new]) { [weak self] player, _ in
            Task { @MainActor in self?.syncCurrentItem(player.currentItem) }
        }

        endObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: nil,
            queue: .main
        ) { [weak self] note in
            Task { @MainActor in self?.handleEnded(note.object as? AVPlayerItem) }
        }

        failureObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemFailedToPlayToEndTime,
            object: nil,
            queue: .main
        ) { [weak self] note in
            Task { @MainActor in
                self?.handleFailure(
                    note.object as? AVPlayerItem,
                    error: note.userInfo?[AVPlayerItemFailedToPlayToEndTimeErrorKey] as? Error
                )
            }
        }

        interruptionObserver = NotificationCenter.default.addObserver(
            forName: AVAudioSession.interruptionNotification,
            object: AVAudioSession.sharedInstance(),
            queue: .main
        ) { [weak self] note in
            Task { @MainActor in self?.handleInterruption(note) }
        }

        mediaResetObserver = NotificationCenter.default.addObserver(
            forName: AVAudioSession.mediaServicesWereResetNotification,
            object: AVAudioSession.sharedInstance(),
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in self?.configureAudioSession() }
        }
    }

    private func handleEnded(_ endedItem: AVPlayerItem?) {
        guard let endedItem,
              let endedID = itemIDs[ObjectIdentifier(endedItem)],
              let endedIndex = queue.firstIndex(where: { $0.id == endedID }) else { return }
        switch repeatMode {
        case .one:
            seek(to: 0)
            player.playImmediately(atRate: speed)
        case .off:
            if endedIndex + 1 >= queue.count {
                player.pause()
                position = duration
            }
        case .all:
            if endedIndex + 1 >= queue.count {
                index = 0
                rebuildCurrent(position: 0, autoplay: true)
            }
        }
    }

    private func configureEndBehavior() {
        player.actionAtItemEnd = repeatMode == .one ? .pause : .advance
        canGoPrevious = index > 0 || position > 0 || repeatMode == .all
        canGoNext = index + 1 < queue.count || repeatMode == .all
        if current != nil, player.currentItem != nil {
            rebuildCurrent(position: position, autoplay: isPlaying)
        }
        updateNowPlayingInfo()
    }

    private func syncCurrentItem(_ playerItem: AVPlayerItem?) {
        guard let playerItem,
              let id = itemIDs[ObjectIdentifier(playerItem)],
              id != current?.id,
              let newIndex = queue.firstIndex(where: { $0.id == id }) else { return }
        index = newIndex
        let record = queue[newIndex]
        current = record
        position = 0
        duration = record.duration.isFinite ? max(0, record.duration) : 0
        canGoPrevious = true
        canGoNext = newIndex + 1 < queue.count || repeatMode == .all
        playbackError = nil
        nowPlayingArtwork = nil
        savePlaybackPosition()
        updateNowPlayingInfo()
        loadNowPlayingArtwork(record)
        appendFollowingItem(after: newIndex)
        prewarmNextAsset()
    }

    private func appendFollowingItem(after currentIndex: Int) {
        guard repeatMode != .one, currentIndex + 1 < queue.count else { return }
        let following = queue[currentIndex + 1]
        let isAlreadyQueued = player.items().contains { itemIDs[ObjectIdentifier($0)] == following.id }
        guard !isAlreadyQueued else { return }
        player.insert(makePlayerItem(following), after: nil)
    }

    private func handleFailure(_ failedItem: AVPlayerItem?, error: Error?) {
        guard let failedItem,
              itemIDs[ObjectIdentifier(failedItem)] == current?.id else { return }
        playbackError = error?.localizedDescription ?? "This media could not be decoded on this iPhone."
        if index + 1 < queue.count {
            index += 1
            rebuildCurrent(position: 0, autoplay: true)
        } else {
            player.pause()
        }
    }

    private func handleInterruption(_ notification: Notification) {
        guard let rawType = notification.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt,
              let type = AVAudioSession.InterruptionType(rawValue: rawType) else { return }
        if type == .began {
            player.pause()
            return
        }
        let rawOptions = notification.userInfo?[AVAudioSessionInterruptionOptionKey] as? UInt ?? 0
        if AVAudioSession.InterruptionOptions(rawValue: rawOptions).contains(.shouldResume) {
            configureAudioSession()
            player.playImmediately(atRate: speed)
        }
    }

    private func configureRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()
        center.playCommand.addTarget { [weak self] _ in
            Task { @MainActor in
                guard let self, !self.isPlaying else { return }
                self.toggle()
            }
            return .success
        }
        center.pauseCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.pause() }
            return .success
        }
        center.togglePlayPauseCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.toggle() }
            return .success
        }
        center.nextTrackCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.next() }
            return .success
        }
        center.previousTrackCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.previous() }
            return .success
        }
        center.changePlaybackPositionCommand.addTarget { [weak self] event in
            guard let event = event as? MPChangePlaybackPositionCommandEvent else { return .commandFailed }
            Task { @MainActor in self?.seek(to: event.positionTime) }
            return .success
        }
    }

    private func loadNowPlayingArtwork(_ record: MediaRecord) {
        let url = LibraryStore.shared.url(for: record)
        Task {
            let image = await ArtworkCache.shared.image(for: record, mediaURL: url)
            guard current?.id == record.id else { return }
            nowPlayingArtwork = image
            updateNowPlayingInfo()
        }
    }

    private func updateNowPlayingInfo() {
        guard let current else {
            MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
            return
        }
        var info: [String: Any] = [
            MPMediaItemPropertyTitle: current.title,
            MPMediaItemPropertyPlaybackDuration: duration,
            MPNowPlayingInfoPropertyElapsedPlaybackTime: position,
            MPNowPlayingInfoPropertyPlaybackRate: isPlaying ? speed : 0,
            MPNowPlayingInfoPropertyDefaultPlaybackRate: speed
        ]
        if let image = nowPlayingArtwork {
            info[MPMediaItemPropertyArtwork] = MPMediaItemArtwork(boundsSize: image.size) { _ in image }
        }
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func savePlaybackPosition() {
        guard let current else { return }
        defaults.set(current.id.uuidString, forKey: Key.lastItemID)
        defaults.set(position, forKey: Key.lastPosition)
    }

    private func prewarmNextAsset() {
        guard index + 1 < queue.count else { return }
        let next = queue[index + 1]
        let url = LibraryStore.shared.url(for: next)
        let asset = assets[next.id] ?? AVURLAsset(url: url)
        assets[next.id] = asset
        Task.detached(priority: .utility) {
            _ = try? await asset.load(.isPlayable)
            _ = try? await asset.load(.duration)
        }
    }
}

enum PlaybackMath {
    static func progress(position: TimeInterval, duration: TimeInterval) -> Double {
        guard position.isFinite, duration.isFinite, duration > 0 else { return 0 }
        return min(max(position / duration, 0), 1)
    }

    static func clock(_ seconds: TimeInterval) -> String {
        guard seconds.isFinite, seconds >= 0 else { return "0:00" }
        let total = Int(seconds.rounded(.down))
        let hours = total / 3600
        let minutes = (total % 3600) / 60
        let remainder = total % 60
        return hours > 0
            ? String(format: "%d:%02d:%02d", hours, minutes, remainder)
            : String(format: "%d:%02d", minutes, remainder)
    }
}
