package com.simplecalculatordeign.ui.scanner

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.simplecalculatordeign.core.math.FormatOptions
import com.simplecalculatordeign.core.math.NumberFormatter
import java.io.File
import java.io.OutputStream
import java.text.DateFormat
import java.util.Date

/** Builds the shareable text and PDF reports for a scan. */
object ScanExport {

    fun buildText(state: ScannerUiState, format: FormatOptions): String = buildString {
        appendLine("Simple Calculator Deign — Scan results")
        appendLine(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date()))
        appendLine()
        state.items.forEachIndexed { i, item ->
            val expr = NumberFormatter.formatExpression(item.expression, format.grouping)
            val r = item.result
            if (r != null) {
                val sign = if (r.approximate) "≈" else "="
                appendLine("${i + 1}. $expr $sign ${NumberFormatter.format(r.value, format, r.approximate)}")
                r.steps.forEachIndexed { s, step -> appendLine("     Step ${s + 1}: ${NumberFormatter.formatStep(step, format)}") }
                if (item.uncertain) appendLine("     ⚠ Please verify: ${item.notes.joinToString("; ")}")
                when (item.writtenAnswerMatches) {
                    true -> appendLine("     ✓ Written answer ${item.writtenAnswer} is correct")
                    false -> appendLine("     ✗ Written answer ${item.writtenAnswer} differs")
                    null -> Unit
                }
            } else {
                appendLine("${i + 1}. $expr — ${item.error ?: "not calculated"}")
            }
        }
        state.total?.let {
            appendLine()
            appendLine("Total of included results: ${if (state.totalApproximate) "≈ " else ""}${NumberFormatter.format(it, format)}")
        }
        state.selectedNumbersSum?.let {
            appendLine("Sum of selected numbers: ${NumberFormatter.format(it, format)}")
        }
    }

    /** Writes an A4 PDF report (with the scanned image) to [out]. Long lines wrap; extra pages are added as needed. */
    fun writePdf(state: ScannerUiState, format: FormatOptions, out: OutputStream) {
        val pageW = 595
        val pageH = 842
        val margin = 40f
        val doc = PdfDocument()
        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create())
        var canvas = page.canvas
        var y = margin

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f; typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(74, 0, 224) }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11.5f; color = Color.rgb(30, 30, 50) }
        val bold = Paint(body).apply { typeface = Typeface.DEFAULT_BOLD; textSize = 12.5f }
        val subtle = Paint(body).apply { color = Color.rgb(110, 110, 130); textSize = 10f }
        val warn = Paint(body).apply { color = Color.rgb(200, 110, 0); textSize = 10.5f }
        val maxWidth = pageW - 2 * margin

        fun newPageIfNeeded(height: Float) {
            if (y + height > pageH - margin) {
                doc.finishPage(page)
                pageNumber++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create())
                canvas = page.canvas
                y = margin
            }
        }

        fun line(text: String, paint: Paint, indent: Float = 0f) {
            var remaining = text
            val width = maxWidth - indent
            while (remaining.isNotEmpty()) {
                val count = paint.breakText(remaining, true, width, null).coerceAtLeast(1)
                var cut = count
                if (count < remaining.length) {
                    val space = remaining.lastIndexOf(' ', count)
                    if (space > count / 2) cut = space + 1
                }
                newPageIfNeeded(paint.textSize + 6f)
                y += paint.textSize + 4f
                canvas.drawText(remaining.substring(0, cut).trimEnd(), margin + indent, y, paint)
                remaining = remaining.substring(cut)
            }
        }

        line("Simple Calculator Deign — Scan report", title)
        line(DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(Date()), subtle)
        y += 10f

        (state.processedBitmap ?: state.sourceBitmap)?.let { bmp ->
            val scale = minOf(maxWidth / bmp.width, 260f / bmp.height)
            val w = bmp.width * scale
            val h = bmp.height * scale
            newPageIfNeeded(h + 10f)
            canvas.drawBitmap(bmp, null, RectF(margin, y, margin + w, y + h), Paint(Paint.FILTER_BITMAP_FLAG))
            y += h + 14f
        }

        line("Recognised calculations", bold)
        y += 4f
        if (state.items.isEmpty()) line("No calculations were recognised.", body)
        state.items.forEachIndexed { i, item ->
            val expr = NumberFormatter.formatExpression(item.expression, format.grouping)
            val r = item.result
            y += 6f
            if (r != null) {
                val sign = if (r.approximate) "≈" else "="
                line("${i + 1}.  $expr  $sign  ${NumberFormatter.format(r.value, format, r.approximate)}", bold)
                r.steps.forEachIndexed { s, step -> line("Step ${s + 1}: ${NumberFormatter.formatStep(step, format)}", body, 18f) }
            } else {
                line("${i + 1}.  $expr  —  ${item.error ?: "not calculated"}", bold)
            }
            line("Read from: \"${item.sourceText}\"", subtle, 18f)
            if (item.uncertain) line("Please verify: ${item.notes.joinToString("; ")}", warn, 18f)
            when (item.writtenAnswerMatches) {
                true -> line("Written answer ${item.writtenAnswer} is correct", subtle, 18f)
                false -> line("Written answer ${item.writtenAnswer} differs from the calculated result", warn, 18f)
                null -> Unit
            }
        }
        state.total?.let {
            y += 10f
            line("Total of included results: ${if (state.totalApproximate) "≈ " else ""}${NumberFormatter.format(it, format)}", bold)
        }
        state.selectedNumbersSum?.let { line("Sum of selected numbers: ${NumberFormatter.format(it, format)}", bold) }
        if (state.ocrText.isNotBlank()) {
            y += 12f
            line("Extracted text", bold)
            state.ocrText.lines().forEach { line(it, subtle) }
        }
        doc.finishPage(page)
        doc.writeTo(out)
        doc.close()
    }

    /** Writes the PDF into the app's private cache and returns a shareable content:// URI. */
    fun createPdfFile(context: Context, state: ScannerUiState, format: FormatOptions): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "scan_report.pdf")
        file.outputStream().use { writePdf(state, format, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareFile(context: Context, uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Scan report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share report"))
    }

    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share results"))
    }
}
