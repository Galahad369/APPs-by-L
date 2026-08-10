import XCTest
@testable import GreaterArt

final class PlaybackMathTests: XCTestCase {
    func testProgressUsesRealDurationWithoutStickingAtEnd() {
        XCTAssertEqual(PlaybackMath.progress(position: 9, duration: 209), 9.0 / 209.0, accuracy: 0.000_001)
        XCTAssertEqual(PlaybackMath.progress(position: 999, duration: 209), 1)
    }

    func testInvalidDurationDoesNotProduceNaN() {
        XCTAssertEqual(PlaybackMath.progress(position: 10, duration: 0), 0)
        XCTAssertEqual(PlaybackMath.progress(position: .infinity, duration: 100), 0)
    }

    func testClockFormatting() {
        XCTAssertEqual(PlaybackMath.clock(9), "0:09")
        XCTAssertEqual(PlaybackMath.clock(209), "3:29")
        XCTAssertEqual(PlaybackMath.clock(3_661), "1:01:01")
    }
}

