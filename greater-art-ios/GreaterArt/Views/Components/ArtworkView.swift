import SwiftUI
import UIKit

struct ArtworkView: View {
    let item: MediaRecord
    let mediaURL: URL
    var cornerRadius: CGFloat = 10

    @State private var image: UIImage?

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: item.kind == .video
                            ? [Color(red: 0.08, green: 0.14, blue: 0.15), Color(red: 0.13, green: 0.29, blue: 0.27)]
                            : [Color(red: 0.08, green: 0.10, blue: 0.10), Color(red: 0.18, green: 0.22, blue: 0.20)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else {
                Image(systemName: item.kind == .video ? "play.rectangle.fill" : "music.note")
                    .font(.system(size: 28, weight: .semibold))
                    .foregroundStyle(Color.mint.opacity(0.9))
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        .contentShape(Rectangle())
        .task(id: item.id) {
            image = await ArtworkCache.shared.image(for: item, mediaURL: mediaURL)
        }
    }
}
