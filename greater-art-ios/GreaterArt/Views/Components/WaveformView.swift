import SwiftUI

struct WaveformView: View {
    let progress: Double
    private let bars: [CGFloat] = [
        .28, .42, .66, .38, .52, .82, .58, .34, .47, .73, .92, .61,
        .40, .55, .76, .46, .30, .57, .88, .64, .43, .69, .50, .79,
        .35, .60, .95, .71, .45, .54, .84, .48, .32, .62, .78, .56
    ]

    var body: some View {
        GeometryReader { geometry in
            let width = geometry.size.width
            HStack(alignment: .center, spacing: 2) {
                ForEach(Array(bars.enumerated()), id: \.offset) { index, height in
                    let reached = Double(index) / Double(max(bars.count - 1, 1)) <= progress
                    Capsule()
                        .fill(reached ? Color.mint : Color.secondary.opacity(0.35))
                        .frame(maxWidth: .infinity)
                        .frame(height: max(4, geometry.size.height * height))
                }
            }
            .frame(width: width, height: geometry.size.height)
        }
        .accessibilityHidden(true)
    }
}

