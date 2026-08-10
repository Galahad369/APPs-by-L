import XCTest
@testable import GreaterArt

final class LibraryOrderingTests: XCTestCase {
    private let alpha = MediaRecord(
        id: UUID(uuidString: "00000000-0000-0000-0000-000000000001")!,
        storedFilename: "alpha.mp3",
        displayName: "Alpha.mp3",
        fileSize: 1,
        duration: 1,
        kind: .audio,
        importedAt: .distantPast
    )
    private let beta = MediaRecord(
        id: UUID(uuidString: "00000000-0000-0000-0000-000000000002")!,
        storedFilename: "beta.mp3",
        displayName: "beta.mp3",
        fileSize: 1,
        duration: 1,
        kind: .audio,
        importedAt: .distantPast
    )

    func testNameSorting() {
        XCTAssertEqual(LibraryStore.sorted([beta, alpha], by: .nameAscending, customOrder: []).map(\.id), [alpha.id, beta.id])
        XCTAssertEqual(LibraryStore.sorted([alpha, beta], by: .nameDescending, customOrder: []).map(\.id), [beta.id, alpha.id])
    }

    func testCustomSortingPersistsIDs() {
        XCTAssertEqual(
            LibraryStore.sorted([alpha, beta], by: .custom, customOrder: [beta.id, alpha.id]).map(\.id),
            [beta.id, alpha.id]
        )
    }
}
