package com.simplecalculatordeign.ui.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.simplecalculatordeign.core.math.OcrLine
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.roundToInt

/** Maximum edge length kept in memory; plenty for OCR and avoids out-of-memory on big photos. */
const val MAX_IMAGE_EDGE = 2048

data class OcrOutput(val fullText: String, val lines: List<OcrLine>)

/** On-device text recognition with the bundled Google ML Kit Latin model (works offline). */
class OcrService {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): OcrOutput = suspendCancellableCoroutine { cont ->
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { text -> if (cont.isActive) cont.resume(convert(text)) }
            .addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
    }

    private fun convert(text: Text): OcrOutput {
        // Sort lines top-to-bottom, then left-to-right, so rows of a worksheet stay in order.
        val lines = text.textBlocks.flatMap { it.lines }
            .sortedWith(compareBy({ (it.boundingBox?.centerY() ?: 0) / 24 }, { it.boundingBox?.left ?: 0 }))
            .map { OcrLine(it.text, it.confidence) }
        return OcrOutput(text.text, lines)
    }

    fun close() = recognizer.close()
}

object ImageUtils {

    fun downscale(bitmap: Bitmap, maxEdge: Int = MAX_IMAGE_EDGE): Bitmap {
        val edge = max(bitmap.width, bitmap.height)
        if (edge <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / edge
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).roundToInt(), (bitmap.height * scale).roundToInt(), true)
    }

    fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return bitmap
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    /** Converts a CameraX capture into an upright, memory-friendly bitmap. */
    fun fromImageProxy(image: ImageProxy): Bitmap {
        val raw = image.toBitmap()
        val rotated = rotate(raw, image.imageInfo.rotationDegrees)
        return downscale(rotated)
    }

    /** Loads a gallery image respecting EXIF orientation, downsampled to [MAX_IMAGE_EDGE]. */
    fun loadFromUri(context: Context, uri: Uri): Bitmap {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val edge = max(info.size.width, info.size.height)
                if (edge > MAX_IMAGE_EDGE) {
                    val scale = MAX_IMAGE_EDGE.toFloat() / edge
                    decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
                }
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_IMAGE_EDGE) sample *= 2
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IllegalArgumentException("Could not open the image")
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        return downscale(rotate(bitmap, degrees))
    }

    /**
     * 4×5 colour matrix (row-major, Android/Compose layout) for brightness [-100, 100],
     * contrast [0.5, 2] and optional grayscale (helps OCR on coloured paper).
     */
    fun colorMatrix(brightness: Float, contrast: Float, grayscale: Boolean): FloatArray {
        val c = contrast
        val t = (1f - c) * 128f + brightness * 1.28f
        if (!grayscale) {
            return floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f,
            )
        }
        val r = 0.299f * c
        val g = 0.587f * c
        val b = 0.114f * c
        return floatArrayOf(
            r, g, b, 0f, t,
            r, g, b, 0f, t,
            r, g, b, 0f, t,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    /** Applies a normalised crop rectangle and colour adjustments, producing a new bitmap. */
    fun applyEdits(
        source: Bitmap,
        crop: CropRect,
        brightness: Float,
        contrast: Float,
        grayscale: Boolean,
    ): Bitmap {
        val left = (crop.left * source.width).roundToInt().coerceIn(0, source.width - 1)
        val top = (crop.top * source.height).roundToInt().coerceIn(0, source.height - 1)
        val right = (crop.right * source.width).roundToInt().coerceIn(left + 1, source.width)
        val bottom = (crop.bottom * source.height).roundToInt().coerceIn(top + 1, source.height)
        val cropped = Bitmap.createBitmap(source, left, top, right - left, bottom - top)
        val neutral = brightness == 0f && contrast == 1f && !grayscale
        if (neutral) return cropped
        val out = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(android.graphics.ColorMatrix(colorMatrix(brightness, contrast, grayscale)))
        }
        Canvas(out).drawBitmap(cropped, 0f, 0f, paint)
        return out
    }
}

/** Crop rectangle in normalised [0, 1] image coordinates. */
data class CropRect(val left: Float = 0f, val top: Float = 0f, val right: Float = 1f, val bottom: Float = 1f) {
    val width get() = right - left
    val height get() = bottom - top
}
