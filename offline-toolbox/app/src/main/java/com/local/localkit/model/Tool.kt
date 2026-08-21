package com.local.localkit.model

enum class ToolSection(val title: String, val subtitle: String) {
    SCAN("Scan", "Camera and recognition"),
    DOCUMENTS("Documents", "Read, assemble and export"),
    MEDIA("Media", "Images, video and audio"),
    FILES("Files", "Inspect, pack and verify"),
    CALCULATE("Calculate", "Numbers, units and dates"),
    POCKET("Pocket tools", "Text, sensors and privacy")
}

enum class ToolId {
    QR_SCANNER,
    QR_GENERATOR,
    DOCUMENT_SCANNER,
    OCR,
    MAGNIFIER_COLOR,
    PDF_READER,
    PDF_ORGANIZER,
    IMAGE_TO_PDF,
    IMAGE_COMPRESSOR,
    IMAGE_CONVERTER,
    METADATA_CLEANER,
    VIDEO_EDITOR,
    AUDIO_EDITOR,
    FILE_BROWSER,
    ARCHIVE,
    STORAGE_ANALYZER,
    DUPLICATE_FINDER,
    HASH_VERIFIER,
    CALCULATOR,
    UNIT_CONVERTER,
    DATE_CALCULATOR,
    TEXT_WORKBENCH,
    PASSWORD_GENERATOR,
    FLASHLIGHT,
    COMPASS_LEVEL
}

data class Tool(
    val id: ToolId,
    val title: String,
    val description: String,
    val section: ToolSection,
    val keywords: Set<String> = emptySet()
)

object ToolRegistry {
    val tools = listOf(
        Tool(ToolId.QR_SCANNER, "QR & barcode scanner", "Read codes safely on-device", ToolSection.SCAN, setOf("camera", "barcode")),
        Tool(ToolId.QR_GENERATOR, "QR generator", "Create QR codes from text, links or Wi-Fi", ToolSection.SCAN, setOf("wifi", "share")),
        Tool(ToolId.DOCUMENT_SCANNER, "Document scanner", "Capture and clean paper documents", ToolSection.SCAN, setOf("camera", "paper")),
        Tool(ToolId.OCR, "Offline OCR", "Extract text from an image", ToolSection.SCAN, setOf("recognize", "copy")),
        Tool(ToolId.MAGNIFIER_COLOR, "Magnifier & color picker", "Zoom in and sample a color", ToolSection.SCAN, setOf("camera", "contrast")),

        Tool(ToolId.PDF_READER, "PDF reader", "Open, zoom and inspect PDF pages", ToolSection.DOCUMENTS, setOf("read", "page")),
        Tool(ToolId.PDF_ORGANIZER, "PDF organizer", "Merge, split, rotate and reorder pages", ToolSection.DOCUMENTS, setOf("merge", "split", "rotate")),
        Tool(ToolId.IMAGE_TO_PDF, "Images to PDF", "Arrange images into one PDF", ToolSection.DOCUMENTS, setOf("photo", "export")),

        Tool(ToolId.IMAGE_COMPRESSOR, "Image compressor", "Resize images and target a smaller file", ToolSection.MEDIA, setOf("resize", "quality")),
        Tool(ToolId.IMAGE_CONVERTER, "Image converter", "Convert PNG, JPEG and WebP", ToolSection.MEDIA, setOf("format", "png", "jpeg", "webp")),
        Tool(ToolId.METADATA_CLEANER, "Metadata cleaner", "Inspect and strip EXIF and location data", ToolSection.MEDIA, setOf("privacy", "gps", "exif")),
        Tool(ToolId.VIDEO_EDITOR, "Video trim & compress", "Trim, rotate or transcode a video", ToolSection.MEDIA, setOf("clip", "size")),
        Tool(ToolId.AUDIO_EDITOR, "Audio trim & convert", "Cut and export an audio clip", ToolSection.MEDIA, setOf("recording", "ringtone")),

        Tool(ToolId.FILE_BROWSER, "File browser", "Open a user-selected file or folder", ToolSection.FILES, setOf("rename", "move", "copy")),
        Tool(ToolId.ARCHIVE, "ZIP archive", "Create or safely extract ZIP files", ToolSection.FILES, setOf("pack", "unpack")),
        Tool(ToolId.STORAGE_ANALYZER, "Storage analyzer", "See what occupies a selected folder", ToolSection.FILES, setOf("large", "space")),
        Tool(ToolId.DUPLICATE_FINDER, "Duplicate finder", "Compare files by content hash", ToolSection.FILES, setOf("same", "clean")),
        Tool(ToolId.HASH_VERIFIER, "Checksum verifier", "Calculate and compare SHA-256", ToolSection.FILES, setOf("hash", "integrity")),

        Tool(ToolId.CALCULATOR, "Calculator", "Basic and scientific calculations", ToolSection.CALCULATE, setOf("math", "percent")),
        Tool(ToolId.UNIT_CONVERTER, "Unit converter", "Convert common physical and data units", ToolSection.CALCULATE, setOf("temperature", "length", "mass")),
        Tool(ToolId.DATE_CALCULATOR, "Date calculator", "Measure or shift dates", ToolSection.CALCULATE, setOf("age", "days", "difference")),

        Tool(ToolId.TEXT_WORKBENCH, "Text workbench", "Count, sort, clean and transform text", ToolSection.POCKET, setOf("case", "dedupe", "replace")),
        Tool(ToolId.PASSWORD_GENERATOR, "Password generator", "Create secrets without saving them", ToolSection.POCKET, setOf("passphrase", "random")),
        Tool(ToolId.FLASHLIGHT, "Flashlight", "Torch, strobe and SOS", ToolSection.POCKET, setOf("light", "torch")),
        Tool(ToolId.COMPASS_LEVEL, "Compass & level", "Direction and surface angle", ToolSection.POCKET, setOf("sensor", "bubble"))
    )

    fun byId(id: ToolId): Tool = tools.first { it.id == id }
}

