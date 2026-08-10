import AVFoundation
import AVKit
import SwiftUI

final class PlayerLayerContainerView: UIView {
    override static var layerClass: AnyClass { AVPlayerLayer.self }
    var playerLayer: AVPlayerLayer { layer as! AVPlayerLayer }
}

struct VideoPlayerSurface: UIViewRepresentable {
    let player: AVPlayer
    var pictureInPictureRequest: Int
    var automaticPictureInPicture: Bool

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> PlayerLayerContainerView {
        let view = PlayerLayerContainerView()
        view.backgroundColor = .black
        view.playerLayer.player = player
        view.playerLayer.videoGravity = .resizeAspect
        if AVPictureInPictureController.isPictureInPictureSupported() {
            let controller = AVPictureInPictureController(playerLayer: view.playerLayer)
            controller.canStartPictureInPictureAutomaticallyFromInline = automaticPictureInPicture
            context.coordinator.pictureInPictureController = controller
        }
        return view
    }

    func updateUIView(_ view: PlayerLayerContainerView, context: Context) {
        view.playerLayer.player = player
        context.coordinator.pictureInPictureController?.canStartPictureInPictureAutomaticallyFromInline = automaticPictureInPicture
        if context.coordinator.lastRequest != pictureInPictureRequest {
            context.coordinator.lastRequest = pictureInPictureRequest
            let controller = context.coordinator.pictureInPictureController
            if controller?.isPictureInPictureActive == true {
                controller?.stopPictureInPicture()
            } else if controller?.isPictureInPicturePossible == true {
                controller?.startPictureInPicture()
            }
        }
    }

    final class Coordinator: NSObject {
        var pictureInPictureController: AVPictureInPictureController?
        var lastRequest = 0
    }
}

