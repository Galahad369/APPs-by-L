import AVFoundation
import Foundation
import UIKit

actor ArtworkCache {
    static let shared = ArtworkCache()

    private let memory = NSCache<NSString, UIImage>()
    private let directory: URL
    private let fileManager = FileManager.default
    private let pixelSize = CGSize(width: 720, height: 720)

    private init() {
        let caches = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first!
        directory = caches.appendingPathComponent("GreaterArtArtwork-v1", isDirectory: true)
        try? fileManager.createDirectory(at: directory, withIntermediateDirectories: true)
        memory.totalCostLimit = 96 * 1_024 * 1_024
        memory.countLimit = 160
    }

    func image(for item: MediaRecord, mediaURL: URL) async -> UIImage? {
        let key = item.id.uuidString as NSString
        if let cached = memory.object(forKey: key) { return cached }

        let diskURL = cacheURL(for: item.id)
        if let data = try? Data(contentsOf: diskURL), let image = UIImage(data: data) {
            memory.setObject(image, forKey: key, cost: imageCost(image))
            return image
        }

        let raw: UIImage?
        switch item.kind {
        case .audio:
            raw = await embeddedArtwork(from: mediaURL)
        case .video:
            raw = await videoFrame(from: mediaURL, duration: item.duration)
                ?? embeddedArtwork(from: mediaURL)
        }
        guard let raw, let prepared = squareCrop(raw, size: pixelSize) else { return nil }
        if let data = prepared.jpegData(compressionQuality: 0.88) {
            try? data.write(to: diskURL, options: .atomic)
        }
        memory.setObject(prepared, forKey: key, cost: imageCost(prepared))
        return prepared
    }

    func preheat(_ items: [MediaRecord], mediaDirectory: URL) async {
        for item in items {
            if Task.isCancelled { return }
            _ = await image(for: item, mediaURL: mediaDirectory.appendingPathComponent(item.storedFilename))
        }
    }

    func remove(itemID: UUID) {
        memory.removeObject(forKey: itemID.uuidString as NSString)
        try? fileManager.removeItem(at: cacheURL(for: itemID))
    }

    func clear() {
        memory.removeAllObjects()
        try? fileManager.removeItem(at: directory)
        try? fileManager.createDirectory(at: directory, withIntermediateDirectories: true)
    }

    private func embeddedArtwork(from url: URL) async -> UIImage? {
        let asset = AVURLAsset(url: url)
        guard let metadata = try? await asset.load(.commonMetadata) else { return nil }
        for item in metadata where item.commonKey == .commonKeyArtwork {
            if let data = try? await item.load(.dataValue), let image = UIImage(data: data) {
                return image
            }
        }
        return nil
    }

    private func videoFrame(from url: URL, duration: TimeInterval) async -> UIImage? {
        let asset = AVURLAsset(url: url)
        let generator = AVAssetImageGenerator(asset: asset)
        generator.appliesPreferredTrackTransform = true
        generator.maximumSize = pixelSize
        generator.requestedTimeToleranceBefore = CMTime(seconds: 0.35, preferredTimescale: 600)
        generator.requestedTimeToleranceAfter = CMTime(seconds: 0.35, preferredTimescale: 600)

        let candidateSeconds = [
            duration.isFinite && duration > 2 ? min(max(duration * 0.12, 1), 12) : 1,
            0.25,
            0
        ]
        for seconds in candidateSeconds {
            let time = CMTime(seconds: seconds, preferredTimescale: 600)
            if let cgImage = try? generator.copyCGImage(at: time, actualTime: nil) {
                return UIImage(cgImage: cgImage)
            }
        }
        return nil
    }

    private func squareCrop(_ image: UIImage, size: CGSize) -> UIImage? {
        guard image.size.width > 0, image.size.height > 0 else { return nil }
        let scale = max(size.width / image.size.width, size.height / image.size.height)
        let drawnSize = CGSize(width: image.size.width * scale, height: image.size.height * scale)
        let origin = CGPoint(x: (size.width - drawnSize.width) / 2, y: (size.height - drawnSize.height) / 2)
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { _ in
            image.draw(in: CGRect(origin: origin, size: drawnSize))
        }
    }

    private func cacheURL(for id: UUID) -> URL {
        directory.appendingPathComponent(id.uuidString).appendingPathExtension("jpg")
    }

    private func imageCost(_ image: UIImage) -> Int {
        Int(image.size.width * image.size.height * image.scale * image.scale * 4)
    }
}
