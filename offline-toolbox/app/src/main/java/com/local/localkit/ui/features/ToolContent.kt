package com.local.localkit.ui.features

import androidx.compose.runtime.Composable
import com.local.localkit.model.ToolId

@Composable
fun ToolContent(id: ToolId) {
    when (id) {
        ToolId.QR_SCANNER -> QrScannerScreen()
        ToolId.QR_GENERATOR -> QrGeneratorScreen()
        ToolId.DOCUMENT_SCANNER -> DocumentScannerScreen()
        ToolId.OCR -> OcrScreen()
        ToolId.MAGNIFIER_COLOR -> MagnifierColorScreen()
        ToolId.PDF_READER -> PdfReaderScreen()
        ToolId.PDF_ORGANIZER -> PdfOrganizerScreen()
        ToolId.IMAGE_TO_PDF -> ImagesToPdfScreen()
        ToolId.IMAGE_COMPRESSOR -> ImageCompressorScreen()
        ToolId.IMAGE_CONVERTER -> ImageConverterScreen()
        ToolId.METADATA_CLEANER -> MetadataCleanerScreen()
        ToolId.VIDEO_EDITOR -> VideoEditorScreen()
        ToolId.AUDIO_EDITOR -> AudioEditorScreen()
        ToolId.FILE_BROWSER -> FileBrowserScreen()
        ToolId.ARCHIVE -> ArchiveScreen()
        ToolId.STORAGE_ANALYZER -> StorageAnalyzerScreen()
        ToolId.DUPLICATE_FINDER -> DuplicateFinderScreen()
        ToolId.HASH_VERIFIER -> HashVerifierScreen()
        ToolId.CALCULATOR -> CalculatorScreen()
        ToolId.UNIT_CONVERTER -> UnitConverterScreen()
        ToolId.DATE_CALCULATOR -> DateCalculatorScreen()
        ToolId.TEXT_WORKBENCH -> TextWorkbenchScreen()
        ToolId.PASSWORD_GENERATOR -> PasswordGeneratorScreen()
        ToolId.FLASHLIGHT -> FlashlightScreen()
        ToolId.COMPASS_LEVEL -> CompassLevelScreen()
    }
}
