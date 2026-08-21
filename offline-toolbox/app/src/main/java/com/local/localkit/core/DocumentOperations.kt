package com.local.localkit.core

import android.content.ContentResolver
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.FileNotFoundException

data class PdfPageSpec(val source: Uri, val sourcePage: Int, val label: String, val rotation: Int = 0)

object DocumentOperations {
    fun loadBitmap(resolver: ContentResolver, uri: Uri, maxSide: Int = 4096): Bitmap {
        val decoded = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val max = maxOf(info.size.width, info.size.height)
                if (max > maxSide) {
                    val scale = maxSide.toFloat() / max
                    decoder.setTargetSize((info.size.width * scale).roundPositive(), (info.size.height * scale).roundPositive())
                }
            }
        } else {
            @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(resolver, uri)
        }
        return decoded.copy(Bitmap.Config.ARGB_8888, false)
    }

    fun writeBitmap(resolver: ContentResolver, output: Uri, bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
        resolver.openOutputStream(output, "w")?.use { stream ->
            require(bitmap.compress(format, quality.coerceIn(1, 100), stream)) { "Encoder rejected image" }
        } ?: throw FileNotFoundException("Unable to open output")
    }

    fun scaled(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val height = (bitmap.height * (maxWidth.toFloat() / bitmap.width)).roundPositive()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, height, true)
    }

    fun imagesToPdf(resolver: ContentResolver, images: List<Uri>, output: Uri) {
        val document = PdfDocument()
        try {
            images.forEachIndexed { index, uri ->
                val bitmap = loadBitmap(resolver, uri, 2400)
                val landscape = bitmap.width > bitmap.height
                val pageWidth = if (landscape) 1754 else 1240
                val pageHeight = if (landscape) 1240 else 1754
                val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = document.startPage(info)
                page.canvas.drawColor(Color.WHITE)
                drawFit(page.canvas, bitmap, RectF(48f, 48f, pageWidth - 48f, pageHeight - 48f))
                document.finishPage(page)
                bitmap.recycle()
            }
            resolver.openOutputStream(output, "w")?.use(document::writeTo) ?: error("Unable to create PDF")
        } finally { document.close() }
    }

    fun bitmapToPdf(resolver: ContentResolver, bitmap: Bitmap, output: Uri) {
        val document = PdfDocument()
        try {
            val landscape = bitmap.width > bitmap.height
            val pageWidth = if (landscape) 1754 else 1240
            val pageHeight = if (landscape) 1240 else 1754
            val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            page.canvas.drawColor(Color.WHITE)
            drawFit(page.canvas, bitmap, RectF(48f, 48f, pageWidth - 48f, pageHeight - 48f))
            document.finishPage(page)
            resolver.openOutputStream(output, "w")?.use(document::writeTo) ?: error("Unable to create PDF")
        } finally { document.close() }
    }

    fun enhanceDocument(source: Bitmap, grayscale: Boolean, contrast: Float): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val saturation = if (grayscale) 0f else 1f
        val scale = contrast.coerceIn(.5f, 2f)
        val translate = (-.5f * scale + .5f) * 255f
        val matrix = ColorMatrix().apply {
            setSaturation(saturation)
            postConcat(ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        Canvas(result).drawBitmap(source, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix) })
        return result
    }

    fun pageCount(resolver: ContentResolver, uri: Uri): Int {
        resolver.openFileDescriptor(uri, "r")?.use { descriptor -> PdfRenderer(descriptor).use { return it.pageCount } }
        return 0
    }

    fun renderPdfPage(resolver: ContentResolver, uri: Uri, pageIndex: Int, maxWidth: Int = 1600, rotation: Int = 0): Bitmap {
        resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                renderer.openPage(pageIndex.coerceIn(0, renderer.pageCount - 1)).use { page ->
                    val scale = (maxWidth.toFloat() / page.width).coerceAtMost(2.5f)
                    val bitmap = Bitmap.createBitmap((page.width * scale).roundPositive(), (page.height * scale).roundPositive(), Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    if (rotation % 360 == 0) return bitmap
                    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also { if (it !== bitmap) bitmap.recycle() }
                }
            }
        }
        error("Unable to open PDF")
    }

    /**
     * Compatibility-first organizer: pages are rendered and written into a new PDF.
     * This works without a cloud service but rasterizes searchable text.
     */
    fun organizePdf(resolver: ContentResolver, pages: List<PdfPageSpec>, output: Uri) {
        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, spec ->
                val bitmap = renderPdfPage(resolver, spec.source, spec.sourcePage, 2000, spec.rotation)
                val info = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = document.startPage(info)
                page.canvas.drawColor(Color.WHITE)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                document.finishPage(page)
                bitmap.recycle()
            }
            resolver.openOutputStream(output, "w")?.use(document::writeTo) ?: error("Unable to create PDF")
        } finally { document.close() }
    }

    fun exifSummary(resolver: ContentResolver, uri: Uri): Map<String, String> {
        val tags = listOf(
            ExifInterface.TAG_MAKE to "Camera maker",
            ExifInterface.TAG_MODEL to "Camera model",
            ExifInterface.TAG_DATETIME_ORIGINAL to "Captured",
            ExifInterface.TAG_IMAGE_WIDTH to "Width",
            ExifInterface.TAG_IMAGE_LENGTH to "Height",
            ExifInterface.TAG_GPS_LATITUDE to "GPS latitude",
            ExifInterface.TAG_GPS_LONGITUDE to "GPS longitude",
            ExifInterface.TAG_SOFTWARE to "Software",
            ExifInterface.TAG_ARTIST to "Artist",
            ExifInterface.TAG_COPYRIGHT to "Copyright"
        )
        return resolver.openInputStream(uri)?.use { input ->
            val exif = ExifInterface(input)
            buildMap { tags.forEach { (tag, label) -> exif.getAttribute(tag)?.let { put(label, it) } } }
        }.orEmpty()
    }

    private fun drawFit(canvas: Canvas, bitmap: Bitmap, bounds: RectF) {
        val scale = minOf(bounds.width() / bitmap.width, bounds.height() / bitmap.height)
        val width = bitmap.width * scale
        val height = bitmap.height * scale
        val target = RectF(bounds.centerX() - width / 2, bounds.centerY() - height / 2, bounds.centerX() + width / 2, bounds.centerY() + height / 2)
        canvas.drawBitmap(bitmap, null, target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    private fun Float.roundPositive(): Int = toInt().coerceAtLeast(1)
}
