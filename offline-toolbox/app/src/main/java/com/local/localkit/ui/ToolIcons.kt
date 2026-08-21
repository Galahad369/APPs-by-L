package com.local.localkit.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.local.localkit.model.ToolId

fun iconFor(id: ToolId): ImageVector = when (id) {
    ToolId.QR_SCANNER -> Icons.Outlined.QrCodeScanner
    ToolId.QR_GENERATOR -> Icons.Outlined.QrCode2
    ToolId.DOCUMENT_SCANNER -> Icons.Outlined.DocumentScanner
    ToolId.OCR -> Icons.Outlined.TextFields
    ToolId.MAGNIFIER_COLOR -> Icons.Outlined.Colorize
    ToolId.PDF_READER -> Icons.Outlined.PictureAsPdf
    ToolId.PDF_ORGANIZER -> Icons.Outlined.Reorder
    ToolId.IMAGE_TO_PDF -> Icons.Outlined.Collections
    ToolId.IMAGE_COMPRESSOR -> Icons.Outlined.Compress
    ToolId.IMAGE_CONVERTER -> Icons.Outlined.Transform
    ToolId.METADATA_CLEANER -> Icons.Outlined.PrivacyTip
    ToolId.VIDEO_EDITOR -> Icons.Outlined.VideoFile
    ToolId.AUDIO_EDITOR -> Icons.Outlined.AudioFile
    ToolId.FILE_BROWSER -> Icons.Outlined.FolderOpen
    ToolId.ARCHIVE -> Icons.Outlined.FolderZip
    ToolId.STORAGE_ANALYZER -> Icons.Outlined.Storage
    ToolId.DUPLICATE_FINDER -> Icons.Outlined.ContentCopy
    ToolId.HASH_VERIFIER -> Icons.Outlined.Tag
    ToolId.CALCULATOR -> Icons.Outlined.Calculate
    ToolId.UNIT_CONVERTER -> Icons.Outlined.Straighten
    ToolId.DATE_CALCULATOR -> Icons.Outlined.DateRange
    ToolId.TEXT_WORKBENCH -> Icons.AutoMirrored.Outlined.TextSnippet
    ToolId.PASSWORD_GENERATOR -> Icons.Outlined.Password
    ToolId.FLASHLIGHT -> Icons.Outlined.FlashlightOn
    ToolId.COMPASS_LEVEL -> Icons.Outlined.Explore
}
